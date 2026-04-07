# 🏠 Hướng Dẫn Sử Dụng Máy Local Làm Server

Tài liệu này hướng dẫn cách biến máy tính cá nhân (Linux/Windows/macOS) thành server để host hệ thống Smart Home IoT.

## 📋 Mục Lục

- [So Sánh Local Server vs Cloud Server](#so-sánh-local-server-vs-cloud-server)
- [Yêu Cầu Hệ Thống](#yêu-cầu-hệ-thống)
- [Công Việc Cần Làm](#công-việc-cần-làm)
- [Bước 1: Cấu Hình Mạng](#bước-1-cấu-hình-mạng)
- [Bước 2: Cài Đặt Dependencies](#bước-2-cài-đặt-dependencies)
- [Bước 3: Deploy Application](#bước-3-deploy-application)
- [Bước 4: Port Forwarding (Router)](#bước-4-port-forwarding-router)
- [Bước 5: Dynamic DNS](#bước-5-dynamic-dns)
- [Bước 6: SSL với Let's Encrypt](#bước-6-ssl-với-lets-encrypt)
- [Bước 7: Cấu Hình Auto-Start](#bước-7-cấu-hình-auto-start)
- [Monitoring và Maintenance](#monitoring-và-maintenance)
- [Troubleshooting](#troubleshooting)

## ⚖️ So Sánh Local Server vs Cloud Server

### ✅ Ưu Điểm Local Server

| Tiêu Chí | Local Server | Cloud Server |
|----------|--------------|--------------|
| **Chi phí** | ✅ MIỄN PHÍ (chỉ tốn điện ~$5-10/tháng) | ❌ $22-50/tháng |
| **Tốc độ Local** | ✅ Cực nhanh trong LAN | ⚠️ Phụ thuộc internet |
| **Quyền kiểm soát** | ✅ Toàn quyền phần cứng | ⚠️ Giới hạn bởi provider |
| **Dữ liệu riêng tư** | ✅ Không rời khỏi nhà | ⚠️ Lưu trên cloud |
| **Học tập** | ✅ Học networking thực tế | ⚠️ Ít control hơn |

### ❌ Nhược Điểm Local Server

| Vấn Đề | Local Server | Cloud Server |
|--------|--------------|--------------|
| **IP công khai** | ❌ IP động, cần DDNS | ✅ IP tĩnh miễn phí |
| **Uptime** | ❌ Tắt máy = mất kết nối | ✅ 99.99% uptime |
| **Điện** | ❌ Mất điện = offline | ✅ Backup power |
| **Bảo mật** | ⚠️ Cần cấu hình cẩn thận | ✅ Professional security |
| **Truy cập từ xa** | ⚠️ Phụ thuộc mạng nhà | ✅ Luôn sẵn sàng |
| **Băng thông upload** | ⚠️ Thường chậm (1-5 Mbps) | ✅ Cao (100+ Mbps) |

### 📊 Kết Luận

**Nên dùng Local Server nếu:**
- ✅ Chủ yếu sử dụng trong LAN (nhà/văn phòng)
- ✅ Muốn tiết kiệm chi phí
- ✅ Có internet ổn định với IP công khai
- ✅ Chấp nhận downtime khi tắt máy/mất điện
- ✅ Chỉ cần 1-5 người dùng đồng thời

**Nên dùng Cloud Server (AWS EC2) nếu:**
- ✅ Cần truy cập từ xa 24/7
- ✅ Nhiều người dùng đồng thời
- ✅ Yêu cầu uptime cao (>99%)
- ✅ Cần performance ổn định
- ✅ Quan tâm đến bảo mật chuyên nghiệp

## 🖥️ Yêu Cầu Hệ Thống

### Phần Cứng Tối Thiểu

| Component | Development | Production |
|-----------|-------------|------------|
| CPU | 2 cores | 4 cores |
| RAM | 4 GB | 8 GB |
| Disk | 20 GB | 50 GB |
| Network | 10 Mbps upload | 20+ Mbps upload |

### Hệ Điều Hành

- ✅ **Linux** (Ubuntu 20.04+, Debian 11+) - **KHUYẾN NGHỊ**
- ✅ Windows 10/11 với WSL2
- ✅ macOS 11+

### Mạng

- ✅ Router hỗ trợ Port Forwarding
- ✅ ISP cung cấp IP công khai (không phải CGNAT)
- ✅ Băng thông upload tối thiểu 5 Mbps

### Kiểm Tra IP Công Khai

```bash
# Lấy IP công khai
curl ifconfig.me

# Kiểm tra có phải CGNAT không
# Nếu IP bắt đầu bằng 100.64.x.x → CGNAT (không thể port forward)
# Liên hệ ISP để xin IP công khai
```

## 📝 Công Việc Cần Làm

### Checklist Tổng Quan

- [ ] **1. Cấu hình IP tĩnh cho máy local** (15 phút)
- [ ] **2. Cài đặt Docker + Docker Compose** (10 phút)
- [ ] **3. Deploy application** (5 phút)
- [ ] **4. Port Forwarding trên Router** (10 phút)
- [ ] **5. Setup Dynamic DNS (DDNS)** (15 phút)
- [ ] **6. Cấu hình SSL certificate** (20 phút)
- [ ] **7. Auto-start services khi boot** (5 phút)
- [ ] **8. Setup firewall** (10 phút)
- [ ] **9. Backup automation** (Optional - 10 phút)

**Tổng thời gian**: ~1.5 - 2 giờ

---

## 🌐 Bước 1: Cấu Hình Mạng

### 1.1. Gán IP Tĩnh Cho Máy Local

**Linux (Ubuntu/Debian):**

```bash
# Xem network interface name
ip addr show

# Giả sử interface là eth0 hoặc enp0s3
# Backup config cũ
sudo cp /etc/netplan/01-netcfg.yaml /etc/netplan/01-netcfg.yaml.bak

# Edit netplan config
sudo nano /etc/netplan/01-netcfg.yaml
```

Nội dung file (thay `192.168.1.100` bằng IP muốn đặt):

```yaml
network:
  version: 2
  renderer: networkd
  ethernets:
    eth0:                      # Thay bằng interface của bạn
      dhcp4: no
      addresses:
        - 192.168.1.100/24     # IP tĩnh cho máy
      gateway4: 192.168.1.1    # IP router
      nameservers:
        addresses:
          - 8.8.8.8
          - 8.8.4.4
```

Apply config:

```bash
sudo netplan apply

# Verify
ip addr show
```

**Windows:**

1. Control Panel → Network and Sharing Center
2. Change adapter settings → Right click adapter → Properties
3. Internet Protocol Version 4 (TCP/IPv4) → Properties
4. Chọn "Use the following IP address":
   ```
   IP address: 192.168.1.100
   Subnet mask: 255.255.255.0
   Default gateway: 192.168.1.1 (IP router)
   DNS: 8.8.8.8, 8.8.4.4
   ```

### 1.2. Kiểm Tra Kết Nối

```bash
# Ping router
ping 192.168.1.1

# Ping internet
ping 8.8.8.8

# Kiểm tra IP công khai
curl ifconfig.me
```

---

## 🐳 Bước 2: Cài Đặt Dependencies

### Linux (Ubuntu/Debian)

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install essentials
sudo apt install -y curl wget git vim

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add user to docker group
sudo usermod -aG docker $USER

# Logout and login again
exit

# Verify Docker
docker --version

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verify
docker-compose --version
```

### Windows (WSL2)

```powershell
# Install WSL2
wsl --install

# Install Docker Desktop for Windows
# Download from: https://www.docker.com/products/docker-desktop

# Enable WSL2 backend in Docker Desktop settings
```

### macOS

```bash
# Install Docker Desktop
# Download from: https://www.docker.com/products/docker-desktop

# Or use Homebrew
brew install --cask docker
```

---

## 🚀 Bước 3: Deploy Application

```bash
# Clone repository
cd ~
git clone <repository-url> DoAnIoT
cd DoAnIoT

# Create production .env
cat > .env << 'EOF'
# Database
POSTGRES_DB=smart_home_db
POSTGRES_USER=admin
POSTGRES_PASSWORD=YourStrongPassword123!  # ĐỔI PASSWORD!

# Redis
REDIS_PASSWORD=RedisPassword456!          # ĐỔI PASSWORD!

# Backend
BACKEND_PORT=8093
SPRING_PROFILES_ACTIVE=prod

# Nginx
NGINX_HTTP_PORT=80
NGINX_HTTPS_PORT=443

# EMQX
EMQX_DASHBOARD_PASSWORD=EmqxPassword789!  # ĐỔI PASSWORD!
EOF

# Set permissions
chmod 600 .env

# Deploy
./deploy.sh

# Verify all services running
docker ps

# Expected: 6 containers running
# - doaniot-backend
# - doaniot-postgres
# - doaniot-redis
# - emqx
# - doaniot-nginx
# - mqttx-web
```

**Test từ máy khác trong LAN:**

```bash
# Thay 192.168.1.100 bằng IP máy server
curl http://192.168.1.100/api/devices

# Hoặc mở browser: http://192.168.1.100
```

---

## 🔀 Bước 4: Port Forwarding (Router)

Để truy cập từ internet, cần mở ports trên router.

### 4.1. Truy Cập Router Admin

```bash
# Router thường có IP: 192.168.1.1 hoặc 192.168.0.1
# Mở browser: http://192.168.1.1

# Login với username/password (thường là admin/admin)
# Kiểm tra mặt sau router hoặc manual
```

### 4.2. Port Forwarding Configuration

Tùy router có tên khác nhau: **Port Forwarding**, **Virtual Server**, **NAT**, **Firewall**

**Cấu hình các ports sau:**

| Service Name | External Port | Internal Port | Internal IP | Protocol |
|--------------|---------------|---------------|-------------|----------|
| HTTP | 80 | 80 | 192.168.1.100 | TCP |
| HTTPS | 443 | 443 | 192.168.1.100 | TCP |
| MQTT | 1883 | 1883 | 192.168.1.100 | TCP |
| MQTT-WS | 8083 | 8083 | 192.168.1.100 | TCP |

**Lưu ý:**
- `Internal IP` = IP tĩnh của máy server (192.168.1.100)
- `External Port` = Port mở ra internet
- `Internal Port` = Port trên máy server

### 4.3. Verify Port Forwarding

Từ điện thoại (4G/tắt WiFi):

```bash
# Lấy IP công khai từ máy server
curl ifconfig.me
# Ví dụ: 123.45.67.89

# Từ điện thoại, test qua browser:
http://123.45.67.89/api/devices
```

Online tools:
- https://www.yougetsignal.com/tools/open-ports/
- https://canyouseeme.org/

---

## 🌍 Bước 5: Dynamic DNS (DDNS)

IP công khai của nhà thường thay đổi → cần DDNS để có domain cố định.

### 5.1. Tạo Free DDNS Domain

**Recommended Providers:**

1. **No-IP** (https://www.noip.com/)
   - Free: 3 hostnames
   - Phải confirm mỗi 30 ngày
   - Ví dụ: `yourhome.ddns.net`

2. **DuckDNS** (https://www.duckdns.org/)
   - Hoàn toàn free
   - Không cần confirm
   - Ví dụ: `yourhome.duckdns.org`

3. **Dynu** (https://www.dynu.com/)
   - Free: 4 hostnames
   - Không cần confirm
   - Ví dụ: `yourhome.dynu.net`

### 5.2. Setup DuckDNS (Recommended)

```bash
# 1. Truy cập https://www.duckdns.org/
# 2. Login bằng Google/GitHub
# 3. Tạo subdomain, ví dụ: mysmarthome
# 4. Copy token (ví dụ: abcd1234-efgh-5678-ijkl-9012mnop3456)

# 5. Install update script
mkdir -p ~/duckdns
cd ~/duckdns

cat > duck.sh << 'EOF'
#!/bin/bash
echo url="https://www.duckdns.org/update?domains=mysmarthome&token=abcd1234-efgh-5678-ijkl-9012mnop3456&ip=" | curl -k -o ~/duckdns/duck.log -K -
EOF

chmod 700 duck.sh

# Test script
./duck.sh
cat duck.log  # Should show "OK"

# 6. Add to crontab (update mỗi 5 phút)
crontab -e

# Thêm dòng này:
*/5 * * * * ~/duckdns/duck.sh >/dev/null 2>&1
```

### 5.3. Verify DDNS

```bash
# Ping domain
ping mysmarthome.duckdns.org

# Should trả về IP công khai của bạn

# Test API
curl http://mysmarthome.duckdns.org/api/devices
```

---

## 🔒 Bước 6: SSL với Let's Encrypt

### 6.1. Cài Đặt Certbot

```bash
# Ubuntu/Debian
sudo apt install -y certbot python3-certbot-nginx

# hoặc qua snap (recommended)
sudo snap install --classic certbot
sudo ln -s /snap/bin/certbot /usr/bin/certbot
```

### 6.2. Obtain SSL Certificate

```bash
# Stop nginx container tạm thời
docker stop doaniot-nginx

# Obtain certificate (thay domain)
sudo certbot certonly --standalone \
  -d mysmarthome.duckdns.org \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email

# Certificates sẽ được lưu tại:
# /etc/letsencrypt/live/mysmarthome.duckdns.org/fullchain.pem
# /etc/letsencrypt/live/mysmarthome.duckdns.org/privkey.pem
```

### 6.3. Update Nginx Config với SSL

```bash
cd ~/DoAnIoT

# Backup nginx config
cp nginx.conf nginx.conf.bak

# Update config
cat > nginx.conf << 'EOF'
events {
    worker_connections 1024;
}

http {
    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;

    upstream backend {
        server backend:8080;
    }

    # HTTP → HTTPS redirect
    server {
        listen 80;
        server_name mysmarthome.duckdns.org;  # ĐỔI DOMAIN!

        location /.well-known/acme-challenge/ {
            root /var/www/certbot;
        }

        location / {
            return 301 https://$server_name$request_uri;
        }
    }

    # HTTPS server
    server {
        listen 443 ssl http2;
        server_name mysmarthome.duckdns.org;  # ĐỔI DOMAIN!

        # SSL certificates
        ssl_certificate /etc/letsencrypt/live/mysmarthome.duckdns.org/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/mysmarthome.duckdns.org/privkey.pem;

        # SSL configuration
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;
        ssl_prefer_server_ciphers on;
        ssl_session_cache shared:SSL:10m;
        ssl_session_timeout 10m;

        # Security headers
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-XSS-Protection "1; mode=block" always;

        # Backend API
        location /api/ {
            limit_req zone=api_limit burst=20 nodelay;

            proxy_pass http://backend;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection 'upgrade';
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_cache_bypass $http_upgrade;

            proxy_connect_timeout 60s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
        }

        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
    }
}
EOF
```

### 6.4. Update docker-compose.yml với SSL Volumes

```bash
# Backup
cp docker-compose.yml docker-compose.yml.bak

# Update nginx service để mount SSL certificates
```

Thêm vào nginx service trong `docker-compose.yml`:

```yaml
nginx:
  image: nginx:alpine
  container_name: doaniot-nginx
  depends_on:
    - backend
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - ./nginx.conf:/etc/nginx/nginx.conf:ro
    - /etc/letsencrypt:/etc/letsencrypt:ro    # SSL certificates
    - /var/www/certbot:/var/www/certbot:ro    # ACME challenge
  networks:
    - iot-network
  restart: unless-stopped
```

### 6.5. Restart với SSL

```bash
# Restart containers
./deploy.sh

# Verify HTTPS
curl https://mysmarthome.duckdns.org/api/devices

# Test SSL certificate
curl -vI https://mysmarthome.duckdns.org 2>&1 | grep "SSL certificate verify ok"
```

### 6.6. Auto-Renew SSL Certificate

```bash
# Test renewal
sudo certbot renew --dry-run

# Add cron job cho auto-renew
sudo crontab -e

# Thêm dòng này (renew mỗi ngày lúc 2am)
0 2 * * * certbot renew --quiet --deploy-hook "docker restart doaniot-nginx"
```

---

## ⚙️ Bước 7: Cấu Hình Auto-Start

Đảm bảo services tự động start khi máy khởi động lại.

### 7.1. Docker Auto-Start

```bash
# Enable Docker service
sudo systemctl enable docker

# Verify
sudo systemctl is-enabled docker
# Output: enabled
```

### 7.2. Docker Compose Auto-Start

**Option 1: Systemd Service (Linux)**

```bash
# Create systemd service
sudo nano /etc/systemd/system/smarthome.service
```

Nội dung file:

```ini
[Unit]
Description=Smart Home IoT Docker Compose
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/home/viet/DoAnIoT  # Đổi thành đường dẫn của bạn
ExecStart=/usr/local/bin/docker-compose up -d
ExecStop=/usr/local/bin/docker-compose down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
```

Enable service:

```bash
# Reload systemd
sudo systemctl daemon-reload

# Enable auto-start
sudo systemctl enable smarthome.service

# Start service
sudo systemctl start smarthome.service

# Check status
sudo systemctl status smarthome.service

# Test reboot
sudo reboot

# Sau khi reboot, check containers
docker ps
```

**Option 2: Crontab (Fallback)**

```bash
crontab -e

# Thêm dòng này:
@reboot sleep 30 && cd /home/viet/DoAnIoT && /usr/local/bin/docker-compose up -d
```

---

## 🛡️ Bước 8: Cấu Hình Firewall

### Linux (UFW)

```bash
# Install UFW
sudo apt install -y ufw

# Default policies
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Allow SSH (CRITICAL - làm trước)
sudo ufw allow 22/tcp

# Allow web traffic
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS

# Allow MQTT
sudo ufw allow 1883/tcp  # MQTT
sudo ufw allow 8083/tcp  # MQTT WebSocket

# Optional: Limit SSH brute force
sudo ufw limit 22/tcp

# Enable firewall
sudo ufw enable

# Check status
sudo ufw status verbose
```

### Windows Firewall

1. Windows Defender Firewall → Advanced Settings
2. Inbound Rules → New Rule
3. Port → TCP → 80, 443, 1883, 8083
4. Allow the connection
5. Apply to Domain, Private, Public
6. Name: Smart Home IoT

---

## 📊 Monitoring và Maintenance

### Daily Monitoring Script

```bash
cat > ~/monitor.sh << 'EOF'
#!/bin/bash

echo "========================================="
echo "   Smart Home IoT System Monitor"
echo "   $(date)"
echo "========================================="

# Docker containers
echo -e "\n📦 Docker Containers:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Disk usage
echo -e "\n💾 Disk Usage:"
df -h / | tail -n 1

# Memory usage
echo -e "\n🧠 Memory Usage:"
free -h | grep Mem

# CPU load
echo -e "\n⚡ CPU Load:"
uptime

# Network connections
echo -e "\n🌐 Active Connections:"
netstat -tn 2>/dev/null | grep ESTABLISHED | wc -l

# Backend logs (last 5 errors)
echo -e "\n🔴 Recent Backend Errors:"
docker logs doaniot-backend --tail 100 2>&1 | grep -i error | tail -n 5

# Redis info
echo -e "\n📊 Redis Stats:"
docker exec doaniot-redis redis-cli INFO stats | grep -E "total_connections_received|total_commands_processed|keyspace_hits|keyspace_misses"

echo -e "\n========================================="
EOF

chmod +x ~/monitor.sh

# Run monitoring
~/monitor.sh
```

### Backup Script

```bash
cat > ~/backup.sh << 'EOF'
#!/bin/bash

BACKUP_DIR=~/backups
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=7

mkdir -p $BACKUP_DIR

echo "Starting backup at $(date)"

# Backup database
echo "Backing up PostgreSQL..."
docker exec doaniot-postgres pg_dump -U admin smart_home_db | gzip > $BACKUP_DIR/db_backup_$DATE.sql.gz

# Backup Redis
echo "Backing up Redis..."
docker exec doaniot-redis redis-cli BGSAVE
sleep 2
docker cp doaniot-redis:/data/dump.rdb $BACKUP_DIR/redis_backup_$DATE.rdb

# Backup .env
echo "Backing up configuration..."
cp ~/DoAnIoT/.env $BACKUP_DIR/.env_backup_$DATE

# Delete old backups
echo "Cleaning old backups..."
find $BACKUP_DIR -name "*.gz" -mtime +$RETENTION_DAYS -delete
find $BACKUP_DIR -name "*.rdb" -mtime +$RETENTION_DAYS -delete
find $BACKUP_DIR -name ".env_*" -mtime +$RETENTION_DAYS -delete

echo "Backup completed: $BACKUP_DIR"
ls -lh $BACKUP_DIR | tail -n 5
EOF

chmod +x ~/backup.sh

# Setup daily backup cron job
crontab -e

# Thêm dòng này (backup mỗi ngày lúc 3am)
0 3 * * * /home/viet/backup.sh >> /home/viet/backup.log 2>&1
```

---

## 🔧 Troubleshooting

### Issue 1: Không Truy Cập Được Từ Internet

**Kiểm tra:**

```bash
# 1. Port forwarding đã setup chưa?
# Login router → check port forwarding rules

# 2. Firewall có block không?
sudo ufw status
# Phải allow ports 80, 443, 1883, 8083

# 3. IP công khai có đúng không?
curl ifconfig.me
# So sánh với IP trong DDNS

# 4. Test từ bên ngoài
# Dùng điện thoại 4G (tắt WiFi)
curl http://mysmarthome.duckdns.org/api/devices

# 5. Check ISP có block ports không?
# Một số ISP block port 80/443 cho residential
# Giải pháp: Dùng port khác (8080, 8443) và forward
```

### Issue 2: DDNS Không Update IP

```bash
# Check DDNS update script
~/duckdns/duck.sh
cat ~/duckdns/duck.log
# Should show "OK"

# Check cron job đang chạy
crontab -l | grep duck

# Manual update
curl "https://www.duckdns.org/update?domains=mysmarthome&token=YOUR_TOKEN&ip="

# Check domain resolution
nslookup mysmarthome.duckdns.org
```

### Issue 3: SSL Certificate Renewal Failed

```bash
# Stop nginx
docker stop doaniot-nginx

# Manual renew
sudo certbot renew --standalone

# Check expiry
sudo certbot certificates

# Restart nginx
docker start doaniot-nginx
```

### Issue 4: Containers Không Auto-Start Sau Reboot

```bash
# Check systemd service
sudo systemctl status smarthome.service

# Check logs
sudo journalctl -u smarthome.service

# Restart service
sudo systemctl restart smarthome.service

# If failed, manual start
cd ~/DoAnIoT
docker-compose up -d
```

### Issue 5: High CPU/Memory Usage

```bash
# Check resource usage
docker stats

# Restart consuming container
docker restart doaniot-backend

# Check logs
docker logs doaniot-backend --tail 100

# If persistent, reduce polling in Flutter app
# Or increase swap memory
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

---

## 📱 Flutter App Configuration

### Cấu Hình Cho Local Server

```dart
// smart_home_app/lib/core/config/api_config.dart

class ApiConfig {
  // Local network (trong nhà)
  static const String baseUrlLocal = 'http://192.168.1.100/api';
  
  // Internet (từ bên ngoài)
  static const String baseUrlRemote = 'https://mysmarthome.duckdns.org/api';
  
  // Auto-detect
  static String get baseUrl {
    // Implement logic to detect local vs remote
    // For now, use remote URL for production
    return baseUrlRemote;
  }
  
  static const Duration timeout = Duration(seconds: 30);
  static const Duration pollInterval = Duration(seconds: 10);
}
```

### Build Production APK

```bash
cd smart_home_app

# Build release APK với production URL
flutter build apk --release \
  --dart-define=API_URL=https://mysmarthome.duckdns.org/api

# APK location: build/app/outputs/flutter-apk/app-release.apk

# Transfer to phone and install
```

---

## 💡 Best Practices

### Security

1. ✅ Đổi tất cả passwords mặc định trong `.env`
2. ✅ Enable UFW firewall, chỉ mở ports cần thiết
3. ✅ Không mở port 22 (SSH) ra internet
4. ✅ Enable SSL/HTTPS với Let's Encrypt
5. ✅ Setup fail2ban để chống brute force
6. ✅ Regular updates: `sudo apt update && sudo apt upgrade`
7. ✅ Backup database hàng ngày

### Performance

1. ✅ Monitor resource usage với `htop`
2. ✅ Setup swap nếu RAM < 8GB
3. ✅ Check logs định kỳ để phát hiện issues
4. ✅ Restart containers khi consume quá nhiều RAM
5. ✅ Dọn dẹp old Docker images: `docker system prune`

### Reliability

1. ✅ Setup UPS (Uninterruptible Power Supply) nếu có thể
2. ✅ Enable auto-start cho tất cả services
3. ✅ Monitor uptime với cron job
4. ✅ Setup email/SMS alerts khi service down
5. ✅ Backup data thường xuyên

---

## 📊 Monitoring với Uptime Robot (Free)

```bash
# 1. Đăng ký tài khoản tại: https://uptimerobot.com/
# 2. Add New Monitor:
#    - Monitor Type: HTTP(s)
#    - Friendly Name: Smart Home API
#    - URL: https://mysmarthome.duckdns.org/health
#    - Monitoring Interval: 5 minutes
# 3. Setup Alert Contacts (Email/SMS)
# 4. Nhận thông báo khi server down > 1 minute
```

---

## 🎯 Quick Reference Commands

### Start/Stop System

```bash
cd ~/DoAnIoT

# Start all services
./deploy.sh

# Stop all services
docker-compose down

# Restart specific service
docker-compose restart backend

# View logs
docker logs doaniot-backend -f
```

### Check Status

```bash
# Containers
docker ps

# System resources
htop

# Network connections
netstat -tlnp

# Firewall status
sudo ufw status

# DDNS update
~/duckdns/duck.sh && cat ~/duckdns/duck.log

# SSL certificate expiry
sudo certbot certificates
```

### Maintenance

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Clean Docker
docker system prune -a

# Backup now
~/backup.sh

# Check backups
ls -lh ~/backups/

# Monitor system
~/monitor.sh
```

---

## 💰 Chi Phí Ước Tính

| Item | Cost |
|------|------|
| Điện (~100W 24/7) | ~$5-10/tháng |
| Internet (đã có sẵn) | $0 |
| Domain (DDNS miễn phí) | $0 |
| SSL (Let's Encrypt) | $0 |
| **Total** | **~$5-10/tháng** |

**So với Cloud:**
- AWS EC2 t3.small: ~$22/tháng
- **Tiết kiệm**: $12-17/tháng (~$150-200/năm)

---

## ✅ Final Checklist

Sau khi setup xong, verify tất cả:

- [ ] ✅ Containers đang chạy: `docker ps`
- [ ] ✅ Truy cập local: `http://192.168.1.100/api/devices`
- [ ] ✅ Port forwarding: Test từ điện thoại 4G
- [ ] ✅ DDNS hoạt động: `ping mysmarthome.duckdns.org`
- [ ] ✅ HTTPS hoạt động: `https://mysmarthome.duckdns.org`
- [ ] ✅ SSL certificate valid: Không có warning
- [ ] ✅ Auto-start enabled: `sudo systemctl status smarthome`
- [ ] ✅ Firewall configured: `sudo ufw status`
- [ ] ✅ Backup cron job: `crontab -l`
- [ ] ✅ DDNS cron job: `crontab -l`
- [ ] ✅ SSL auto-renew: `sudo crontab -l`
- [ ] ✅ Flutter app kết nối được từ internet

---

## 🔗 Resources

- [Docker Documentation](https://docs.docker.com/)
- [Let's Encrypt](https://letsencrypt.org/)
- [DuckDNS](https://www.duckdns.org/)
- [No-IP](https://www.noip.com/)
- [UptimeRobot](https://uptimerobot.com/)

---

**🎉 Hoàn Thành!** Máy local của bạn đã trở thành server production!

**Lưu ý quan trọng:**
- Giữ máy luôn bật để có uptime cao
- Kiểm tra thường xuyên để phát hiện issues
- Backup data định kỳ
- Monitor uptime với UptimeRobot
- Cân nhắc UPS nếu vùng hay mất điện

Nếu gặp vấn đề, tham khảo phần Troubleshooting hoặc cân nhắc chuyển sang Cloud Server (AWS EC2) để có uptime tốt hơn.
