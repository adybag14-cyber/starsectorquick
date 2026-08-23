#!/usr/bin/env python3
from pathlib import Path
p=Path('build/final/wasm-modules/lwjgl.js')
s=p.read_text(encoding='utf-8')
if 'FRAME_TAIL_TELEMETRY_BENCH_V1' not in s:
    anchor='var frameCount = 0;\n'; assert anchor in s
    s=s.replace(anchor, anchor+'// FRAME_TAIL_TELEMETRY_BENCH_V1\nvar presentationTargetFps = 60;\n',1)
    anchor='\tlastViewport: null,\n'; assert anchor in s
    s=s.replace(anchor,anchor+'\ttargetFps: presentationTargetFps,\n\ttargetFrameMs: 1000 / presentationTargetFps,\n',1)
    anchor='\trecentFrameMs: 0,\n'; assert anchor in s
    s=s.replace(anchor,anchor+'\tlastFrameMs: 0,\n\tframeP50Ms: 0,\n\tframeP95Ms: 0,\n\tframeP99Ms: 0,\n\tframeMinMs: 0,\n\tframeMaxMs: 0,\n\tframeJitterStdDevMs: 0,\n\tframeJitterP95Ms: 0,\n\trecentLongFrameCount: 0,\n\tlongFrameCount: 0,\n\trecentDroppedFrameEstimate: 0,\n\tdroppedFrameEstimate: 0,\n',1)
    anchor='if(typeof window !== "undefined")\n'; assert anchor in s
    funcs='''var recentFrameIntervals = [];
var lastPresentationSwapTime = NaN;
function presentationPercentile(sorted, fraction)
{
\tif(sorted.length == 0) return 0;
\tvar index = Math.ceil(fraction * sorted.length) - 1;
\tindex = Math.max(0, Math.min(sorted.length - 1, index));
\treturn sorted[index];
}
function updatePresentationTimingStats()
{
\tif(recentFrameIntervals.length == 0) return;
\tvar sorted = recentFrameIntervals.slice().sort(function(a, b) { return a - b; });
\tvar sum = 0;
\tfor(var i = 0;i < recentFrameIntervals.length;i++) sum += recentFrameIntervals[i];
\tvar mean = sum / recentFrameIntervals.length;
\tvar variance = 0;
\tfor(var j = 0;j < recentFrameIntervals.length;j++) { var delta = recentFrameIntervals[j] - mean; variance += delta * delta; }
\tvariance /= recentFrameIntervals.length;
\tvar median = presentationPercentile(sorted, 0.5);
\tvar deviations = new Array(recentFrameIntervals.length);
\tvar recentLongFrames = 0, recentDroppedFrames = 0, targetFrameMs = presentationStats.targetFrameMs;
\tvar longFrameThresholdMs = Math.max(25, targetFrameMs * 1.5);
\tfor(var k = 0;k < recentFrameIntervals.length;k++) {
\t\tvar interval = recentFrameIntervals[k]; deviations[k] = Math.abs(interval - median);
\t\tif(interval > longFrameThresholdMs) recentLongFrames++;
\t\trecentDroppedFrames += Math.max(0, Math.round(interval / targetFrameMs) - 1);
\t}
\tdeviations.sort(function(a, b) { return a - b; });
\tpresentationStats.frameP50Ms = median;
\tpresentationStats.frameP95Ms = presentationPercentile(sorted, 0.95);
\tpresentationStats.frameP99Ms = presentationPercentile(sorted, 0.99);
\tpresentationStats.frameMinMs = sorted[0];
\tpresentationStats.frameMaxMs = sorted[sorted.length - 1];
\tpresentationStats.frameJitterStdDevMs = Math.sqrt(variance);
\tpresentationStats.frameJitterP95Ms = presentationPercentile(deviations, 0.95);
\tpresentationStats.recentLongFrameCount = recentLongFrames;
\tpresentationStats.recentDroppedFrameEstimate = recentDroppedFrames;
}
'''
    s=s.replace(anchor,funcs+anchor,1)
    anchor='\tvar swapNow = performance.now();\n'; assert anchor in s
    block='''\tif(Number.isFinite(lastPresentationSwapTime))
\t{
\t\tvar frameInterval = swapNow - lastPresentationSwapTime;
\t\tif(Number.isFinite(frameInterval) && frameInterval >= 0 && frameInterval < 10000)
\t\t{
\t\t\tpresentationStats.lastFrameMs = frameInterval;
\t\t\trecentFrameIntervals.push(frameInterval);
\t\t\tif(recentFrameIntervals.length > 240) recentFrameIntervals.shift();
\t\t\tvar targetFrameMs = presentationStats.targetFrameMs;
\t\t\tif(frameInterval > Math.max(25, targetFrameMs * 1.5)) presentationStats.longFrameCount++;
\t\t\tpresentationStats.droppedFrameEstimate += Math.max(0, Math.round(frameInterval / targetFrameMs) - 1);
\t\t}
\t}
\tlastPresentationSwapTime = swapNow;
'''
    s=s.replace(anchor,anchor+block,1)
    anchor='\tif(presentationReadbackDiagnostics && presentationStats.samples.length < 8 && (presentationStats.swapCount == 1 || (presentationStats.swapCount % 300) == 0))\n'; assert anchor in s
    s=s.replace(anchor,'\tif(presentationStats.swapCount <= 3 || (presentationStats.swapCount % 15) == 0) updatePresentationTimingStats();\n'+anchor,1)
p.write_text(s,encoding='utf-8',newline='\n')

p=Path('ci/campaign-render-test.js')
s=p.read_text(encoding='utf-8')
if 'frameP95Ms: Number(perfAfter.frameP95Ms' not in s:
    anchor='      recentFrameMs: Number(perfAfter.recentFrameMs || 0),\n'; assert anchor in s
    s=s.replace(anchor,anchor+'      frameP50Ms: Number(perfAfter.frameP50Ms || 0),\n      frameP95Ms: Number(perfAfter.frameP95Ms || 0),\n      frameP99Ms: Number(perfAfter.frameP99Ms || 0),\n      frameMaxMs: Number(perfAfter.frameMaxMs || 0),\n      frameJitterStdDevMs: Number(perfAfter.frameJitterStdDevMs || 0),\n      frameJitterP95Ms: Number(perfAfter.frameJitterP95Ms || 0),\n      longFrameDelta: Number(perfAfter.longFrameCount || 0) - Number(perfBefore.longFrameCount || 0),\n      droppedFrameEstimateDelta: Number(perfAfter.droppedFrameEstimate || 0) - Number(perfBefore.droppedFrameEstimate || 0),\n',1)
p.write_text(s,encoding='utf-8',newline='\n')
print('Applied benchmark-only frame-tail telemetry')
