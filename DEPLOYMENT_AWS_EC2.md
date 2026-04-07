# 🚀 Hướng Dẫn Triển Khai Lên AWS EC2 Ubuntu Server 22.04

Tài liệu này hướng dẫn chi tiết cách deploy hệ thống Smart Home IoT lên AWS EC2 Ubuntu Server 22.04.

## 📋 Mục Lục

- [Yêu Cầu](#yêu-cầu)
- [Bước 1: Tạo và Cấu Hình EC2 Instance](#bước-1-tạo-và-cấu-hình-ec2-instance)
- [Bước 2: Kết Nối SSH và Cài Đặt Dependencies](#bước-2-kết-nối-ssh-và-cài-đặt-dependencies)
- [Bước 3: Cấu Hình Security Group](#bước-3-cấu-hình-security-group)
- [Bước 4: Deploy Application](#bước-4-deploy-application)
- [Bước 5: Cấu Hình Domain và SSL](#bước-5-cấu-hình-domain-và-ssl)
- [Bước 6: Monitoring và Maintenance](#bước-6-monitoring-và-maintenance)
- [Troubleshooting](#troubleshooting)

## 🔧 Yêu Cầu

### EC2 Instance Specifications

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| Instance Type | t3.small | t3.medium |
| vCPU | 2 | 2 |
| RAM | 2 GB | 4 GB |
| Storage | 20 GB | 30 GB |
| OS | Ubuntu Server 22.04 LTS | Ubuntu Server 22.04 LTS |

### Chi Phí Ước Tính

- **t3.small**: ~$15/tháng (2 vCPU, 2GB RAM)
- **t3.medium**: ~$30/tháng (2 vCPU, 4GB RAM)
- **Storage**: ~$2/tháng cho 20GB
- **Data Transfer**: Free tier 1GB/tháng

### Ports Cần Mở

| Port | Service | Protocol | Description |
|------|---------|----------|-------------|
| 22 | SSH | TCP | Remote access |
| 80 | HTTP | TCP | Web access |
| 443 | HTTPS | TCP | Secure web access |
| 1883 | MQTT | TCP | MQTT broker |
| 8083 | MQTT-WS | TCP | MQTT WebSocket |
| 18083 | EMQX Dashboard | TCP | EMQX management (optional) |

## 📦 Bước 1: Tạo và Cấu Hình EC2 Instance

### 1.1. Tạo EC2 Instance

1. **Đăng nhập AWS Console** → EC2 → Launch Instance

2. **Cấu hình instance:**
   ```
   Name: smart-home-iot-server
   AMI: Ubuntu Server 22.04 LTS (HVM), SSD Volume Type
   Instance type: t3.small hoặc t3.medium
   Key pair: Tạo mới hoặc chọn existing key pair
   ```

3. **Network settings:**
   - VPC: Default VPC
   - Subnet: Chọn availability zone bất kỳ
   - Auto-assign public IP: Enable
   - Firewall (security groups): Create new security group

4. **Configure storage:**
   ```
   Volume 1 (root): 20 GB gp3
   ```

5. **Launch instance** và đợi status = "Running"

### 1.2. Lấy Public IP

```bash
# Trong EC2 Dashboard, lấy IPv4 Public của instance
# Ví dụ: 54.123.45.67
```

### 1.3. Tải Key Pair

```bash
# Trên máy local, đặt permissions cho key file
chmod 400 ~/Downloads/smart-home-iot-key.pem
```

## 🔌 Bước 2: Kết Nối SSH và Cài Đặt Dependencies

### 2.1. SSH vào Server

```bash
# Thay <your-key.pem> và <public-ip>
ssh -i ~/Downloads/smart-home-iot-key.pem ubuntu@<public-ip>

# Ví dụ:
ssh -i ~/Downloads/smart-home-iot-key.pem ubuntu@54.123.45.67
```

### 2.2. Update System

```bash
# Update package list
sudo apt update && sudo apt upgrade -y

# Install essential packages
sudo apt install -y \
    curl \
    wget \
    git \
    vim \
    htop \
    net-tools \
    ufw
```

### 2.3. Cài Đặt Docker

```bash
# Remove old versions (nếu có)
sudo apt remove docker docker-engine docker.io containerd runc

# Install Docker using official script
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add user to docker group (không cần sudo)
sudo usermod -aG docker ubuntu

# Logout và login lại để apply group
exit
# SSH lại vào server
ssh -i ~/Downloads/smart-home-iot-key.pem ubuntu@<public-ip>

# Verify Docker installation
docker --version
# Output: Docker version 25.x.x
```

### 2.4. Cài Đặt Docker Compose

```bash
# Download Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# Make executable
sudo chmod +x /usr/local/bin/docker-compose

# Verify installation
docker-compose --version
# Output: Docker Compose version v2.x.x
```

### 2.5. Cài Đặt Git và Clone Repository

```bash
# Git đã được cài ở bước 2.2
git --version

# Clone repository
cd ~
git clone <your-repository-url> DoAnIoT
cd DoAnIoT

# Hoặc upload code qua SCP từ máy local:
# scp -i ~/Downloads/smart-home-iot-key.pem -r /path/to/DoAnIoT ubuntu@<public-ip>:~/
```

## 🔐 Bước 3: Cấu Hình Security Group

### 3.1. Từ AWS Console

1. **EC2 Dashboard** → Security Groups → Chọn security group của instance

2. **Edit inbound rules** → Add các rules sau:

| Type | Protocol | Port Range | Source | Description |
|------|----------|------------|--------|-------------|
| SSH | TCP | 22 | My IP | SSH access |
| HTTP | TCP | 80 | 0.0.0.0/0 | HTTP web access |
| HTTPS | TCP | 443 | 0.0.0.0/0 | HTTPS web access |
| Custom TCP | TCP | 1883 | 0.0.0.0/0 | MQTT broker |
| Custom TCP | TCP | 8083 | 0.0.0.0/0 | MQTT WebSocket |
| Custom TCP | TCP | 18083 | My IP | EMQX Dashboard (optional) |

3. **Save rules**

### 3.2. Hoặc Dùng AWS CLI

```bash
# Lấy security group ID
aws ec2 describe-instances --instance-ids <instance-id> \
  --query 'Reservations[0].Instances[0].SecurityGroups[0].GroupId'

# Add inbound rules
aws ec2 authorize-security-group-ingress \
  --group-id <sg-id> \
  --ip-permissions \
    IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges='[{CidrIp=0.0.0.0/0}]' \
    IpProtocol=tcp,FromPort=443,ToPort=443,IpRanges='[{CidrIp=0.0.0.0/0}]' \
    IpProtocol=tcp,FromPort=1883,ToPort=1883,IpRanges='[{CidrIp=0.0.0.0/0}]' \
    IpProtocol=tcp,FromPort=8083,ToPort=8083,IpRanges='[{CidrIp=0.0.0.0/0}]'
```

### 3.3. Cấu Hình UFW (Ubuntu Firewall)

```bash
# Enable UFW
sudo ufw enable

# Allow SSH (CRITICAL - làm trước khi enable ufw)
sudo ufw allow 22/tcp

# Allow application ports
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 1883/tcp  # MQTT
sudo ufw allow 8083/tcp  # MQTT WS

# Check status
sudo ufw status verbose

# Output:
# Status: active
# To                         Action      From
# --                         ------      ----
# 22/tcp                     ALLOW       Anywhere
# 80/tcp                     ALLOW       Anywhere
# 443/tcp                    ALLOW       Anywhere
# 1883/tcp                   ALLOW       Anywhere
# 8083/tcp                   ALLOW       Anywhere
```

## 🚀 Bước 4: Deploy Application

### 4.1. Cấu Hình Environment Variables

```bash
cd ~/DoAnIoT

# Tạo file .env cho production
cat > .env << 'EOF'
# Database Configuration
POSTGRES_DB=smart_home_db
POSTGRES_USER=admin
POSTGRES_PASSWORD=SuperSecurePassword123!  # ĐỔI PASSWORD MẠN H!

# Redis Configuration
REDIS_PASSWORD=RedisSecurePass456!         # ĐỔI PASSWORD MẠNH!

# Backend Configuration
BACKEND_PORT=8093
SPRING_PROFILES_ACTIVE=prod

# Nginx Configuration
NGINX_HTTP_PORT=80
NGINX_HTTPS_PORT=443

# MQTT Configuration
EMQX_DASHBOARD_PASSWORD=EMQXAdminPass789!  # ĐỔI PASSWORD MẠNH!
EOF

# Set permissions
chmod 600 .env
```

### 4.2. Tùy Chỉnh application.properties cho Production

```bash
# Backup original config
cp backend/src/main/resources/application.properties \
   backend/src/main/resources/application.properties.bak

# Thêm production overrides
cat >> backend/src/main/resources/application.properties << 'EOF'

# Production Configuration
spring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}

# Production database (nếu dùng RDS)
# spring.datasource.url=jdbc:postgresql://<rds-endpoint>:5432/smart_home_db

# Production Redis (nếu dùng ElastiCache)
# spring.data.redis.host=<elasticache-endpoint>
# spring.data.redis.port=6379

# Actuator cho monitoring
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
EOF
```

### 4.3. Build và Deploy

```bash
cd ~/DoAnIoT

# Make deploy script executable
chmod +x deploy.sh

# Deploy application
./deploy.sh

# Script sẽ tự động:
# - Build backend JAR
# - Build Docker images
# - Start all services
# - Run database migrations
```

### 4.4. Verify Deployment

```bash
# Check all containers running
docker ps

# Expected output: 6 containers
# - doaniot-backend
# - doaniot-postgres
# - doaniot-redis
# - emqx
# - doaniot-nginx
# - mqttx-web

# Test backend API
curl http://localhost/api/devices

# Test from external (thay <public-ip>)
curl http://<public-ip>/api/devices

# Check logs
docker logs doaniot-backend --tail 50
docker logs doaniot-nginx --tail 50
```

## 🌐 Bước 5: Cấu Hình Domain và SSL

### 5.1. Trỏ Domain về EC2

**Option A: Route 53 (AWS DNS)**

1. Route 53 Dashboard → Hosted zones → Create hosted zone
2. Domain name: `yourdomain.com`
3. Create Record:
   ```
   Type: A
   Name: @ (hoặc subdomain như "iot")
   Value: <ec2-public-ip>
   TTL: 300
   ```

**Option B: External DNS Provider**

1. Vào DNS management của domain provider
2. Tạo A Record:
   ```
   Host: @ hoặc iot
   Points to: <ec2-public-ip>
   TTL: 3600
   ```

### 5.2. Cài Đặt SSL với Let's Encrypt

```bash
# Install Certbot
sudo apt install -y certbot python3-certbot-nginx

# Stop nginx container tạm thời
docker stop doaniot-nginx

# Obtain SSL certificate
sudo certbot certonly --standalone \
  -d yourdomain.com \
  -d www.yourdomain.com \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email

# Certificates sẽ được lưu tại:
# /etc/letsencrypt/live/yourdomain.com/fullchain.pem
# /etc/letsencrypt/live/yourdomain.com/privkey.pem
```

### 5.3. Cấu Hình Nginx với SSL

```bash
cd ~/DoAnIoT

# Backup nginx config
cp nginx.conf nginx.conf.bak

# Update nginx.conf
cat > nginx.conf << 'EOF'
events {
    worker_connections 1024;
}

http {
    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;

    # Upstream backend
    upstream backend {
        server backend:8080;
    }

    # HTTP → HTTPS redirect
    server {
        listen 80;
        server_name yourdomain.com www.yourdomain.com;  # ĐỔI DOMAIN!

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
        server_name yourdomain.com www.yourdomain.com;  # ĐỔI DOMAIN!

        # SSL certificates
        ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

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

            # Timeouts
            proxy_connect_timeout 60s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
        }

        # Health check endpoint
        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
    }
}
EOF
```

### 5.4. Update docker-compose.yml với SSL

```bash
# Backup docker-compose.yml
cp docker-compose.yml docker-compose.yml.bak

# Thêm SSL volumes vào nginx service
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: doaniot-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-smart_home_db}
      POSTGRES_USER: ${POSTGRES_USER:-admin}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-admin123}
    ports:
      - "5434:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - iot-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-admin}"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: doaniot-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - iot-network
    command: redis-server --appendonly yes

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: doaniot-backend
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-smart_home_db}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-admin}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-admin123}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      MQTT_BROKER_URL: tcp://emqx:1883
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
    ports:
      - "${BACKEND_PORT:-8093}:8080"
    networks:
      - iot-network
    restart: unless-stopped

  emqx:
    image: emqx/emqx:latest
    container_name: emqx
    ports:
      - "1883:1883"      # MQTT
      - "8083:8083"      # WebSocket
      - "18083:18083"    # Dashboard
    environment:
      EMQX_NAME: emqx
      EMQX_HOST: 127.0.0.1
      EMQX_DASHBOARD__DEFAULT_PASSWORD: ${EMQX_DASHBOARD_PASSWORD:-public}
    volumes:
      - emqx_data:/opt/emqx/data
      - emqx_log:/opt/emqx/log
    networks:
      - iot-network
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    container_name: doaniot-nginx
    depends_on:
      - backend
    ports:
      - "${NGINX_HTTP_PORT:-80}:80"
      - "${NGINX_HTTPS_PORT:-443}:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro
      - /var/www/certbot:/var/www/certbot:ro
    networks:
      - iot-network
    restart: unless-stopped

  mqttx-web:
    image: emqx/mqttx-web
    container_name: mqttx-web
    ports:
      - "8080:80"
    networks:
      - iot-network
    restart: unless-stopped

networks:
  iot-network:
    driver: bridge

volumes:
  postgres_data:
  redis_data:
  emqx_data:
  emqx_log:
EOF
```

### 5.5. Restart với SSL

```bash
# Rebuild và restart
./deploy.sh

# Verify HTTPS
curl https://yourdomain.com/api/devices

# Test SSL certificate
curl -vI https://yourdomain.com 2>&1 | grep "SSL certificate verify ok"
```

### 5.6. Auto-Renew SSL Certificate

```bash
# Certbot cron job (auto-renew)
sudo crontab -e

# Thêm dòng này (renew mỗi ngày lúc 2am)
0 2 * * * certbot renew --quiet --deploy-hook "docker restart doaniot-nginx"
```

## 📊 Bước 6: Monitoring và Maintenance

### 6.1. Setup CloudWatch Logs (Optional)

```bash
# Install CloudWatch agent
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i -E ./amazon-cloudwatch-agent.deb

# Configure CloudWatch agent
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-config-wizard

# Start agent
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
    -a fetch-config \
    -m ec2 \
    -s \
    -c file:/opt/aws/amazon-cloudwatch-agent/bin/config.json
```

### 6.2. System Monitoring Scripts

```bash
# Tạo script monitoring
cat > ~/monitor.sh << 'EOF'
#!/bin/bash

echo "=== Docker Containers Status ==="
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo -e "\n=== Disk Usage ==="
df -h | grep -E "Filesystem|/$"

echo -e "\n=== Memory Usage ==="
free -h

echo -e "\n=== CPU Load ==="
uptime

echo -e "\n=== Backend Logs (last 10 lines) ==="
docker logs doaniot-backend --tail 10

echo -e "\n=== Redis Memory ==="
docker exec doaniot-redis redis-cli INFO memory | grep human

echo -e "\n=== Cache Keys Count ==="
docker exec doaniot-redis redis-cli DBSIZE
EOF

chmod +x ~/monitor.sh

# Chạy monitoring
~/monitor.sh
```

### 6.3. Backup Script

```bash
# Tạo backup script
cat > ~/backup.sh << 'EOF'
#!/bin/bash

BACKUP_DIR=~/backups
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

echo "Backing up database..."
docker exec doaniot-postgres pg_dump -U admin smart_home_db | gzip > $BACKUP_DIR/db_backup_$DATE.sql.gz

echo "Backing up redis..."
docker exec doaniot-redis redis-cli BGSAVE
docker cp doaniot-redis:/data/dump.rdb $BACKUP_DIR/redis_backup_$DATE.rdb

echo "Backing up .env..."
cp ~/DoAnIoT/.env $BACKUP_DIR/.env_backup_$DATE

echo "Backup completed: $BACKUP_DIR"

# Delete backups older than 7 days
find $BACKUP_DIR -name "*.gz" -mtime +7 -delete
find $BACKUP_DIR -name "*.rdb" -mtime +7 -delete
EOF

chmod +x ~/backup.sh

# Setup cron job (backup mỗi ngày lúc 3am)
crontab -e
# Thêm dòng:
0 3 * * * /home/ubuntu/backup.sh >> /home/ubuntu/backup.log 2>&1
```

### 6.4. Auto-restart on Failure

```bash
# docker-compose.yml đã có "restart: unless-stopped"
# Kiểm tra:
docker inspect doaniot-backend | grep -A 5 RestartPolicy
```

### 6.5. System Updates

```bash
# Tạo update script
cat > ~/update.sh << 'EOF'
#!/bin/bash

echo "Pulling latest code..."
cd ~/DoAnIoT
git pull origin main

echo "Rebuilding and restarting..."
./deploy.sh

echo "Update completed!"
docker ps
EOF

chmod +x ~/update.sh
```

## 🔧 Troubleshooting

### Issue 1: Cannot Connect to EC2

**Symptom:**
```bash
ssh: connect to host <public-ip> port 22: Connection timed out
```

**Solutions:**
1. Check Security Group có allow port 22 từ IP của bạn
2. Check instance đang running
3. Check key pair đúng
4. Check UFW: `sudo ufw status`

### Issue 2: Docker Permission Denied

**Symptom:**
```bash
docker: Got permission denied while trying to connect to the Docker daemon socket
```

**Solution:**
```bash
# Add user to docker group
sudo usermod -aG docker ubuntu

# Logout và login lại
exit
ssh -i ~/key.pem ubuntu@<public-ip>

# Verify
docker ps
```

### Issue 3: Out of Memory

**Symptom:**
```bash
docker logs doaniot-backend
# Java heap space error
```

**Solution:**
```bash
# Tăng swap space
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# Make permanent
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Verify
free -h
```

### Issue 4: SSL Certificate Renewal Failed

**Symptom:**
```bash
certbot renew
# Error: Failed to renew certificate
```

**Solution:**
```bash
# Stop nginx
docker stop doaniot-nginx

# Renew manually
sudo certbot renew --standalone

# Restart nginx
docker start doaniot-nginx
```

### Issue 5: High CPU Usage

**Check:**
```bash
# Top processes
htop

# Docker stats
docker stats

# Check logs
docker logs doaniot-backend --tail 100 | grep ERROR
```

**Solutions:**
- Tăng cache TTL trong application.properties
- Scale database connection pool
- Optimize indexes trong PostgreSQL

## 📈 Performance Tuning

### Database Optimization

```bash
# Connect to PostgreSQL
docker exec -it doaniot-postgres psql -U admin -d smart_home_db

-- Check slow queries
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- Analyze tables
ANALYZE devices;
ANALYZE telemetry;
ANALYZE automations;

-- Vacuum
VACUUM ANALYZE;
```

### Redis Optimization

```bash
# Check memory usage
docker exec doaniot-redis redis-cli INFO memory

# Check hit rate
docker exec doaniot-redis redis-cli INFO stats | grep keyspace

# If cache too large, reduce TTL in application.properties
spring.cache.redis.time-to-live=180000  # 3 minutes instead of 5
```

## 🔒 Security Checklist

- [x] Đổi tất cả passwords mặc định trong .env
- [x] Enable UFW firewall
- [x] Configure Security Group chỉ mở ports cần thiết
- [x] SSH chỉ từ IP cụ thể (không phải 0.0.0.0/0)
- [x] Enable SSL/HTTPS với Let's Encrypt
- [x] Setup auto-renew cho SSL certificate
- [x] Disable EMQX dashboard public access (chỉ My IP)
- [x] Enable rate limiting trong Nginx
- [x] Regular security updates: `sudo apt update && sudo apt upgrade`
- [x] Setup CloudWatch alerts cho anomalies
- [ ] Enable Spring Security JWT authentication (TODO)
- [ ] Setup fail2ban cho brute force protection
- [ ] Enable database encryption at rest

## 📱 Flutter App Configuration

Update Flutter app để kết nối tới production server:

```dart
// smart_home_app/lib/core/config/api_config.dart

class ApiConfig {
  // Development
  // static const String baseUrl = 'http://localhost/api';
  
  // Production - thay yourdomain.com
  static const String baseUrl = 'https://yourdomain.com/api';
  
  static const Duration timeout = Duration(seconds: 30);
  static const Duration pollInterval = Duration(seconds: 10);
}
```

Build APK cho production:
```bash
cd smart_home_app

# Build release APK
flutter build apk --release --dart-define=API_URL=https://yourdomain.com/api

# APK location: build/app/outputs/flutter-apk/app-release.apk
```

## 🎯 Quick Reference

### Connect to Server
```bash
ssh -i ~/key.pem ubuntu@<public-ip>
```

### Check Logs
```bash
docker logs doaniot-backend -f
docker logs doaniot-nginx -f
docker logs emqx -f
```

### Restart Services
```bash
./deploy.sh                      # Full rebuild and restart
docker-compose restart backend   # Restart backend only
docker-compose restart nginx     # Restart nginx only
```

### Check Status
```bash
~/monitor.sh          # System monitoring
docker ps             # All containers
docker stats          # Resource usage
sudo ufw status       # Firewall rules
```

### Backup/Restore
```bash
~/backup.sh                                    # Manual backup
~/restore.sh /path/to/db_backup_DATE.sql.gz   # Restore backup
```

## 📞 Support

Nếu gặp vấn đề:
1. Check logs: `docker logs <container-name>`
2. Verify network: `docker network inspect iot-network`
3. Check ports: `sudo netstat -tlnp`
4. Review security groups trong AWS Console
5. Check system resources: `htop`, `df -h`

---

**✅ Deployment Complete!** Hệ thống đã sẵn sàng phục vụ production trên AWS EC2.

🔗 Access points:
- **API**: https://yourdomain.com/api
- **MQTT**: mqtt://yourdomain.com:1883
- **EMQX Dashboard**: http://<public-ip>:18083 (admin/your-password)
