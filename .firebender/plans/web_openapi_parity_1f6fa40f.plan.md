<!--firebender-plan
name: Web OpenAPI Parity
overview: Привести Android-клиент к актуальному `journal.yaml` и поведению web-клиента для уже начатых ролей teacher и methodologist, без перехода к student/admin на этом этапе.
todos:
  - id: sync-openapi-android
    content: "Обновить Android `JournalApi` и DTO под актуальный OpenAPI для teacher/methodologist."
  - id: teacher-dashboard-web-parity
    content: "Перевести личный кабинет преподавателя на реальные dashboard/grants endpoints и UI flow из web."
  - id: methodologist-journals-parity
    content: "Довести список журналов методиста и фильтры до поведения `MethodologistJournalsView.vue`."
  - id: methodologist-create-parity
    content: "Довести создание журнала методиста до web flow без импорта состава группы."
  - id: ui-consistency-pass
    content: "Провести UI-unification модалок, навигации, login и меню по web-референсу."
  - id: build-and-smoke-test
    content: "Собрать APK, установить на телефон и пройти smoke-тест teacher/methodologist."
-->

# План синхронизации Android с OpenAPI и web-клиентом

## Источники истины
- OpenAPI: [C:/Users/fmkro/iis.jrnl/androind.client/journal.yaml](C:/Users/fmkro/iis.jrnl/androind.client/journal.yaml)
- Web teacher dashboard: [C:/Users/fmkro/iis.jrnl/web.client/src/views/TeacherLK.vue](C:/Users/fmkro/iis.jrnl/web.client/src/views/TeacherLK.vue)
- Web dashboard API: [C:/Users/fmkro/iis.jrnl/web.client/src/api/dashboard.ts](C:/Users/fmkro/iis.jrnl/web.client/src/api/dashboard.ts)
- Web grants API: [C:/Users/fmkro/iis.jrnl/web.client/src/api/grants.ts](C:/Users/fmkro/iis.jrnl/web.client/src/api/grants.ts)
- Web methodologist journals: [C:/Users/fmkro/iis.jrnl/web.client/src/views/MethodologistJournalsView.vue](C:/Users/fmkro/iis.jrnl/web.client/src/views/MethodologistJournalsView.vue)
- Web methodologist create journal: [C:/Users/fmkro/iis.jrnl/web.client/src/views/MethodologistJournalCreateView.vue](C:/Users/fmkro/iis.jrnl/web.client/src/views/MethodologistJournalCreateView.vue)

## Что меняем

### 1. Network/DTO parity с актуальным OpenAPI
- Обновить [JournalApi.kt](C:/Users/fmkro/iis.jrnl/androind.client/core/network/src/main/java/com/journal/core/network/api/JournalApi.kt):
  - `GET /teacher/stats`
  - `GET /teacher/attendance-summary`
  - `GET /teacher/groups-performance`
  - `GET/POST/DELETE /journal-access-grants`
  - `GET /journals` для методиста
  - расширить параметры `getGroupJournalGrid`: `teacher_id`, `lesson_type`
  - расширить catalog endpoints параметрами из OpenAPI (`period_id`, `teacher_id`, `discipline_id`, `group_id`)
- Добавить/дополнить модели в `core/model/.../teacher` для dashboard, grants, journals list.

### 2. Личный кабинет преподавателя как в web
- Перевести [TeacherDashboardRoute.kt](C:/Users/fmkro/iis.jrnl/androind.client/features/teacher/dashboard/src/main/java/com/journal/features/teacher/dashboard/TeacherDashboardRoute.kt) с локальных расчётов на реальные endpoints:
  - статистика из `/teacher/stats`
  - успеваемость групп из `/teacher/groups-performance`
  - посещаемость из `/teacher/attendance-summary`
- Добавить фильтры как в `TeacherLK.vue`: тип занятия, предмет, группа, кнопка `Применить`.
- Добавить блок `Открыть доступ`:
  - список преподавателей из `/teachers`
  - уровень доступа `read/write`
  - создание гранта через `/journal-access-grants`

### 3. Методист: журналы и фильтры как в web
- Обновить [MethodistRoutes.kt](C:/Users/fmkro/iis.jrnl/androind.client/features/methodist/templates/src/main/java/com/journal/features/methodist/templates/MethodistRoutes.kt):
  - список журналов строить по актуальному `/journals` или строго повторить web aggregation только если backend endpoint не вернёт нужные поля
  - фильтры: поиск, период, дисциплина, группа, тип занятия
  - карточки журналов: период, дисциплина, группа, преподаватель, количество занятий, проведено, диапазон дат
- При открытии журнала методиста обязательно передавать `teacher_id` и `lesson_type` в `getGroupJournalGrid`.

### 4. Методист: создание журнала
- Довести экран создания журнала по `MethodologistJournalCreateView.vue`:
  - контекст журнала: период, дисциплина, тип, преподаватель, группа
  - выбор КТП
  - создание `POST /journals`
  - назначение КТП через `/lesson-template-assignments`
- Импорт состава группы пока планировать отдельной итерацией, так как это большой блок с XLSX/CSV parsing и admin-import endpoints.

### 5. UI-unification по web
- Привести login к `LoginView.vue` после API parity, если не будет конфликтов по ролям debug-auth.
- Выравнять модалки контроля и КТП по единому web-like стилю, не ломая уже согласованный внешний вид КТП.
- Проверить единые правила:
  - `← Назад` слева сверху на вложенных экранах
  - `☰` меню справа сверху
  - logout внизу меню
  - без логотипов в рабочих шапках

## Проверка
- `:app:assembleDebug`
- Установка APK на телефон через `adb install -r`
- Smoke-тесты:
  - teacher: login → расписание → журнал → личный кабинет → открыть доступ
  - methodologist: login → журналы → фильтры → открыть журнал → КТП → создание журнала

## Не входит в эту итерацию
- Student/admin роли.
- Полный импорт состава группы из Excel/CSV на Android.
- Реальная Keycloak/OIDC авторизация вместо текущего debug-role режима.
