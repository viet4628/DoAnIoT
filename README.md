# 🏠 Smart Home IoT System

Hệ thống quản lý thiết bị IoT thông minh với khả năng mở rộng cao, tích hợp MQTT, Redis cache, và Flutter mobile app.

## 📋 Mục Lục

- [Tính Năng](#-tính-năng)
- [Kiến Trúc Hệ Thống](#-kiến-trúc-hệ-thống)
- [Khả Năng Mở Rộng](#-khả-năng-mở-rộng)
- [Yêu Cầu Hệ Thống](#-yêu-cầu-hệ-thống)
- [Cài Đặt](#-cài-đặt)
- [Cấu Hình](#-cấu-hình)
- [Chạy Demo](#-chạy-demo)
- [Triển Khai Production](#-triển-khai-production)
- [Tối Ưu Hiệu Năng](#-tối-ưu-hiệu-năng)
- [API Documentation](#-api-documentation)
- [Troubleshooting](#-troubleshooting)

## ⭐ Tính Năng

### Backend (Spring Boot 3.5)
- ✅ **Quản lý thiết bị IoT** - Thêm, sửa, xóa, xem danh sách thiết bị
- ✅ **MQTT Integration** - Điều khiển thiết bị real-time qua EMQX broker
- ✅ **Automation Engine** - Tự động hóa dựa trên điều kiện thời gian/cảm biến
- ✅ **Telemetry Storage** - Lưu trữ dữ liệu cảm biến với index tối ưu
- ✅ **Redis Caching** - Cache 5 phút cho API GET (tăng 22-31x tốc độ)
- ✅ **Connection Pooling** - HikariCP với 20 connections
- ✅ **Security** - Spring Security + JWT authentication (ready)

### Mobile App (Flutter)
- ✅ **Danh sách thiết bị** - Hiển thị real-time status
- ✅ **Điều khiển thiết bị** - Toggle on/off qua MQTT
- ✅ **Automation Management** - Tạo/sửa/xóa automation rules
- ✅ **Smart Polling** - 10s interval với debouncing (giảm 50% network load)
- ✅ **Cross-platform** - Android, iOS, Web, Linux, macOS

### Web Dashboard (ReactJS)
- ✅ **Real-time Dashboard** - Hiển thị trạng thái thiết bị và thống kê
- ✅ **Device Control** - Điều khiển thiết bị qua giao diện web
- ✅ **Automation Manager** - Tạo/chỉnh sửa/xóa automation schedules
- ✅ **Auto-refresh** - Cập nhật tự động mỗi 10 giây
- ✅ **Responsive Design** - Tương thích desktop, tablet, mobile browsers

### ESP32 Firmware (C/FreeRTOS)
- ✅ **MQTT Client** - Subscribe/publish device status
- ✅ **WiFi Manager** - Cấu hình mạng qua QR code
- ✅ **Relay Control** - Điều khiển thiết bị điện 220V
- ✅ **Physical Switch Support** - Đọc trạng thái công tắc vật lý và đồng bộ với app
- ✅ **OTA Updates** - Cập nhật firmware từ xa

## 🏗️ Kiến Trúc Hệ Thống

```
┌─────────────────┐       ┌─────────────────┐
│  Flutter App    │       │  React Web App  │ ←──── Clients
└────────┬────────┘       └────────┬────────┘
         │                         │
         └──────────┬──────────────┘
                    │ HTTP/REST
                    ↓
         ┌─────────────────┐
         │     Nginx       │ ←──── Reverse Proxy
         └────────┬────────┘
                  │
                  ↓
         ┌─────────────────┐      ┌──────────────┐
         │  Spring Boot    │←────→│    Redis     │ Cache Layer
         │    Backend      │      └──────────────┘
         └────────┬────────┘
                  │
         ┌────────┴─────┬──────────┐
         ↓              ↓          ↓
    ┌────────┐     ┌────────┐ ┌────────┐
    │PostgreSQL│     │  EMQX  │ │ESP32   │
    │Database │     │  MQTT  │ │Devices │
    └────────┘     └────────┘ └────────┘
```

### Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Backend | Spring Boot | 3.5.13 |
| Database | PostgreSQL | 15 |
| Cache | Redis | 7 |
| Message Broker | EMQX | latest |
| Mobile App | Flutter | 3.x |
| Web Dashboard | ReactJS | 18.2 |
| Firmware | ESP-IDF | 5.x |
| Proxy | Nginx | Alpine |

## 🚀 Khả Năng Mở Rộng

### 1. Database Scalability (PostgreSQL)

**Đã triển khai:**
- ✅ **Connection Pooling** - HikariCP 20 connections (có thể tăng lên 50-100)
- ✅ **Database Indexes** - 11 indexes trên các bảng chính
  ```sql
  -- backend/src/main/resources/db/migration/V3__add_performance_indexes.sql
  CREATE INDEX idx_devices_status_topic ON devices(status_topic);
  CREATE INDEX idx_telemetry_device_timestamp ON telemetry(device_id, timestamp DESC);
  CREATE INDEX idx_automations_type ON automations(type);
  ```

**Có thể mở rộng:**
- 📈 **Read Replicas** - Thêm PostgreSQL read replicas cho read-heavy workload
- 📈 **Partitioning** - Partition bảng `telemetry` theo `timestamp` (tháng/năm)
  ```sql
  -- Ví dụ partition theo tháng
  CREATE TABLE telemetry_2026_04 PARTITION OF telemetry
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
  ```
- 📈 **Sharding** - Shard theo `device_id` nếu có >10M devices

**Code chuẩn bị sẵn:**
```java
// application.properties - Có thể tăng pool size
spring.datasource.hikari.maximum-pool-size=20  // Tăng lên 50-100 cho production
spring.datasource.hikari.minimum-idle=5        // Tăng tương ứng
```

### 2. Cache Scalability (Redis)

**Đã triển khai:**
- ✅ **Redis Cache** - TTL 5 phút cho devices/automations
- ✅ **Jackson JSR310** - Serialize LocalDateTime objects
  ```java
  // backend/src/main/java/com/nguyenxuanviet/backend/config/CacheConfig.java
  ObjectMapper objectMapper = new ObjectMapper();
  objectMapper.registerModule(new JavaTimeModule());
  ```

**Có thể mở rộng:**
- 📈 **Redis Cluster** - Sharding data across multiple Redis nodes
- 📈 **Redis Sentinel** - High availability với automatic failover
- 📈 **Separate Cache Layers**:
  ```java
  @Cacheable(value = "devices", key = "#id")        // L1: Device cache
  @Cacheable(value = "telemetry", key = "#deviceId") // L2: Telemetry cache
  @Cacheable(value = "users", key = "#username")     // L3: User cache
  ```

**Code chuẩn bị sẵn:**
```properties
# application.properties - Dễ dàng chuyển sang Redis Cluster
spring.data.redis.cluster.nodes=redis1:6379,redis2:6379,redis3:6379
spring.data.redis.cluster.max-redirects=3
```

### 3. Application Scalability (Spring Boot)

**Đã triển khai:**
- ✅ **Stateless Design** - Không lưu session trong memory
- ✅ **Batch Operations** - Hibernate batch size 20
  ```properties
  # application.properties
  spring.jpa.properties.hibernate.jdbc.batch_size=20
  spring.jpa.properties.hibernate.order_inserts=true
  ```

**Có thể mở rộng:**
- 📈 **Horizontal Scaling** - Chạy nhiều backend instances với Docker Compose
  ```yaml
  # docker-compose.yml - Scale backend
  backend:
    deploy:
      replicas: 3  # 3 backend instances
  ```
- 📈 **Load Balancer** - Nginx round-robin cho multiple backends
- 📈 **Async Processing** - Spring @Async cho heavy tasks
  ```java
  @Async
  @CacheEvict(value = "telemetry", allEntries = true)
  public CompletableFuture<Void> processTelemetryBatch(List<Telemetry> batch) {
      // Process large telemetry batches asynchronously
  }
  ```

### 4. MQTT Scalability (EMQX)

**Đã triển khai:**
- ✅ **EMQX Broker** - Hỗ trợ 1M+ concurrent connections
- ✅ **Topic Structure** - Hierarchical topics
  ```
  device/{device_id}/status
  device/{device_id}/control
  ```

**Có thể mở rộng:**
- 📈 **EMQX Cluster** - Multiple EMQX nodes với shared subscriptions
- 📈 **Bridge to Cloud** - Bridge MQTT messages to AWS IoT / Azure IoT Hub
- 📈 **Rule Engine** - EMQX built-in rule engine để filter/transform messages

### 5. Mobile App Scalability (Flutter)

**Đã triển khai:**
- ✅ **Optimized Polling** - 10s interval (giảm 50% requests vs 5s)
- ✅ **Debouncing** - Prevent concurrent API calls
  ```dart
  // smart_home_app/lib/core/repositories/device_repository.dart
  if (_isRefreshing) return; // Debouncing
  _isRefreshing = true;
  ```

**Có thể mở rộng:**
- 📈 **WebSocket** - Real-time updates thay vì polling
- 📈 **Pagination** - Lazy loading cho danh sách thiết bị lớn
  ```dart
  // Ví dụ pagination
  Future<List<Device>> fetchDevices({int page = 0, int size = 20});
  ```
- 📈 **Local Database** - SQLite/Hive để cache offline

## 🖥️ Yêu Cầu Hệ Thống

### Phần Mềm
- **Docker** >= 20.10
- **Docker Compose** >= 2.0
- **Java** 21 (nếu build local)
- **Maven** >= 3.9 (nếu build local)
- **Flutter** >= 3.0 (để chạy mobile app)
- **Git**

### Phần Cứng
- **RAM**: Tối thiểu 4GB (khuyến nghị 8GB)
- **Disk**: 10GB trống
- **CPU**: 2 cores trở lên

### Hệ Điều Hành
- ✅ Linux (Ubuntu 20.04+, Debian 11+)
- ✅ macOS 11+
- ✅ Windows 10/11 với WSL2

## 📦 Cài Đặt

### Bước 1: Clone Repository

```bash
git clone <repository-url>
cd DoAnIoT
```

### Bước 2: Cấu Hình Environment (Tùy Chọn)

Tạo file `.env` nếu muốn override cấu hình mặc định:

```bash
# .env
POSTGRES_DB=smart_home_db
POSTGRES_USER=admin
POSTGRES_PASSWORD=admin123
REDIS_PASSWORD=
BACKEND_PORT=8093
NGINX_HTTP_PORT=80
NGINX_HTTPS_PORT=443
```

### Bước 3: Khởi Động Hệ Thống

```bash
./deploy.sh
```

Script sẽ tự động:
1. ✅ Build backend JAR với Maven
2. ✅ Build Docker image cho backend
3. ✅ Start tất cả services (PostgreSQL, Redis, EMQX, Backend, Nginx)
4. ✅ Chạy Flyway migrations
5. ✅ Verify services health

### Bước 4: Cài Đặt Flutter App (Mobile)

```bash
cd smart_home_app

# Cài dependencies
flutter pub get

# Chạy trên Android/iOS emulator
flutter run

# Hoặc build APK cho Android
flutter build apk --release

# Build cho Web
flutter build web
```

## ⚙️ Cấu Hình

### Backend Configuration

File: `backend/src/main/resources/application.properties`

```properties
# Database
spring.datasource.url=jdbc:postgresql://postgres:5432/smart_home_db
spring.datasource.username=admin
spring.datasource.password=admin123

# Redis Cache
spring.data.redis.host=redis
spring.data.redis.port=6379
spring.cache.redis.time-to-live=300000  # 5 minutes

# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000

# Hibernate
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# MQTT
mqtt.broker.url=tcp://emqx:1883
mqtt.client.id=backend-publisher
```

### Flutter App Configuration

File: `smart_home_app/lib/core/config/api_config.dart`

```dart
class ApiConfig {
  static const String baseUrl = 'http://localhost/api';
  static const Duration timeout = Duration(seconds: 30);
  static const Duration pollInterval = Duration(seconds: 10);
}
```

### MQTT Topics Structure

```
device/{device_id}/status    → Device publish status
device/{device_id}/control   → Backend publish commands
device/{device_id}/telemetry → Device publish sensor data
```

### Database Schema

Migrations tự động chạy khi khởi động:
- `V1__create_initial_schema.sql` - Tạo tables: devices, telemetry, automations
- `V2__add_automation_fields.sql` - Thêm fields cho automation
- `V3__add_performance_indexes.sql` - Tạo 11 indexes

## 🎬 Chạy Demo

### Demo 1: Quản Lý Thiết Bị Qua API

```bash
# 1. Tạo thiết bị mới
curl -X POST http://localhost/api/devices \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Living Room Light",
    "type": "light",
    "location": "Living Room",
    "statusTopic": "device/light001/status",
    "controlTopic": "device/light001/control"
  }'

# 2. Lấy danh sách thiết bị
curl http://localhost/api/devices

# 3. Bật/tắt thiết bị (gửi MQTT command)
curl -X POST http://localhost/api/devices/1/control \
  -H "Content-Type: application/json" \
  -d '{"action": "ON"}'

# 4. Xem performance (cached response)
time curl http://localhost/api/devices  # ~0.020s (cached)
```

### Demo 2: Automation Rules

```bash
# Tạo automation: Bật đèn lúc 18:00
curl -X POST http://localhost/api/automations \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Evening Light",
    "type": "automation",
    "icon": "lightbulb",
    "isActive": true,
    "triggerTime": "18:00:00",
    "actions": [
      {
        "deviceId": 1,
        "action": "ON"
      }
    ]
  }'

# Toggle automation on/off
curl -X PATCH http://localhost/api/automations/1/toggle
```

### Demo 3: Flutter Mobile App

1. **Khởi động app:**
   ```bash
   cd smart_home_app
   flutter run
   ```

2. **Thao tác trên app:**
   - 📱 Xem danh sách thiết bị (auto-refresh mỗi 10s)
   - 💡 Toggle thiết bị on/off
   - ⚙️ Tạo/chỉnh sửa automation
   - 📊 Xem telemetry data

3. **Test performance:**
   - Pull-to-refresh → Response ~0.020s (Redis cached)
   - Toggle switch → Instant MQTT command
   - Background polling → 50% ít hơn so với trước (10s vs 5s)

### Demo 4: Monitoring

```bash
# Xem Redis cache keys
docker exec doaniot-redis redis-cli KEYS "*"
# Output: automations::SimpleKey [], devices::SimpleKey []

# Monitor MQTT messages real-time
docker exec emqx emqx_ctl clients list

# Xem backend logs
docker logs doaniot-backend -f

# Xem database connections
docker exec doaniot-postgres psql -U admin -d smart_home_db \
  -c "SELECT count(*) FROM pg_stat_activity;"
```

## 🌐 Triển Khai Production

Hệ thống hỗ trợ 2 phương thức deploy production:

### Option 1: Cloud Server (AWS EC2) - Khuyến nghị cho production

Phù hợp nếu:
- ✅ Cần uptime cao (99.99%)
- ✅ Truy cập từ xa 24/7
- ✅ Nhiều người dùng đồng thời
- ✅ Băng thông ổn định

**📘 Hướng dẫn đầy đủ**: [DEPLOYMENT_AWS_EC2.md](DEPLOYMENT_AWS_EC2.md)

**Chi phí**: ~$22/tháng (EC2 t3.small + Storage + Data Transfer)

### Option 2: Local Server (Máy Cá Nhân) - Tiết kiệm chi phí

Phù hợp nếu:
- ✅ Chủ yếu dùng trong LAN (nhà/văn phòng)
- ✅ Muốn tiết kiệm chi phí (~$5-10/tháng tiền điện)
- ✅ Có internet ổn định với IP công khai
- ✅ Chấp nhận downtime khi tắt máy/mất điện

**📘 Hướng dẫn đầy đủ**: [DEPLOYMENT_LOCAL_SERVER.md](DEPLOYMENT_LOCAL_SERVER.md)

**Chi phí**: ~$5-10/tháng (chỉ tiền điện)

---

### Quick Start - AWS EC2

```bash
# 1. SSH vào EC2 instance
ssh -i your-key.pem ubuntu@<public-ip>

# 2. Clone repository
git clone <repository-url> DoAnIoT
cd DoAnIoT

# 3. Setup production environment
cat > .env << EOF
POSTGRES_PASSWORD=SuperSecurePassword123!
REDIS_PASSWORD=RedisSecurePass456!
EMQX_DASHBOARD_PASSWORD=EMQXAdminPass789!
EOF

# 4. Deploy
./deploy.sh

# 5. Setup SSL (với domain)
sudo certbot certonly --standalone -d yourdomain.com
```

### Production Features

- ✅ **SSL/HTTPS** - Let's Encrypt auto-renew
- ✅ **Security Groups** - AWS firewall configuration
- ✅ **Auto-restart** - Container restart policies
- ✅ **Backup Scripts** - Daily database backups
- ✅ **Monitoring** - System health checks
- ✅ **Performance Tuning** - Production-optimized settings

### Required Ports

| Port | Service | Access |
|------|---------|--------|
| 22 | SSH | Admin only |
| 80/443 | HTTP/HTTPS | Public |
| 1883 | MQTT | Public |
| 8083 | MQTT WebSocket | Public |

### Estimated Cost (AWS)

- **EC2 t3.small**: ~$15/month
- **Storage (20GB)**: ~$2/month
- **Data Transfer**: ~$5/month (estimate)
- **Total**: ~$22/month

📖 **Chi tiết cài đặt, cấu hình SSL, monitoring**: [DEPLOYMENT_AWS_EC2.md](DEPLOYMENT_AWS_EC2.md)

## ⚡ Tối Ưu Hiệu Năng

### Performance Metrics

| Operation | Before | After | Improvement |
|-----------|--------|-------|-------------|
| GET /api/automations (cached) | 0.701s | 0.031s | **22.6x faster** ✅ |
| GET /api/devices (cached) | 0.694s | 0.020s | **34.7x faster** ✅ |
| Flutter polling requests | Every 5s | Every 10s | **50% reduction** ✅ |
| Database queries | No indexes | 11 indexes | **2-5x faster** ✅ |

### Implemented Optimizations

1. ✅ **Redis Caching** - 5-minute TTL, Jackson JSR310 support
2. ✅ **Database Indexes** - 11 indexes trên 3 tables
3. ✅ **Connection Pooling** - HikariCP 20 max connections
4. ✅ **Hibernate Batching** - Batch size 20
5. ✅ **Flutter Debouncing** - Prevent concurrent requests
6. ✅ **Smart Polling** - 10s interval thay vì 5s

📖 **Chi tiết**: Xem [PERFORMANCE_OPTIMIZATIONS.md](PERFORMANCE_OPTIMIZATIONS.md)

## 📚 API Documentation

### Devices API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/devices` | Lấy danh sách thiết bị (cached) |
| GET | `/api/devices/{id}` | Lấy thiết bị theo ID |
| POST | `/api/devices` | Tạo thiết bị mới |
| PUT | `/api/devices/{id}` | Cập nhật thiết bị |
| DELETE | `/api/devices/{id}` | Xóa thiết bị |
| POST | `/api/devices/{id}/control` | Điều khiển thiết bị |

### Automations API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/automations` | Lấy danh sách automation (cached) |
| GET | `/api/automations/{id}` | Lấy automation theo ID |
| POST | `/api/automations` | Tạo automation mới |
| PUT | `/api/automations/{id}` | Cập nhật automation |
| DELETE | `/api/automations/{id}` | Xóa automation |
| PATCH | `/api/automations/{id}/toggle` | Bật/tắt automation |

### Example Responses

**GET /api/devices:**
```json
[
  {
    "id": 1,
    "name": "Living Room Light",
    "type": "light",
    "location": "Living Room",
    "status": "ONLINE",
    "isOn": true,
    "statusTopic": "device/light001/status",
    "controlTopic": "device/light001/control",
    "createdAt": "2026-04-07T10:00:00",
    "updatedAt": "2026-04-07T12:30:00"
  }
]
```

## 🔧 Troubleshooting

### Issue 1: Backend không start

**Triệu chứng:**
```bash
docker logs doaniot-backend
# Error: Connection refused to postgres
```

**Giải pháp:**
```bash
# Kiểm tra PostgreSQL đã chạy chưa
docker ps | grep postgres

# Restart services đúng thứ tự
docker-compose down
docker-compose up -d postgres redis emqx
sleep 10  # Đợi PostgreSQL ready
docker-compose up -d backend nginx
```

### Issue 2: Redis Cache không hoạt động

**Triệu chứng:**
```
SerializationException: Java 8 date/time type LocalDateTime not supported
```

**Giải pháp:**
Đã fix trong `CacheConfig.java`:
```java
ObjectMapper objectMapper = new ObjectMapper();
objectMapper.registerModule(new JavaTimeModule());
```

Verify cache hoạt động:
```bash
docker exec doaniot-redis redis-cli KEYS "*"
# Should show: automations::SimpleKey [], devices::SimpleKey []
```

### Issue 3: Flutter app không kết nối được backend

**Triệu chứng:**
```
SocketException: Connection refused
```

**Giải pháp:**
```dart
// Sửa baseUrl trong api_config.dart
// Android emulator
static const String baseUrl = 'http://10.0.2.2/api';

// iOS simulator
static const String baseUrl = 'http://localhost/api';

// Physical device (thay <your-ip>)
static const String baseUrl = 'http://<your-ip>/api';
```

### Issue 4: MQTT messages không nhận được

**Giải pháp:**
```bash
# Kiểm tra EMQX dashboard
http://localhost:18083
# Username: admin, Password: public

# Test MQTT với mqttx-web
http://localhost:8080
```

## 🛢️ Database Management

### Backup Database

```bash
# Backup
docker exec doaniot-postgres pg_dump -U admin smart_home_db > backup.sql

# Restore
docker exec -i doaniot-postgres psql -U admin smart_home_db < backup.sql
```

### Reset Database

```bash
docker-compose down -v  # Xóa volumes
docker-compose up -d    # Recreate với schema mới
```

## 🚦 Ports Used

| Service | Port | Description |
|---------|------|-------------|
| Nginx | 80 | HTTP reverse proxy |
| Nginx | 443 | HTTPS (nếu có SSL) |
| Backend | 8093 | Spring Boot (internal) |
| PostgreSQL | 5434 | Database (host access) |
| Redis | 6379 | Cache |
| EMQX MQTT | 1883 | MQTT broker |
| EMQX WebSocket | 8083 | MQTT over WebSocket |
| EMQX Dashboard | 18083 | Web UI |
| MQTTX Web | 8080 | MQTT client UI |

## 📊 System Monitoring

### Health Checks

```bash
# All services status
docker-compose ps

# Backend health
curl http://localhost/api/actuator/health

# Redis info
docker exec doaniot-redis redis-cli INFO stats

# PostgreSQL connections
docker exec doaniot-postgres psql -U admin -d smart_home_db \
  -c "SELECT * FROM pg_stat_activity;"
```

### Performance Monitoring

```bash
# Cache hit rate
docker exec doaniot-redis redis-cli INFO stats | grep keyspace

# Database query performance
docker exec doaniot-postgres psql -U admin -d smart_home_db \
  -c "SELECT * FROM pg_stat_user_tables;"

# Backend metrics (if actuator enabled)
curl http://localhost/api/actuator/metrics
```

## 🛑 Stop System

```bash
# Stop tất cả services
docker-compose down

# Stop và xóa volumes (reset data)
docker-compose down -v

# Stop và xóa images
docker-compose down --rmi all
```

## 📝 License

MIT License

## 👥 Contributors

- **Backend**: Spring Boot 3.5 + PostgreSQL + Redis + MQTT
- **Mobile**: Flutter 3.x cross-platform
- **Firmware**: ESP32 ESP-IDF 5.x
- **Infrastructure**: Docker Compose + Nginx

## 🔗 Resources

- [DEPLOYMENT_AWS_EC2.md](DEPLOYMENT_AWS_EC2.md) - Hướng dẫn triển khai lên AWS EC2 Ubuntu 22.04
- [DEPLOYMENT_LOCAL_SERVER.md](DEPLOYMENT_LOCAL_SERVER.md) - Hướng dẫn dùng máy local làm server (tiết kiệm chi phí)
- [PERFORMANCE_OPTIMIZATIONS.md](PERFORMANCE_OPTIMIZATIONS.md) - Chi tiết tối ưu hiệu năng
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Flutter Documentation](https://flutter.dev/docs)
- [EMQX Documentation](https://www.emqx.io/docs)
- [ESP-IDF Documentation](https://docs.espressif.com/projects/esp-idf)

---

**🎉 Deployment Success!** Hệ thống đã sẵn sàng mở rộng và hoạt động với hiệu năng cao.

Hệ thống IoT điều khiển nhà thông minh với ESP32, Spring Boot và Flutter.

## Kiến trúc

- **Backend**: Spring Boot 3.5 + PostgreSQL + MQTT (EMQX) + Redis Cache
- **Frontend**: Flutter Mobile App
- **Hardware**: ESP32 với 4 relay điều khiển đèn

## Performance Features

✅ **Redis Caching** - 31x faster API responses  
✅ **Database Indexes** - Optimized queries  
✅ **Connection Pooling** - HikariCP (20 connections)  
✅ **Smart Polling** - 10s intervals, debounced  
✅ **Batch Operations** - Hibernate batch inserts  

📊 See [PERFORMANCE_OPTIMIZATIONS.md](PERFORMANCE_OPTIMIZATIONS.md) for details

## Deploy

```bash
./deploy.sh
```

Script sẽ:
- Build backend (Maven)
- Start tất cả services (Docker Compose)
- Test health check backend

## Chạy Flutter App

```bash
cd smart_home_app
flutter run
```

## Services

- Backend API: `http://localhost/api`
- MQTT Broker: `localhost:1883`
- MQTTX Web: `http://localhost:8888`
- Redis Cache: `localhost:6379`

## Stop Services

```bash
docker-compose down
```
