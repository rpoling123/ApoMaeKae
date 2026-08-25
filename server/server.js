const http=require("http"),fs=require("fs"),path=require("path"),crypto=require("crypto");
const PORT=Number(process.env.PORT||8080),TOKEN=process.env.ADMIN_TOKEN||"ApoMaeKae_Admin_2026_X7k9",PAYMENT_SECRET=process.env.PAYMENT_WEBHOOK_SECRET||"";
const DB=path.join(__dirname,"licenses.json"),ORDERS=path.join(__dirname,"orders.json"),PUB=path.join(__dirname,"public");
const PLANS={day:{label:"1 วัน",days:1,price:5},week:{label:"7 วัน",days:7,price:15},month:{label:"30 วัน",days:30,price:39},year:{label:"365 วัน",days:365,price:199}};
const read=(f,d)=>{try{return JSON.parse(fs.readFileSync(f,"utf8"))}catch{return d}},write=(f,d)=>{let t=f+".tmp";fs.writeFileSync(t,JSON.stringify(d,null,2));fs.renameSync(t,f)};
const send=(r,s,x,t="application/json; charset=utf-8")=>{r.writeHead(s,{"Content-Type":t,"Cache-Control":"no-store"});r.end(t.startsWith("application/json")?JSON.stringify(x):x)};
const body=q=>new Promise((ok,bad)=>{let s="";q.on("data",c=>s+=c);q.on("end",()=>{try{ok(s?JSON.parse(s):{})}catch(e){bad(e)}})});
const auth=q=>!!TOKEN&&q.headers["x-admin-token"]===TOKEN;
const serve=(r,f)=>{try{let d=fs.readFileSync(f),e=path.extname(f),t=e===".html"?"text/html; charset=utf-8":e===".jpg"||e===".jpeg"?"image/jpeg":e===".png"?"image/png":e===".svg"?"image/svg+xml":"application/octet-stream";send(r,200,d,t)}catch{send(r,404,{error:"not found"})}};
const key=()=> "APO-"+crypto.randomBytes(4).toString("hex").toUpperCase()+"-"+crypto.randomBytes(4).toString("hex").toUpperCase();
function makeKey(o){let db=read(DB,{keys:{}}),k=key(),now=Date.now(),z=now+o.days*86400000;db.keys[k]={key:k,plan:o.plan,planLabel:o.planLabel,price:o.price,startAt:new Date(now).toISOString(),expiresAt:new Date(z).toISOString(),maxDevices:Number(o.maxDevices||1),devices:[],revoked:false,orderId:o.orderId,paymentRef:o.paymentRef||"",createdAt:new Date().toISOString()};write(DB,db);return db.keys[k]}
const pub=o=>({orderId:o.orderId,plan:o.plan,planLabel:o.planLabel,price:o.price,status:o.status,createdAt:o.createdAt,paidAt:o.paidAt||null,key:o.status==="paid"?o.key:null,expiresAt:o.status==="paid"?o.expiresAt:null});
http.createServer(async(q,r)=>{try{let u=new URL(q.url,"http://localhost"),p=u.pathname;
if(q.method==="GET"&&(p==="/"||p==="/buy-key"||p==="/buy-key.html"))return serve(r,path.join(PUB,"buy-key.html"));
if(q.method==="GET"&&(p==="/admin"||p==="/admin.html"))return serve(r,path.join(PUB,"admin.html"));
if(q.method==="GET"&&(p==="/buy-key"||p==="/buy-key.html"))return serve(r,path.join(PUB,"buy-key.html"));
if(q.method==="GET"&&p==="/apomaekae_bg.jpg")return serve(r,path.join(PUB,"apomaekae_bg.jpg"));
if(q.method==="GET"&&p==="/payment_qr.svg")return serve(r,path.join(PUB,"payment_qr.svg"));
if(q.method==="GET"&&p==="/payment_qr.jpg")return serve(r,path.join(PUB,"payment_qr.jpg"));
if(q.method==="GET"&&p==="/api/health")return send(r,200,{ok:true,version:"V9.1",serverTime:Date.now()});
if(q.method==="POST"&&p==="/api/license/check"){let b=await body(q),db=read(DB,{keys:{}}),k=db.keys[String(b.key||"").trim().toUpperCase()],now=Date.now();if(!k)return send(r,404,{active:false,message:"ไม่พบ Key",serverTime:now});let z=Date.parse(k.expiresAt);if(k.revoked)return send(r,200,{active:false,message:"Key ถูกระงับ",serverTime:now,expiresAt:z});if(now<Date.parse(k.startAt))return send(r,200,{active:false,message:"ยังไม่ถึงวันเริ่มใช้งาน",serverTime:now,expiresAt:z});if(now>=z)return send(r,200,{active:false,message:"Key หมดอายุแล้ว",serverTime:now,expiresAt:z});return send(r,200,{active:true,online:true,version:"V9.1",serverTime:now,expiresAt:z,plan:k.planLabel})}
if(q.method==="POST"&&p==="/api/order/create"){let b=await body(q),pl=PLANS[b.plan];if(!pl)return send(r,400,{error:"แพ็กเกจไม่ถูกต้อง"});let id="ORD-"+Date.now().toString(36).toUpperCase()+"-"+crypto.randomBytes(2).toString("hex").toUpperCase(),db=read(ORDERS,{orders:{}});db.orders[id]={orderId:id,plan:b.plan,planLabel:pl.label,days:pl.days,price:pl.price,status:"pending",createdAt:new Date().toISOString(),paymentRef:"",key:"",expiresAt:""};write(ORDERS,db);return send(r,200,pub(db.orders[id]))}
if(q.method==="GET"&&p.startsWith("/api/order/")){let id=decodeURIComponent(p.slice(11)),o=read(ORDERS,{orders:{}}).orders[id];if(!o)return send(r,404,{error:"ไม่พบ Order"});return send(r,200,pub(o))}
if(q.method==="POST"&&p==="/api/payment/webhook"){if(!PAYMENT_SECRET)return send(r,503,{error:"PAYMENT_WEBHOOK_SECRET ยังไม่ได้ตั้งค่า"});if(q.headers["x-payment-secret"]!==PAYMENT_SECRET)return send(r,401,{error:"unauthorized"});let b=await body(q),db=read(ORDERS,{orders:{}}),o=db.orders[b.orderId];if(!o)return send(r,404,{error:"ไม่พบ Order"});if(String(b.status).toLowerCase()!=="paid")return send(r,200,{ok:true,processed:false});if(Number(b.amount)!==o.price)return send(r,400,{error:"ยอดชำระไม่ตรงแพ็กเกจ"});if(o.status==="paid")return send(r,200,{ok:true,processed:false,...pub(o)});o.status="paid";o.paidAt=new Date().toISOString();o.paymentRef=String(b.paymentRef||"");let k=makeKey(o);o.key=k.key;o.expiresAt=k.expiresAt;write(ORDERS,db);return send(r,200,{ok:true,processed:true,...pub(o)})}

// LINE BK Alerts bridge:
// The APK notification listener forwards only candidate money-in notifications.
// Auto-create is intentionally conservative: exact amount + exactly one pending order.
// If multiple pending orders have the same amount, it returns ambiguous and does NOT create a key.
if(q.method==="POST"&&p==="/api/payment/linebk-alert"){
  if(q.headers["x-app-token"]!=="APO_V91_LINEBK")return send(r,401,{error:"unauthorized"});
  const b=await body(q),raw=String((b.title||"")+" "+(b.text||""));
  const m=raw.match(/(?:฿|บาท|THB|\b)(?:\s*)?([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)/i);
  if(!m)return send(r,200,{ok:true,processed:false,reason:"amount_not_found"});
  const amount=Number(m[1].replace(/,/g,""));
  const od=read(ORDERS,{orders:{}});
  const pending=Object.values(od.orders).filter(o=>o.status==="pending"&&Number(o.price)===amount);
  if(pending.length!==1)return send(r,200,{ok:true,processed:false,reason:pending.length===0?"no_matching_order":"ambiguous_matching_orders"});
  const o=pending[0];
  o.status="paid";o.paidAt=new Date().toISOString();o.paymentRef="LINE_BK_ALERT";
  const k=makeKey(o);o.key=k.key;o.expiresAt=k.expiresAt;write(ORDERS,od);
  return send(r,200,{ok:true,processed:true,orderId:o.orderId,key:o.key,expiresAt:o.expiresAt});
}

if(p.startsWith("/api/admin/")){if(!auth(q))return send(r,401,{error:"unauthorized"});let b=q.method==="GET"?{}:await body(q),db=read(DB,{keys:{}}),od=read(ORDERS,{orders:{}});
if(q.method==="GET"&&p==="/api/admin/keys")return send(r,200,db.keys);
if(q.method==="GET"&&p==="/api/admin/orders")return send(r,200,od.orders);
if(q.method==="POST"&&p==="/api/admin/orders/confirm"){let o=od.orders[b.orderId];if(!o)return send(r,404,{error:"ไม่พบ Order"});if(o.status!=="paid"){o.status="paid";o.paidAt=new Date().toISOString();o.paymentRef=String(b.paymentRef||"");let k=makeKey(o);o.key=k.key;o.expiresAt=k.expiresAt;write(ORDERS,od)}return send(r,200,pub(o))}
if(q.method==="POST"&&p==="/api/admin/keys/revoke"){let k=db.keys[String(b.key||"").toUpperCase()];if(!k)return send(r,404,{error:"ไม่พบ Key"});k.revoked=true;write(DB,db);return send(r,200,k)}
if(q.method==="POST"&&p==="/api/admin/keys/unrevoke"){let k=db.keys[String(b.key||"").toUpperCase()];if(!k)return send(r,404,{error:"ไม่พบ Key"});k.revoked=false;write(DB,db);return send(r,200,k)}}

   if(!auth(q))return send(r,401,{error:"unauthorized"});

   // ADMIN สร้าง KEY ใหม่
   if(q.method==="POST"&&p==="/api/admin/keys"){
    let days=Number(b.days||0);
    let plan=String(b.plan||"custom");
    let planLabel=String(b.planLabel||"กำหนดเอง");
    let price=Number(b.price||0);

    if(PLANS[plan]){
     days=PLANS[plan].days;
     planLabel=PLANS[plan].label;
     price=PLANS[plan].price;
    }

    if(!Number.isFinite(days)||days<=0||days>3650)
     return send(r,400,{error:"จำนวนวันไม่ถูกต้อง (1-3650)"});

    const k=makeKey({
     days,plan,planLabel,price,
     maxDevices:Number(b.maxDevices||1),
     paymentRef:String(b.paymentRef||"")
    });

    return send(r,200,{ok:true,key:k.key,license:k});
   }

   // ADMIN แก้ไข KEY
   if(q.method==="POST"&&p==="/api/admin/keys/update"){
    const k=db.keys[String(b.key||"").trim().toUpperCase()];
    if(!k)return send(r,404,{error:"ไม่พบ Key"});

    if(b.expiresAt!==undefined){
     const t=Date.parse(b.expiresAt);
     if(!Number.isFinite(t))return send(r,400,{error:"expiresAt ไม่ถูกต้อง"});
     k.expiresAt=new Date(t).toISOString();
    }

    if(b.startAt!==undefined){
     const t=Date.parse(b.startAt);
     if(!Number.isFinite(t))return send(r,400,{error:"startAt ไม่ถูกต้อง"});
     k.startAt=new Date(t).toISOString();
    }

    if(b.maxDevices!==undefined){
     const n=Number(b.maxDevices);
     if(!Number.isFinite(n)||n<1)return send(r,400,{error:"maxDevices ไม่ถูกต้อง"});
     k.maxDevices=n;
    }

    if(b.revoked!==undefined)k.revoked=!!b.revoked;
    if(b.planLabel!==undefined)k.planLabel=String(b.planLabel);

    k.updatedAt=new Date().toISOString();
    write(DB,db);
    return send(r,200,{ok:true,license:k});
   }

   // ADMIN ลบ KEY
   if(q.method==="POST"&&p==="/api/admin/keys/delete"){
    const id=String(b.key||"").trim().toUpperCase();
    if(!db.keys[id])return send(r,404,{error:"ไม่พบ Key"});
    delete db.keys[id];
    write(DB,db);
    return send(r,200,{ok:true,deleted:id});
   }

return send(r,404,{error:"not found"})}catch(e){console.error(e);return send(r,500,{error:"server error"})}}).listen(PORT,()=>console.log("ApoMaeKae V9.1 :"+PORT));
