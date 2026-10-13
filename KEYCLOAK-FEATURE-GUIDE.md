# Keycloak User Directory Integration — Feature Guide & Verification

This document provides a comprehensive overview of the **Keycloak User Directory** feature in CommaFeed, including architecture, environment setup, full testing flows, cURL requests, edge-case validations, and automated test commands.

---

## 1. Feature & Task Overview

### Objective
The goal of this task is to integrate Keycloak with CommaFeed to provide administrators with a unified **Keycloak User Directory** in the administrative panel. This allows CommaFeed admins to:
- Browse and search users managed in an external Keycloak realm (`commafeed`).
- Paginate through user directories seamlessly.
- Inspect single-user details on demand (including assigned **Realm Roles** and **Groups**).
- Filter out internal service accounts so only regular users are displayed.
- Enforce strict role-based access control (RBAC), ensuring only CommaFeed users with the `ADMIN` role can access directory endpoints.
- Ensure high performance by separating list queries (lightweight DTOs) from detail queries (fetching roles and groups on demand), preventing the $N+1$ HTTP request bottleneck.

---

## 2. Architecture & Components

```
┌────────────────────────────────┐         ┌─────────────────────────────────┐
│        CommaFeed Client        │         │        CommaFeed Server         │
│  (/admin/directory Frontend)   │         │       (Quarkus 3 Backend)       │
└───────────────┬────────────────┘         └────────────────┬────────────────┘
                │                                           │
                │ 1. GET /rest/admin/directory/users        │
                │    (Authenticated as Admin)               │
                ├──────────────────────────────────────────>│
                │                                           │ 2. OAuth2 Client Credentials
                │                                           │    Token Exchange
                │                                           │    (cached in-memory)
                │                                           ├──────────────────────────┐
                │                                           │                          │
                │                                           │                          ▼
                │                                           │               ┌──────────────────────┐
                │                                           │               │   Keycloak Server    │
                │                                           │               │    (Realm: commafeed)│
                │                                           │               └──────────┬───────────┘
                │                                           │ 3. GET /admin/realms/    │
                │                                           │    commafeed/users       │
                │                                           │<─────────────────────────┘
                │                                           │
                │                                           │ 4. Filter service accounts
                │                                           │    & Map to DTOs
                │ 5. Returns KeycloakUserResponse[]         │
                │<──────────────────────────────────────────┤
                │                                           │
                │ 6. GET /rest/admin/directory/users/{id}   │
                ├──────────────────────────────────────────>│
                │                                           │ 7. Query User Profile,
                │                                           │    /role-mappings/realm,
                │                                           │    & /groups
                │                                           ├──────────────────────────┐
                │                                           │<─────────────────────────┘
                │ 8. Returns KeycloakUserDetailResponse     │
                │<──────────────────────────────────────────┤
```

### Backend Components (`commafeed-server`)
- **REST Resource**: `com.commafeed.frontend.resource.KeycloakAdminREST`
  - `@Path("/rest/admin/directory")`
  - `@RolesAllowed(Roles.ADMIN)`: Requires CommaFeed admin privileges.
  - Endpoints:
    - `GET /rest/admin/directory/users?search={query}&first={offset}&max={limit}`
    - `GET /rest/admin/directory/users/{keycloakId}`
- **Service Layer**: `com.commafeed.backend.service.KeycloakService`
  - Validates and sanitizes pagination boundaries (`first >= 0`, `1 <= max <= 100`).
  - Filters out service accounts (`service-account-*` or `serviceAccountClientId != null`).
  - Coordinates user lookup, role mappings, and group memberships.
- **Client & Token Provider**:
  - `DefaultKeycloakClient`: Uses Java's standard `java.net.http.HttpClient` with configurable timeouts (`KEYCLOAK_CONNECT_TIMEOUT`, `KEYCLOAK_REQUEST_TIMEOUT`).
  - `DefaultKeycloakTokenProvider`: Obtains Bearer access tokens using OAuth2 `client_credentials` grant type and manages token caching with expiration buffers.
  - `KeycloakUserMapper`: Translates Keycloak JSON nodes into typed Java DTOs.
- **DTOs (`com.commafeed.frontend.model`)**:
  - `KeycloakUserResponse`: Lightweight summary (`keycloakId`, `username`, `email`, `firstName`, `lastName`, `enabled`).
  - `KeycloakUserDetailResponse`: Enriched profile including `List<String> roles` and `List<String> groups`.
- **Exception Handling**:
  - `KeycloakUserNotFoundException` $\rightarrow$ maps to HTTP `404 Not Found`.
  - `KeycloakUpstreamException` $\rightarrow$ maps to HTTP `502 Bad Gateway`.

### Frontend Components (`commafeed-client`)
- **Page**: `src/pages/admin/KeycloakUserDirectoryPage.tsx`
  - Accessible via route `/admin/directory`.
  - Search input with automatic page reset.
  - Paginated data table displaying Username, Email, Full Name, and Enabled status badges.
  - "Details" modal triggering on-demand lookup of UUID, roles, and group memberships.
  - Resilient error handling showing service-unavailable alert banner when Keycloak is offline.

---

## 3. Environment & Prerequisites

### 3.1 Keycloak Realm Configuration
Keycloak is defined in `docker-compose.yml` and auto-imports `./keycloak/import/commafeed-realm.json`:
- **Image**: `quay.io/keycloak/keycloak:24.0.5`
- **Host Port**: `8081` (mapped to container port `8080`)
- **Realm Name**: `commafeed`
- **Client ID**: `commafeed-service` (Confidential client, `client-secret: secret`, Service Accounts enabled)
- **Service Account Permissions**: Read-only `realm-management` roles:
  - `view-users`
  - `query-users`
  - `query-groups`

### 3.2 Pre-seeded Test Users in Realm
| UUID | Username | Email | Name | Enabled | Roles | Groups |
|---|---|---|---|:---:|---|---|
| `a1000000-0000-0000-0000-000000000001` | `alice` | `alice@example.com` | Alice Smith | Yes | `admin`, `user` | `/Engineering` |
| `a1000000-0000-0000-0000-000000000002` | `bob` | `bob@example.com` | Bob Jones | Yes | `editor`, `user` | `/Marketing` |
| `a1000000-0000-0000-0000-000000000003` | `charlie` | `charlie@example.com` | Charlie Brown | Yes | `user` | `/Engineering` |
| `a1000000-0000-0000-0000-000000000004` | `david` | `david@example.com` | David Miller | No | `user` | `/Operations` |
| `a1000000-0000-0000-0000-000000000005` | `service-account-commafeed-service` | `...` | Service Account | Yes | `realm-management` | *None* |

*(Note: `service-account-commafeed-service` is filtered out from list queries automatically).*

---

## 4. How to Start the Services

### Step 1: Start Keycloak
Run from the repository root:
```bash
docker compose up -d
```
Verify the container is healthy and realm is imported:
```bash
docker compose logs -f keycloak
```
> Keycloak Admin Console is available at: **http://localhost:8081** (Admin credentials: `admin` / `admin`).

### Step 2: Start CommaFeed Backend
Run in development mode (which enables `%dev` configuration with default port `8083` and creates the `admin:admin` demo user):
```bash
./mvnw -pl commafeed-server quarkus:dev
```
Verify the backend starts and connects to Keycloak at `http://localhost:8081`.

### Step 3: Start CommaFeed Frontend (Optional for UI testing)
```bash
cd commafeed-client
npm start
```
The web application is served at **http://localhost:8080** (or proxied to the backend).

---

## 5. Testing the Feature with cURL (Full Flow)

CommaFeed in development mode runs on **`http://localhost:8083`** with the default administrator credentials:
- **Username**: `admin`
- **Password**: `admin`

---

### Request 1: Direct Keycloak Service Account Token Check
Verify that the `commafeed-service` client can successfully obtain an OAuth2 access token directly from Keycloak:

```bash
curl -s -X POST "http://localhost:8081/realms/commafeed/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=commafeed-service" \
  -d "client_secret=secret"
```

**Expected Response**:
```json
{
  "access_token": "eyJhbGciOi...",
  "expires_in": 300,
  "refresh_expires_in": 0,
  "token_type": "Bearer",
  "not-before-policy": 0,
  "scope": "email profile"
}
```

---

### Request 2: List Keycloak Users via CommaFeed Admin API
Retrieve the regular users through the CommaFeed endpoint.

```bash
curl -s -u admin:admin "http://localhost:8083/rest/admin/directory/users"
```

**Expected Response** (Notice: Service account is omitted, returning 4 regular users):
```json
[
  {
    "keycloakId": "a1000000-0000-0000-0000-000000000001",
    "username": "alice",
    "email": "alice@example.com",
    "firstName": "Alice",
    "lastName": "Smith",
    "enabled": true
  },
  {
    "keycloakId": "a1000000-0000-0000-0000-000000000002",
    "username": "bob",
    "email": "bob@example.com",
    "firstName": "Bob",
    "lastName": "Jones",
    "enabled": true
  },
  {
    "keycloakId": "a1000000-0000-0000-0000-000000000003",
    "username": "charlie",
    "email": "charlie@example.com",
    "firstName": "Charlie",
    "lastName": "Brown",
    "enabled": true
  },
  {
    "keycloakId": "a1000000-0000-0000-0000-000000000004",
    "username": "david",
    "email": "david@example.com",
    "firstName": "David",
    "lastName": "Miller",
    "enabled": false
  }
]
```

---

### Request 3: Pagination (`first` and `max`)
Test pagination controls:

```bash
# Page 1: First 2 users
curl -s -u admin:admin "http://localhost:8083/rest/admin/directory/users?first=0&max=2"
```

```bash
# Page 2: Next 2 users
curl -s -u admin:admin "http://localhost:8083/rest/admin/directory/users?first=2&max=2"
```

---

### Request 4: Search Filtering
Search by username or partial string:

```bash
curl -s -u admin:admin "http://localhost:8083/rest/admin/directory/users?search=alice"
```

**Expected Response**:
```json
[
  {
    "keycloakId": "a1000000-0000-0000-0000-000000000001",
    "username": "alice",
    "email": "alice@example.com",
    "firstName": "Alice",
    "lastName": "Smith",
    "enabled": true
  }
]
```

---

### Request 5: Get Single User Details (Roles & Groups)
Fetch complete details by Keycloak UUID for `alice` (`a1000000-0000-0000-0000-000000000001`):

```bash
curl -s -u admin:admin "http://localhost:8083/rest/admin/directory/users/a1000000-0000-0000-0000-000000000001"
```

**Expected Response**:
```json
{
  "keycloakId": "a1000000-0000-0000-0000-000000000001",
  "username": "alice",
  "email": "alice@example.com",
  "firstName": "Alice",
  "lastName": "Smith",
  "enabled": true,
  "roles": [
    "admin",
    "user"
  ],
  "groups": [
    "Engineering"
  ]
}
```

Fetch details for `bob` (`a1000000-0000-0000-0000-000000000002`):
```bash
curl -s -u admin:admin "http://localhost:8083/rest/admin/directory/users/a1000000-0000-0000-0000-000000000002"
```
**Expected Response**:
```json
{
  "keycloakId": "a1000000-0000-0000-0000-000000000002",
  "username": "bob",
  "email": "bob@example.com",
  "firstName": "Bob",
  "lastName": "Jones",
  "enabled": true,
  "roles": [
    "editor",
    "user"
  ],
  "groups": [
    "Marketing"
  ]
}
```

---

### Request 6: Security & Role-Based Access Control (RBAC) Verification
Verify that unauthenticated users or non-admin users cannot access the directory endpoints.

#### A. Unauthenticated Request:
```bash
curl -s -i "http://localhost:8083/rest/admin/directory/users" | grep "HTTP"
```
**Expected Status**: `HTTP/1.1 401 Unauthorized`

#### B. Non-Admin User Request:
If a regular user (e.g. `regularuser:password`) exists in CommaFeed:
```bash
curl -s -i -u regularuser:password "http://localhost:8083/rest/admin/directory/users" | grep "HTTP"
```
**Expected Status**: `HTTP/1.1 403 Forbidden`

---

### Request 7: Error Handling & Edge Cases

#### A. Non-Existent User UUID:
```bash
curl -s -i -u admin:admin "http://localhost:8083/rest/admin/directory/users/non-existent-uuid"
```
**Expected Status**: `HTTP/1.1 404 Not Found`

#### B. Attempting to Query Service Account by ID:
If an admin attempts to query the service account's UUID (`a1000000-0000-0000-0000-000000000005`):
```bash
curl -s -i -u admin:admin "http://localhost:8083/rest/admin/directory/users/a1000000-0000-0000-0000-000000000005"
```
**Expected Status**: `HTTP/1.1 404 Not Found` *(Service accounts are protected and masked from the directory).*

#### C. Upstream Keycloak Outage (Resilience Test):
Stop the Keycloak container:
```bash
docker compose stop keycloak
```
Then query the CommaFeed endpoint:
```bash
curl -s -i -u admin:admin "http://localhost:8083/rest/admin/directory/users" | grep "HTTP"
```
**Expected Status**: `HTTP/1.1 502 Bad Gateway`  
*(In the frontend UI, this displays the user-friendly alert: `Keycloak Service Unavailable: Unable to connect to Keycloak Admin REST API. Please check server configuration.`)*

Restart Keycloak after testing:
```bash
docker compose start keycloak
```

---

## 6. Web UI Flow Walkthrough

1. Open your browser and navigate to **`http://localhost:8080`** (or CommaFeed URL).
2. Log in using administrator credentials (`admin` / `admin`).
3. Navigate to **Admin** $\rightarrow$ **User Directory** (`/admin/directory`).
4. **List View**:
   - Confirm the table renders columns: **Username**, **Email**, **Name**, **Enabled**, and **Actions**.
   - Confirm `alice`, `bob`, `charlie`, and `david` appear in the list.
   - Confirm `david` shows a red **Disabled** badge, while others show green **Enabled** badges.
5. **Search**:
   - Type `charlie` in the search input; observe the table updates to show only Charlie Brown.
   - Clear the search input; observe all users reappear.
6. **User Details Modal**:
   - Click the **Details** button on `alice`'s row.
   - A modal opens displaying:
     - **UUID**: `a1000000-0000-0000-0000-000000000001`
     - **Username**: `alice`
     - **Email**: `alice@example.com`
     - **Realm Roles**: `admin`, `user` (rendered as blue badges)
     - **Groups**: `Engineering` (rendered as teal badges)
   - Close the modal.

---

## 7. Automated Test Suite Execution

To verify the implementation across backend and frontend, execute the following test suites:

### 7.1 Backend Unit Tests
Validates DTO mappings, pagination resolution, service account filtering, upstream error mapping (404, 502), and verifies that list queries never trigger role or group HTTP requests:
```bash
./mvnw -pl commafeed-server test -Dtest=KeycloakServiceTest
```

### 7.2 Backend Integration & Security Tests
Validates that endpoints are secured with `@RolesAllowed(Roles.ADMIN)` and return `403 Forbidden` for non-admin accounts:
```bash
./mvnw -pl commafeed-server test -Dtest=KeycloakAdminIT
```

### 7.3 Frontend Unit Tests
Validates table rendering, absence of sorting columns, search input bindings, detail modal opening/closing, and Keycloak outage error banner:
```bash
cd commafeed-client
npm test -- src/pages/admin/KeycloakUserDirectoryPage.test.tsx --run
```

---

## 8. Summary of Key Files

| File | Purpose |
|---|---|
| `keycloak/import/commafeed-realm.json` | Keycloak realm export containing roles, groups, clients, and test users |
| `docker-compose.yml` | Container definition running Keycloak 24.0.5 on port `8081` |
| `commafeed-server/src/main/java/.../KeycloakService.java` | Core service layer handling Keycloak interaction & business logic |
| `commafeed-server/src/main/java/.../DefaultKeycloakClient.java` | HTTP client for Keycloak Admin REST API |
| `commafeed-server/src/main/java/.../DefaultKeycloakTokenProvider.java` | Client credentials OAuth2 token provider & cache |
| `commafeed-server/src/main/java/.../KeycloakAdminREST.java` | JAX-RS REST endpoints for the CommaFeed admin UI |
| `commafeed-server/src/test/java/.../KeycloakServiceTest.java` | Unit tests for service layer & DTO mapping |
| `commafeed-server/src/test/java/.../KeycloakAdminIT.java` | Integration test for admin RBAC security |
| `commafeed-client/src/pages/admin/KeycloakUserDirectoryPage.tsx` | Frontend React/Mantine user directory interface |
| `commafeed-client/src/pages/admin/KeycloakUserDirectoryPage.test.tsx` | Frontend Vitest/Testing Library test suite |
