# How Keycloak Works in This Project

Статус: `active`

## 1. Назначение

Этот документ фиксирует рабочий flow подключения внешнего Keycloak Realm к системе электронного журнала:

- backend API через Edge `http://localhost:8080`;
- внутренний Domain Service `http://localhost:8081`;
- Web frontend preview `http://localhost:4173`;
- mobile client.

Keycloak больше не хранится в `config/keycloak` и не разворачивается этим репозиторием. Он находится во внешнем серверном контуре. Источник истины по фактическим clients, roles и protocol mappers — админка/выгрузка внешнего тестового Realm.

Документ сверен с runtime-кодом:

- `internal/edge/server/server.go`;
- `internal/edge/middleware/auth.go`;
- `internal/edge/middleware/profile_sync.go`;
- `internal/edge/config/config.go`;
- `internal/domain/server/auth_handlers.go`.

## 2. Ключевое решение

Одного `journal-backend` client недостаточно для схемы backend + web + mobile.

Нужно минимум три OIDC client:

| Client ID | Тип | Кто использует | Назначение |
|---|---|---|---|
| `journal-backend` | API / resource server | Edge backend | Audience защищаемого API. Пользователя не логинит. |
| `journal-web` | public OIDC client | Web frontend на `http://localhost:4173` | Логин пользователя через Authorization Code + PKCE. |
| `journal-mobile` | public OIDC client | Mobile app | Логин пользователя через Authorization Code + PKCE и deep link/app link. |

Web и mobile получают access token в Keycloak и ходят в backend:

```http
Authorization: Bearer <access_token>
```

Backend принимает только access token. `id_token` и `refresh_token` в API не отправляются.

## 3. Runtime-Схема

```text
Web frontend :4173 / Mobile
    |
    | 1. Authorization Code + PKCE
    v
External Keycloak Realm
    |
    | 2. login via local users / LDAP / AD federation
    v
Web frontend :4173 / Mobile
    |
    | 3. token endpoint: code + code_verifier -> tokens
    | 4. Authorization: Bearer <access_token>
    v
Edge backend :8080
    |
    | 5. OIDC/JWT validation + route RBAC
    | 6. internal HTTP call
    v
Domain Service :8081
    |
    | 7. profile link/upsert + business permissions
    v
PostgreSQL
```

Edge — единственная внешняя API-точка. Domain Service не принимает пользовательский Bearer token и не общается с Keycloak напрямую.

## 4. Что Проверяет Backend Сейчас

Текущий код делает следующее:

| Проверка | Где | Статус |
|---|---|---|
| OIDC discovery по `{KEYCLOAK_URL}/realms/{KEYCLOAK_REALM}` | `server.go` | реализовано |
| JWKS/JWT signature через `go-oidc` | `server.go`, `auth.go` | реализовано |
| `iss` | `go-oidc` provider/verifier | реализовано |
| `exp` | `go-oidc` verifier | реализовано |
| `aud` содержит `KEYCLOAK_CLIENT_ID` | `provider.Verifier(&oidc.Config{ClientID: ...})` | реализовано |
| `typ == Bearer` | `auth.go` | реализовано |
| роли из `realm_access.roles` | `auth.go` | реализовано |
| роли из `resource_access[KEYCLOAK_ROLES_CLIENT_ID].roles` | `auth.go` | реализовано |
| route-level RBAC | `RequireRoles(...)` | реализовано |
| business permissions по БД | Domain services/repositories | реализовано по flow |
| `azp == journal-web/journal-mobile` | нет | не реализовано, см. TODO |

Важно: код не использует `KEYCLOAK_ISSUER`, `KEYCLOAK_AUDIENCE` или `KEYCLOAK_JWKS_URL`. Сейчас issuer и discovery строятся из:

```env
KEYCLOAK_URL=https://sso.example.edu
KEYCLOAK_REALM=Test
```

Audience задаётся через:

```env
KEYCLOAK_CLIENT_ID=journal-backend
```

## 5. Backend Client `journal-backend`

`journal-backend` представляет API, а не приложение для логина пользователя.

Рекомендуемые настройки во внешнем Realm:

```text
Client ID: journal-backend
Client type: OpenID Connect
Client authentication: OFF, если backend только валидирует JWT по JWKS
Standard flow: OFF
Direct access grants: OFF
Implicit flow: OFF
Service accounts: OFF, пока нет machine-to-machine сценариев
Valid redirect URIs: пусто
Web origins: пусто
```

Если когда-нибудь понадобится token introspection, admin API или service account, тогда `journal-backend` нужно будет сделать confidential client и включить client authentication. Текущий backend этого не требует: он валидирует JWT локально через discovery/JWKS.

Backend env:

```env
APP_ENV=prod
DEBUG_BYPASS_AUTH=false
KEYCLOAK_URL=https://sso.example.edu
KEYCLOAK_REALM=Test
KEYCLOAK_CLIENT_ID=journal-backend
KEYCLOAK_ROLES_CLIENT_ID=journal-backend
KEYCLOAK_TLS_SKIP_VERIFY=false
CORS_ALLOWED_ORIGINS=http://localhost:4173
DOMAIN_SERVICE_URL=http://domain:8081
```

Локально при запуске Edge на хосте API доступен на:

```text
http://localhost:8080/api/v1/*
http://localhost:8080/ready
```

Domain Service:

```text
http://localhost:8081/ready
```

## 6. Web Client `journal-web`

Web frontend работает как public client. Для нашего локального frontend preview используется порт `4173`.

Настройки:

```text
Client ID: journal-web
Client type: OpenID Connect
Client authentication: OFF
Standard flow: ON
PKCE: S256
Direct access grants: OFF
Implicit flow: OFF
```

Redirect URIs:

```text
http://localhost:4173/*
https://journal.example.edu/*
```

Web origins:

```text
http://localhost:4173
https://journal.example.edu
```

В production не ставить `*` в redirect URI или web origins.

Web flow:

```text
1. Frontend :4173 открывает Keycloak auth endpoint:
   /realms/Test/protocol/openid-connect/auth
     ?client_id=journal-web
     &redirect_uri=http://localhost:4173/callback
     &response_type=code
     &scope=openid profile email
     &code_challenge=...
     &code_challenge_method=S256

2. Keycloak логинит пользователя.

3. Keycloak возвращает:
   http://localhost:4173/callback?code=...

4. Frontend меняет code на tokens:
   POST /realms/Test/protocol/openid-connect/token
   grant_type=authorization_code
   client_id=journal-web
   code=...
   redirect_uri=http://localhost:4173/callback
   code_verifier=...

5. Frontend вызывает API:
   GET http://localhost:8080/api/v1/academic-periods
   Authorization: Bearer <access_token>
```

Для SPA access token хранить в памяти приложения. Не сохранять токены в `localStorage`/`sessionStorage` без отдельного security decision.

## 7. Mobile Client `journal-mobile`

Mobile app тоже public client.

Настройки:

```text
Client ID: journal-mobile
Client type: OpenID Connect
Client authentication: OFF
Standard flow: ON
PKCE: S256
Direct access grants: OFF
Implicit flow: OFF
```

Redirect URI варианты:

```text
com.university.journal:/oauth2redirect
https://journal.example.edu/mobile/callback
```

Для production предпочтительны Universal Links / App Links. Mobile не должен показывать форму логина Keycloak во встроенной WebView; использовать системный браузер, Custom Tabs или ASWebAuthenticationSession.

Mobile flow:

```text
1. Mobile открывает системный браузер:
   /realms/Test/protocol/openid-connect/auth
     ?client_id=journal-mobile
     &redirect_uri=com.university.journal:/oauth2redirect
     &response_type=code
     &scope=openid profile email
     &code_challenge=...
     &code_challenge_method=S256

2. Keycloak возвращает:
   com.university.journal:/oauth2redirect?code=...

3. Mobile меняет code + code_verifier на tokens.

4. Mobile вызывает:
   GET http://localhost:8080/api/v1/students/me
   Authorization: Bearer <access_token>
```

## 8. Audience Для Backend

Access token, полученный через `journal-web` или `journal-mobile`, должен быть предназначен для backend API. Поэтому в token payload должен быть:

```json
{
  "aud": ["journal-backend"]
}
```

или:

```json
{
  "aud": ["journal-backend", "account"]
}
```

Без `journal-backend` в `aud` Edge вернёт `401 Invalid or expired token`, потому что `go-oidc` verifier настроен с:

```go
provider.Verifier(&oidc.Config{ClientID: cfg.KeycloakClientID})
```

Рекомендуемая настройка в Keycloak:

```text
Client scopes -> Create client scope
Name: journal-api
Protocol: OpenID Connect

journal-api -> Mappers -> Add mapper -> Audience
Included Client Audience: journal-backend
Add to access token: ON
Add to ID token: OFF

Clients -> journal-web -> Client scopes -> add journal-api
Clients -> journal-mobile -> Client scopes -> add journal-api
```

Для MVP проще подключить `journal-api` как default scope. Если сделать optional scope, frontend/mobile должны явно запрашивать `scope=openid profile email journal-api`.

## 9. Роли

Текущие роли проекта:

```text
student
teacher
dean
methodologist
admin
```

Роли `curator` в текущем backend нет. Не создавать её для этого сервиса, пока роль не появится в `access-control-matrix.md` и `RequireRoles(...)`.

Рекомендуемая модель для внешнего Realm: client roles на `journal-backend`.

```text
Clients -> journal-backend -> Roles
  student
  teacher
  dean
  methodologist
  admin
```

Тогда access token содержит:

```json
{
  "resource_access": {
    "journal-backend": {
      "roles": ["teacher"]
    }
  }
}
```

Backend читает эти роли при:

```env
KEYCLOAK_ROLES_CLIENT_ID=journal-backend
```

Также код поддерживает `realm_access.roles`. Это оставлено для совместимости и тестовых контуров:

```json
{
  "realm_access": {
    "roles": ["teacher"]
  }
}
```

Если во внешнем Realm принято хранить роли только как realm roles, можно поставить:

```env
KEYCLOAK_ROLES_CLIENT_ID=-
```

## 10. Claims, Нужные Для Профилей

Edge извлекает из access token:

```json
{
  "sub": "keycloak-user-uuid",
  "email": "ivanov@university.local",
  "preferred_username": "ivanovii",
  "given_name": "Иван",
  "family_name": "Иванов"
}
```

В этом проекте нет MVP-таблицы `users`.

Связка с БД такая:

| Роль | Keycloak claim | Таблица/поле |
|---|---|---|
| `teacher`, `admin`, `dean`, `methodologist` | `sub` | `teacher_profiles.keycloak_id` через `POST /internal/v1/auth/register-login` |
| `student` | `sub` | `students.student_id` через `POST /internal/v1/students/link` |

Для students link-процедура матчится по приоритету:

1. `email`;
2. `preferred_username`;
3. `given_name + family_name` как fallback, только если найден ровно один unlinked student.

Ошибки profile sync не блокируют пользовательский запрос: middleware запускает синхронизацию асинхронно с timeout 3 секунды.

## 11. Refresh Flow

Edge не обновляет токены. Refresh — ответственность web/mobile клиента.

```text
access token истёк
  -> API :8080 вернул 401
  -> client вызывает Keycloak token endpoint с grant_type=refresh_token
  -> получает новый access token
  -> повторяет API-запрос
```

Для refresh использовать client id того приложения, которое логинило пользователя:

```text
journal-web
journal-mobile
```

Не использовать `journal-backend` для refresh пользовательских сессий web/mobile.

## 12. Что Не Включать

Для `journal-web` и `journal-mobile`:

```text
Direct Access Grants: OFF
Implicit Flow: OFF
Client authentication: OFF
```

Direct Access Grants/password grant допустим только как временный ручной тест в отдельном dev-клиенте. Web/mobile не должны принимать пароль пользователя и не должны отправлять логин/пароль в token endpoint.

Для `journal-backend`:

```text
Standard Flow: OFF
Implicit Flow: OFF
Direct Access Grants: OFF
Redirect URIs: empty
Web Origins: empty
```

## 13. Проверка Access Token

После логина через `journal-web` decoded access token должен выглядеть примерно так:

```json
{
  "iss": "https://sso.example.edu/realms/Test",
  "aud": ["journal-backend"],
  "azp": "journal-web",
  "typ": "Bearer",
  "sub": "keycloak-user-uuid",
  "preferred_username": "ivanovii",
  "email": "ivanov@university.local",
  "given_name": "Иван",
  "family_name": "Иванов",
  "resource_access": {
    "journal-backend": {
      "roles": ["teacher"]
    }
  }
}
```

Для mobile:

```json
{
  "azp": "journal-mobile"
}
```

На 2026-05-19 `azp` полезен для диагностики, но backend его не валидирует. Для обязательной проверки `azp` нужен отдельный кодовый change.

## 14. Диагностика

| Симптом | Вероятная причина | Что делать |
|---|---|---|
| `401 Authorization header required` | Нет Bearer token | Передать `Authorization: Bearer <access_token>` |
| `401 Invalid or expired token` сразу после логина | `aud` не содержит `journal-backend`, неверный issuer/realm или токен истёк | Проверить `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, mapper `journal-api` |
| `401 Invalid token type: access token required` | В API отправлен `id_token` | Отправлять только `access_token` |
| `403 Insufficient permissions` и `user_roles: []` в логах Edge | Роль не попала в `realm_access` или `resource_access.journal-backend` | Назначить роль и проверить `KEYCLOAK_ROLES_CLIENT_ID` |
| `403 Insufficient permissions`, роль есть | Роль не разрешена на route | Сверить `RequireRoles(...)` в `internal/edge/server/server.go` и `access-control-matrix.md` |
| CORS error из frontend `:4173` | Origin не добавлен в backend env | Добавить `http://localhost:4173` в `CORS_ALLOWED_ORIGINS` |
| Edge не стартует | Keycloak discovery недоступен | Проверить `{KEYCLOAK_URL}/realms/{KEYCLOAK_REALM}/.well-known/openid-configuration` |

## 15. Чеклист Настройки Realm

1. Оставить/создать `journal-backend`.
2. Настроить `journal-backend` как API/resource server: flows off, redirect/web origins empty.
3. Создать client roles на `journal-backend`: `student`, `teacher`, `dean`, `methodologist`, `admin`.
4. Создать `journal-web`: public, Standard flow on, PKCE S256, redirect URI `http://localhost:4173/*`.
5. Создать `journal-mobile`: public, Standard flow on, PKCE S256, mobile redirect URI.
6. Создать client scope `journal-api`.
7. В `journal-api` добавить Audience mapper на `journal-backend`, access token on, ID token off.
8. Подключить `journal-api` к `journal-web` и `journal-mobile`.
9. Назначить пользователям роли из `journal-backend` client roles или, временно, realm roles.
10. Декодировать access token и проверить `iss`, `aud`, `typ`, `sub`, `email`, `preferred_username`, roles.
11. Проверить API:

```bash
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/academic-periods
```

## 16. Открытые Хвосты

| Хвост | Почему открыт | Где вести |
|---|---|---|
| Сверить фактический внешний тестовый Realm | Realm не хранится в репозитории | `TODO.md`: Keycloak Realm для backend/web/mobile |
| Проверка `azp` | В присланном целевом flow есть, но текущий код её не делает | `TODO.md`: добавить allow-list `journal-web,journal-mobile` |
| Mobile redirect URI | Нужен фактический bundle/package id mobile app | после появления mobile проекта |

## 17. Связанные Файлы

| Файл | Что описывает |
|---|---|
| `internal/edge/middleware/auth.go` | Bearer token, JWT claims, роли, `typ == Bearer` |
| `internal/edge/middleware/profile_sync.go` | Асинхронная связь Keycloak `sub` с профилями |
| `internal/edge/server/server.go` | OIDC provider, CORS, routes, `RequireRoles(...)` |
| `internal/edge/config/config.go` | `KEYCLOAK_*`, `DEBUG_BYPASS_AUTH`, `CORS_ALLOWED_ORIGINS` |
| `internal/domain/server/auth_handlers.go` | `teacher_profiles.keycloak_id`, `students.student_id` |
| `profile-identity-model.md` | Каноническая модель identity/profile |

---

- Версия: 2.0
- Дата: 2026-05-19
- Основано на: анализе runtime-кода и решении о внешнем Keycloak Realm
