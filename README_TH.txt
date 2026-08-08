APOMAEKAE V8.3 — LICENSE SERVER + ADMIN

ชุดนี้เพิ่มระบบ License แบบ Server เป็นตัวตัดสิน:
- สร้าง Key อัตโนมัติ เช่น APO-XXXXXXXX-XXXXXXXX
- กำหนดวัน/เวลาเริ่มและหมดอายุ
- จำกัดจำนวนเครื่องต่อ Key
- ผูกเครื่องด้วย Android ID
- Revoke / Unrevoke
- Reset รายการเครื่อง
- ตรวจ Server time
- หน้า Admin พร้อมคัดลอก Key
- แอปจะไม่เริ่ม Zone Guard ถ้าไม่มี Key หรือ Key หมดอายุ
- Countdown ในแอปใช้เวลา Server offset ที่ได้รับจากการตรวจครั้งล่าสุด

1. ติดตั้ง Server
------------------
cd server
npm install

ตั้งค่า Admin Token:
Linux/macOS:
  export ADMIN_TOKEN='ใส่รหัสยาวๆที่เดายาก'
  npm start

Windows PowerShell:
  $env:ADMIN_TOKEN='ใส่รหัสยาวๆที่เดายาก'
  npm start

หรือใช้ .env.example เป็นตัวอย่าง แต่ Node ตัวอย่างนี้อ่านจาก environment โดยตรง

เปิด:
  http://SERVER-IP:8080

หน้า Admin จะถาม Admin Token ก่อนโหลดรายการ

2. เชื่อม APK
-------------
แก้ไฟล์:
android/app/src/main/java/com/apo/maekae/LicenseManager.java

เปลี่ยน:
  https://YOUR-SERVER.example.com/api/license/check
เป็น:
  https://โดเมนของคุณ/api/license/check

จากนั้น Build APK ด้วย Android/Gradle project ตัวเต็มของคุณ

3. API
-------
POST /api/license/check
Body:
  { "key":"APO-XXXX-XXXX", "deviceId":"android-id" }

GET /api/health

Admin endpoints ต้องส่ง Header:
  x-admin-token: <ADMIN_TOKEN>

POST /api/admin/keys
POST /api/admin/revoke
POST /api/admin/unrevoke
POST /api/admin/reset-devices
GET  /api/admin/keys

4. สำคัญเรื่องความปลอดภัย
---------------------------
- อย่าเปิด Admin Token ให้คนอื่น
- ถ้าเอาใช้งานจริงบนอินเทอร์เน็ต ควรใช้ HTTPS ผ่าน reverse proxy เช่น Nginx/Cloudflare
- JSON database เหมาะสำหรับเริ่มต้น/จำนวนผู้ใช้ไม่มาก ถ้าโตขึ้นควรย้าย PostgreSQL/MySQL
- APK ที่มี URL Server ต้อง Build ใหม่หลังเปลี่ยน URL
