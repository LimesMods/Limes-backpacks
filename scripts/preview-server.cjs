const http=require('node:http'),fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'..');
http.createServer((req,res)=>{
 const p=path.resolve(root,'.'+decodeURIComponent(new URL(req.url,'http://localhost').pathname));
 if(!p.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(p,(err,data)=>{if(err){res.writeHead(404);return res.end();}res.setHeader('Content-Type',({'.png':'image/png','.json':'application/json','.html':'text/html'})[path.extname(p)]||'text/plain');res.end(data);});
}).listen(8766,'127.0.0.1',()=>console.log('http://127.0.0.1:8766/docs/tier-preview.html'));
