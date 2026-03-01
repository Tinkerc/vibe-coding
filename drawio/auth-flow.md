sequenceDiagram
    autonumber

    participant Client
    participant APIGateway as API Gateway
    participant AuthService as Auth Service
    participant DB as User Database
    participant Resource as Resource Service

    %% LOGIN FLOW
    Client->>APIGateway: POST /login (username, password)
    APIGateway->>AuthService: Validate credentials

    AuthService->>DB: Query user record
    DB-->>AuthService: User + password hash

    AuthService->>AuthService: Verify password hash
    AuthService->>AuthService: Generate Access Token (JWT)
    AuthService->>AuthService: Generate Refresh Token

    AuthService-->>APIGateway: Access Token + Refresh Token + Expiry
    APIGateway-->>Client: 200 OK (tokens + expiry)

    Client->>Client: Store tokens securely

    %% AUTHENTICATED REQUEST
    Client->>APIGateway: GET /resource (Access Token)
    APIGateway->>AuthService: Validate JWT

    AuthService->>AuthService: Verify signature & expiry
    AuthService-->>APIGateway: Token valid

    APIGateway->>Resource: Forward request
    Resource-->>APIGateway: Resource data
    APIGateway-->>Client: 200 OK (data)

    %% TOKEN REFRESH FLOW
    Client->>APIGateway: POST /refresh (Refresh Token)
    APIGateway->>AuthService: Validate refresh token

    AuthService->>DB: Verify refresh token
    DB-->>AuthService: Token valid

    AuthService->>AuthService: Generate new Access Token
    AuthService-->>APIGateway: New Access Token + Expiry
    APIGateway-->>Client: 200 OK (new token)

    Client->>Client: Replace stored access token

    %% LOGOUT FLOW
    Client->>APIGateway: POST /logout (Refresh Token)
    APIGateway->>AuthService: Revoke refresh token

    AuthService->>DB: Delete/Blacklist refresh token
    DB-->>AuthService: Revoked

    AuthService-->>APIGateway: Logout success
    APIGateway-->>Client: 200 OK

    Client->>Client: Remove stored tokens