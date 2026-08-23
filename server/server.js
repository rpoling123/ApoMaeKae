const http=require("http"),fs=require("fs"),path=require("path"),crypto=require("crypto");
const PORT=Number(process.env.PORT||8080),TOKEN=process.env.ADMIN_TOKEN||"";
const DB=path.join(__dirname,"licenses.json"),PUB=path.join(__dirname,"public");
if(!TOKEN){console.error("ADMIN_TOKEN required");process.exit(1)}
const read=()=>{try{return JSON.parse(fs.readFileSync(DB,"utf8"))}catch{return {keys:{}}}};
const write=x=>fs.writeFileSync(DB,JSON.stringify(x,null,2));
const send=(r,s,x)=>{r.writeHead(s,{"Content-Type":"application/json; charset=utf-8","Cache-Control":"no-store"});r.end(JSON.stringify(x))};
const body=req=>new Promise((ok,bad)=>{let s="";req.on("data",c=>s+=c);req.on("end",()=>{try{ok(s?JSON.parse(s):{})}catch(e){bad(e)}})});
const auth=req=>req.headers["x-admin-token"]===TOKEN;
const serve=(r,f)=>{try{let d=fs.readFileSync(f),e=path.extname(f).toLowerCase(),t=e===".html"?"text/html; charset=utf-8":e===".jpg"?"image/jpeg":"application/octet-stream";r.writeHead(200,{"Content-Type":t,"Cache-Control":"no-store"});r.end(d)}catch(e){send(r,404,{error:"not found"})}};
http.createServer(async(req,res)=>{try{let u=new URL(req.url,"http://localhost");
if(req.method==="GET"&&(u.pathname==="/"||u.pathname==="/index.html"))return serve(res,path.join(PUB,"index.html"));
if(req.method==="GET"&&u.pathname==="/apomaekae_bg.jpg")return serve(res,path.join(PUB,"apomaekae_bg.jpg"));
if(req.method==="GET"&&u.pathname==="/api/health")return send(res,200,{ok:true,version:"V9.1",serverTime:Date.now()});
if(req.method==="POST"&&u.pathname==="/api/license/check"){let b=await body(req),db=read(),k=db.keys[String(b.key||"").trim().toUpperCase()],now=Date.now();if(!k)return send(res,404,{active:false,message:"ไม่พบ Key",serverTime:now});let a=Date.parse(k.startAt),z=Date.parse(k.expiresAt);if(k.revoked)return send(res,200,{active:false,message:"Key ถูกระงับ",serverTime:now,expiresAt:z});if(now<a)return send(res,200,{active:false,message:"ยังไม่ถึงวันเริ่มใช้งาน",serverTime:now,expiresAt:z});if(now>=z)return send(res,200,{active:false,message:"Key หมดอายุแล้ว",serverTime:now,expiresAt:z});return send(res,200,{active:true,online:true,version:"V9.1",serverTime:now,expiresAt:z})}
if(u.pathname.startsWith("/api/admin/")){if(!auth(req))return send(res,401,{error:"unauthorized"});let db=read(),b=await body(req);if(req.method==="GET"&&u.pathname==="/api/admin/keys")return send(res,200,db.keys);
if(req.method==="POST"&&u.pathname==="/api/admin/keys"){let s=Date.parse(b.startDate),e=Date.parse(b.expiresDate);if(!s||!e||e<=s)return send(res,400,{error:"วันที่ไม่ถูกต้อง"});let key="APO-"+crypto.randomBytes(4).toString("hex").toUpperCase()+"-"+crypto.randomBytes(4).toString("hex").toUpperCase();db.keys[key]={key,startAt:new Date(s).toISOString(),expiresAt:new Date(e).toISOString(),maxDevices:Number(b.maxDevices||1),devices:[],revoked:false,createdAt:new Date().toISOString()};write(db);return send(res,200,db.keys[key])}
if(req.method==="POST"&&u.pathname.includes("/revoke")){let k=db.keys[String(b.key||"").toUpperCase()];if(!k)return send(res,404,{error:"ไม่พบ Key"});k.revoked=true;write(db);return send(res,200,k)}
if(req.method==="POST"&&u.pathname.includes("/unrevoke")){let k=db.keys[String(b.key||"").toUpperCase()];if(!k)return send(res,404,{error:"ไม่พบ Key"});k.revoked=false;write(db);return send(res,200,k)}
}
send(res,404,{error:"not found"})}catch(e){send(res,500,{error:"server error"})}}).listen(PORT,()=>console.log("ApoMaeKae V9.1 :"+PORT));
