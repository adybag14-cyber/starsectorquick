'use strict';

// Tests extract production functions into small real-WebGL or mock fixtures.
// Keep their newly introduced dependency as the actual production code, rather
// than a test-only replacement. Older source revisions remain valid controls.
function vertexArraySection(source) {
  const marker='LWJGL_VERTEX_ARRAY_COALESCING_V1';
  const start=source.indexOf('// '+marker+'_BEGIN');
  if(start<0)return '';
  const end=source.indexOf('// '+marker+'_END');
  if(end<=start)throw new Error('Incomplete vertex array cache source section');
  return source.slice(start,end)+'\n';
}
module.exports={vertexArraySection};
