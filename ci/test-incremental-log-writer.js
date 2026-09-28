'use strict';
const assert=require('node:assert/strict');const {createIncrementalLogWriter}=require('./incremental-log-writer');
let content='previous run',writes=0,appends=0;
const io={writeFileSync(file,data){assert.equal(file,'test.log');content=data;writes++;},appendFileSync(file,data){assert.equal(file,'test.log');content+=data;appends++;}};
const writer=createIncrementalLogWriter('test.log',io);const entries=[];
assert.equal(writer.flush(entries),true);assert.equal(content,'');
for(const text of ['alpha','', 'Unicode: \u03b1 \u4e2d \ud83d\ude80','embedded\nnewlines','tail']){
 entries.push(text);assert.equal(writer.flush(entries),true);assert.equal(content,entries.join('\n'));assert.equal(writer.flush(entries),true);
}
assert.equal(writes,1);assert.equal(appends,5);assert.equal(writer.stats.entries,5);assert.equal(writer.stats.bytesWritten,Buffer.byteLength(content));
entries.push('one','two');writer.flush(entries);assert.equal(content,entries.join('\n'));
assert.equal(writer.flush(['shorter']),false);assert.match(writer.stats.error,/append-only/);
const failed=createIncrementalLogWriter('fail.log',{writeFileSync(){},appendFileSync(){throw new Error('disk full');}});
assert.equal(failed.flush(['a']),false);assert.equal(failed.stats.entries,0);assert.equal(failed.stats.error,'disk full');
assert.equal(failed.flush(['a','b']),false);assert.equal(failed.stats.appendCalls,0);
let replay='',oldBytes=0;const replayEntries=[];
const candidate=createIncrementalLogWriter('replay',{writeFileSync(){replay='';},appendFileSync(_,s){replay+=s;}});
for(let i=0;i<2000;i++){
 replayEntries.push('event-'+i+' exact data '.repeat(8));oldBytes+=Buffer.byteLength(replayEntries.join('\n'));
 assert.equal(candidate.flush(replayEntries),true);
}
assert.equal(replay,replayEntries.join('\n'));assert.equal(candidate.stats.bytesWritten,Buffer.byteLength(replay));
assert.ok(oldBytes>candidate.stats.bytesWritten*900);
console.log('test-incremental-log-writer: OK byte-exact unicode/multiline/empty/batch/idempotence/failure; replay bytes '+oldBytes+' -> '+candidate.stats.bytesWritten);
