# Auth Service Deployment Guide

This guide covers Docker-based deployment for the Auth Service using the configuration files provided.

---

## Prerequisites

- Docker Desktop 4.0+ (or Docker Engine 20.10+)
- Docker Compose 2.0+
- Java 21 JDK (for local development)
- Maven 3.9+ (for local development)

---

## Quick Start

### 1. Build and Start Services

```bash
# Navigate to auth-service directory
cd auth-service/

# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f auth-service
```

### 2. Verify Services

```bash
# Check health status
curl http://localhost:8080/actuator/health

# Check JWKS endpoint
curl http://localhost:8080/.well-known/jwks.json

# List containers
docker-compose ps
```

### 3. Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes data)
docker-compose down -v
```

---

## Configuration

### Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
cp .env.example .env
```

Edit `.env` with your values:

```bash
# Required: Database password
DB_PASSWORD=your_secure_password

# Optional: OAuth2 credentials
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret
GITHUB_CLIENT_ID=your_client_id
GITHUB_CLIENT_SECRET=your_client_secret

# Optional: Frontend URL
FRONTEND_URL=http://localhost:3000
```

### Volume Persistence

The following directories are persisted:

- `./keys` - JWT signing keys (created on first startup)
- `./pgdata` - PostgreSQL data (managed by Docker volume)

**Important:** Never commit the `keys/` directory to version control!

---

## OAuth2 Setup

### Google OAuth2

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable Google+ API
4. Create OAuth 2.0 credentials:
   - Application type: Web application
   - Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google`
5. Copy Client ID and Secret to `.env`

### GitHub OAuth2

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click "New OAuth App"
3. Fill in:
   - Application name: Auth Service (Dev)
   - Homepage URL: `http://localhost:8080`
   - Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`
4. Copy Client ID and Secret to `.env`

---

## Production Deployment

### Using Nginx Reverse Proxy

1. Build and start services:

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

2. Configure Nginx (optional):

```bash
# Build nginx image
docker build -t auth-nginx ./nginx

# Run nginx
docker run -d \
  --name auth-nginx \
  -p 80:80 \
  -p 443:443 \
  --link auth-service:auth-service \
  auth-nginx
```

### SSL/TLS Configuration

For production, update `nginx/conf.d/default.conf`:

1. Obtain SSL certificates (Let's Encrypt recommended)
2. Uncomment HTTPS server block
3. Update certificate paths
4. Restart nginx

---

## Database Management

### Access PostgreSQL

```bash
# Connect to PostgreSQL container
docker-compose exec postgres psql -U authuser -d authdb

# Or use psql from host
psql -h localhost -U authuser -d authdb
```

### Run Flyway Migrations

```bash
# Migrations run automatically on startup
# To manually trigger:
docker-compose exec auth-service \
  java -jar app.jar --spring.flyway.migrate
```

### Backup Database

```bash
# Create backup
docker-compose exec postgres \
  pg_dump -U authuser authdb > backup_$(date +%Y%m%d).sql

# Restore backup
docker-compose exec -T postgres \
  psql -U authuser authdb < backup_20260302.sql
```

---

## Troubleshooting

### Service Won't Start

```bash
# Check logs
docker-compose logs auth-service

# Check container status
docker-compose ps

# Restart services
docker-compose restart auth-service
```

### Database Connection Issues

```bash
# Check PostgreSQL is healthy
docker-compose exec postgres pg_isready -U authuser -d authdb

# Check database logs
docker-compose logs postgres

# Verify network
docker network inspect auth-network
```

### Key Persistence Issues

```bash
# Check keys directory
ls -la keys/

# Should contain:
# - private.key
# - public.key
# - key-version.txt

# If missing, restart service to regenerate
docker-compose restart auth-service
```

### Rate Limiting Issues

For production deployments with multiple instances, the in-memory rate limiting won't work. Use Redis:

```bash
# Uncomment redis service in docker-compose.yml
# Update application-prod.yml to use Redis
```

---

## Monitoring

### Health Checks

```bash
# Service health
curl http://localhost:8080/actuator/health

# Docker health status
docker inspect --format='{{.State.Health.Status}}' auth-service
```

### Metrics (Optional)

Enable Actuator metrics in `application-prod.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

Access metrics at: `http://localhost:8080/actuator/metrics`

---

## Security Checklist

Before deploying to production:

- [ ] Change default database password
- [ ] Obtain and configure OAuth2 credentials
- [ ] Enable SSL/TLS (HTTPS)
- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Configure CORS for your frontend domain
- [ ] Review rate limiting limits
- [ ] Enable email service for password reset
- [ ] Set up log aggregation
- [ ] Configure backup strategy
- [ ] Review JWT key rotation schedule (90 days)
- [ ] Set up monitoring and alerts

---

## Maintenance

### Key Rotation

Every 90 days, rotate JWT signing keys:

```bash
# Backup old keys
cp keys/private.key keys/private.key.backup
cp keys/public.key keys/public.key.backup

# Restart service (generates new keys)
docker-compose restart auth-service

# Keep old keys for token validity period (30 days)
# Then delete backups
```

### Database Cleanup

Expired tokens are cleaned up hourly. To manually trigger:

```bash
docker-compose exec postgres psql -U authuser -d authdb \
  -c "SELECT cleanup_expired_tokens();"
```

### Updates

```bash
# Pull latest code
git pull

# Rebuild and restart
docker-compose up -d --build

# Run migrations (automatic)
docker-compose exec auth-service \
  java -jar app.jar --spring.flyway.migrate
```

---

## Performance Tuning

### JVM Options

Edit `Dockerfile` ENTRYPOINT for your environment:

```bash
# For production with 2GB RAM limit
"-Xms512m", \
"-Xmx1536m", \
"-XX:MaxRAMPercentage=75", \
```

### Database Pool

Edit `application-prod.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## Additional Resources

- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)
- [Nginx Reverse Proxy](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/)
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

**Support:** For issues or questions, please refer to the main project README or create an issue in the repository.
