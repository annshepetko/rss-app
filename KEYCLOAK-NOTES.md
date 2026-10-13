# Keycloak Integration Engineering Decision Record (Level 3)

## 1. Local Environment Management & Endpoints
### Starting, Stopping, and Resetting Keycloak
- **Start Keycloak**: `docker compose up -d`
- **Inspect Logs**: `docker compose logs -f keycloak`
- **Stop Keycloak**: `docker compose down`
- **Reset Environment & Re-import Realm**: `docker compose down -v && docker compose up -d`
  - By destroying containers and volumes, Keycloak starts afresh and automatically imports `./keycloak/import/commafeed-realm.json` into its embedded store on boot.

### Realm Name & Local Endpoints
- **Realm Name**: `commafeed`
- **Keycloak Local Port**: `8081` (default, configurable via `KEYCLOAK_PORT`)
- **Keycloak Web Admin Console**: `http://localhost:8081` (Admin: `admin` / `admin`)
- **Keycloak Token Endpoint**: `http://localhost:8081/realms/commafeed/protocol/openid-connect/token`
- **Keycloak Admin REST Users**: `http://localhost:8081/admin/realms/commafeed/users`
- **CommaFeed Application Endpoints**:
  - `GET /rest/admin/directory/users?search=&first=&max=`
  - `GET /rest/admin/directory/users/{keycloakId}`

---

## 2. Configuration & Environment Variables
The CommaFeed backend configures the Keycloak integration via application properties and environment variables:

| Variable | Default Value | Description |
|---|---|---|
| `KEYCLOAK_PORT` | `8081` | Host port mapped to Keycloak container |
| `KEYCLOAK_ADMIN` | `admin` | Bootstrap Keycloak admin username |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin` | Bootstrap Keycloak admin password |
| `KEYCLOAK_URL` | `http://localhost:8081` | Keycloak server base URL for CommaFeed |
| `KEYCLOAK_REALM` | `commafeed` | Keycloak realm name |
| `KEYCLOAK_CLIENT_ID` | `commafeed-service` | Confidential client ID for CommaFeed |
| `KEYCLOAK_CLIENT_SECRET` | `secret` | Confidential client secret (OAuth2 client credentials) |
| `KEYCLOAK_CONNECT_TIMEOUT`| `5s` | HTTP connection timeout |
| `KEYCLOAK_REQUEST_TIMEOUT`| `5s` | HTTP request read timeout |

Secrets and credentials are decoupled from repository files and managed locally via `.env` (template in `.env.example`).

---

## 3. Service Account Authentication & Client Permissions
- **Authentication Flow**: Uses the standard OpenID Connect `client_credentials` grant type. The service account authenticates with Keycloak's token endpoint using `client_id` and `client_secret` to obtain a short-lived Bearer access token. Tokens are cached in-memory with expiration buffer management.
- **Required Client Roles/Permissions**:
  - The confidential client `commafeed-service` has `serviceAccountsEnabled: true`.
  - The service account is granted the minimum client roles under `realm-management`:
    - `view-users`: Required to inspect user profiles, role mappings (`/role-mappings/realm`), and group memberships (`/groups`).
    - `query-users`: Required to query the paginated `/users` list.
    - `query-groups`: Required to read group definitions.
- **Principle of Least Privilege**: The service account is strictly read-only. It has no `manage-users` or `manage-realm` permissions, ensuring CommaFeed cannot modify Keycloak users or realm settings.

Official Reference: [Keycloak Service Accounts Documentation](https://www.keycloak.org/docs/latest/server_admin/#_service_accounts)

---

## 4. End-to-End Keycloak UUID Usage
- Keycloak UUIDs (`id`) are immutable, globally unique identifiers assigned by Keycloak upon user creation.
- Using Keycloak UUIDs end-to-end (in API paths, DTOs, and frontend navigation) prevents ambiguity caused by mutable fields (such as `username` or `email`) and guarantees reliable user lookup without creating mirrors or local primary keys.

---

## 5. Separation of Data: List vs. Detail Responses
- **List Endpoint (`GET /rest/admin/directory/users`)**: Returns lightweight summary DTOs containing basic identity fields (`keycloakId`, `username`, `email`, `firstName`, `lastName`, `enabled`).
- **Detail Endpoint (`GET /rest/admin/directory/users/{keycloakId}`)**: Returns enriched DTOs containing roles and group memberships.
- **Rationale**: The Keycloak Admin API does not embed role mappings or groups within the bulk `/users` endpoint. Fetching roles and groups for every user in a list would trigger an $N+1$ HTTP request anti-pattern ($1 + 2N$ network requests per page), severely degrading latency and performance.

---

## 6. Limitations: Filtering, Pagination, Counting & Sorting
- **Sorting**: The Keycloak Admin API does not support server-side sorting parameters on `/admin/realms/{realm}/users`. Sorting controls are intentionally omitted from both API parameters and UI components.
- **Counting**: Keycloak provides `/admin/realms/{realm}/users/count`, but running separate count queries when service accounts are present creates discrepancies if service accounts are excluded in memory.
- **Service Account Exclusion & Pagination Reconciliation**: Service accounts are filtered out during list processing (`service-account-*` or `serviceAccountClientId != null`). To maintain accurate page sizes, the backend requests records with requested page bounds (`first`, `max`) and excludes non-regular users before returning results to the client.

---

## 7. Upstream Errors, Timeout & Security Boundaries
- **Timeout Handling**: HTTP client connections to Keycloak are configured with a strict 5-second connection and response timeout (`KEYCLOAK_CONNECT_TIMEOUT`, `KEYCLOAK_REQUEST_TIMEOUT`).
- **Error Status Mapping**:
  - Keycloak `404 Not Found` $\rightarrow$ CommaFeed `404 Not Found` (`KeycloakUserNotFoundException`).
  - Keycloak `401/403/5xx` or connectivity timeout $\rightarrow$ CommaFeed `502 Bad Gateway` (`KeycloakUpstreamException`).
  - Non-admin CommaFeed user $\rightarrow$ CommaFeed `403 Forbidden` (`@RolesAllowed(Roles.ADMIN)`).
- **Security & Data Sanitization**: Upstream Keycloak response bodies, Java stack traces, access tokens, client secrets, and `Authorization` headers are sanitized and never exposed to the client or written to application logs.

---

## 8. AI Recommendation Verification & Correction
- **AI Recommendation**: An AI initial suggestion proposed calling `/admin/realms/{realm}/users?briefRepresentation=false` to get user roles and groups directly inside the list query.
- **Verification against Official Docs**: Verified Keycloak Admin API specification for `GET /admin/realms/{realm}/users`. Official documentation confirms `briefRepresentation` controls attributes and credential summaries, but does **not** embed realm role mappings or group membership arrays in the user list response.
- **Correction**: Rejected the single-call recommendation and properly decoupled list responses (basic attributes) from detail responses (fetching `/role-mappings/realm` and `/groups` on-demand for single user inspection).

Official Reference: [Keycloak Admin REST API Documentation](https://www.keycloak.org/docs-api/latest/rest-api/index.html)

---

## 9. Verification & Testing
### Automated Tests
- **Backend Unit Tests**:
  ```bash
  ./mvnw -pl commafeed-server test -Dtest=KeycloakServiceTest
  ```
  Validates DTO mapping, UUID propagation, search/pagination forwarding, service-account exclusion, 404 handling, upstream failure mapping, and confirms list requests do not fetch roles/groups.
- **Backend Integration Tests**:
  ```bash
  ./mvnw -pl commafeed-server test -Dtest=KeycloakAdminIT
  ```
  Validates non-admin 403 Forbidden access control.
- **Frontend Unit Tests**:
  ```bash
  cd commafeed-client && npm test -- src/pages/admin/KeycloakUserDirectoryPage.test.tsx --run
  ```
  Validates table rendering, absence of sorting icons, detail modal loading roles/groups by UUID, and Keycloak-unavailable error alerts.

### Manual Smoke Test Verification
1. Start Keycloak: `docker compose up -d`
2. Start CommaFeed: `./mvnw -pl commafeed-server quarkus:dev`
3. Verify regular user list:
   ```bash
   curl -s -u admin:admin "http://localhost:8083/rest/admin/directory/users" | jq .
   ```
4. Verify user detail with roles and groups:
   ```bash
   curl -s -u admin:admin "http://localhost:8083/rest/admin/directory/users/a1000000-0000-0000-0000-000000000001" | jq .
   ```
