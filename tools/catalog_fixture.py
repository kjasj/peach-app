"""435 条纯合成数据；除登录之外拒绝全部 POST。"""
from http.server import ThreadingHTTPServer,BaseHTTPRequestHandler
import argparse,json,urllib.parse
from pathlib import Path
parser=argparse.ArgumentParser()
parser.add_argument('--media',type=Path)
parser.add_argument('--cover',type=Path)
parser.add_argument('--mixed-covers',action='store_true')
options=parser.parse_args()
failed=False
class Handler(BaseHTTPRequestHandler):
    protocol_version='HTTP/1.1'
    def send(self,status,data,headers=None):
        body=json.dumps(data,ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header('Content-Type','application/json')
        self.send_header('Content-Length',str(len(body)))
        for k,v in (headers or {}).items(): self.send_header(k,v)
        self.end_headers(); self.wfile.write(body)
    def do_POST(self):
        self.rfile.read(int(self.headers.get('Content-Length','0')))
        if self.path=='/login': self.send(303,{}, {'Location':'/','Set-Cookie':'peach=synthetic; Path=/; HttpOnly'})
        else: self.send(405,{'error':'Fixture refuses mutations'})
    def do_GET(self):
        global failed
        url=urllib.parse.urlsplit(self.path); q=urllib.parse.parse_qs(url.query)
        if url.path=='/healthz': return self.send(200,{'ok':True})
        if url.path=='/api/libraries': return self.send(200,{'libraries':[]})
        if url.path=='/api/items':
            offset=int(q.get('offset',['0'])[0]); limit=int(q.get('limit',['60'])[0])
            if offset==120 and not failed:
                failed=True
                return self.send(503,{'error':'合成测试：下一页暂时不可用'})
            total=3 if any(q.get(k) for k in ('performer','tag','studio')) else 435
            rows=[{'id':i+1,'name':f'TEST-{i+1:04}.mp4','code':f'TEST-{i+1:03}','is_jav':q.get('jav')==['1'],'has_cover':q.get('jav')==['1'],'cover_version':'synthetic-1','location':'local','medium':'video','duration':60} for i in range(offset,min(offset+limit,total))]
            if options.mixed_covers:
                for row in rows:
                    if row['id']%2==0: row['cover_frame']={'layout':'still'}
            return self.send(200,{'total':total,'items':rows,'has_more':None})
        if url.path=='/api/item': return self.send(200,{'id':int(q.get('id',['1'])[0]),'name':'TEST-DETAIL.mp4','location':'local','duration':60})
        if url.path=='/api/index':
            kind=q.get('kind',[''])[0]
            names={'performers':'合成艺人','tags':'合成标签','studios':'合成厂牌'}
            return self.send(200,{'kind':kind,'items':[{'entity_id':1,'k':names[kind],'n':3}] if kind in names else [],'has_more':False})
        if url.path=='/api/follow': return self.send(200,{'groups':[],'sources':[{'id':1,'label':'合成关注来源','provider':'synthetic','provider_label':'合成平台','enabled':True,'url':'https://example.org/fixture'}],'counts':{'unseen':0},'sort':'new','dir':'desc','has_more':False,'result_versions':{'internal':1}})
        if url.path=='/api/follow/authors': return self.send(200,{'items':[{'k':'合成创作者','key':'synthetic','n':3}],'has_more':False})
        if url.path=='/api/follow/schedule': return self.send(200,{'enabled':True,'interval_minutes':60})
        if url.path=='/api/entity': return self.send(200,{'name':q.get('name',['合成资料'])[0],'description':'这是用于返回导航测试的合成资料'})
        if url.path=='/api/tasks': return self.send(200,{'running':[{'id':1,'task_key':'follow-check','task_label':'追更检查','trigger':'scheduled','status':'running','progress_current':2,'progress_total':10,'mutex_key':'synthetic-machine-key'}],'finished':[],'skipped':[],'statuses':{'active':['running']},'result_versions':{'machine':1}})
        if url.path=='/api/stream-plan':
            if q.get('id')==['999']: return self.send(404,{'error':'unavailable'})
            return self.send(200,{'src':'/fixture.mp4','protocol':'range','mime_type':'video/mp4'})
        if url.path in ('/fixture.mp4','/cover'):
            file=options.media if url.path=='/fixture.mp4' else options.cover
            if file is None or not file.is_file(): return self.send(404,{'error':'missing synthetic asset'})
            length=file.stat().st_size
            bounds=self.headers.get('Range','').removeprefix('bytes=').split('-',1)
            start=int(bounds[0]) if bounds[0].isdigit() else 0
            end=min(length-1,int(bounds[1])) if len(bounds)>1 and bounds[1].isdigit() else length-1
            self.send_response(206 if self.headers.get('Range') else 200)
            self.send_header('Content-Type','video/mp4' if url.path=='/fixture.mp4' else 'image/png')
            self.send_header('Accept-Ranges','bytes')
            self.send_header('Content-Length',str(end-start+1))
            if self.headers.get('Range'): self.send_header('Content-Range',f'bytes {start}-{end}/{length}')
            self.end_headers()
            try:
                with file.open('rb') as source:
                    source.seek(start)
                    remaining=end-start+1
                    while remaining>0:
                        data=source.read(min(65536,remaining))
                        if not data: break
                        self.wfile.write(data); remaining-=len(data)
            except (BrokenPipeError,ConnectionResetError): pass
            return
        return self.send(404,{'error':'not found'})
    def log_message(self,*args): pass
ThreadingHTTPServer(('127.0.0.1',18902),Handler).serve_forever()
