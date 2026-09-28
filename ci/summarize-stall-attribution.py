#!/usr/bin/env python3
"""Read-only Chrome/GL attribution summary; inclusive trace durations do not add."""
import argparse
from collections import defaultdict
import json
from pathlib import Path


def summarize(directory):
    report=json.loads((directory/'stall-attribution.json').read_text(encoding='utf-8'))
    if report.get('ok') is not True: raise ValueError('Incomplete attribution')
    events=json.loads((directory/'campaign-stalls.trace.json').read_text(encoding='utf-8'))['traceEvents']
    threads={};processes={};totals=defaultdict(lambda: [0,0.0,0.0])
    for e in events:
        if e.get('ph')=='M' and e.get('name')=='thread_name': threads[(e['pid'],e['tid'])]=e['args']['name']
        if e.get('ph')=='M' and e.get('name')=='process_name': processes[e['pid']]=e['args']['name']
        if e.get('ph')!='X' or not isinstance(e.get('dur'),(int,float)): continue
        row=totals[(e['pid'],e['tid'],e['name'])];row[0]+=1;row[1]+=e['dur']/1000;row[2]=max(row[2],e['dur']/1000)
    rows=[{'pid':pid,'tid':tid,'process':processes.get(pid,''),'thread':threads.get((pid,tid),''),'event':name,
        'count':v[0],'totalInclusiveMs':v[1],'maxMs':v[2]} for (pid,tid,name),v in totals.items()]
    w=report['webgl'];wall=w['end']-w['start']
    calls=[{'method':name,**v,'percentOfInstrumentedWall':100*v['ms']/wall} for name,v in w['totals'].items()]
    return {'scope':'Attribution only; not a benchmark. Tracing/wrappers add overhead and nested event durations are not additive.',
        'wallMs':wall,'glSynchronousMs':sum(v['ms'] for v in calls),'metricDelta':report['metricDelta'],
        'methods':sorted(calls,key=lambda v:v['ms'],reverse=True),
        'traceEvents':len(events),'topInclusiveTraceEvents':sorted(rows,key=lambda v:v['totalInclusiveMs'],reverse=True)[:60],
        'frames':len(w['frames']),'slowestInstrumentedFrames':sorted(w['frames'][1:],key=lambda v:v['elapsedMs'],reverse=True)[:12],
        'allMethodsRestored':w['restored']==w['expected']}


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('directory',type=Path);args=parser.parse_args()
    result=summarize(args.directory)
    (args.directory/'stall-summary.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:result[k] for k in ['wallMs','glSynchronousMs','frames','metricDelta','allMethodsRestored']},indent=2))
    print(json.dumps(result['methods'][:5],indent=2))
