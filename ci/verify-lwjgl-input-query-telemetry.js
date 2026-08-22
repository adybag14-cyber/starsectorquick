'use strict';
const fs=require('fs'), vm=require('vm');
function extract(src,name){
  const start=src.indexOf(`function ${name}(`); if(start<0) throw new Error(`missing ${name}`);
  const brace=src.indexOf('{',start); let depth=0,q=null,esc=false;
  for(let i=brace;i<src.length;i++){ const ch=src[i]; if(q){ if(esc)esc=false; else if(ch==='\\')esc=true; else if(ch===q)q=null; continue; }
    if(ch==='"'||ch==="'"||ch==='`'){q=ch;continue;} if(ch==='{')depth++; else if(ch==='}'&&--depth===0)return src.slice(start,i+1); }
  throw new Error(`unterminated ${name}`);
}
function expect(v,m){if(!v)throw new Error(m);}
const path=process.argv[2]; if(!path) throw new Error('usage: node ci/verify-lwjgl-input-query-telemetry.js <lwjgl.js>');
const src=fs.readFileSync(path,'utf8');
expect(src.includes('LWJGL_INPUT_QUERY_TELEMETRY_OPTIN_V1'),'marker missing');
expect(src.includes('__LWJGL_INPUT_QUERY_TELEMETRY__'),'runtime opt-in missing');
const names=['Java_org_lwjgl_input_Keyboard_nIsKeyDown','Java_org_lwjgl_input_Mouse_nIsButtonDown','Java_org_lwjgl_input_Mouse_nGetX','Java_org_lwjgl_input_Mouse_nGetY'];
const code=names.map(n=>extract(src,n)).join('\n');
function make(enabled){
  const c=vm.createContext({
    inputQueryTelemetryEnabled:enabled,
    inputStats:{keyboardStateQueries:0,keyboardPressedQueries:0,mouseButtonQueries:0,mousePressedQueries:0,mousePositionQueries:0},
    keyboardInputState:{down:new Uint8Array([0,1,0])},
    mouseInputState:{buttons:new Uint8Array([1,0]),x:12.4,y:77.6},
    Math,Uint8Array
  });
  vm.runInContext(code,c); return c;
}
let c=make(false);
expect(c.Java_org_lwjgl_input_Keyboard_nIsKeyDown(null,1)===true,'keyboard down changed with telemetry off');
expect(c.Java_org_lwjgl_input_Keyboard_nIsKeyDown(null,2)===false,'keyboard up changed with telemetry off');
expect(c.Java_org_lwjgl_input_Mouse_nIsButtonDown(null,0)===true,'mouse down changed with telemetry off');
expect(c.Java_org_lwjgl_input_Mouse_nIsButtonDown(null,1)===false,'mouse up changed with telemetry off');
expect(c.Java_org_lwjgl_input_Mouse_nGetX()===12&&c.Java_org_lwjgl_input_Mouse_nGetY()===78,'mouse coordinates changed with telemetry off');
expect(Object.values(c.inputStats).every(v=>v===0),`telemetry-off counters changed: ${JSON.stringify(c.inputStats)}`);
c=make(true);
c.Java_org_lwjgl_input_Keyboard_nIsKeyDown(null,1); c.Java_org_lwjgl_input_Keyboard_nIsKeyDown(null,2);
c.Java_org_lwjgl_input_Mouse_nIsButtonDown(null,0); c.Java_org_lwjgl_input_Mouse_nIsButtonDown(null,1);
c.Java_org_lwjgl_input_Mouse_nGetX(); c.Java_org_lwjgl_input_Mouse_nGetY();
expect(c.inputStats.keyboardStateQueries===2&&c.inputStats.keyboardPressedQueries===1,'keyboard telemetry opt-in failed');
expect(c.inputStats.mouseButtonQueries===2&&c.inputStats.mousePressedQueries===1&&c.inputStats.mousePositionQueries===2,'mouse telemetry opt-in failed');
console.log('verify-lwjgl-input-query-telemetry: OK off=zero on=2/1/2/1/2');
