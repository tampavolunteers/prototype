# OAuth2 Implementation TODO

This document tracks the tasks needed to complete and re-enable OAuth2 authentication support.

## Current Status

OAuth2 support has been **temporarily disabled** to allow the backend to start without OAuth credentials. The groundwork is in place, but the feature needs configuration and testing before it can be enabled.

## Database Schema

- [x] Add `auth_provider` column to users table (V3 migration)
- [x] Add `provider_id` column to users table (V3 migration)
- [x] Add indexes for OAuth lookups

## Backend Configuration

### Security Configuration (`SecurityConfig.java`)

- [ ] Obtain OAuth2 client credentials from providers:
  - [ ] Google Cloud Console - Create OAuth 2.0 Client ID
  - [ ] GitHub Developer Settings - Create OAuth App

- [ ] Add OAuth2 configuration to `application.properties`:
  ```properties
  # Google OAuth2
  spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
  spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
  spring.security.oauth2.client.registration.google.scope=profile,email
  spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/api/oauth2/callback/{registrationId}

  # GitHub OAuth2
  spring.security.oauth2.client.registration.github.client-id=${GITHUB_CLIENT_ID}
  spring.security.oauth2.client.registration.github.client-secret=${GITHUB_CLIENT_SECRET}
  spring.security.oauth2.client.registration.github.scope=user:email,read:user
  spring.security.oauth2.client.registration.github.redirect-uri={baseUrl}/api/oauth2/callback/{registrationId}

  # OAuth2 Provider Configuration
  spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/v2/auth
  spring.security.oauth2.client.provider.google.token-uri=https://oauth2.googleapis.com/token
  spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo
  spring.security.oauth2.client.provider.google.user-name-attribute=sub

  spring.security.oauth2.client.provider.github.authorization-uri=https://github.com/login/oauth/authorize
  spring.security.oauth2.client.provider.github.token-uri=https://github.com/login/oauth/access_token
  spring.security.oauth2.client.provider.github.user-info-uri=https://api.github.com/user
  spring.security.oauth2.client.provider.github.user-name-attribute=id
  ```

- [ ] Uncomment OAuth2 import in `SecurityConfig.java` (line 21)
- [ ] Uncomment OAuth2 service field in `SecurityConfig.java` (line 39)
- [ ] Uncomment OAuth2 parameter in `securityFilterChain` method (line 42)
- [ ] Uncomment `.oauth2Login()` configuration in `SecurityConfig.java` (lines 55-56)

### OAuth2 Service Implementation

- [ ] Review and test `CustomOAuth2UserService.java`
- [ ] Ensure proper mapping of OAuth2 attributes to User entity
- [ ] Handle edge cases:
  - [ ] User already exists with same email (different provider)
  - [ ] Missing email from OAuth provider
  - [ ] Email not verified by provider

### OAuth2 Success Handler

- [ ] Create `OAuth2AuthenticationSuccessHandler.java`
- [ ] Generate JWT token after successful OAuth2 authentication
- [ ] Redirect user to frontend with token
- [ ] Example redirect: `http://localhost:5173/auth/callback?token=<jwt_token>`

### OAuth2 Failure Handler

- [ ] Create `OAuth2AuthenticationFailureHandler.java`
- [ ] Handle authentication failures gracefully
- [ ] Redirect to frontend with error message

## Frontend Integration

### UI Components

- [ ] Create Google login button component
- [ ] Create GitHub login button component
- [ ] Add OAuth buttons to login page
- [ ] Style OAuth buttons to match provider branding

### OAuth Flow Implementation

- [ ] Implement OAuth callback route (`/auth/callback`)
- [ ] Extract JWT token from URL parameters
- [ ] Store token in localStorage/sessionStorage
- [ ] Redirect to dashboard after successful authentication
- [ ] Display error messages for failed authentication

### API Integration

- [ ] Update authentication service to support OAuth flows
- [ ] Test token storage and retrieval
- [ ] Test automatic redirect to dashboard

## OAuth Provider Setup

### Google Cloud Console

- [ ] Create new project or select existing project
- [ ] Enable Google+ API
- [ ] Create OAuth 2.0 Client ID (Web application)
- [ ] Add authorized redirect URIs:
  - Development: `http://localhost:8080/api/oauth2/callback/google`
  - Production: `https://yourdomain.com/api/oauth2/callback/google`
- [ ] Copy Client ID and Client Secret
- [ ] Add to environment variables

### GitHub Developer Settings

- [ ] Go to Settings → Developer settings → OAuth Apps
- [ ] Create new OAuth App
- [ ] Set Authorization callback URL:
  - Development: `http://localhost:8080/api/oauth2/callback/github`
  - Production: `https://yourdomain.com/api/oauth2/callback/github`
- [ ] Copy Client ID and generate Client Secret
- [ ] Add to environment variables

## Environment Variables

- [ ] Create `.env` file (add to `.gitignore`)
- [ ] Add environment variables:
  ```bash
  # Google OAuth
  GOOGLE_CLIENT_ID=your-google-client-id
  GOOGLE_CLIENT_SECRET=your-google-client-secret

  # GitHub OAuth
  GITHUB_CLIENT_ID=your-github-client-id
  GITHUB_CLIENT_SECRET=your-github-client-secret
  ```

- [ ] Update `docker-compose.yml` to include OAuth environment variables:
  ```yaml
  backend:
    environment:
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
      GITHUB_CLIENT_ID: ${GITHUB_CLIENT_ID}
      GITHUB_CLIENT_SECRET: ${GITHUB_CLIENT_SECRET}
  ```

## Testing

### Unit Tests

- [ ] Test `CustomOAuth2UserService` with mock OAuth2 user data
- [ ] Test user creation with OAuth provider data
- [ ] Test linking OAuth account to existing user
- [ ] Test handling of duplicate email addresses

### Integration Tests

- [ ] Test complete OAuth2 flow with Google
- [ ] Test complete OAuth2 flow with GitHub
- [ ] Test JWT token generation after OAuth success
- [ ] Test redirect URLs and parameters

### Manual Testing

- [ ] Test Google login flow end-to-end
- [ ] Test GitHub login flow end-to-end
- [ ] Test user can log in with both LOCAL and OAUTH providers
- [ ] Test switching between providers with same email
- [ ] Test error handling for various failure scenarios

## Security Considerations

- [ ] Ensure OAuth redirect URIs are whitelisted
- [ ] Validate state parameter to prevent CSRF attacks
- [ ] Use HTTPS in production (HTTP only allowed for localhost development)
- [ ] Store OAuth client secrets securely (never commit to git)
- [ ] Implement rate limiting on OAuth endpoints
- [ ] Add logging for OAuth authentication attempts
- [ ] Review OAuth scopes - request minimum necessary permissions

## Documentation

- [ ] Update README.md with OAuth setup instructions
- [ ] Document OAuth configuration in deployment guide
- [ ] Add OAuth troubleshooting section
- [ ] Document user flow diagrams for OAuth authentication
- [ ] Update API documentation with OAuth endpoints

## Production Deployment

- [ ] Set up OAuth credentials for production environment
- [ ] Configure production redirect URIs
- [ ] Add OAuth secrets to production environment variables
- [ ] Test OAuth flow in production environment
- [ ] Monitor OAuth authentication logs
- [ ] Set up alerts for OAuth failures

## References

- [Spring Security OAuth2 Client Documentation](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Google OAuth2 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [GitHub OAuth Documentation](https://docs.github.com/en/developers/apps/building-oauth-apps/authorizing-oauth-apps)

---

## Notes

- OAuth2 was temporarily disabled in commit [commit hash] due to missing client configuration
- Database schema is ready - V3 migration includes `auth_provider` and `provider_id` columns
- Backend code structure is in place - just needs configuration and uncommenting
- Priority: Get Google OAuth working first, then add GitHub support
