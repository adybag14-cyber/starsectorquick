'use strict';
const fs=require('fs');
function expect(v,m){if(!v)throw new Error(m);}
function extract(src,name){const start=src.indexOf(`function ${name}(`);if(start<0)throw new Error(`missing ${name}`);const brace=src.indexOf('{',start);let depth=0,q=null,esc=false;for(let i=brace;i<src.length;i++){const ch=src[i];if(q){if(esc)esc=false;else if(ch==='\\\\')esc=true;else if(ch===q)q=null;continue;}if(ch==='"'||ch==="\'"||ch==='`'){q=ch;continue;}if(ch==='{')depth++;else if(ch==='}'&&--depth===0)return src.slice(start,i+1);}throw new Error(`unterminated ${name}`);}
const path=process.argv[2]; if(!path)throw new Error('usage: node ci/verify-lwjgl-immediate-counter-trim.js <lwjgl.js>');
const src=fs.readFileSync(path,'utf8'); expect(src.includes('LWJGL_IMMEDIATE_COUNTER_TRIM_V1'),'counter trim marker missing'); expect(src.includes('immediateCounterTrimActive: true'),'counter trim activation telemetry missing');
const append=extract(src,'appendImmediateVertex');
const fastStart=append.indexOf('if(immediateInterleavedEnabled)'); const fastEnd=append.indexOf('\t\treturn;',fastStart); expect(fastStart>=0&&fastEnd>fastStart,'interleaved fast block missing');
const fast=append.slice(fastStart,fastEnd);
for(const dead of ['vertexPos +=','colorPos +=','texCoordPos +=']) expect(!fast.includes(dead),`dead interleaved counter write remains: ${dead}`);
for(const live of ['interleavedPos = pos + 9','currentTexCoord[0] = texS','currentTexCoord[1] = texT']) expect(fast.includes(live),`required fast-path state missing: ${live}`);
const vertex=extract(src,'Java_org_lwjgl_opengl_GL11_nglVertex3f');
const enabled=vertex.indexOf('if(immediateInterleavedEnabled)'); const legacy=vertex.indexOf('else',enabled); expect(enabled>=0&&legacy>enabled,'vertex interleaved split missing');
expect(!vertex.slice(enabled,legacy).includes('texCoordPos'),'interleaved vertex still reads legacy texCoordPos');
expect(vertex.slice(legacy).includes('texCoordPos'),'legacy vertex lost texCoordPos fallback');
const end=extract(src,'Java_org_lwjgl_opengl_GL11_nglEnd');
expect(end.includes('immediateInterleavedEnabled ? (immediateModeData.interleavedPos / 9) : (immediateModeData.vertexPos / 3)'),'nglEnd does not derive interleaved vertex count from interleavedPos');
console.log('verify-lwjgl-immediate-counter-trim: OK deadWrites=0 legacyFallback=preserved');
