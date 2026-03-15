# AI Physical — Полная документация реализации

> **Дата:** 15 марта 2026  
> **Платформы:** Android + iOS (Kotlin Multiplatform + Compose Multiplatform 1.10.0)  
> **Статус сборки:** ✅ BUILD SUCCESSFUL (Android Debug APK + iOS Simulator)

---

## 📦 Стек технологий

| Технология | Версия | Назначение |
|------------|--------|------------|
| Kotlin Multiplatform | 2.3.0 | Общая бизнес-логика |
| Compose Multiplatform | 1.10.0 | UI для Android и iOS |
| Material3 | 1.10.0-alpha05 | Дизайн-система |
| Firebase Auth (Android) | BOM 33.7.0 | Аутентификация |
| Firebase Firestore (Android) | BOM 33.7.0 | База данных |
| kotlinx-coroutines | 1.9.0 | Асинхронность |
| AndroidX Lifecycle ViewModel | 2.9.6 | MVVM / MVI |
| Google Services Plugin | 4.4.2 | Firebase интеграция |

---

## 🗂️ Полная структура проекта

```
composeApp/src/
├── commonMain/kotlin/com/example/aiphysical/
│   ├── App.kt
│   │
│   ├── data/
│   │   ├── model/
│   │   │   ├── Organization.kt
│   │   │   ├── UserProfile.kt              ← +burnoutScore, emotionScore, motivationScore, anxietyScore, isBlocked
│   │   │   ├── TestResult.kt
│   │   │   ├── CourseProgress.kt
│   │   │   └── KpiData.kt                  ← +avgBurnout, avgEmotion, avgMotivation, avgAnxiety
│   │   └── service/
│   │       ├── FirebaseAuthService.kt
│   │       └── FirestoreService.kt         ← +updateUserRole, +updateUserBlockStatus
│   │
│   ├── presentation/
│   │   ├── auth/
│   │   │   ├── AuthState.kt
│   │   │   ├── AuthEvent.kt
│   │   │   └── AuthViewModel.kt
│   │   └── director/
│   │       ├── DirectorDashboardState.kt   ← +DirectorTab, +showInviteSheet, +aiInsightText, +analyticsFilter
│   │       ├── DirectorDashboardEvent.kt   ← +NavigateToTab, +LoadAiInsight, +OpenInviteSheet, +ChangeUserRole...
│   │       ├── DirectorEffect.kt           ← NEW: MVI Side-Effects (SharedFlow)
│   │       └── DirectorDashboardViewModel.kt ← +SharedFlow<DirectorEffect>, +generateAiInsight()
│   │
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── AppColors.kt                ← +NeonBackground(#0B0B1E), +NeonViolet(#8A2BE2), +CyanAccent(#00CED1), +AlertOrange(#FF8C00)
│   │   │   ├── AppTheme.kt
│   │   │   └── AppStrings.kt               ← +30 новых строк (табы, AI, метрики, управление)
│   │   ├── components/
│   │   │   ├── GlassComponents.kt          ← +shimmerEffect(), +DirectorBackground()
│   │   │   └── DashboardComponents.kt      ← +SectionHeader, +EmptyState, +GlassSearchBar, +MemberListCard, +CriticalAlertsPanel
│   │   └── screens/
│   │       ├── LoginScreen.kt
│   │       ├── RoleSelectionScreen.kt
│   │       ├── RegistrationScreen.kt
│   │       └── director/
│   │           ├── DirectorDashboardScreen.kt  ← ПОЛНЫЙ РЕДИЗАЙН: 4-Tab Host + BottomNav + Effects
│   │           ├── DashboardTab.kt             ← NEW: AI Card + Circular KPI + Trend Chart
│   │           ├── AnalyticsTab.kt             ← NEW: Filter Chips + Expandable Member Cards (5 метрик)
│   │           ├── ManagementTab.kt            ← NEW: Invite BottomSheet + Role Change + Block/Unblock
│   │           ├── ContentTab.kt               ← NEW: 5 Mandatory Tests + Course Library
│   │           └── MemberDetailScreen.kt
│   │
│   └── util/
│       ├── ServiceFactory.kt
│       └── BackPressHandler.kt
│
├── androidMain/
│   ├── data/service/
│   │   ├── FirebaseAuthServiceImpl.kt
│   │   └── FirestoreServiceImpl.kt         ← +updateUserRole(), +updateUserBlockStatus(), +новые поля в toUserProfile()
│   └── util/...
│
└── iosMain/
    ├── data/service/
    │   ├── FirebaseAuthServiceStubImpl.kt
    │   └── FirestoreServiceStubImpl.kt     ← +stub updateUserRole(), +stub updateUserBlockStatus()
    └── util/...
```

---

## 🎨 KASU Director Panel — Новый Design System

### Цветовая палитра (Future-Style Glassmorphism)

| Токен | Hex | Назначение |
|-------|-----|------------|
| `NeonBackground` | `#0B0B1E` | Основной фон панели директора |
| `NeonViolet` | `#8A2BE2` | Основной неон-акцент |
| `CyanAccent` | `#00CED1` | Вторичный акцент |
| `AlertOrange` | `#FF8C00` | Предупреждения, уровень стресса |
| `CardSurface` | `#12122A` | Поверхность карточек |
| `CardSurfaceLight` | `#1A1A35` | Светлая поверхность (snackbar, sheet) |

### Glassmorphism-карточки
- `cornerRadius`: 24dp (стандарт), 20dp (компактный), 18dp (список)
- `background`: `Color.White.copy(alpha = 0.05f)`
- `border`: `1.dp` градиентная (`NeonViolet → CyanAccent` или `GlassBorder`)
- `shimmerEffect()`: анимированный LinearGradient с `Offset(-600 → 1800)` за 1.6 сек

---

## 🏗️ Архитектура MVI + SharedFlow Effects

```
UI Event
   ↓
DirectorDashboardViewModel.onEvent()
   ↓
_state: MutableStateFlow<DirectorDashboardState>   → UI перерисовка
_effects: MutableSharedFlow<DirectorEffect>         → Одноразовые действия
   ↓
LaunchedEffect(vm.effects.collectLatest) in UI:
  CopyToClipboard → ClipboardManager + Snackbar
  OpenUrl         → UriHandler
  ShowSnackbar    → SnackbarHostState
  TriggerHaptic   → LocalHapticFeedback.performHapticFeedback(LongPress)
```

---

## 📱 Экраны панели директора (4 вкладки)

### Bottom Navigation Bar
```
[🏠 Главная] [📊 Аналитика] [👥 Управление] [📚 Контент]
```
- Активная вкладка: `NeonViolet` текст + точка-индикатор сверху
- Фон: вертикальный градиент `NeonBackground(0f → 0.97f)`
- Разделитель: `1dp NeonViolet.copy(0.35f)` линия наверху

---

### Вкладка 1: 🏠 Dashboard (DashboardTab)

**Заголовок**
- `orgHealthTitle`: "Ментальное здоровье KASU" (CyanAccent, letterspacing 2sp)
- Название организации: `Brush.horizontalGradient(NeonViolet → CyanAccent)`, 28sp ExtraBold
- Language Switcher + кнопки Обновить / Выйти

**AI-Ассистент Card** (`AiInsightCard`)
- Фон: `Brush.linearGradient(NeonViolet.copy(0.28f) → CyanAccent.copy(0.18f))`
- Border: `1dp Brush.linearGradient(NeonViolet.copy(0.7f) → CyanAccent.copy(0.5f))`
- Shimmer overlay при `isAiLoading = true`
- Содержимое: 🤖 иконка + "AI-Ассистент" + сгенерированный текст инсайта
- **Анализ** генерируется из данных участников (criticalCount, avgBurnout, avgMotivation)
- Задержка 1.8 сек при загрузке — симуляция AI

**KPI-виджеты** (`CircularKpiWidget`) — 3 штуки в Row
- Canvas circular progress (арка `–90° → sweepAngle = value * 3.6f`)
- `animateFloatAsState(tween(1400))` — анимация заполнения
- Цвета: 🔥 Burnout=ErrorColor | ⚡ Stress=AlertOrange | 📚 Engagement=SuccessColor

**Критические оповещения** (`CriticalAlertsPanel`)
- Красная неоновая рамка, показывается только при наличии critical-участников

**Тренд-чарт** (`EmotionalTrendChart`) — 30 дней, Canvas

**Быстрый список** — первые 3 участника, кнопка "ещё →" → переход в Analytics

---

### Вкладка 2: 📊 Analytics (AnalyticsTab)

**Filter Chips** (горизонтальный LazyRow):
| Ключ | Фильтр |
|------|--------|
| `ALL` | Все участники |
| `JUNIOR` | ageGroup = JUNIOR |
| `MIDDLE` | ageGroup = MIDDLE |
| `SENIOR` | ageGroup = SENIOR |
| `STAFF` | role = psychologist/director |

- Активный чип: `Brush.horizontalGradient(NeonViolet → CyanAccent)`, белый текст
- Неактивный: `Color.White.copy(0.06f)`, TextSecondary

**Expandable Member Cards** (`ExpandableMemberCard`)
- Клик → `animateContentSize() + AnimatedVisibility`
- **Свёрнут**: аватар, имя, email, StatusBadge
- **Развёрнут**: 5 progress bars + кнопка "Подробнее"

**5 MetricProgressBar**:
| Метрика | Цвет логики |
|---------|-------------|
| Выгорание | isHighBad=true: Red >70%, Yellow >40%, Green иначе |
| Стресс | isHighBad=true |
| Эмоции | isHighBad=false: Red <30%, Yellow <60%, Green иначе |
| Мотивация | isHighBad=false |
| Тревога | isHighBad=true |

---

### Вкладка 3: 👥 Management (ManagementTab)

**Invite User Button**
- Полная ширина, `2dp Brush.horizontalGradient(NeonViolet.copy(0.9f) → CyanAccent.copy(0.8f))` рамка
- Клик → `OpenInviteSheet` event + HapticFeedback

**InviteCodeBottomSheet** (`ModalBottomSheet`)
- `containerColor = NeonBackground`
- Drag-handle (48×4dp Box)
- **Card A — Психолог** (NeonViolet): заголовок, описание, код (monospace 22sp), [Копировать] [Поделиться]
- **Card B — Студент** (CyanAccent): аналогично
- Copy → `CopyToClipboard` effect + HapticFeedback
- Share → `OpenUrl("https://aiphysical.app/join?role=...&code=...")` effect

**Searchable Member List** — `GlassSearchBar` + список карточек
- Каждая карточка: аватар, имя, роль (цветной), StatusBadge, кнопка `⋮`
- Menu по `⋮`: 👁 Подробнее / 🎭 Изменить роль / 🔒 Заблокировать

**RoleChangeBottomSheet** — список ролей с чекмаркой на текущей

---

### Вкладка 4: 📚 Content (ContentTab)

**5 обязательных тестов**:
| # | Тест | Цвет |
|---|------|------|
| 1 | Burnout Inventory (MBI) | ErrorColor |
| 2 | PSS-10 Stress Scale | AlertOrange |
| 3 | PHQ-9 Depression | #E040FB |
| 4 | GAD-7 Anxiety | #FF8A65 |
| 5 | WLEIS Motivation | SuccessColor |

- Каждая карточка: emoji + название + описание + кнопка [📊 Статистика]

**Библиотека курсов** (5 курсов):
- Карточка: emoji-иконка + название + описание + ⏱ длительность
- Значок **"🤖 AI Рекомендует"**: `Brush.horizontalGradient(NeonViolet → CyanAccent)` badge

---

## 🔥 Модель данных (обновлённая)

### UserProfile
```kotlin
data class UserProfile(
    // ...существующие поля...
    val burnoutScore: Float = 0f,        // 0–100
    val emotionScore: Float = 50f,       // 0–100 (выше = лучше)
    val motivationScore: Float = 50f,    // 0–100 (выше = лучше)
    val anxietyScore: Float = 0f,        // 0–100
    val isBlocked: Boolean = false
)
```

### KpiData
```kotlin
data class KpiData(
    val burnoutIndex: Float,
    val avgStressLevel: Float,
    val courseEngagement: Float,
    val avgBurnout: Float,
    val avgEmotion: Float,
    val avgMotivation: Float,
    val avgAnxiety: Float
)
```

### Firestore (новые поля)
```
users/{uid}:
  + burnoutScore: Float
  + emotionScore: Float
  + motivationScore: Float
  + anxietyScore: Float
  + isBlocked: Boolean
```

---

## ⚙️ Gradle конфигурация (без изменений)

```kotlin
// commonMain
implementation(libs.kotlinx.coroutines.core)
implementation(libs.androidx.lifecycle.viewmodelCompose)
implementation(libs.androidx.lifecycle.runtimeCompose)

// androidMain
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.auth)
implementation(libs.firebase.firestore)
implementation(libs.kotlinx.coroutines.android)
implementation(libs.kotlinx.coroutines.play.services)
```

---

## 📱 Запуск

```bash
# Android Debug APK
./gradlew :composeApp:assembleDebug
# APK: composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

---

## 🚀 Дорожная карта

| Функция | Приоритет | Описание |
|---------|-----------|----------|
| Psychologist Dashboard | 🔴 Высокий | Просмотр назначенных студентов |
| Student Dashboard | 🔴 Высокий | Тесты, курсы, AI-анализ |
| Real-time Firestore listeners | 🟡 Средний | `snapshotFlow` вместо one-time fetch |
| Push Notifications (FCM) | 🟡 Средний | Критические оповещения |
| iOS Firebase impl | 🔴 Высокий | Заменить stubs на реальный код |
| Real AI Backend | 🟡 Средний | Подключить GPT/Gemini API для инсайтов |
| Unit Tests | 🟡 Средний | ViewModel + Repository тесты |
