#!/usr/bin/env python3
import hashlib
import importlib.util
import json
from pathlib import Path
import struct
import tempfile
import unittest
from zipfile import ZipFile
import zlib


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).parent/filename)
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    return module


repair_module = load('repair_graphics', 'repair-official-graphics.py')
audit_module = load('audit_graphics', 'audit-runtime-graphics.py')
overlay_module = load('overlay_graphics', 'prepare-pages-overlay.py')


def png(size, rgba):
    def chunk(name, data):
        body = name + data
        return struct.pack('>I',len(data))+body+struct.pack('>I',zlib.crc32(body))
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR',struct.pack('>IIBBBBB',size,size,8,6,0,0,0))
        + chunk(b'IDAT',zlib.compress((b'\0'+bytes(rgba)*size)*size)) + chunk(b'IEND',b''))


class GraphicsAuditTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(); self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)/'runtime'; (self.root/'graphics/ships').mkdir(parents=True)
        self.archive = Path(self.temp.name)/'stock.zip'; self.manifest = Path(self.temp.name)/'repairs.json'
        self.old = png(1,[255,255,255,0]); self.official = png(2,[100,110,120,255])
        self.name = 'graphics/ships/test.png'; (self.root/self.name).write_bytes(self.old)
        with ZipFile(self.archive,'w') as archive: archive.writestr('release/starsector-core/'+self.name,self.official)
        self.row = {'path': self.name, 'expectedLocalSha256': hashlib.sha256(self.old).hexdigest(),
            'officialSha256': hashlib.sha256(self.official).hexdigest(), 'reason': 'fixture'}
        self.save_manifest([self.row])

    def save_manifest(self, rows):
        self.manifest.write_text(json.dumps({'schemaVersion':1,'release':'test','images':rows}))

    def run_repair(self, **kwargs):
        return repair_module.repair(self.root,self.archive,self.manifest,**kwargs)

    def test_exact_repair_then_idempotent(self):
        self.assertEqual(self.run_repair()['repaired'],1)
        self.assertEqual((self.root/self.name).read_bytes(),self.official)
        self.assertEqual(self.run_repair()['alreadyOfficial'],1)
        self.assertEqual(audit_module.audit(self.root,self.archive)['allOfficialImagesByteExact'],True)

    def test_dry_run_preserves_original(self):
        result=self.run_repair(dry_run=True)
        self.assertEqual(result['plannedRepairs'],1)
        self.assertEqual(result['repaired'],0)
        self.assertEqual((self.root/self.name).read_bytes(),self.old)

    def test_unreviewed_local_edit_is_preserved(self):
        edited = png(2,[200,110,120,255]); (self.root/self.name).write_bytes(edited)
        with self.assertRaisesRegex(ValueError,'Unreviewed'): self.run_repair()
        self.assertEqual((self.root/self.name).read_bytes(),edited)

    def test_official_hash_mismatch_is_rejected(self):
        self.save_manifest([{**self.row,'officialSha256':'0'*64}])
        with self.assertRaisesRegex(ValueError,'Official image hash mismatch'): self.run_repair()
        self.assertEqual((self.root/self.name).read_bytes(),self.old)

    def test_entire_plan_validated_before_replacing_first_image(self):
        second = 'graphics/ships/second.png'; (self.root/second).write_bytes(self.old)
        with ZipFile(self.archive,'a') as archive: archive.writestr('release/starsector-core/'+second,self.official)
        self.save_manifest([self.row,{**self.row,'path':second,'officialSha256':'0'*64}])
        with self.assertRaises(ValueError): self.run_repair()
        self.assertEqual((self.root/self.name).read_bytes(),self.old)

    def test_duplicate_manifest_rejected(self):
        self.save_manifest([self.row,self.row])
        with self.assertRaisesRegex(ValueError,'Duplicate repair'): self.run_repair()
        self.assertEqual((self.root/self.name).read_bytes(),self.old)

    def test_unsafe_path_rejected(self):
        for name in ['../x.png','/graphics/x.png','graphics/../x.png','graphics/C:/x.png','graphics//x.png','graphics\\x.png']:
            with self.subTest(name=name), self.assertRaises(ValueError): repair_module.relative_graphic(name)

    def test_missing_image_remains_missing_for_restore_phase(self):
        (self.root/self.name).unlink(); result = self.run_repair()
        self.assertEqual(result['missing'],1); self.assertFalse((self.root/self.name).exists())
        self.assertFalse(audit_module.audit(self.root,self.archive)['completeOfficialImageSet'])

    def test_invalid_pointer_detected(self):
        (self.root/self.name).write_bytes(audit_module.LFS_POINTER+b'\n')
        report = audit_module.audit(self.root,self.archive)
        self.assertEqual(report['invalidImages'],[self.name]); self.assertFalse(report['completeOfficialImageSet'])

    def test_different_image_is_not_claimed_byte_exact(self):
        report = audit_module.audit(self.root,self.archive)
        self.assertTrue(report['completeOfficialImageSet']); self.assertFalse(report['allOfficialImagesByteExact'])
        self.assertEqual(len(report['different']),1)

    def test_case_mismatch_detected_on_windows_too(self):
        source=self.root/self.name; source.rename(source.with_name('TEST.png'))
        report = audit_module.audit(self.root,self.archive)
        self.assertEqual(report['missing'],[self.name]); self.assertEqual(len(report['caseMismatches']),1)

    def test_browser_extra_alias_is_not_deleted_or_failed(self):
        alias=self.root/'graphics/alias.png'; alias.write_bytes(self.old); self.run_repair()
        report=audit_module.audit(self.root,self.archive)
        self.assertTrue(report['allOfficialImagesByteExact']); self.assertEqual(report['extraImages'],['graphics/alias.png'])
        self.assertEqual(alias.read_bytes(),self.old)

    def test_overlay_promotes_unchanged_graphics_not_only_git_diff(self):
        root=Path(self.temp.name)/'repo'
        art=root/'starsector/starsector/graphics/ui/launcher_bg.jpg'
        art.parent.mkdir(parents=True);art.write_bytes(self.official)
        self.assertEqual(overlay_module.graphics_overlay_paths(root),{'starsector/starsector/graphics/ui/launcher_bg.jpg'})

    def test_overlay_missing_graphics_fails(self):
        with self.assertRaisesRegex(RuntimeError,'missing'):
            overlay_module.graphics_overlay_paths(Path(self.temp.name)/'empty')

    def test_valid_image_content_with_different_suffix_is_not_corruption(self):
        self.assertTrue(audit_module.image_signature_ok('graphics/valid.jpg',self.official))

    def test_reference_miss_classified_separately(self):
        (self.root/'data').mkdir(); (self.root/'data/fixture.json').write_text('{"sprite":"graphics/not_in_official.png"}')
        report = audit_module.audit(self.root,self.archive)
        self.assertEqual(report['referenceMisses'][0]['existsInOfficialRelease'],False)
        self.assertTrue(report['completeOfficialImageSet'])


if __name__=='__main__': unittest.main()
