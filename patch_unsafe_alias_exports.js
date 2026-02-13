const fs = require('fs');
const path = require('path');

function readVarUint(bytes, offset) {
  let result = 0;
  let shift = 0;
  let pos = offset;
  while (true) {
    if (pos >= bytes.length) {
      throw new Error(`Unexpected EOF while reading LEB128 at ${offset}`);
    }
    const b = bytes[pos++];
    result |= (b & 0x7f) << shift;
    if ((b & 0x80) === 0) break;
    shift += 7;
  }
  return { value: result >>> 0, next: pos };
}

function writeVarUint(value) {
  let v = value >>> 0;
  const out = [];
  do {
    let b = v & 0x7f;
    v >>>= 7;
    if (v !== 0) b |= 0x80;
    out.push(b);
  } while (v !== 0);
  return Buffer.from(out);
}

function encodeExportEntry(entry) {
  const nameBytes = Buffer.from(entry.name, 'utf8');
  return Buffer.concat([
    writeVarUint(nameBytes.length),
    nameBytes,
    Buffer.from([entry.kind]),
    writeVarUint(entry.index)
  ]);
}

function patchUnsafeExports(inputPath, outputPath) {
  const wasm = fs.readFileSync(inputPath);
  if (wasm.length < 8) throw new Error('Invalid wasm: too small');
  if (
    wasm[0] !== 0x00 ||
    wasm[1] !== 0x61 ||
    wasm[2] !== 0x73 ||
    wasm[3] !== 0x6d
  ) {
    throw new Error('Invalid wasm magic');
  }

  const out = [];
  out.push(wasm.subarray(0, 8)); // magic + version

  let pos = 8;
  let patched = false;
  let aliasCount = 0;

  while (pos < wasm.length) {
    const sectionStart = pos;
    const id = wasm[pos++];
    const sizeInfo = readVarUint(wasm, pos);
    const sectionSize = sizeInfo.value;
    const sizeEnd = sizeInfo.next;
    const bodyStart = sizeEnd;
    const bodyEnd = bodyStart + sectionSize;
    if (bodyEnd > wasm.length) {
      throw new Error(`Invalid section length for section ${id}`);
    }

    if (id !== 7) {
      out.push(wasm.subarray(sectionStart, bodyEnd));
      pos = bodyEnd;
      continue;
    }

    // Export section
    let p = bodyStart;
    const countInfo = readVarUint(wasm, p);
    let exportCount = countInfo.value;
    p = countInfo.next;

    const entries = [];
    for (let i = 0; i < exportCount; i++) {
      const nameLenInfo = readVarUint(wasm, p);
      const nameLen = nameLenInfo.value;
      p = nameLenInfo.next;
      const nameEnd = p + nameLen;
      if (nameEnd > bodyEnd) throw new Error('Invalid export name length');
      const name = Buffer.from(wasm.subarray(p, nameEnd)).toString('utf8');
      p = nameEnd;
      if (p >= bodyEnd) throw new Error('Invalid export entry kind');
      const kind = wasm[p++];
      const indexInfo = readVarUint(wasm, p);
      const index = indexInfo.value;
      p = indexInfo.next;
      entries.push({ name, kind, index });
    }
    if (p !== bodyEnd) throw new Error('Trailing bytes in export section parse');

    const names = new Set(entries.map((e) => e.name));
    const newEntries = [...entries];
    for (const e of entries) {
      if (!e.name.includes('sun_misc_Unsafe')) continue;
      const alias = e.name.replace('sun_misc_Unsafe', 'jdk_internal_misc_Unsafe');
      if (names.has(alias)) continue;
      names.add(alias);
      newEntries.push({ name: alias, kind: e.kind, index: e.index });
      aliasCount++;
    }

    const encodedEntries = newEntries.map(encodeExportEntry);
    const newBody = Buffer.concat([writeVarUint(newEntries.length), ...encodedEntries]);
    const newSection = Buffer.concat([Buffer.from([id]), writeVarUint(newBody.length), newBody]);
    out.push(newSection);
    patched = true;
    pos = bodyEnd;
  }

  if (!patched) {
    throw new Error('No export section found in wasm');
  }

  fs.writeFileSync(outputPath, Buffer.concat(out));
  return aliasCount;
}

function main() {
  const root = process.cwd();
  const inputPath = path.join(root, 'build', 'final', 'wasm-modules', 'unsafe.wasm');
  const outputPath = path.join(root, 'build', 'final', 'wasm-modules', 'unsafe_jdk_alias.wasm');
  const aliasCount = patchUnsafeExports(inputPath, outputPath);
  console.log(`Wrote ${outputPath}`);
  console.log(`Added ${aliasCount} jdk_internal_misc_Unsafe export aliases`);
}

main();
