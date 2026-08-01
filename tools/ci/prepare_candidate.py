#!/usr/bin/env python3
import argparse
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile


def run(cmd, cwd=None):
    print('+', ' '.join(str(x) for x in cmd), flush=True)
    subprocess.run(cmd, cwd=cwd, check=True)


def replace_exact(text, old, new, label):
    if old not in text:
        raise RuntimeError(f'expected {label} block was not found')
    return text.replace(old, new, 1)


def classpath_names(root: Path):
    names = []
    for line in (root / 'jars' / 'index.list').read_text(encoding='utf-8-sig').splitlines():
        line = line.strip()
        if line:
            name = line.split()[0]
            if name not in ('fixer-runtime.jar', 'basegame-runtime.jar'):
                names.append(name)
    return names


def patch_resource_indices(root: Path):
    """Repair static resource manifests for CheerpJ's HTTP-backed filesystem.

    Many generated index.list files were committed with a UTF-8 BOM. The
    browser VFS treats that BOM as part of the *first child name*, producing
    requests such as `%EF%BB%BFproj/index.list` and `%EF%BB%BFlasher_Assault.variant`.
    That silently drops the first file/directory from every affected resource
    directory and is fatal for projectile/weapon spec loading.
    """
    bom = b'\xef\xbb\xbf'
    stripped = []
    for path in root.rglob('index.list'):
        try:
            data = path.read_bytes()
        except OSError:
            continue
        if data.startswith(bom):
            path.write_bytes(data[len(bom):])
            stripped.append(path)
    print(f'prepare_candidate: stripped UTF-8 BOM from {len(stripped)} index.list files')
    if stripped:
        for path in stripped[:12]:
            print('prepare_candidate: BOM repaired', path.relative_to(root))
        if len(stripped) > 12:
            print(f'prepare_candidate: ... plus {len(stripped) - 12} more BOM repairs')

    # Some legacy Starsector code asks for graphics/particlealpha32sq.png,
    # while the shipped asset is under graphics/fx/. Keep a real static alias
    # so the resource manager does not fail during early campaign/variant init.
    src = root / 'starsector' / 'starsector' / 'graphics' / 'fx' / 'particlealpha32sq.png'
    dst = root / 'starsector' / 'starsector' / 'graphics' / 'particlealpha32sq.png'
    if src.exists():
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)
        print('prepare_candidate: restored legacy particle alias', dst.relative_to(root))
    else:
        raise RuntimeError(f'missing particle alias source: {src}')


def patch_fixer(root: Path):
    fixer = root / 'jars' / 'Fixer.java'
    text = fixer.read_text(encoding='utf-8-sig')

    old_driver = '''                    if (!didCampaignTransitionAdvance(ctx, resolvedTitleState, beforeState)) {
                        if (forceCampaignStateActivation(ctx, resolvedTitleState, beforeState)) {
                            System.out.println(
                                    "Fixer: Campaign State transition fallback succeeded via direct state activation after driver.goToState no-advance.");
                            return true;
                        }
                        System.out.println(
                                "Fixer: requested Campaign State via driver.goToState; awaiting asynchronous state advance.");
                        return true;
                    }'''
    new_driver = '''                    if (!didCampaignTransitionAdvance(ctx, resolvedTitleState, beforeState)) {
                        System.out.println(
                                "Fixer: requested Campaign State via driver.goToState; allowing AppDriver main loop to complete the transition.");
                        return true;
                    }'''
    text = replace_exact(text, old_driver, new_driver, 'driver transition')

    old_title = '''            if (!didCampaignTransitionAdvance(ctx, resolvedTitleState, beforeState)) {
                if (forceCampaignStateActivation(ctx, resolvedTitleState, beforeState)) {
                    System.out.println(
                            "Fixer: Campaign State transition fallback succeeded via direct state activation after titleState.goToState no-advance.");
                    return true;
                }
                System.out.println(
                        "Fixer: requested Campaign State via titleState.goToState; awaiting asynchronous state advance.");
                return true;
            }'''
    new_title = '''            if (!didCampaignTransitionAdvance(ctx, resolvedTitleState, beforeState)) {
                System.out.println(
                        "Fixer: requested Campaign State via titleState.goToState; allowing AppDriver main loop to complete the transition.");
                return true;
            }'''
    text = replace_exact(text, old_title, new_title, 'title transition')

    old_mirror = '''        initializeResourceManager();
        mirrorCoreSpecDirectoriesToFiles();
        installUncaughtExceptionLogging();'''
    new_mirror = '''        initializeResourceManager();
        if (Boolean.parseBoolean(System.getProperty("starsector.mirrorCoreSpecs", "false"))) {
            mirrorCoreSpecDirectoriesToFiles();
        } else {
            System.out.println("Fixer: skipping redundant core spec mirror; using /app resource manager directly.");
        }
        installUncaughtExceptionLogging();'''
    text = replace_exact(text, old_mirror, new_mirror, 'core spec mirror startup')
    fixer.write_text(text, encoding='utf-8')

    cp = os.pathsep.join(str(root / 'jars' / name) for name in classpath_names(root))
    out = root / 'build' / 'ci' / 'fixer'
    shutil.rmtree(out, ignore_errors=True)
    out.mkdir(parents=True, exist_ok=True)
    run(['javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8', '-cp', cp, '-d', str(out), str(fixer)])

    classes = sorted(out.glob('Fixer*.class'))
    if not classes:
        raise RuntimeError('Fixer compilation produced no classes')
    runtime_jar = root / 'jars' / 'fixer-runtime.jar'
    runtime_jar.unlink(missing_ok=True)
    run(['jar', 'cf', str(runtime_jar), '-C', str(out), '.'])
    print(f'prepare_candidate: built fixer-runtime.jar with {len(classes)} Fixer classes')


def patch_base_game_state(root: Path):
    target = 'com/fs/starfarer/BaseGameState.class'
    ordered = [root / 'jars' / 'starfarer.api.jar', root / 'jars' / 'starfarer_obf.jar']
    containing = []
    for jar_path in ordered:
        with zipfile.ZipFile(jar_path, 'r') as zf:
            if target in zf.namelist():
                containing.append(jar_path)
    if not containing:
        raise RuntimeError('BaseGameState.class was not found in starfarer jars')
    print('prepare_candidate: BaseGameState providers in classpath order:', ', '.join(p.name for p in containing))
    provider = containing[0]
    print('prepare_candidate: reading runtime provider', provider.name)

    build = root / 'build' / 'ci' / 'basegame'
    shutil.rmtree(build, ignore_errors=True)
    build.mkdir(parents=True, exist_ok=True)

    helper_src = root / 'tools' / 'ci' / 'WebRuntimeCompat.java'
    helper_out = build / 'helper'
    helper_out.mkdir(parents=True, exist_ok=True)
    run(['javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8', '-d', str(helper_out), str(helper_src)])
    helper_cls = helper_out / 'com' / 'fs' / 'starfarer' / 'WebRuntimeCompat.class'

    patcher_src = root / 'tools' / 'ci' / 'PatchBaseGameState.java'
    patcher_out = build / 'patcher'
    patcher_out.mkdir(parents=True, exist_ok=True)
    exports = 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED'
    run(['javac', '--add-exports', exports, '-d', str(patcher_out), str(patcher_src)])

    with zipfile.ZipFile(provider, 'r') as zf:
        original = zf.read(target)
    original_path = build / 'BaseGameState.class'
    patched_path = build / 'BaseGameState.patched.class'
    original_path.write_bytes(original)
    run(['java', '--add-exports', exports, '-cp', str(patcher_out), 'PatchBaseGameState', str(original_path), str(patched_path)])

    overlay = build / 'overlay'
    target_path = overlay / 'com' / 'fs' / 'starfarer' / 'BaseGameState.class'
    helper_path = overlay / 'com' / 'fs' / 'starfarer' / 'WebRuntimeCompat.class'
    target_path.parent.mkdir(parents=True, exist_ok=True)
    target_path.write_bytes(patched_path.read_bytes())
    helper_path.write_bytes(helper_cls.read_bytes())
    runtime_jar = root / 'jars' / 'basegame-runtime.jar'
    runtime_jar.unlink(missing_ok=True)
    run(['jar', 'cf', str(runtime_jar), '-C', str(overlay), '.'])
    print('prepare_candidate: built basegame-runtime.jar overlay')


def swap_source(mode: str):
    if mode == 'sync':
        return '''function Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers()\n{\n\tframeCount++;\n\tvar __swapNo = frameCount;\n\tif(__swapNo <= 4) console.log("[SWAP] sync begin count=" + __swapNo);\n\tensureFramebufferSize();\n\tglCtx.bindFramebuffer(glCtx.READ_FRAMEBUFFER, mainFb);\n\tglCtx.bindFramebuffer(glCtx.DRAW_FRAMEBUFFER, null);\n\tglCtx.blitFramebuffer(0, 0, fbWidth, fbHeight, 0, 0, fbWidth, fbHeight, glCtx.COLOR_BUFFER_BIT, glCtx.NEAREST);\n\tglCtx.bindFramebuffer(glCtx.READ_FRAMEBUFFER, mainFb);\n\tglCtx.bindFramebuffer(glCtx.DRAW_FRAMEBUFFER, mainFb);\n\tif(__swapNo <= 4) console.log("[SWAP] sync end count=" + __swapNo);\n\tif(frameLimit && frameCount >= frameLimit) console.warn("Frame limit reached");\n\treturn;\n}\n'''
    if mode == 'noop':
        return '''function Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers()\n{\n\tframeCount++;\n\tif(frameCount <= 4) console.log("[SWAP] noop count=" + frameCount);\n\treturn;\n}\n'''
    if mode == 'async':
        return '''var __starsectorPresentScheduled = false;\nvar __starsectorPresentCount = 0;\nfunction Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers()\n{\n\tframeCount++;\n\tif(frameCount <= 4) console.log("[SWAP] async request count=" + frameCount);\n\tif(!__starsectorPresentScheduled)\n\t{\n\t\t__starsectorPresentScheduled = true;\n\t\trequestAnimationFrame(function()\n\t\t{\n\t\t\t__starsectorPresentScheduled = false;\n\t\t\t__starsectorPresentCount++;\n\t\t\ttry\n\t\t\t{\n\t\t\t\tensureFramebufferSize();\n\t\t\t\tglCtx.bindFramebuffer(glCtx.READ_FRAMEBUFFER, mainFb);\n\t\t\t\tglCtx.bindFramebuffer(glCtx.DRAW_FRAMEBUFFER, null);\n\t\t\t\tglCtx.blitFramebuffer(0, 0, fbWidth, fbHeight, 0, 0, fbWidth, fbHeight, glCtx.COLOR_BUFFER_BIT, glCtx.NEAREST);\n\t\t\t\tglCtx.bindFramebuffer(glCtx.READ_FRAMEBUFFER, mainFb);\n\t\t\t\tglCtx.bindFramebuffer(glCtx.DRAW_FRAMEBUFFER, mainFb);\n\t\t\t\tif(__starsectorPresentCount <= 4) console.log("[SWAP] async present count=" + __starsectorPresentCount);\n\t\t\t}\n\t\t\tcatch(e)\n\t\t\t{\n\t\t\t\tconsole.error("[SWAP] async present failed", e);\n\t\t\t}\n\t\t});\n\t}\n\treturn;\n}\n'''
    raise ValueError(mode)


def patch_swap(root: Path, mode: str):
    path = root / 'build' / 'final' / 'wasm-modules' / 'lwjgl.js'
    text = path.read_text(encoding='utf-8')
    pattern = re.compile(
        r'function Java_org_lwjgl_opengl_LinuxContextImplementation_nSwapBuffers\(\)\s*\{.*?\n\}\n\n(?=function Java_org_lwjgl_opengl_LinuxEvent_getPending\(\))',
        re.S,
    )
    text2, count = pattern.subn(swap_source(mode) + '\n', text, count=1)
    if count != 1:
        raise RuntimeError(f'could not replace nSwapBuffers function; matches={count}')
    path.write_text(text2, encoding='utf-8')
    print('prepare_candidate: swap mode', mode)


def refresh_index(root: Path):
    original_names = classpath_names(root)
    names = ['fixer-runtime.jar', 'basegame-runtime.jar'] + original_names
    lines = []
    for name in names:
        path = root / 'jars' / name
        if not path.exists():
            raise RuntimeError(f'missing classpath jar {name}')
        lines.append(f'{name}\t{path.stat().st_size}')
    (root / 'jars' / 'index.list').write_text('\n'.join(lines) + '\n', encoding='utf-8')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--root', required=True)
    ap.add_argument('--swap', choices=['sync', 'noop', 'async'], required=True)
    args = ap.parse_args()
    root = Path(args.root).resolve()
    patch_resource_indices(root)
    patch_fixer(root)
    patch_base_game_state(root)
    patch_swap(root, args.swap)
    refresh_index(root)
    print('prepare_candidate: complete', root, args.swap)


if __name__ == '__main__':
    main()
