# 🎮 Discord Clone



**Ứng dụng chat real-time giống Discord với Spring Boot & WebSocket**

[Tính năng](#-tính-năng) • [Cài đặt](#-cài-đặt) • [API Docs](#-api-documentation) • [Demo](#-demo)

</div>

---

## 📖 Giới thiệu

Discord Clone là một ứng dụng chat real-time được xây dựng với **Spring Boot** backend và **Vanilla JavaScript** frontend. Dự án mô phỏng các tính năng chính của Discord bao gồm:

- 💬 Nhắn tin real-time qua WebSocket
- 🏰 Quản lý server & channel
- 👥 Hệ thống role & permission chi tiết
- 🤝 Kết bạn & tin nhắn riêng tư
- 🛡️ Xác thực JWT & bảo mật

---

## ✨ Tính năng

<table>
<tr>
<td width="50%">

### 🔐 Xác thực & Bảo mật
- ✅ Đăng ký/Đăng nhập với JWT
- ✅ Xác thực email qua OTP
- ✅ Spring Security integration
- ✅ Force logout khi bị ban
- ✅ Password hashing với BCrypt

</td>
<td width="50%">

### 💬 Nhắn tin
- ✅ Channel messages
- ✅ Direct messages (DM)
- ✅ File attachments
- ✅ Reply & edit messages
- ✅ Emoji reactions
- ✅ Real-time với WebSocket

</td>
</tr>
<tr>
<td>

### 🏰 Quản lý Server
- ✅ Tạo/sửa/xóa server
- ✅ Upload server icon
- ✅ Invite links (có expire)
- ✅ Kick/Ban members
- ✅ Audit log tracking

</td>
<td>

### 👥 Role & Permission
- ✅ Custom roles với colors
- ✅ Phân quyền chi tiết (15+ permissions)
- ✅ Role hierarchy
- ✅ Channel-specific permissions
- ✅ Real-time permission updates

</td>
</tr>
</table>

<details>
<summary><b>📋 Danh sách đầy đủ Permissions</b></summary>

```
🔹 Server Management
├── MANAGE_SERVER      - Chỉnh sửa server
├── MANAGE_CHANNELS    - Quản lý channels
├── MANAGE_ROLES       - Quản lý roles
└── VIEW_AUDIT_LOG     - Xem audit log

🔹 Member Management
├── KICK_MEMBERS       - Kick members
├── BAN_MEMBERS        - Ban members
└── CREATE_INVITE      - Tạo invite links

🔹 Messaging
├── SEND_MESSAGES      - Gửi tin nhắn
├── MANAGE_MESSAGES    - Xóa/edit tin nhắn người khác
├── ATTACH_FILES       - Gửi file
└── ADD_REACTIONS      - Thêm reactions
```

</details>

---

## 🛠️ Công nghệ sử dụng

### Backend Stack

```yaml
Framework: Spring Boot 3.5.7
Language: Java 21
Build Tool: Maven

Databases:
  - MySQL 8.0       # User data, servers, channels
  - MongoDB 4.4     # Messages storage
  - Redis 6.0       # OTP caching

Security:
  - Spring Security
  - JWT (jjwt 0.11.5)
  
Real-time:
  - Spring WebSocket
  - STOMP Protocol
  - SockJS Fallback

Others:
  - Thymeleaf       # Template engine
  - Lombok          # Boilerplate reduction
  - MapStruct       # Object mapping
  - Swagger/OpenAPI # API documentation
```

### Frontend Stack

```yaml
Core:  Vanilla JavaScript (ES6+)
WebSocket: SockJS + STOMP. js
Styling: CSS3 + Font Awesome
UI Notifications: SweetAlert2
Architecture: Modular JS pattern
```

---

## 📦 Yêu cầu hệ thống

| Công nghệ | Version | Required |
|-----------|---------|----------|
| Java      | 21+     | ✅ Yes   |
| Maven     | 3.8+    | ✅ Yes   |
| MySQL     | 8.0+    | ✅ Yes   |
| MongoDB   | 4.4+    | ✅ Yes   |
| Redis     | 6.0+    | ✅ Yes   |

---

## 🚀 Cài đặt

### 1️⃣ Clone Repository

```bash
git clone https://github.com/QuangLam0208/discord-clone.git
cd discord-clone
git checkout ui-finalv3-tue
```

### 2️⃣ Cài đặt Database

#### MySQL

```sql
CREATE DATABASE discord_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

#### MongoDB

```bash
# Đảm bảo MongoDB đang chạy
mongod --dbpath /data/db

# Hoặc với Docker
docker run -d -p 27017:27017 --name mongodb mongo:4.4
```

#### Redis

```bash
# Ubuntu/Debian
sudo apt install redis-server
sudo systemctl start redis

# macOS (Homebrew)
brew install redis
brew services start redis

# Docker
docker run -d -p 6379:6379 --name redis redis:6.0
```

### 3️⃣ Cấu hình

Tạo file `src/main/resources/application. properties`:

```properties
# ============================================
# SERVER CONFIGURATION
# ============================================
server.port=8081
spring.profiles.active=dev

# ============================================
# DATABASE - MySQL
# ============================================
spring.datasource.url=jdbc:mysql://localhost:3306/discord_db? useSSL=false&serverTimezone=Asia/Ho_Chi_Minh
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# ============================================
# DATABASE - MongoDB
# ============================================
spring.data.mongodb.uri=mongodb://localhost:27017/discord_db

# ============================================
# DATABASE - Redis (OTP Storage)
# ============================================
spring.data.redis.host=localhost
spring.data.redis.port=6379

# ============================================
# SECURITY - JWT
# ============================================
discord.app. jwtSecret=YourSuperSecretKeyHere123456789
discord.app.jwtExpirationMs=86400000

# ============================================
# EMAIL CONFIGURATION (Gmail)
# ============================================
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail. smtp.auth=true
spring. mail.properties.mail.smtp. starttls.enable=true

# ============================================
# FILE UPLOAD
# ============================================
spring. servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
discord.upload.dir=uploads

# ============================================
# WEBSOCKET
# ============================================
app.websocket.allowed-origins=http://localhost:3000,http://localhost:5173
```

<details>
<summary>💡 <b>Hướng dẫn lấy Gmail App Password</b></summary>

1. Truy cập [Google Account Security](https://myaccount.google.com/security)
2. Bật **2-Step Verification**
3. Tìm **App passwords**
4. Chọn **Mail** và thiết bị của bạn
5. Copy password được tạo và paste vào config

</details>

### 4️⃣ Build & Run

```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run

# Hoặc chạy file JAR
java -jar target/discord-1.0.jar
```

### 5️⃣ Truy cập ứng dụng

| Service | URL | Description |
|---------|-----|-------------|
| 🏠 Frontend | http://localhost:8081 | Giao diện chính |
| 📚 Swagger API | http://localhost:8081/swagger-ui.html | API Documentation |
| 🔍 H2 Console | http://localhost:8081/h2-console | Database console (nếu dùng H2) |

---

## 📂 Cấu trúc dự án

```
discord-clone/
│
├── 📁 src/main/java/hcmute/edu/vn/discord/
│   │
│   ├── 📁 controller/
│   │   ├── api/                    # REST API Controllers
│   │   │   ├── AuthController.java
│   │   │   ├── MessageController.java
│   │   │   ├── ServerController.java
│   │   │   └── ... 
│   │   └── view/                   # Thymeleaf View Controllers
│   │
│   ├── 📁 service/                 # Business Logic Layer
│   │   ├── UserService.java
│   │   ├── MessageService.java
│   │   └── ... 
│   │
│   ├── 📁 repository/              # Data Access Layer
│   │   ├── UserRepository.java
│   │   ├── MessageRepository.java
│   │   └── ...
│   │
│   ├── 📁 entity/
│   │   ├── jpa/                   # MySQL Entities
│   │   │   ├── User.java
│   │   │   ├── Server.java
│   │   │   └── Channel.java
│   │   └── mongo/                 # MongoDB Documents
│   │       └── Message.java
│   │
│   ├── 📁 dto/                    # Data Transfer Objects
│   │   ├── request/
│   │   └── response/
│   │
│   ├── 📁 security/               # Security Configuration
│   │   ├── jwt/
│   │   └── servers/
│   │
│   ├── 📁 config/                 # App Configuration
│   │   ├── WebSocketConfig.java
│   │   ├── SecurityConfig.java
│   │   └── ... 
│   │
│   └── 📁 exception/              # Custom Exceptions
│
├── 📁 src/main/resources/
│   ├── 📁 static/
│   │   ├── css/
│   │   │   └── style.css
│   │   ├── js/                    # Frontend JavaScript Modules
│   │   │   ├── auth.js           # Authentication
│   │   │   ├── dm.js             # Direct Messages
│   │   │   ├── chat.js           # Channel Messages
│   │   │   ├── ws.js             # WebSocket Handler
│   │   │   ├── server.js         # Server Management
│   │   │   └── ...
│   │   └── uploads/              # User uploaded files
│   │
│   ├── 📁 templates/              # Thymeleaf HTML Templates
│   │   ├── login.html
│   │   ├── home.html
│   │   └── admin/
│   │
│   └── application.properties     # Configuration File
│
└── pom.xml                        # Maven Dependencies
```

---

## 📖 API Documentation

### 🔐 Authentication

<details>
<summary><code>POST</code> <b>/api/auth/register</b> - Đăng ký tài khoản</summary>

**Request Body:**
```json
{
  "username": "user123",
  "email": "user@example.com",
  "password": "SecurePass123!",
  "displayName": "John Doe"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "user": {
    "id": 1,
    "username": "user123",
    "email":  "user@example.com",
    "displayName": "John Doe"
  }
}
```
</details>

<details>
<summary><code>POST</code> <b>/api/auth/login</b> - Đăng nhập</summary>

**Request Body:**
```json
{
  "username": "user123",
  "password": "SecurePass123!"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "user": { ... }
}
```
</details>

### 💬 Messages

<details>
<summary><code>GET</code> <b>/api/channels/{channelId}/messages</b> - Lấy tin nhắn</summary>

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 20)

**Response:**
```json
[
  {
    "id":  "msg123",
    "content": "Hello world!",
    "senderId": 1,
    "senderName": "John Doe",
    "createdAt":  "2026-01-07T10:30:00",
    "isEdited": false,
    "attachments": [],
    "reactions": []
  }
]
```
</details>

<details>
<summary><code>POST</code> <b>/api/channels/{channelId}/messages</b> - Gửi tin nhắn</summary>

**Request Body:**
```json
{
  "content": "Hello everyone!",
  "replyToId": "msg122",
  "attachments": [
    "https://example.com/image.png"
  ]
}
```
</details>

### 🏰 Servers

<details>
<summary><code>GET</code> <b>/api/servers/me</b> - Lấy servers của user</summary>

**Response:**
```json
[
  {
    "id":  1,
    "name":  "My Server",
    "iconUrl": "/uploads/server-icon.png",
    "ownerId": 1,
    "memberCount": 42,
    "onlineCount": 15
  }
]
```
</details>

<details>
<summary><code>POST</code> <b>/api/servers</b> - Tạo server mới</summary>

**Request Body:**
```json
{
  "name": "My Awesome Server",
  "description": "A cool place to hang out"
}
```
</details>

### 👥 Friends

<details>
<summary><code>POST</code> <b>/api/friends/request</b> - Gửi lời mời kết bạn</summary>

**Request Body:**
```json
{
  "receiverUsername": "friend123"
}
```
</details>

📚 **Xem đầy đủ API tại:** http://localhost:8081/swagger-ui.html

---

## 🎨 Screenshots

<div align="center">

### 🏠 Trang chủ
![Home Page](https://via.placeholder.com/800x400/2c2f33/ffffff?text=Discord+Clone+Home)

### 💬 Chat Interface
![Chat](https://via.placeholder.com/800x400/36393f/ffffff?text=Real-time+Chat)

### ⚙️ Server Settings
![Settings](https://via.placeholder.com/800x400/202225/ffffff?text=Server+Management)

</div>

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run with coverage
mvn clean test jacoco:report
```

---

## 🐳 Docker Deployment

```bash
# Build Docker image
docker build -t discord-clone: latest .

# Run with Docker Compose
docker-compose up -d
```

**docker-compose.yml:**
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8081:8081"
    environment:
      - MYSQL_URL=jdbc:mysql://mysql:3306/discord_db
      - MONGO_URI=mongodb://mongo:27017/discord_db
      - REDIS_HOST=redis
    depends_on:
      - mysql
      - mongo
      - redis

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: discord_db
      MYSQL_ROOT_PASSWORD: root

  mongo:
    image: mongo:4.4
    ports:
      - "27017:27017"

  redis:
    image: redis:6.0
    ports:
      - "6379:6379"
```

---

## 🤝 Đóng góp

Contributions, issues và feature requests luôn được chào đón! 🎉

### Cách đóng góp:

1. 🍴 Fork repository này
2. 🌿 Tạo branch mới (`git checkout -b feature/amazing-feature`)
3. ✍️ Commit changes (`git commit -m 'Add some amazing feature'`)
4. 📤 Push lên branch (`git push origin feature/amazing-feature`)
5. 🔁 Mở Pull Request

### Coding Guidelines:

- ✅ Follow Java naming conventions
- ✅ Write meaningful commit messages
- ✅ Add comments for complex logic
- ✅ Write unit tests for new features
- ✅ Update documentation

---

## 📝 License

Dự án này thuộc quyền sở hữu của **HCMUTE** và được sử dụng cho mục đích học tập.

---

## 👥 Contributors

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/QuangLam0208">
        <img src="https://github.com/QuangLam0208.png" width="100px;" alt=""/>
        <br />
        <sub><b>Quang Lam</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/tuenguyenprograming1003">
        <img src="https://github.com/tuenguyenprograming1003.png" width="100px;" alt=""/>
        <br />
        <sub><b>Tue Nguyen</b></sub>
      </a>
    </td>
  </tr>
</table>

---

## 🎯 Roadmap

- [ ] 🎤 Voice chat (WebRTC)
- [ ] 📹 Video call 1-1
- [ ] 🖥️ Screen sharing
- [ ] 📝 Rich text formatting (Markdown)
- [ ] 🔔 Push notifications
- [ ] 📱 Mobile responsive UI
- [ ] 🌐 i18n (Internationalization)
- [ ] 🤖 Bot system
- [ ] 📊 Analytics dashboard

---

## 📞 Liên hệ

- 📧 Email: support@discord-clone.com
- 🐛 Issues: [GitHub Issues](https://github.com/QuangLam0208/discord-clone/issues)
- 💬 Discussions: [GitHub Discussions](https://github.com/QuangLam0208/discord-clone/discussions)

---

## ⭐ Support

Nếu dự án này hữu ích, hãy cho một ⭐️! 

<div align="center">

**Made with ❤️ by HCMUTE Students**

[⬆ Back to top](#-discord-clone)

</div>
