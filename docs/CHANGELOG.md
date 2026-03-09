# Changelog

All notable changes to Auth Service will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- OAuth2 provider: Microsoft
- OAuth2 provider: Apple
- 2FA/MFA support (TOTP)
- Session management UI
- GDPR data export
- Redis-based distributed rate limiting

## [1.0.0] - 2026-03-02

### Added
- User authentication with username/password
- JWT access token generation (RS256)
- Refresh token with automatic rotation
- JWKS endpoint for public key distribution
- Token validation at API Gateway (local)
- Logout with refresh token revocation
- Password reset flow with email
- Token blacklist for immediate revocation
- Multi-device support (max 5 tokens per user)
- OAuth2 integration (Google, GitHub)
- Rate limiting (login, refresh, password reset)
- Scheduled cleanup of expired tokens
- Health check endpoints
- OpenAPI/Swagger documentation
- Docker deployment support
- Kubernetes deployment manifests
- Database migrations with Flyway
- Comprehensive test suite (unit + integration)

### Security
- Password hashing with bcrypt (work factor 12)
- RS256 JWT signing
- Token rotation on refresh
- Clock skew tolerance (±30 seconds)
- TLS-ready configuration
- Secret management support
- Key rotation support (90 days)

### Performance
- Token validation < 10ms (local)
- Login < 500ms
- Token refresh < 200ms
- Connection pooling for database
- Scheduled cleanup jobs

### Documentation
- API documentation (API.md)
- Deployment guide (DEPLOYMENT.md)
- Development guide (DEVELOPMENT.md)
- Docker and Kubernetes examples
- Troubleshooting section

### Dependencies
- Spring Boot 3.2+
- Spring Security 6+
- Java 21
- PostgreSQL 15+
- JJWT for JWT handling
- TestContainers for integration tests
- SpringDoc OpenAPI for API docs

---

## [0.9.0] - 2026-02-28 (Beta)

### Added
- Initial authentication flow
- Basic JWT token generation
- Login endpoint
- Refresh token endpoint
- Logout endpoint

### Known Issues
- No OAuth2 support
- No password reset
- No rate limiting
- No token blacklist
- No multi-device support

---

## Version History Summary

| Version | Date | Status | Key Features |
|---------|------|--------|--------------|
| 1.0.0 | 2026-03-02 | Stable | Full MVP with OAuth2, rate limiting, password reset |
| 0.9.0 | 2026-02-28 | Beta | Basic authentication flow |

---

## Release Notes

### 1.0.0 (2026-03-02)

This is the first stable release of Auth Service, providing a complete authentication and authorization solution for microservices architectures.

**Highlights:**

- **JWT-based Authentication**: RS256 asymmetric encryption for secure token validation
- **Token Management**: Automatic refresh token rotation, multi-device support, and token blacklist
- **OAuth2 Integration**: Social login with Google and GitHub
- **Security Features**: Rate limiting, bcrypt password hashing, key rotation support
- **Developer Experience**: Comprehensive documentation, Docker/Kubernetes support, health monitoring

**Migration from 0.9.0:**

No migration needed. This is the first stable release.

**Breaking Changes:**

None. This is the initial stable release.

**Known Issues:**

- Rate limiting is in-memory (does not scale across multiple instances)
- Token blacklist is optional (requires manual enablement)
- Email service must be configured for password reset

**Upgrade Instructions:**

For new deployments, follow the deployment guide in `docs/DEPLOYMENT.md`.

---

## Future Roadmap

### 1.1.0 (Planned: Q2 2026)

- Microsoft OAuth2 provider
- Apple OAuth2 provider
- Redis-based distributed rate limiting
- Enhanced monitoring with custom metrics

### 1.2.0 (Planned: Q3 2026)

- 2FA/MFA support (TOTP)
- Session management UI
- User profile management
- GDPR data export

### 2.0.0 (Planned: Q4 2026)

- Multi-tenancy support
- SAML integration
- Advanced analytics
- Audit logging

---

## Contributing

To contribute to Auth Service, please see the development guide in `docs/DEVELOPMENT.md`.

## Support

For issues, questions, or contributions, please visit:
- GitHub: https://github.com/Tinkerc/vibe-coding/issues
- Documentation: https://github.com/Tinkerc/vibe-coding/tree/main/docs

---

**Note:** This changelog follows the [Keep a Changelog](https://keepachangelog.com/) format and versions adhere to [Semantic Versioning](https://semver.org/).
