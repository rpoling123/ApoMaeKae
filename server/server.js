const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = Number(process.env.PORT || 8080);
const ADMIN_TOKEN = process.env.ADMIN_TOKEN || '';
const DB = path.join(__dirname, 'licenses.json');
const PUBLIC = path.join(__dirname, 'public');

if (!ADMIN_TOKEN) {
  console.error('ERROR: ADMIN_TOKEN is required. Example: ADMIN_TOKEN="change-me" npm start');
  process.exit(1);
}

function readDb(){ try{return JSON.parse(fs.readFileSync(DB,'utf8'));}catch{return {keys:{}};} }
function writeDb(x){ const tmp=DB+'.tmp'; fs.writeFileSync(tmp,JSON.stringify(x,null,2)); fs.renameSync(tmp,DB); }
function keyNorm(v){return String(v||'').trim().toUpperCase();}
function makeKey(){return 'APO-'+crypto.randomBytes(4).toString('hex').toUpperCase()+'-'+crypto.randomBytes(4).toString('hex').toUpperCase();}
function dateMs(v){const t=new Date(v).getTime();return Number.isFinite(t)?t:0;}
function send(res,status,obj){const body=JSON.stringify(obj);res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});res.end(body);}
function authorized(req){const a=Buffer.from(req.headers['x-admin-token']||'');const b=Buffer.from(ADMIN_TOKEN);return a.length===b.length&&crypto.timingSafeEqual(a,b);}
function body(req){return new Promise((resolve,reject)=>{let s='';req.on('data',c=>{s+=c;if(s.length>32768)req.destroy();});req.on('end',()=>{try{resolve(s?JSON.parse(s):{});}catch(e){reject(e);}});req.on('error',reject);});}
function serveFile(res,file){try{const ext=path.extname(file);const type=ext==='.html'?'text/html; charset=utf-8':'application/octet-stream';res.writeHead(200,{'Content-Type':type});res.end(fs.readFileSync(file));}catch{send(res,404,{error:'not found'});}}

const server=http.createServer(async(req,res)=>{
  try{
    const u=new URL(req.url,'http://localhost');

// Serve website and background image
if (req.method === 'GET' && (u.pathname === '/' || u.pathname === '/index.html')) {
  return serveFile(res, 'index.html');
}
if (req.method === 'GET' && u.pathname === '/apomaekae_bg.jpg') {
  return serveFile(res, 'apomaekae_bg.jpg');
}

    if(req.method==='GET'&&u.pathname==='/api/health')return send(res,200,{ok:true,serverTime:Date.now()});
    if(req.method==='GET'&&u.pathname==='/')return serveFile(res,path.join(PUBLIC,'index.html'));

    if(u.pathname==='/api/license/check'&&req.method==='POST'){
      const b=await body(req),key=keyNorm(b.key),deviceId=String(b.deviceId||'').trim(),now=Date.now(),db=readDb(),lic=db.keys[key];
      if(!key||!deviceId)return send(res,400,{active:false,message:'ต้องมี key และ deviceId',serverTime:now});
      if(!lic)return send(res,404,{active:false,message:'ไม่พบ Key',serverTime:now});
      lic.devices=Array.isArray(lic.devices)?lic.devices:[];
      const start=dateMs(lic.startAt),end=dateMs(lic.expiresAt);
      if(lic.revoked)return send(res,200,{active:false,message:'Key ถูกระงับ',serverTime:now,expiresAt:end});
      if(!start||!end||end<=start)return send(res,200,{active:false,message:'ข้อมูลวันของ Key ไม่ถูกต้อง',serverTime:now});
      if(now<start)return send(res,200,{active:false,message:'ยังไม่ถึงวันเริ่มใช้งาน',serverTime:now,startsAt:start,expiresAt:end});
      if(now>=end)return send(res,200,{active:false,message:'Key หมดอายุแล้ว',serverTime:now,expiresAt:end});
      if(!lic.devices.includes(deviceId)){
        if(lic.devices.length>=Number(lic.maxDevices||1))return send(res,200,{active:false,message:'Key ถูกใช้ครบจำนวนเครื่องแล้ว',serverTime:now,expiresAt:end,maxDevices:lic.maxDevices,usedDevices:lic.devices.length});
        lic.devices.push(deviceId);
      }
      lic.lastSeenAt=new Date(now).toISOString();writeDb(db);
      return send(res,200,{active:true,message:'OK',serverTime:now,startsAt:start,expiresAt:end,maxDevices:Number(lic.maxDevices||1),usedDevices:lic.devices.length});
    }

    if(u.pathname.startsWith('/api/admin/')){
      if(!authorized(req))return send(res,401,{error:'unauthorized'});
      const b=await body(req);
      if(req.method==='GET'&&u.pathname==='/api/admin/keys')return send(res,200,readDb().keys);
      if(req.method==='POST'&&u.pathname==='/api/admin/keys'){
        const start=dateMs(b.startDate),end=dateMs(b.expiresDate),maxDevices=Math.max(1,Math.min(10000,Number(b.maxDevices||1)));
        if(!start||!end||end<=start)return send(res,400,{error:'startDate/expiresDate ไม่ถูกต้อง'});
        const db=readDb();let key=makeKey();while(db.keys[key])key=makeKey();
        db.keys[key]={key,startAt:new Date(start).toISOString(),expiresAt:new Date(end).toISOString(),maxDevices,devices:[],revoked:false,createdAt:new Date().toISOString()};writeDb(db);return send(res,200,db.keys[key]);
      }
      if(req.method==='POST'&&['/api/admin/revoke','/api/admin/unrevoke','/api/admin/reset-devices'].includes(u.pathname)){
        const key=keyNorm(b.key),db=readDb(),lic=db.keys[key];if(!lic)return send(res,404,{error:'ไม่พบ Key'});
        if(u.pathname.endsWith('/revoke')){lic.revoked=true;lic.revokedAt=new Date().toISOString();}
        if(u.pathname.endsWith('/unrevoke')){lic.revoked=false;delete lic.revokedAt;}
        if(u.pathname.endsWith('/reset-devices')){lic.devices=[];lic.resetAt=new Date().toISOString();}
        writeDb(db);return send(res,200,lic);
      }
    }
    return send(res,404,{error:'not found'});
  }catch(e){console.error(e);return send(res,500,{error:'server error'});}
});
server.listen(PORT,()=>console.log(`ApoMaeKae License Server listening on :${PORT}`));
