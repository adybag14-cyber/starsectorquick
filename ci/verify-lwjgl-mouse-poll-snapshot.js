'use strict';
const fs = require('fs');
const vm = require('vm');
function extractFunction(source,name){
  const start=source.indexOf(`function ${name}(`); if(start<0)throw new Error(`missing ${name}`);
  const brace=source.indexOf('{',start); let depth=0,quote=null,escaped=false;
  for(let i=brace;i<source.length;i++){
    const ch=source[i]; if(quote){if(escaped)escaped=false;else if(ch==='\\')escaped=true;else if(ch===quote)quote=null;continue;}
    if(ch==='"'||ch==="'"||ch==='`'){quote=ch;continue;} if(ch==='{')depth++; else if(ch==='}'&&--depth===0)return source.slice(start,i+1);
  } throw new Error(`unterminated ${name}`);
}
function expect(v,m){if(!v)throw new Error(m);}
function signed20(raw){ raw%=1048576; if(raw<0)raw+=1048576; return (raw&0x80000)!==0 ? raw|~0xfffff : raw; }
function unpack(state){
  const xRaw=state%1048576;
  const yRaw=Math.floor(state/1048576)%1048576;
  const meta=Math.floor(state/1099511627776);
  return {x:signed20(xRaw),y:signed20(yRaw),buttons:meta&0xff,inside:(meta&0x100)!==0};
}
const path=process.argv[2]; if(!path)throw new Error('usage: node ci/verify-lwjgl-mouse-poll-snapshot.js <lwjgl.js>');
const source=fs.readFileSync(path,'utf8');
expect(source.includes('LWJGL_MOUSE_POLL_SNAPSHOT_V2'),'missing poll snapshot marker');
const pack=extractFunction(source,'packMousePollSigned20');
const poll=extractFunction(source,'Java_org_lwjgl_input_Mouse_nPoll');
const inputStats={mousePollSnapshots:0,mousePollPackedClamps:0};
const mouseInputState={x:523456,y:-500000,inside:true,buttons:new Uint8Array(8)};
mouseInputState.buttons[0]=1; mouseInputState.buttons[3]=1; mouseInputState.buttons[7]=1;
const context=vm.createContext({inputStats,mouseInputState,Math,Number,Uint8Array});
vm.runInContext(`${pack}\n${poll}`,context);
let state=context.Java_org_lwjgl_input_Mouse_nPoll();
expect(Number.isSafeInteger(state),'packed mouse snapshot is not an exact JS integer');
let decoded=unpack(state);
expect(decoded.x===523456&&decoded.y===-500000,`coordinate mismatch ${JSON.stringify(decoded)}`);
expect(decoded.buttons===(1|8|128)&&decoded.inside,`meta mismatch ${JSON.stringify(decoded)}`);
expect(inputStats.mousePollSnapshots===1&&inputStats.mousePollPackedClamps===0,'initial poll telemetry mismatch');
mouseInputState.x=-524288; mouseInputState.y=524287; mouseInputState.inside=false; mouseInputState.buttons.fill(0); mouseInputState.buttons[6]=1;
state=context.Java_org_lwjgl_input_Mouse_nPoll(); decoded=unpack(state);
expect(decoded.x===-524288&&decoded.y===524287&&decoded.buttons===64&&!decoded.inside,`edge range mismatch ${JSON.stringify(decoded)}`);
mouseInputState.x=900000; mouseInputState.y=-900000;
state=context.Java_org_lwjgl_input_Mouse_nPoll(); decoded=unpack(state);
expect(decoded.x===524287&&decoded.y===-524288,'clamp range mismatch');
expect(inputStats.mousePollPackedClamps===2,'clamp telemetry mismatch');
console.log(`verify-lwjgl-mouse-poll-snapshot: OK snapshots=${inputStats.mousePollSnapshots} clamps=${inputStats.mousePollPackedClamps} packed=${state}`);
