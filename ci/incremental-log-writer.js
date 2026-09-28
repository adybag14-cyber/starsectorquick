'use strict';
const fs=require('node:fs');

// Preserve the exact logs.join('\n') bytes at each flush, without rewriting the
// entire growing history on every post-campaign console event. No log is filtered.
function createIncrementalLogWriter(file, io=fs) {
  let cursor=0,initialized=false;
  const stats={flushCalls:0,appendCalls:0,entries:0,bytesWritten:0,error:null};
  return {
    stats,
    flush(entries) {
      stats.flushCalls++;
      if(stats.error) return false; // A failed append may be partial; never replay blindly.
      try {
        if(!Array.isArray(entries)||entries.length<cursor)throw new Error('Live log must be append-only');
        if(!initialized){io.writeFileSync(file,'',{encoding:'utf8'});initialized=true;}
        if(entries.length===cursor)return true;
        const end=entries.length;
        const payload=(cursor?'\n':'')+entries.slice(cursor,end).join('\n');
        io.appendFileSync(file,payload,{encoding:'utf8'});
        cursor=end;stats.entries=cursor;stats.appendCalls++;stats.bytesWritten+=Buffer.byteLength(payload,'utf8');
        return true;
      } catch(error) { stats.error=String(error.message||error); return false; }
    },
  };
}
module.exports={createIncrementalLogWriter};
