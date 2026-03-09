# Auth Service Deployment Guide

Complete guide for deploying Auth Service to production environments.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Environment Setup](#environment-setup)
- [Docker Deployment](#docker-deployment)
- [Kubernetes Deployment](#kubernetes-deployment)
- [OAuth2 Application Setup](#oauth2-application-setup)
- [Key Management](#key-management)
- [Database Migrations](#database-migrations)
- [Health Checks](#health-checks)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### System Requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| CPU | 1 core | 2 cores |
| Memory | 512 MB | 1 GB |
| Disk | 100 MB | 500 MB |
| Database | PostgreSQL 15+ | PostgreSQL 15+ with replication |
| Network | Outbound HTTPS | Outbound HTTPS + OAuth2 provider access |

### Software Requirements

- **Docker** 20.10+ (for container deployment)
- **kubectl** 1.25+ (for Kubernetes deployment)
- **PostgreSQL Client** (for database management)
- **OpenSSL** (for certificate management)

### Network Requirements

**Outbound Access:**

- OAuth2 Providers:
  - `accounts.google.com` (Google OAuth2)
  - `github.com` (GitHub OAuth2)
- SMTP Server (for password reset emails)

**Inbound Access:**

- Health checks from load balancer/orchestrator
- API Gateway access to JWKS endpoint (`/.well-known/jwks.json`)

---

## Environment Setup

### Environment Variables

Create a `.env` file or configure in your deployment platform:

```bash
# Application Profile
SPRING_PROFILES_ACTIVE=prod

# Database Configuration
DB_HOST=postgres.example.com
DB_PORT=5432
DB_NAME=authdb
DB_USER=authuser
DB_PASSWORD=your_secure_password_here

# OAuth2 Configuration
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret

# Frontend URL (for OAuth2 redirects)
FRONTEND_URL=https://app.example.com

# Email Configuration (optional, for password reset)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-specific-password

# JWT Configuration
JWT_ACCESS_TOKEN_EXPIRY=1800  # 30 minutes
JWT_REFRESH_TOKEN_EXPIRY=604800  # 7 days
```

### Secret Management

**Production Best Practices:**

1. Use a secret manager (AWS Secrets Manager, Azure Key Vault, HashiCorp Vault)
2. Never commit secrets to version control
3. Rotate secrets regularly
4. Use different secrets for different environments

**Example: AWS Secrets Manager**

```bash
# Store database password
aws secretsmanager create-secret \
  --name auth-service/db-password \
  --secret-string "your_secure_password"

# Store OAuth2 secrets
aws secretsmanager create-secret \
  --name auth-service/oauth2 \
  --secret-string '{"google_client_id":"xxx","google_client_secret":"yyy"}'
```

---

## Docker Deployment

### Build Docker Image

```bash
# Navigate to auth-service directory
cd auth-service

# Build JAR
mvn clean package -DskipTests

# Build Docker image
docker build -t auth-service:1.0.0 .

# Tag for registry
docker tag auth-service:1.0.0 registry.example.com/auth-service:1.0.0

# Push to registry
docker push registry.example.com/auth-service:1.0.0
```

### Docker Compose Deployment

Create `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  auth-service:
    image: registry.example.com/auth-service:1.0.0
    container_name: auth-service
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=authdb
      - DB_USER=authuser
      - DB_PASSWORD=${DB_PASSWORD}
      - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
      - GITHUB_CLIENT_ID=${GITHUB_CLIENT_ID}
      - GITHUB_CLIENT_SECRET=${GITHUB_CLIENT_SECRET}
      - FRONTEND_URL=${FRONTEND_URL}
    volumes:
      - ./keys:/opt/auth-service/keys:rw
      - ./logs:/app/logs:rw
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    restart: unless-stopped
    networks:
      - auth-network
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

  postgres:
    image: postgres:15-alpine
    container_name: auth-postgres
    environment:
      - POSTGRES_DB=authdb
      - POSTGRES_USER=authuser
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d:ro
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U authuser -d authdb"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    networks:
      - auth-network
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

volumes:
  pgdata:
    driver: local

networks:
  auth-network:
    driver: bridge
```

### Deploy with Docker Compose

```bash
# Create environment file
cat > .env << EOF
DB_PASSWORD=your_secure_password
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
FRONTEND_URL=https://app.example.com
EOF

# Create keys directory
mkdir -p keys logs

# Set proper permissions
chmod 700 keys

# Deploy
docker-compose -f docker-compose.prod.yml up -d

# Check status
docker-compose -f docker-compose.prod.yml ps

# View logs
docker-compose -f docker-compose.prod.yml logs -f
```

### Docker Health Check

```bash
# Check container health
docker inspect auth-service | jq '.[0].State.Health'

# Check service health
curl http://localhost:8080/actuator/health | jq .

# Check JWKS endpoint
curl http://localhost:8080/.well-known/jwks.json | jq .
```

---

## Kubernetes Deployment

### Namespace

```bash
# Create namespace
kubectl create namespace auth-service

# Set as default namespace
kubectl config set-context --current --namespace=auth-service
```

### ConfigMap

Create `configmap.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: auth-service-config
  namespace: auth-service
data:
  SPRING_PROFILES_ACTIVE: "prod"
  DB_HOST: "postgres-service"
  DB_PORT: "5432"
  DB_NAME: "authdb"
  DB_USER: "authuser"
  FRONTEND_URL: "https://app.example.com"
  JWT_ACCESS_TOKEN_EXPIRY: "1800"
  JWT_REFRESH_TOKEN_EXPIRY: "604800"
```

```bash
kubectl apply -f configmap.yaml
```

### Secret

Create `secret.yaml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: auth-service-secret
  namespace: auth-service
type: Opaque
stringData:
  DB_PASSWORD: "your_secure_password"
  GOOGLE_CLIENT_ID: "your_google_client_id"
  GOOGLE_CLIENT_SECRET: "your_google_client_secret"
  GITHUB_CLIENT_ID: "your_github_client_id"
  GITHUB_CLIENT_SECRET: "your_github_client_secret"
```

```bash
kubectl apply -f secret.yaml
```

### Persistent Volume

Create `pv.yaml`:

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: auth-keys-pv
spec:
  capacity:
    storage: 1Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: manual
  hostPath:
    path: /mnt/auth-keys
```

Create `pvc.yaml`:

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: auth-keys-pvc
  namespace: auth-service
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
  storageClassName: manual
```

```bash
kubectl apply -f pv.yaml
kubectl apply -f pvc.yaml
```

### Deployment

Create `deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: auth-service
  labels:
    app: auth-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: registry.example.com/auth-service:1.0.0
        ports:
        - containerPort: 8080
          name: http
        envFrom:
        - configMapRef:
            name: auth-service-config
        - secretRef:
            name: auth-service-secret
        volumeMounts:
        - name: keys-volume
          mountPath: /opt/auth-service/keys
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
      volumes:
      - name: keys-volume
        persistentVolumeClaim:
          claimName: auth-keys-pvc
```

```bash
kubectl apply -f deployment.yaml
```

### Service

Create `service.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: auth-service
  labels:
    app: auth-service
spec:
  type: ClusterIP
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
    name: http
  selector:
    app: auth-service
```

```bash
kubectl apply -f service.yaml
```

### Ingress

Create `ingress.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: auth-service-ingress
  namespace: auth-service
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - auth.example.com
    secretName: auth-service-tls
  rules:
  - host: auth.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: auth-service
            port:
              number: 8080
```

```bash
kubectl apply -f ingress.yaml
```

### Deploy to Kubernetes

```bash
# Apply all resources
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
kubectl apply -f pv.yaml
kubectl apply -f pvc.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f ingress.yaml

# Check deployment
kubectl get pods -n auth-service
kubectl get services -n auth-service
kubectl get ingress -n auth-service

# View logs
kubectl logs -f deployment/auth-service -n auth-service

# Check health
kubectl port-forward svc/auth-service 8080:8080 -n auth-service
curl http://localhost:8080/actuator/health
```

---

## OAuth2 Application Setup

### Google OAuth2

1. **Go to [Google Cloud Console](https://console.cloud.google.com/)**

2. **Create or select a project**

3. **Enable Google+ API**
   - Navigate to "APIs & Services" > "Library"
   - Search for "Google+ API"
   - Click "Enable"

4. **Configure OAuth2 consent screen**
   - Navigate to "APIs & Services" > "OAuth consent screen"
   - Select "External" user type
   - Fill in required fields:
     - App name: Your App Name
     - User support email: Your email
     - Developer contact: Your email
   - Add scopes:
     - `.../auth/userinfo.email`
     - `.../auth/userinfo.profile`
   - Add test users (for testing)

5. **Create OAuth2 credentials**
   - Navigate to "APIs & Services" > "Credentials"
   - Click "Create Credentials" > "OAuth 2.0 Client IDs"
   - Application type: "Web application"
   - Name: "Auth Service Production"
   - Authorized redirect URIs:
     ```
     https://auth.example.com/login/oauth2/code/google
     http://localhost:8080/login/oauth2/code/google  # For local development
     ```
   - Click "Create"

6. **Copy credentials**
   - Copy Client ID and Client Secret
   - Store in secret manager

### GitHub OAuth2

1. **Go to [GitHub Developer Settings](https://github.com/settings/developers)**

2. **Register a new OAuth app**
   - Click "New OAuth App"
   - Fill in details:
     - Application name: Your App Name
     - Homepage URL: `https://app.example.com`
     - Application description: Your app description
     - Authorization callback URL:
       ```
       https://auth.example.com/login/oauth2/code/github
       http://localhost:8080/login/oauth2/code/github  # For local development
       ```
   - Click "Register application"

3. **Copy credentials**
   - Copy Client ID
   - Click "Generate a new client secret"
   - Copy Client Secret
   - Store in secret manager

### OAuth2 Verification

Test OAuth2 configuration:

```bash
# Test Google OAuth2 initiation
curl -I https://accounts.google.com/o/oauth2/v2/auth?client_id=YOUR_CLIENT_ID&redirect_uri=http://localhost:8080/login/oauth2/code/google&response_type=code&scope=openid%20profile%20email

# Test GitHub OAuth2 initiation
curl -I https://github.com/login/oauth/authorize?client_id=YOUR_CLIENT_ID&redirect_uri=http://localhost:8080/login/oauth2/code/github&scope=user:email
```

---

## Key Management

### Key Generation

JWT signing keys are automatically generated on first startup if they don't exist.

**Manual Key Generation (Optional):**

```bash
# Generate RSA key pair
openssl genrsa -out private.key 2048
openssl rsa -in private.key -pubout -out public.key

# Generate key version
echo "key-$(date +%Y-%m-%d)-v1" > key-version.txt

# Set permissions
chmod 600 private.key
chmod 644 public.key

# Copy to keys directory
cp private.key public.key key-version.txt /path/to/keys/
```

### Key Rotation

Keys should be rotated every 90 days.

**Manual Key Rotation:**

```bash
# Backup old keys
cp /opt/auth-service/keys/private.key /opt/auth-service/keys/private.key.backup
cp /opt/auth-service/keys/public.key /opt/auth-service/keys/public.key.backup

# Generate new keys
openssl genrsa -out /opt/auth-service/keys/private.key 2048
openssl rsa -in /opt/auth-service/keys/private.key -pubout -out /opt/auth-service/keys/public.key

# Update key version
echo "key-$(date +%Y-%m-%d)-v2" > /opt/auth-service/keys/key-version.txt

# Restart service (gracefully)
kubectl rollout restart deployment/auth-service -n auth-service
```

**Automated Key Rotation (Future):**

Implement a scheduled task to:
1. Generate new key pair
2. Store both old and new keys
3. Sign new tokens with new key
4. API Gateway validates with either key (kid header)
5. After 30 min (access token expiry), remove old key

### Key Backup

```bash
# Create backup directory
mkdir -p /backups/auth-keys/$(date +%Y%m%d)

# Backup keys
cp /opt/auth-service/keys/* /backups/auth-keys/$(date +%Y%m%d)/

# Encrypt backup
tar -czf - /backups/auth-keys/$(date +%Y%m%d) | \
  openssl enc -aes-256-cbc -salt -out /backups/auth-keys/backup-$(date +%Y%m%d).tar.gz.enc

# Store backup securely (e.g., S3 with encryption)
aws s3 cp /backups/auth-keys/backup-$(date +%Y%m%d).tar.gz.enc \
  s3://your-backup-bucket/auth-keys/
```

---

## Database Migrations

### Flyway Migrations

Migrations run automatically on startup. Manual migration:

```bash
# View migration status
mvn flyway:info

# Run migrations
mvn flyway:migrate

# Validate migrations
mvn flyway:validate

# Baseline existing database
mvn flyway:baseline
```

### Migration Scripts

Place migration scripts in `src/main/resources/db/migration/`:

```sql
-- V1__Create_user_table.sql
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    auth_type VARCHAR(50) DEFAULT 'password',
    oauth_provider VARCHAR(50),
    oauth_subject_id VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_oauth ON users(oauth_provider, oauth_subject_id) WHERE deleted_at IS NULL;

-- V2__Create_refresh_tokens_table.sql
CREATE TABLE refresh_tokens (
    token_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    token_hash VARCHAR(255) NOT NULL,
    device_id VARCHAR(255),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
```

### Database Backup

```bash
# Backup database
docker exec auth-postgres pg_dump -U authuser authdb > backup-$(date +%Y%m%d).sql

# Restore database
docker exec -i auth-postgres psql -U authuser authdb < backup-20260302.sql

# Automated backup script
cat > /usr/local/bin/backup-auth-db.sh << 'EOF'
#!/bin/bash
BACKUP_DIR=/backups/auth-db
DATE=$(date +%Y%m%d)
mkdir -p $BACKUP_DIR
docker exec auth-postgres pg_dump -U authuser authdb | gzip > $BACKUP_DIR/backup-$DATE.sql.gz
# Keep last 7 days
find $BACKUP_DIR -name "backup-*.sql.gz" -mtime +7 -delete
EOF

chmod +x /usr/local/bin/backup-auth-db.sh

# Add to crontab
0 2 * * * /usr/local/bin/backup-auth-db.sh
```

---

## Health Checks

### Health Check Endpoints

```bash
# Basic health check
curl http://localhost:8080/actuator/health

# Detailed health check
curl http://localhost:8080/actuator/health | jq .

# Check specific components
curl http://localhost:8080/actuator/health/db | jq .
curl http://localhost:8080/actuator/health/keyStore | jq .
```

### Load Balancer Health Check

Configure your load balancer to check `/actuator/health`:

**AWS ALB:**
```
Health Check Path: /actuator/health
Interval: 30 seconds
Timeout: 10 seconds
Healthy Threshold: 3
Unhealthy Threshold: 3
```

**Kubernetes Liveness/Readiness:**
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30
  timeoutSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

---

## Monitoring

### Actuator Endpoints

```yaml
# application-prod.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

### Key Metrics

| Metric | Description | Target |
|--------|-------------|--------|
| `login_success_rate` | Percentage of successful logins | > 99% |
| `token_validation_latency` | JWT validation time | p95 < 15ms |
| `db_connection_pool_usage` | Database pool utilization | < 80% |
| `failed_login_attempts` | Count of failed logins | Alert on spike |
| `refresh_token_rate` | Token refresh rate | Track patterns |

### Prometheus Scraping

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'auth-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['auth-service:8080']
```

### Grafana Dashboard

Import or create a dashboard with:
- Request rate
- Response times
- Error rates
- Database connection pool
- Active refresh tokens per user
- Failed login attempts

---

## Troubleshooting

### Common Issues

#### 1. Database Connection Failed

**Symptoms:**
- Health check shows `db` status `DOWN`
- Logs show connection errors

**Solutions:**

```bash
# Check PostgreSQL is running
kubectl get pods -n auth-service | grep postgres

# Check database connection
kubectl exec -it deployment/auth-service -n auth-service -- \
  psql -h postgres -U authuser -d authdb -c "SELECT 1"

# Check environment variables
kubectl exec -it deployment/auth-service -n auth-service -- env | grep DB_

# Verify database credentials
kubectl get secret auth-service-secret -n auth-service -o yaml
```

#### 2. OAuth2 Callback Fails

**Symptoms:**
- OAuth2 flow redirects but fails
- Frontend doesn't receive tokens

**Solutions:**

```bash
# Verify OAuth2 credentials
kubectl get secret auth-service-secret -n auth-service -o yaml | grep GOOGLE

# Check redirect URI matches
# 1. OAuth app configuration
# 2. FRONTEND_URL environment variable

# Test OAuth2 initiation
curl -I https://accounts.google.com/o/oauth2/v2/auth?client_id=YOUR_CLIENT_ID

# Check frontend is accessible
curl -I $FRONTEND_URL
```

#### 3. JWT Validation Fails

**Symptoms:**
- API Gateway rejects valid tokens
- "Invalid signature" errors

**Solutions:**

```bash
# Check JWKS endpoint is accessible
curl http://auth-service/.well-known/jwks.json | jq .

# Verify key ID matches
JWT_TOKEN=$(cat token.txt)
echo $JWT_TOKEN | jq -r '.header.kid'
curl http://auth-service/.well-known/jwks.json | jq '.keys[0].kid'

# Check clock synchronization
kubectl exec -it deployment/auth-service -n auth-service -- date
# Install NTP if clocks are skewed

# Verify API Gateway has latest keys
kubectl exec -it deployment/api-gateway -- \
  curl http://auth-service/.well-known/jwks.json
```

#### 4. Keys Lost After Restart

**Symptoms:**
- Service generates new keys on restart
- Old tokens become invalid

**Solutions:**

```bash
# Check volume mount
kubectl exec -it deployment/auth-service -n auth-service -- df -h | grep keys

# Verify keys directory exists
kubectl exec -it deployment/auth-service -n auth-service -- ls -la /opt/auth-service/keys

# Check PVC is bound
kubectl get pvc -n auth-service

# Restore from backup if needed
kubectl cp /backups/auth-keys/* \
  auth-service-pod:/opt/auth-service/keys/
```

#### 5. Rate Limiting Too Aggressive

**Symptoms:**
- Legitimate users get rate limited
- 429 errors during normal usage

**Solutions:**

```bash
# Update rate limiting configuration
kubectl edit configmap auth-service-config -n auth-service

# Add environment variables:
# RATE_LIMIT_LOGIN_PER_MINUTE=10
# RATE_LIMIT_REFRESH_PER_MINUTE=20

# Restart pods
kubectl rollout restart deployment/auth-service -n auth-service

# Monitor rate limit effectiveness
kubectl logs -f deployment/auth-service -n auth-service | grep "Rate limit"
```

### Debug Mode

Enable debug logging:

```bash
# Update deployment
kubectl set env deployment/auth-service \
  -n auth-service \
  LOGGING_LEVEL_ROOT=DEBUG \
  LOGGING_LEVEL_COM_VIBE_AUTH=DEBUG

# View debug logs
kubectl logs -f deployment/auth-service -n auth-service

# Reset to normal
kubectl set env deployment/auth-service \
  -n auth-service \
  LOGGING_LEVEL_ROOT=INFO \
  LOGGING_LEVEL_COM_VIBE_AUTH=INFO
```

### Performance Issues

```bash
# Check resource usage
kubectl top pods -n auth-service
kubectl top nodes

# Check database performance
kubectl exec -it deployment/postgres -n auth-service -- \
  psql -U authuser -d authdb -c "SELECT * FROM pg_stat_activity;"

# Check for long-running queries
kubectl exec -it deployment/postgres -n auth-service -- \
  psql -U authuser -d authdb -c \
  "SELECT pid, query_start, state, query FROM pg_stat_activity WHERE state != 'idle' ORDER BY query_start;"

# Check JVM metrics
kubectl exec -it deployment/auth-service -n auth-service -- \
  jcmd 1 VM.native_memory summary
```

---

## Security Checklist

- [ ] All secrets stored in secret manager
- [ ] TLS enabled for all endpoints
- [ ] Database connections encrypted
- [ ] Keys directory has proper permissions (700)
- [ ] Key rotation scheduled (90 days)
- [ ] OAuth2 redirect URIs match exactly
- [ ] Rate limiting enabled
- [ ] Database backups automated
- [ ] Health checks configured
- [ ] Monitoring and alerting configured
- [ ] Security scanning integrated in CI/CD
- [ ] Incident response plan documented

---

## Rollback Procedure

```bash
# Kubernetes rollback
kubectl rollout undo deployment/auth-service -n auth-service

# Check specific revision
kubectl rollout history deployment/auth-service -n auth-service

# Rollback to specific revision
kubectl rollout undo deployment/auth-service -n auth-service --to-revision=2

# Docker Compose rollback
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d \
  --image registry.example.com/auth-service:0.9.0
```

---

## Support

For issues or questions:
- Documentation: [docs/](docs/)
- Issues: [GitHub Issues](https://github.com/Tinkerc/vibe-coding/issues)
- Health Check: http://auth-service:8080/actuator/health
