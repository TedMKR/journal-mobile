Здесь часто путают **две разные вещи**: откуда берутся **ключи подписи** и зачем в конфиге стоит **`journal-backend`**.

## Backend не ходит «в client за ключами»

Edge подключается к **Realm**, а не к конкретному OAuth-client:

```52:74:internal/edge/server/server.go
issuerURL := fmt.Sprintf("%s/realms/%s", cfg.KeycloakURL, cfg.KeycloakRealm)
// ...
provider, err := newOIDCProviderWithRetry(oidcCtx, issuerURL, logger)
// ...
authMiddleware = edgemw.Auth(
    provider.Verifier(&oidc.Config{ClientID: cfg.KeycloakClientID}),
```

`oidc.NewProvider` тянет discovery:

`https://sso.example.edu/realms/Test/.well-known/openid-configuration`

Оттуда — **`jwks_uri`** вроде:

`https://sso.example.edu/realms/Test/protocol/openid-connect/certs`

**Один набор ключей на весь Realm.** Токены от `journal-web`, `journal-mobile` и любого другого client в этом Realm подписаны **теми же** ключами. Отдельных «ключей journal-web» у Keycloak нет.

`KEYCLOAK_CLIENT_ID=journal-backend` в env **не выбирает endpoint для JWKS**. Это имя попадает в `oidc.Config{ClientID: ...}` и влияет на проверку claim **`aud`** (audience): «этот JWT предназначен для нашего API».

## Три client — три роли

| Client | Кто с ним работает | Зачем |
|--------|-------------------|--------|
| `journal-web` / `journal-mobile` | SPA, мобилка | **Логин** пользователя (Authorization Code + PKCE), получение токенов |
| `journal-backend` | Только конфиг Edge | **Метка API** в `aud` — «токен выпущен для этого resource server» |

Схема по потоку:

```text
Web/Mobile  --(client_id=journal-web|mobile)-->  Keycloak  --логин-->  токены
     |
     |  Authorization: Bearer <access_token>
     v
Edge  --(issuer + JWKS realm)-->  проверка подписи
     --(aud должен содержать journal-backend)-->  «токен для нашего API»
     --(azp = journal-web|mobile, пока не проверяем)-->  «кто запросил токен»
```

Web/mobile **не логинятся как `journal-backend`**. Они логинятся своим `client_id`, а в access token обычно:

- **`azp`** — кто запросил токен (`journal-web` или `journal-mobile`);
- **`aud`** — для кого токен (`journal-backend`, если так настроен audience mapper в Realm).

Backend проверяет: подпись realm-ключами + `aud` = API. Отсюда в env и стоит `journal-backend`, а не `journal-web`.

## Почему не `journal-web` в `KEYCLOAK_CLIENT_ID`

Если поставить `KEYCLOAK_CLIENT_ID=journal-web`, verifier будет требовать **`aud` = journal-web**.  
Access token для API обычно выпускают с **`aud` = resource server** (`journal-backend`), а не с aud логин-клиента. Проверка бы **отклоняла нормальные** токены после логина через web/mobile.

`journal-backend` в конфиге — это **«я — защищаемый API»**, не **«я — приложение, которое логинит пользователя»**.

## Как это стыкуется с `journal-backend` в Keycloak

В доке `journal-backend` — client **без login flow**: без redirect URI, без Standard flow. Он нужен в админке как **audience / resource** и для client roles в `resource_access.journal-backend.roles`, если роли вешают на API-client.

Edge **не** ходит в Keycloak за токеном от имени `journal-backend` (нет client secret, нет password grant в нормальной схеме). Он только:

1. качает JWKS Realm (кэш `go-oidc`);
2. проверяет JWT, который прислал браузер/мобилка.

## Кратко

- **Ключи** — у **Realm**, один JWKS для всех clients.
- **`journal-backend` в env** — про **`aud`**, не про URL ключей.
- **`journal-web` / `journal-mobile`** — про **логин**; их токены backend принимает, если подпись валидна и `aud` включает `journal-backend`.

Имена в env сбивают с толку: точнее было бы `KEYCLOAK_AUDIENCE=journal-backend`, но сейчас это то же поле, что `KEYCLOAK_CLIENT_ID`.

---

Redirect URI **не про API** и **не про `localhost:8080`**. Это адрес, куда Keycloak **возвращает пользователя после логина** с `?code=...` в Authorization Code + PKCE. Дальше приложение само меняет `code` на токены и уже как web шлёт:

```http
GET …/api/v1/…
Authorization: Bearer <access_token>
```

на Edge (`8080` / prod API). Этот шаг у mobile и web **одинаковый**; отличается только **куда Keycloak редиректит с code**.

## Два разных URL

| URL | Кто | Зачем |
|-----|-----|--------|
| **Redirect URI** | Keycloak → приложение | Завершить OAuth: передать `code` в web или mobile |
| **API base URL** | Приложение → Edge | Бизнес-запросы с Bearer token |

Web: redirect = `http://localhost:4173/callback` (где крутится SPA).  
Mobile: redirect = **схема или deep link приложения**, не backend.

## Что писать в Keycloak для `journal-mobile`

В Valid redirect URIs — **ровно те значения**, которые mobile передаёт в `redirect_uri` при `/auth` (символ в символ).

**Вариант 1 — custom URL scheme (проще для dev / нативного приложения):**

```text
com.university.journal:/oauth2redirect
```

- `com.university.journal` — bundle id / application id (как в App Store / Play).
- `/oauth2redirect` — путь (часто так называют; можно другой, главное — совпадение в app и в KC).
- В Keycloak иногда пишут с `*` только если KC это разрешает для схемы; лучше **один точный URI**, без `*`.

После логина Keycloak откроет что-то вроде:

`com.university.journal:/oauth2redirect?code=...&state=...`

ОС передаст это в приложение; приложение заберёт `code` и вызовет token endpoint.

**Вариант 2 — HTTPS (production, App Links / Universal Links):**

```text
https://journal.example.edu/mobile/callback
```

- Домен **ваш**, на нём настроены **Digital Asset Links** (Android) / **apple-app-site-association** (iOS).
- По этому URL приложение перехватывает редирект без «сырой» custom scheme (удобнее для prod).

Оба варианта в доке — это **альтернативы одного механизма**, не «API + что-то ещё». Обычно в prod выбирают один основной (часто HTTPS + App Links), в dev — custom scheme.

## Чего в redirect **не** ставить

- `http://localhost:8080/...` — это Edge API, Keycloak туда пользователя после логина **не** ведёт (если только вы сами не делаете OAuth через backend, у вас этого нет).
- URL web-приложения (`http://localhost:4173/...`) — это для `journal-web`, не для mobile client.
- `*` в production redirect — плохая практика.

## Если «mobile чисто API как web»

Имеется в виду только **шаг 4** (Bearer на `/api/v1/*`). Шаги 1–3 OAuth всё равно нужны:

1. Открыть Keycloak auth с `client_id=journal-mobile` и **своим** `redirect_uri`.
2. Получить `code` на redirect приложения.
3. `POST …/token` с `code` + `code_verifier`.
4. API с тем же `access_token`, что и web (с тем же `aud=journal-backend`, если так настроен Realm).

Без redirect URI в KC mobile **не завершит логин**, даже если API тот же.

## Практическая рекомендация для вашего проекта

1. Зафиксировать в mobile-коде один `redirect_uri` (например `com.university.journal:/oauth2redirect` для dev).
2. В Keycloak client `journal-mobile` → Valid redirect URIs — **только этот** (+ prod HTTPS, если будет второй flavor).
3. Web origins для mobile client обычно **не нужны** (это не browser SPA на origin); важны redirect URIs.
4. API в `.env` mobile: `https://api…` или `http://localhost:8080` — отдельная настройка, не путать с redirect.

Итого: в redirect пишете **куда вернуться с `code` после логина в приложении**, а не куда ходить за журналом. Для нативного app — custom scheme или verified HTTPS deep link; примеры в доке как раз про это, а не про дублирование API URL web.

---

В админке Keycloak это **настройки OAuth-клиента (приложения)**, а не API. Поля связаны, но про разные этапы: логин, logout и (для браузера) CORS к **самому Keycloak**.

Ниже — что куда для вашей схемы (`journal-backend`, `journal-web`, `journal-mobile`).

---

## Что означает каждое поле

| Поле | Зачем |
|------|--------|
| **Root URL** | Базовый URL приложения. От него Keycloak может собирать **относительные** redirect/logout (например Root + `/callback`). Удобство в админке, не замена API. |
| **Home URL** | «Вернуться в приложение» (ссылки в UI Keycloak / account). Обычно главная страница SPA. |
| **Valid redirect URIs** | Куда Keycloak **обязан** редиректить с `?code=` после логина. Должно **совпадать** с `redirect_uri` в запросе auth/token. |
| **Valid post logout redirect URIs** | Куда вернуть после **logout** (`end_session_endpoint`), если приложение передаёт `post_logout_redirect_uri`. |
| **Web origins** | CORS для **браузерных** запросов **к Keycloak** (не к вашему `:8080`). Нужно web-клиенту, если SPA с origin X ходит на token endpoint KC. |

**API (`localhost:8080` / prod API)** в эти поля **не пишется** — только URL **frontend / mobile app** (или custom scheme).

---

## `journal-backend` (только API / audience)

Логина пользователя нет → почти всё пусто:

| Поле | Значение |
|------|----------|
| Root URL | пусто |
| Home URL | пусто |
| Valid redirect URIs | пусто |
| Valid post logout redirect URIs | пусто |
| Web origins | пусто |

---

## `journal-web` (SPA на `:4173` / prod домен)

| Поле | Local | Production (пример) |
|------|--------|---------------------|
| **Root URL** | `http://localhost:4173` | `https://journal.example.edu` |
| **Home URL** | `http://localhost:4173/` или `/` | `https://journal.example.edu/` |
| **Valid redirect URIs** | `http://localhost:4173/*` **или** точнее `http://localhost:4173/callback` | `https://journal.example.edu/callback` (без `*` в prod, как в доке) |
| **Valid post logout redirect URIs** | `http://localhost:4173/*` или `http://localhost:4173/`, `.../login` | те же origin + путь, куда реально редиректите после logout |
| **Web origins** | `http://localhost:4173` | `https://journal.example.edu` |

Важно:

- В коде SPA `redirect_uri` при auth и при обмене code **должен быть один и тот же** (у вас в доке — `http://localhost:4173/callback`).
- **Web origins** = origin SPA (`scheme + host + port`), **без** пути. Не путать с CORS Edge — Edge настраивается отдельно (`CORS_ALLOWED_ORIGINS`).
- **Root + относительный redirect** (`/callback`) работает, если Root = `http://localhost:4173`; можно и только полные URI в Valid redirect — как удобнее.

---

## `journal-mobile` (нативное приложение)

Тут **нет** «сайта» как у SPA; API тот же, redirect — **в приложение**.

| Поле | Рекомендация |
|------|----------------|
| **Root URL** | часто **пусто**; либо `https://journal.example.edu`, если используете только HTTPS deep link и относительные пути |
| **Home URL** | **пусто** (для native обычно не нужен) |
| **Valid redirect URIs** | ровно то, что шлёт app, например `com.university.journal:/oauth2redirect` и/или `https://journal.example.edu/mobile/callback` |
| **Valid post logout redirect URIs** | те же URI, если делаете OIDC logout с `post_logout_redirect_uri`; иначе пусто |
| **Web origins** | обычно **пусто** (нет browser origin у нативного flow). Если token exchange идёт с `https://…` в WebView — лучше не WebView; для Custom Tabs иногда добавляют origin, но для чистого native + system browser часто не нужно |

Custom scheme (`com.university.journal:...`) **не** подставляется в Root URL как у http — проще держать **полный redirect** в Valid redirect URIs.

---

## Как это стыкуется с «mobile как web ходит в API»

```text
Keycloak client fields     →  только OAuth (логин / logout / CORS к KC)
API URL (8080, /api/v1)    →  env приложения, не Keycloak redirect
```

Одинаково у web и mobile: после логина **Bearer на Edge**.  
Различается только **Valid redirect** (и у web ещё **Web origins**).

---

## Частые ошибки

1. Писать `http://localhost:8080` в redirect — Keycloak после логина туда не должен ходить.  
2. Путать **Web origins** (KC) и **CORS Edge** (ваш API).  
3. Redirect с `*`, а в коде жёсткий `/callback` — в новых KC лучше явный URI.  
4. Для mobile заполнять Web origins «как у web» — обычно лишнее.  
5. Logout URIs не заполнить, а в SPA вызывать logout с `post_logout_redirect_uri` — Keycloak отклонит logout redirect.

---

## Минимальный чеклист

**Web:** Root/Home = origin SPA → redirect = ваш `/callback` → web origins = тот же origin (без path).  
**Mobile:** только **Valid redirect** (= deep link / scheme в коде) → post logout при необходимости → root/home/web origins чаще пустые.  
**Backend client:** всё пусто, audience настраивается scopes/mappers, не redirect.

Если напишете реальные URL (local web, prod web, bundle id / scheme mobile), можно свести в одну строку на поле без `example.edu`.

---

Создание трёх clients — только шаг 1. Дальше — **донастройка Realm**, **env backend**, **интеграция web/mobile** и **проверка токена + API**.

## 1. В Keycloak (если ещё не сделано)

### `journal-backend`
- Standard / Implicit / Direct Access Grants — **OFF**
- Redirect / Web origins — **пусто**
- Client roles: `student`, `teacher`, `dean`, `methodologist`, `admin`

### Audience (критично)
Без этого Edge даст `401` после логина:

1. Client scope **`journal-api`**
2. Mapper **Audience** → `journal-backend`, **Add to access token: ON**, ID token: OFF
3. Подключить `journal-api` к **`journal-web`** и **`journal-mobile`** (default scope для MVP)

### `journal-web` / `journal-mobile`
- Public, Standard flow ON, **PKCE S256**
- Redirect URI — как в приложении (web: `http://localhost:4173/callback` и prod; mobile: scheme/deep link)
- Root / Home / Web origins — по прошлому разговору (для web — origin SPA)
- Direct Access Grants — **OFF**

### Пользователи
- Назначить роли (лучше **client roles на `journal-backend`**)
- Проверить claims: `email`, `given_name`, `family_name`, `preferred_username` (нужны для profile sync)

Чеклист из доки: §15 в `how-keycloak-works.md`.

---

## 2. Env backend (Edge)

В `.env.local` / `.env.production`:

```env
DEBUG_BYPASS_AUTH=false
KEYCLOAK_URL=https://<ваш-sso>
KEYCLOAK_REALM=<realm>
KEYCLOAK_CLIENT_ID=journal-backend
KEYCLOAK_ROLES_CLIENT_ID=journal-backend
KEYCLOAK_TLS_SKIP_VERIFY=false   # только если legacy TLS на SSO
CORS_ALLOWED_ORIGINS=http://localhost:4173   # + prod origin web
```

`journal-web` / `journal-mobile` в env Edge **не** пишутся — только `journal-backend` (audience + роли).

---

## 3. Web frontend

- OIDC: `client_id=journal-web`, PKCE, `redirect_uri` = тот же, что в KC
- Scope: `openid profile email` (+ `journal-api`, если scope optional)
- В API слать только **`access_token`**, не `id_token`
- API base: `http://localhost:8080` (или prod)
- Refresh — с `client_id=journal-web`, не `journal-backend`

---

## 4. Mobile (когда будет app)

- `client_id=journal-mobile`, свой redirect (scheme / App Link)
- Тот же audience и API, refresh с `journal-mobile`

---

## 5. Проверка end-to-end

**A. Discovery**

```bash
curl -s "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/.well-known/openid-configuration" | head
```

**B. Токен после логина** (jwt.io или `jq`)

Должно быть примерно:

```json
{
  "aud": ["journal-backend"],
  "azp": "journal-web",
  "typ": "Bearer",
  "resource_access": { "journal-backend": { "roles": ["teacher"] } }
}
```

**C. API**

```bash
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/academic-periods
```

Ожидание: **200**, не `401` / `403`.

**D. Edge стартует** — OIDC provider поднимается при старте; если KC недоступен — Edge не встанет (кроме `DEBUG_BYPASS_AUTH=true`).

**E. БД** — для teacher/student: `sub` должен связаться с профилем (seed или link по email/username); иначе API может отвечать, но часть сценариев без данных.

---

## 6. Документация / TODO (не блокер запуска)

| Задача | Когда |
|--------|--------|
| Отметить P2 «сверить Realm» — скрин/выгрузка, зафиксировать фактические URI | после успешного теста |
| P3 `azp` allow-list в коде | по желанию, после стабильного flow |
| Обновить `how-keycloak-works.md` реальными URL вместо `example.edu` | по мере фиксации prod |

---

## Порядок «что сейчас»

1. **Audience mapper** + scopes на web/mobile  
2. **Роли** на пользователях  
3. **Env Edge** + `CORS`  
4. **Логин web** → декодировать token → **curl API**  
5. Потом mobile и prod URL  

Если на каком-то шаге ошибка — смотри §14 диагностики в `how-keycloak-works.md` (`401` почти всегда `aud` или неверный `KEYCLOAK_*`, `403` с пустыми ролями — mapper/назначение ролей).

Напишите, на каком шаге застряли (логин, токен, API) — разберём точечно.


---

Ответ короткий: **ничего вручную с discovery подключать не нужно** — Edge сам ходит по этому URL и качает ключи. Ваш `curl` — проверка, что SSO и Realm доступны.

## Что вы уже проверили

`/.well-known/openid-configuration` — карта Realm:

| Поле в JSON | Зачем |
|-------------|--------|
| `issuer` | Должен совпасть с `iss` в JWT (`KEYCLOAK_URL/realms/KEYCLOAK_REALM`) |
| `jwks_uri` | Откуда берутся **публичные ключи** для проверки подписи JWT |
| `authorization_endpoint` | Логин web/mobile |
| `token_endpoint` | Обмен `code` → tokens, refresh |
| `end_session_endpoint` | Logout (если нужен) |

Если JSON пришёл — для backend этого достаточно как smoke test.

## Как «получить key»

Ключи **не** у client `journal-backend` / `journal-web`. Они **общие для Realm**.

**1.** В discovery найдите `jwks_uri`, обычно:

```text
{KEYCLOAK_URL}/realms/{KEYCLOAK_REALM}/protocol/openid-connect/certs
```

**2.** Запросите JWKS:

```bash
curl -s "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/certs" | jq .
```

Увидите `keys[]` с `kid`, `kty` (RSA), `n`, `e` — это публичные ключи. **Приватных ключей вам не нужно** — только Keycloak подписывает токены.

**3.** Edge при старте делает то же самое через `go-oidc`:

```text
discovery → jwks_uri → кэш ключей → Verify(token)
```

В `.env` достаточно:

```env
KEYCLOAK_URL=...
KEYCLOAK_REALM=...
KEYCLOAK_CLIENT_ID=journal-backend
```

Отдельно `jwks_uri` или «key» в env **не** задаётся.

## Что делать дальше (по смыслу)

Discovery + JWKS — **инфраструктура**. Для работы API нужен **access token пользователя**, не ключи Realm.

```text
1. Логин через journal-web (браузер / PKCE)
2. Взять access_token (не id_token)
3. curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8080/api/v1/...
```

Если API отвечает **401** — смотреть payload токена (`aud`, `exp`, `typ`), а не JWKS.

Декодировать payload без проверки подписи:

```bash
# middle part JWT (base64url)
echo "$ACCESS_TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq .
# или jwt.io
```

Нужно: `"aud"` содержит `journal-backend`, `"typ": "Bearer"`, роли в `resource_access.journal-backend.roles`.

## Когда руками смотреть keys

- Edge не стартует / `OIDC provider unreachable` — проверить `jwks_uri` с той же машины, что Edge  
- Подозрение на ротацию ключей на SSO — сравнить `kid` в заголовке JWT с `keys[].kid` в JWKS  
- TLS-ошибки — тогда `KEYCLOAK_TLS_SKIP_VERIFY` только non-prod  

## Итого

| Вопрос | Ответ |
|--------|--------|
| Что делать с discovery? | Убедиться, что URL/Realm верные; в коде уже используется автоматически |
| Как получить key? | `curl` на `jwks_uri` из discovery (или `/protocol/openid-connect/certs`) |
| Нужно ли класть key в backend? | **Нет** |
| Следующий шаг | Логин web → `access_token` → запрос к API |

Если пришлёте фрагмент discovery (`issuer`, `jwks_uri` без секретов) или текст ошибки Edge/curl API — скажу, на каком шаге затык.

---

Важно разделить два «ключа»:

| Что | Нужно ли вам руками |
|-----|---------------------|
| **JWKS** (публичные ключи Realm) | **Нет** — Edge сам тянет с `jwks_uri` после discovery |
| **Access token** (JWT пользователя) | **Да** — его и кладут в `Authorization: Bearer …` |

Backend **не принимает** JWKS в заголовке. Проверка: есть ли у вас строка JWT после логина.

---

## Вариант 1 — без Keycloak (быстрее всего для backend)

В проекте уже есть обход для локалки:

`.env.local`:

```env
DEBUG_BYPASS_AUTH=true
APP_ENV=local
```

Перезапустить Edge, затем:

```bash
curl -s -H "X-Debug-Role: teacher" \
  http://localhost:8080/api/v1/academic-periods
```

Роли: `teacher`, `student`, `admin`, `dean`, `methodologist`. JWT не нужен.

---

## Вариант 2 — настоящий токен из Keycloak без фронта

### A) Password grant (проще всего)

Временно **только для теста** (отдельный client, не `journal-web` в prod):

1. Client `journal-dev-cli`: public, **Direct access grants ON**, Standard flow OFF.  
2. Подключить scope `journal-api` (audience `journal-backend`).  
3. Пользователю — client role на `journal-backend`.

```bash
export KC="https://sso.example.edu"
export REALM="Test"

curl -s -X POST "$KC/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=journal-dev-cli" \
  -d "username=ВАШ_ЛОГИН" \
  -d "password=ВАШ_ПАРОЛЬ" \
  -d "scope=openid profile email" | jq -r .access_token
```

Сохранить:

```bash
export ACCESS_TOKEN="eyJ..."
```

Проверка API:

```bash
curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/academic-periods | jq .
```

Если `401` — декодировать payload:

```bash
echo "$ACCESS_TOKEN" | awk -F. '{print $2}' | tr '_-' '/+' | base64 -d 2>/dev/null | jq .
```

Смотреть `aud` (должен быть `journal-backend`), `resource_access`, `exp`.

---

### B) Authorization Code + PKCE (как будет web, без SPA)

Если password grant нельзя — один раз через браузер + `curl`.

**1. PKCE:**

```bash
CODE_VERIFIER=$(openssl rand -base64 32 | tr -d '=+/' | cut -c1-43)
CODE_CHALLENGE=$(printf '%s' "$CODE_VERIFIER" | openssl dgst -sha256 -binary | openssl base64 | tr -d '=' | tr '/+' '_-')
echo "verifier=$CODE_VERIFIER"
echo "challenge=$CODE_CHALLENGE"
```

**2. Открыть в браузере** (подставить свои URL, client, redirect из KC):

```text
https://sso.example.edu/realms/Test/protocol/openid-connect/auth
  ?client_id=journal-web
  &redirect_uri=http://localhost:4173/callback
  &response_type=code
  &scope=openid profile email
  &code_challenge=CHALLENGE
  &code_challenge_method=S256
```

**3.** После логина браузер уйдёт на `http://localhost:4173/callback?code=...` — скопировать `code` (страница может не открыться — code в адресной строке всё равно есть).

**4. Обмен на token:**

```bash
curl -s -X POST "$KC/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "client_id=journal-web" \
  -d "code=СКОПИРОВАННЫЙ_CODE" \
  -d "redirect_uri=http://localhost:4173/callback" \
  -d "code_verifier=$CODE_VERIFIER" | jq .
```

Брать **`access_token`**, не `id_token`.

---

## Вариант 3 — «руками сходить за JWKS» (только для любопытства)

```bash
curl -s "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/certs" | jq .
```

Это **не** подставляется в `curl` к API. Edge делает то же при старте.

---

## Env Edge для реального JWT

```env
DEBUG_BYPASS_AUTH=false
KEYCLOAK_URL=...
KEYCLOAK_REALM=...
KEYCLOAK_CLIENT_ID=journal-backend
KEYCLOAK_ROLES_CLIENT_ID=journal-backend
```

Без audience mapper в KC токен будет без `aud: journal-backend` → `401`.

---

## Что выбрать

| Цель | Способ |
|------|--------|
| Проверить handlers, роли, БД | `DEBUG_BYPASS_AUTH` + `X-Debug-Role` |
| Проверить именно JWT/Keycloak | password grant на `journal-dev-cli` или PKCE + браузер |
| «Скачать ключи Realm» | `/certs` — только диагностика SSO |

Для backend «прокинуть» = **`export ACCESS_TOKEN=...`** и **`Authorization: Bearer`**. JWKS вручную не прокидывают.

Если напишете, включён ли у вас Direct Access на каком-то client и есть ли `journal-api` scope — подскажу один конкретный вариант под ваш Realm.


---

Да, путь легче есть. Но **один client id именно `backend` для всего — технически можно, но архитектурно неправильно**, если у тебя Web и Mobile сами получают токены и ходят в API.

Причина простая: **web/mobile — public clients**, они не могут безопасно хранить client secret. Backend же обычно либо вообще не логинится, либо является confidential/resource server. Keycloak JS adapter по умолчанию работает через Authorization Code Flow, где браузерное приложение получает code и меняет его на токены; мобильные приложения по RFC 8252 должны логиниться через внешний браузер/системный user-agent, а не через backend или встроенную форму.

## Самый лёгкий нормальный вариант для MVP

Сделай **2 клиента**, а не 3:

```text
journal-app       — public client для web + mobile
journal-backend   — resource/API client для aud + roles
```

То есть вместо отдельных:

```text
journal-web
journal-mobile
```

на этапе ВКР/теста можно сделать один общий:

```text
journal-app
```

Тогда схема будет такая:

```text
Web/Mobile → login через journal-app → access token → Backend проверяет aud=journal-backend
```

Это проще, чем три клиента, но всё ещё правильно.

---

## Почему не один `journal-backend` на всё

### Вариант 1: сделать `journal-backend` confidential

Тогда backend имеет secret. Но Web/Mobile не должны его знать. Значит, frontend/mobile не смогут безопасно логиниться через этот client.

Плохо:

```text
journal-backend = confidential + secret
web/mobile используют этот secret
```

Это нельзя делать, потому что secret утечёт из frontend/mobile.

---

### Вариант 2: сделать `journal-backend` public

Тогда Web/Mobile смогут логиниться, но `journal-backend` уже фактически становится **frontend-client**, а не backend-client.

То есть получится путаница:

```text
journal-backend
  одновременно:
  - login client
  - API audience
  - роли
  - redirect URI web
  - redirect URI mobile
```

Для локального теста это может работать, но в проектировании и ВКР это выглядит слабо: один client смешивает разные ответственности.

---

## Что я бы сделал на твоём месте сейчас

### Минимально и правильно

Оставь:

```text
journal-backend
```

Создай только один дополнительный client:

```text
journal-app
```

Пока не дели на `journal-web` и `journal-mobile`.

---

## Настройка `journal-app`

```text
Client ID: journal-app
Client authentication: OFF
Standard flow: ON
Direct access grants: OFF в prod / ON временно для теста curl
Implicit flow: OFF
PKCE: S256
```

Redirect URIs:

```text
http://localhost:5173/*
http://localhost:4173/*
com.university.journal:/oauth2redirect
```

Web origins:

```text
http://localhost:5173
http://localhost:4173
```

---

## Настройка `journal-backend`

```text
Client ID: journal-backend
Standard flow: OFF
Direct access grants: OFF
Implicit flow: OFF
Client authentication: можно ON, но не обязательно для JWT-проверки
```

В нём создай роли:

```text
student
teacher
curator
dean
admin
```

---

## Всё равно нужен `journal-api` scope?

Да, если backend проверяет `aud=journal-backend`.

Но можно упростить так:

```text
Client scopes → journal-api → Mappers → Configure new mapper → Audience
Included Client Audience: journal-backend
Add to access token: ON
```

И подключить `journal-api` к `journal-app` как **Default**.

Это один раз настраивается и дальше не мешает.

---

## Итоговый минимальный контур

```text
Realm: Test

Clients:
1. journal-app
   - public
   - логин web/mobile
   - PKCE
   - redirect URI для web и mobile

2. journal-backend
   - API/resource
   - роли
   - audience для backend

Client scope:
journal-api
   - добавляет aud=journal-backend
```

---

## Можно ли временно вообще без `journal-backend`?

Можно, для самого быстрого теста:

```text
один client: journal-app
```

Тогда backend проверяет:

```text
iss = realm
azp = journal-app
roles = realm roles или client roles journal-app
```

Но тогда у тебя не будет нормального разделения:

```text
кто получил токен ≠ для какого API токен предназначен
```

Для MVP на один вечер — допустимо.
Для ВКР и нормальной архитектуры — лучше минимум **`journal-app` + `journal-backend`**.

---

Мой совет: **не делай 3 клиента сейчас**, сделай 2. Это самый лёгкий путь без архитектурной ошибки.

---

Да. Без frontend проще всего сделать так: **получить access token руками через curl** и передать его в backend в заголовке `Authorization: Bearer`.

Ниже — рабочий путь для macOS.

---

## Вариант 1 — самый простой для проверки: password grant через curl

Это только для dev. На `journal-app` или `journal-web` временно включи:

```text
Direct access grants: ON
```

Потом на macOS:

```bash
brew install jq
```

Задай переменные:

```bash
export KC="https://sso.example.local"
export REALM="Test"
export CLIENT_ID="journal-app"       # или journal-web, если ты его используешь
export USERNAME="test.teacher"
export PASSWORD="твой_пароль"
```

Получи токен:

```bash
TOKEN_JSON=$(curl -sk -X POST "$KC/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=$CLIENT_ID" \
  --data-urlencode "username=$USERNAME" \
  --data-urlencode "password=$PASSWORD" \
  --data-urlencode "scope=openid profile email")

echo "$TOKEN_JSON" | jq .
```

Достань access token:

```bash
export ACCESS_TOKEN=$(echo "$TOKEN_JSON" | jq -r '.access_token')
```

Проверь, что токен реально есть:

```bash
echo "$ACCESS_TOKEN" | cut -c1-40
```

---

## Декодировать токен на macOS

На macOS удобнее через Python, чтобы не мучиться с `base64 -D` и padding:

```bash
python3 - <<'PY'
import os, json, base64
token = os.environ["ACCESS_TOKEN"]
payload = token.split(".")[1]
payload += "=" * (-len(payload) % 4)
print(json.dumps(json.loads(base64.urlsafe_b64decode(payload)), indent=2, ensure_ascii=False))
PY
```

Внутри должно быть примерно:

```json
{
  "azp": "journal-app",
  "aud": ["journal-backend"],
  "preferred_username": "test.teacher",
  "resource_access": {
    "journal-backend": {
      "roles": ["teacher"]
    }
  }
}
```

Ключевые вещи:

```text
aud должен содержать journal-backend
resource_access.journal-backend.roles должен содержать teacher/student/admin
```

---

## Прокинуть токен в backend

Если backend локально на macOS:

```bash
curl -v http://localhost:8080/api/v1/academic-periods \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Или другой твой endpoint:

```bash
curl -v http://localhost:8080/api/v1/journal \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Если нужен JSON body:

```bash
curl -v -X POST http://localhost:8080/api/v1/some-endpoint \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"test": true}'
```

---

## Если backend на сервере, а ты с macOS

Если backend слушает на сервере `localhost:8080`, прокинь порт через SSH:

```bash
ssh -L 8080:127.0.0.1:8080 journal-iis
```

После этого с macOS вызывай:

```bash
curl -v http://localhost:8080/api/v1/academic-periods \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

То есть запрос с Mac попадёт на backend сервера.

---

## Что должно быть в env backend

На backend должно быть примерно так:

```env
DEBUG_BYPASS_AUTH=false

KEYCLOAK_URL=https://sso.example.local
KEYCLOAK_REALM=Test

KEYCLOAK_CLIENT_ID=journal-backend
KEYCLOAK_ROLES_CLIENT_ID=journal-backend
```

Смысл:

```text
journal-app / journal-web — получает токен
journal-backend — backend проверяет, что токен предназначен для него
```

---

## Если получил ошибку

### `401 Unauthorized`

Обычно значит:

```text
токен не передан
токен просрочен
backend не может проверить подпись
issuer не совпадает
нет aud=journal-backend
```

Проверяй payload токена: есть ли `aud`.

---

### `403 Forbidden`

Обычно значит:

```text
токен валидный, но нет нужной роли
```

Проверь в payload:

```json
"resource_access": {
  "journal-backend": {
    "roles": ["teacher"]
  }
}
```

Если ролей нет — зайди:

```text
Users → test.teacher → Role mapping → Assign role → Filter by clients → journal-backend → teacher
```

---

## После проверки

Когда убедишься, что backend принимает токен, **выключи**:

```text
Direct access grants: OFF
```

Дальше frontend/mobile будут получать токен нормальным способом через **Authorization Code + PKCE**, но backend от этого не изменится: он всё равно будет ждать обычный Bearer token.

---

Это означает: **Postman/браузер отправил `redirect_uri`, которого нет в Valid Redirect URIs у client’а**.

Чини это не в `journal-backend`, а в том клиенте, через который логинишься: скорее всего **`journal-app`** или **`journal-web`**.

## 1. Для Postman добавь callback URI

В Keycloak:

```text
Clients → journal-app → Settings
```

В поле **Valid redirect URIs** добавь ровно:

```text
https://oauth.pstmn.io/v1/callback
```

И сохрани.

В Postman в OAuth 2.0 настройках должно быть то же самое:

```text
Callback URL:
https://oauth.pstmn.io/v1/callback
```

Важно: `redirect_uri` в запросе должен совпадать с зарегистрированным URI у client’а; в новых версиях Keycloak сравнение стало более строгим и чувствительным к точному виду URI.

---

## 2. Проверь, что используешь правильный client

В Postman должно быть:

```text
Client ID: journal-app
```

Не:

```text
journal-backend
```

`journal-backend` — это API/resource client. Через него не надо логиниться.

---

## 3. Что должно быть в `journal-app`

```text
Client authentication: OFF
Standard flow: ON
Direct access grants: не важно для Postman PKCE
Valid redirect URIs:
  https://oauth.pstmn.io/v1/callback
```

Можно временно для теста добавить шире:

```text
https://oauth.pstmn.io/*
http://localhost:4173/*
http://localhost:5173/*
```

Но лучше начать с точного:

```text
https://oauth.pstmn.io/v1/callback
```

---

## 4. В Postman настройки должны быть такие

```text
Grant Type: Authorization Code (With PKCE)

Auth URL:
https://ТВОЙ_KEYCLOAK/realms/Test/protocol/openid-connect/auth

Access Token URL:
https://ТВОЙ_KEYCLOAK/realms/Test/protocol/openid-connect/token

Client ID:
journal-app

Client Secret:
пусто

Callback URL:
https://oauth.pstmn.io/v1/callback

Code Challenge Method:
SHA-256

Scope:
openid profile email
```

---

## 5. Как быстро понять, какой URI реально не нравится

На странице ошибки Keycloak в адресной строке будет URL с параметром:

```text
redirect_uri=...
```

Скопируй его. Например, может быть:

```text
redirect_uri=https%3A%2F%2Foauth.pstmn.io%2Fv1%2Fcallback
```

Декодированный вариант:

```text
https://oauth.pstmn.io/v1/callback
```

Именно этот адрес нужно добавить в:

```text
Clients → journal-app → Valid redirect URIs
```

---

## 6. Частая ошибка

Ты мог добавить redirect URI в `journal-backend`, а логинишься через `journal-app`.

Должно быть так:

```text
journal-app:
  Valid redirect URIs = https://oauth.pstmn.io/v1/callback

journal-backend:
  redirect URIs пусто
```

После сохранения заново нажми в Postman **Get New Access Token**.

---

На скриншоте видна причина: mapper есть, но **аудитория не задана**.

## Что не так

| Поле | Сейчас | Нужно |
|------|--------|--------|
| **Included Client Audience** | пусто | выбрать **`journal-backend`** |
| **Included Custom Audience** | пусто | оставить пустым (если выбрали client audience) |
| Add to access token | ON | OK |
| Add to ID token | OFF | OK |

Пока **Included Client Audience** пустой, Keycloak **не добавляет** `journal-backend` в `aud`. У вас в токене остаётся `aud: realm-management` — это роли админки realm, не audience API.

Имя mapper (`journal-backend-audence`) на работу не влияет, опечатка только косметическая.

---

## Что сделать в этом окне

1. **Included Client Audience** → в выпадающем списке выберите **`journal-backend`**  
   (client должен существовать в Realm `Test`).

2. **Save**.

3. Убедитесь, что scope с этим mapper’ом подключён к **`journal-web`**:
   - **Clients** → `journal-web` → **Client scopes** → в **Default** (или Assigned default) должен быть scope, где лежит этот mapper (например `journal-api`).

4. **Новый логин** — старый токен не обновится:
   - Postman: **Get New Access Token**
   - или снова OAuth в браузере

5. Лучше тестировать не **test-admin** (realm admin), а пользователя с ролью на **`journal-backend`** (`teacher` / `student`).

---

## Как проверить, что сработало

В новом `access_token` в payload:

```json
"aud": ["journal-backend"]
```

или `"aud": "journal-backend"`

и желательно:

```json
"resource_access": {
  "journal-backend": { "roles": ["teacher"] }
}
```

Если `aud` всё ещё `realm-management` — scope не привязан к `journal-web` или токен старый.

---

## Порядок в Keycloak (кратко)

```text
1. Clients → journal-backend (существует, роли student/teacher/…)
2. Client scopes → journal-api → Mappers → Audience → Included Client Audience = journal-backend
3. Clients → journal-web → Client scopes → journal-api в Default
4. Users → test.teacher → Role mapping → journal-backend → teacher
5. Новый access_token → curl API
```

Главное на вашем скрине — **заполнить Included Client Audience = `journal-backend`** и сохранить. Без этого пункт А формально есть, но mapper ничего не делает.

---

Кратко: **refresh — зона ответственности web/mobile (и Keycloak), не Edge.** Ваш backend сейчас только **принимает и проверяет `access_token`** на каждом запросе.

## Как устроено в этом проекте

```text
┌─────────────┐     login (code+PKCE)      ┌───────────┐
│ Web/Mobile  │ ─────────────────────────► │ Keycloak  │
│             │ ◄─ access + refresh token │           │
└──────┬──────┘                            └─────▲─────┘
       │                                         │
       │  API: Authorization: Bearer <access>    │ refresh (grant_type=refresh_token)
       ▼                                         │
┌─────────────┐                                  │
│ Edge :8080  │  только Verify(access_token)    │
│             │  JWKS + aud + roles              │
└─────────────┘                                  │
       │                                         │
       ▼                                         │
   Domain API                                    │
```

Edge **не** выдаёт токены, **не** ходит в `token_endpoint` за refresh и **нет** эндпоинта вроде `/api/v1/auth/refresh`. Middleware читает Bearer и валидирует JWT:

```45:62:internal/edge/middleware/auth.go
		token, ok := extractBearerToken(c.Request)
		// ...
		idToken, err := verifier.Verify(context.Background(), token)
```

После логина Domain получает только синхронизацию профиля (`register-login`) — это не refresh, а «пользователь зашёл».

---

## Кто что делает

| Участник | Роль |
|----------|------|
| **Keycloak** | Выдаёт `access_token` + `refresh_token`, обновляет пару по refresh |
| **Web / Mobile** | Хранит refresh, следит за `exp` access, обновляет токен, в API шлёт только свежий **access** |
| **Edge (backend)** | Stateless: валидирует текущий access; refresh не знает |

`journal-backend` в env Edge — только **audience** при проверке, не client для refresh.

---

## Refresh на клиенте (стандарт OIDC)

Когда `access_token` скоро истечёт или API вернул **401**:

```http
POST https://auth.iis.miigaik.ru/realms/Test/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token
&client_id=journal-web          ← тот же, через кого логинились
&refresh_token=<refresh_token>
```

Для public client (без secret) — без `client_secret`.  
В ответе снова `access_token` (+ часто новый `refresh_token`). Дальше:

```http
GET /api/v1/...
Authorization: Bearer <новый access_token>
```

**В API по-прежнему только access**, не refresh и не id_token.

---

## Где хранить refresh

| Платформа | Обычная практика |
|-----------|------------------|
| **Web (SPA)** | Память для access; refresh — httpOnly cookie (если BFF) или secure storage; чистый SPA часто держит оба в памяти/sessionStorage (проще, но слабее при XSS) |
| **Mobile** | Keychain / Keystore (не plain SharedPreferences) |

Refresh **не** отправлять на Edge — только на Keycloak.

---

## Нужен ли backend для refresh?

**В вашей архитектуре — нет.** Это нормальная схема: Resource Server (Edge) + OIDC у клиента.

Backend для refresh добавляют, если нужен **BFF** (cookie-сессия, refresh только на сервере) или **централизованный logout/revocation**. В коде Redis упомянут для «refresh blacklist», но **отдельного refresh-flow в Edge сейчас нет** — это задел в README, не реализованный API.

---

## Практика на клиенте

1. После логина сохранить `access_token`, `refresh_token`, `expires_in` (или `exp` из JWT).
2. Перед запросом: если access истёк — refresh к Keycloak, потом запрос.
3. Или interceptor: 401 → refresh → повтор запроса (один раз).
4. Refresh с **`client_id=journal-web`** (или `journal-mobile`), не `journal-backend`.
5. При logout — `end_session_endpoint` + удалить токены локально; опционально revoke refresh в KC.

---

## Итог

- **Refresh = frontend/mobile ↔ Keycloak.**
- **Backend = только Bearer access на `/api/v1/*`.**
- Новый access после refresh снова должен иметь `aud: journal-backend` (scope `journal-api` на login-client).

Если понадобится BFF или logout с blacklist через Redis — это отдельное решение, сейчас в проекте его нет.