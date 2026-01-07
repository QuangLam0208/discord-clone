🎮 Discord Clone
Ứng dụng chat real-time giống Discord được xây dựng với Spring Boot backend và Vanilla JavaScript frontend. Dự án hỗ trợ nhắn tin theo kênh (channel), tin nhắn riêng tư (DM), quản lý server, phân quyền chi tiết và tích hợp WebSocket để chat trực tiếp.

📋 Mục lục
Tính năng
Công nghệ sử dụng
Yêu cầu hệ thống
Cài đặt
Cấu hình
Chạy ứng dụng
Cấu trúc dự án
API Documentation
Đóng góp
✨ Tính năng
🔐 Xác thực & Bảo mật
Đăng ký/Đăng nhập với JWT authentication
Xác thực email qua OTP (lưu trữ trong Redis)
Bảo mật với Spring Security
Force logout cho user bị cấm
💬 Nhắn tin
Channel Messages: Nhắn tin trong các kênh của server
Direct Messages (DM): Tin nhắn riêng tư 1-1
Gửi file đính kèm (hình ảnh, tài liệu)
Reply tin nhắn
Chỉnh sửa & xóa tin nhắn (soft delete)
Emoji reactions
Real-time messaging qua WebSocket (STOMP)
🏰 Quản lý Server
Tạo, chỉnh sửa, xóa server
Upload icon server
Mời thành viên qua invite link (có thể giới hạn số lần dùng & thời gian hết hạn)
Kick/Ban thành viên
Audit log theo dõi các hành động
📢 Kênh & Danh mục
Tạo text/voice channels
Phân loại kênh theo category
Kênh riêng tư (chỉ một số role/member nhìn thấy)
Quản lý quyền xem/gửi tin nhắn theo kênh
👥 Hệ thống Role & Permission
Tạo custom roles với màu sắc riêng
Phân quyền chi tiết:
MANAGE_SERVER, MANAGE_CHANNELS, MANAGE_ROLES
KICK_MEMBERS, BAN_MEMBERS
CREATE_INVITE, VIEW_AUDIT_LOG
SEND_MESSAGES, MANAGE_MESSAGES
Gán role cho thành viên
Kiểm tra quyền động real-time
🤝 Bạn bè
Gửi/nhận lời mời kết bạn
Chấp nhận/từ chối lời mời
Chặn user
Danh sách bạn bè online/offline
👤 Hồ sơ người dùng
Chỉnh sửa display name, avatar, bio
Upload avatar
Xem thông tin ngày sinh, quốc gia
🛠️ Admin Panel
Quản lý users (ban/unban)
Quản lý servers
Xem tin nhắn (channel & DM)
Audit log hệ thống
🛠️ Công nghệ sử dụng
Backend
Framework: Spring Boot 3.5.7
Language: Java 21
Database:
MySQL (JPA/Hibernate) - Lưu users, servers, channels, roles...
MongoDB - Lưu messages
Redis - Lưu OTP tạm thời
Security: Spring Security + JWT (io.jsonwebtoken)
WebSocket: Spring WebSocket + STOMP
Mail: Spring Mail (gửi OTP)
Template Engine: Thymeleaf
API Documentation: Swagger/OpenAPI (springdoc-openapi)
File Processing: Apache Tika, TwelveMonkeys ImageIO (hỗ trợ WebP)
Utils: Lombok, MapStruct
Frontend
Vanilla JavaScript (modular architecture)
SockJS + STOMP. js - WebSocket client
HTML5 + CSS3
Font Awesome - Icons
SweetAlert2 - UI notifications
📦 Yêu cầu hệ thống
Java: 21+
Maven: 3.8+
MySQL: 8.0+
MongoDB: 4.4+
Redis: 6.0+
Node.js (tùy chọn, nếu build frontend riêng)
🚀 Cài đặt
1. Clone repository
bash
git clone https://github.com/QuangLam0208/discord-clone.git
cd discord-clone
git checkout ui-finalv3-tue
2. Cài đặt MySQL
SQL
CREATE DATABASE discord_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
3. Cài đặt MongoDB
Đảm bảo MongoDB đang chạy trên localhost:27017.

4. Cài đặt Redis
bash
# Ubuntu/Debian
sudo apt install redis-server
sudo systemctl start redis

# macOS (Homebrew)
brew install redis
brew services start redis

# Windows: Download từ https://redis.io/download
⚙️ Cấu hình
Chỉnh sửa file src/main/resources/application.properties:

properties
# Server Port
server.port=8081

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/discord_db
spring.datasource.username=root
spring.datasource.password=your_password

# MongoDB
spring. data.mongodb.uri=mongodb://localhost:27017/discord_db

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT Secret (đổi sang key bảo mật của bạn)
discord.app.jwtSecret=YourSecureJWTSecretKey123456789
discord.app.jwtExpirationMs=86400000

# Email (Gmail example)
spring.mail.username=your-email@gmail.com
spring. mail.password=your-app-password

# File Upload
spring.servlet.multipart.max-file-size=10MB
discord.upload.dir=uploads
Lưu ý: Với Gmail, bạn cần bật App Password thay vì dùng mật khẩu thông thường.

🏃 Chạy ứng dụng
Maven
bash
mvn clean install
mvn spring-boot:run
Hoặc chạy file JAR
bash
java -jar target/discord-1.0.jar
Truy cập
Frontend: http://localhost:8081
Swagger API Docs: http://localhost:8081/swagger-ui.html
📂 Cấu trúc dự án
Code
discord-clone/
├── src/main/java/hcmute/edu/vn/discord/
│   ├── controller/          # REST API & View Controllers
│   │   ├── api/            # REST Controllers
│   │   └── view/           # Thymeleaf Controllers
│   ├── service/            # Business Logic
│   ├── repository/         # JPA & MongoDB Repositories
│   ├── entity/             # JPA & MongoDB Entities
│   │   ├── jpa/           # MySQL entities
│   │   └── mongo/         # MongoDB documents
│   ├── dto/               # Request/Response DTOs
│   ├── security/          # JWT, Security Config
│   ├── config/            # WebSocket, CORS... 
│   └── exception/         # Custom Exceptions
│
├── src/main/resources/
│   ├── static/            # CSS, JS, Images
│   │   ├── js/           # Frontend JavaScript modules
│   │   │   ├── dm. js     # Direct Messages
│   │   │   ├── chat.js   # Channel Messages
│   │   │   ├── ws.js     # WebSocket handler
│   │   │   └── ... 
│   │   └── css/
│   ├── templates/         # Thymeleaf HTML
│   └── application.properties
│
└── pom.xml
📖 API Documentation
Sau khi chạy ứng dụng, truy cập Swagger UI:

👉 http://localhost:8081/swagger-ui.html

Các endpoint chính:
🔐 Authentication
POST /api/auth/register - Đăng ký
POST /api/auth/register/send-otp - Gửi OTP qua email
POST /api/auth/login - Đăng nhập
POST /api/auth/logout - Đăng xuất
💬 Messages
GET /api/channels/{channelId}/messages - Lấy tin nhắn trong channel
POST /api/channels/{channelId}/messages - Gửi tin nhắn
PUT /api/messages/{id} - Chỉnh sửa tin nhắn
DELETE /api/messages/{id} - Xóa tin nhắn
🏰 Servers
GET /api/servers/me - Lấy servers của user
POST /api/servers - Tạo server
PUT /api/servers/{id} - Cập nhật server
POST /api/invites/server/{serverId} - Tạo invite link
👥 Friends
GET /api/friends - Danh sách bạn bè
POST /api/friends/request - Gửi lời mời
POST /api/friends/accept/{requestId} - Chấp nhận
POST /api/friends/block/{userId} - Chặn user
