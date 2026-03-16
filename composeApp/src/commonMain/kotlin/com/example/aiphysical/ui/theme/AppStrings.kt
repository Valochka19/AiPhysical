package com.example.aiphysical.ui.theme

import com.example.aiphysical.presentation.auth.AppLanguage

data class Strings(
    // ── Auth ──────────────────────────────────────────────────────────────────
    val login: String, val register: String, val email: String, val password: String,
    val noAccount: String, val haveAccount: String, val loading: String, val back: String,
    val appTitle: String, val appSubtitle: String,
    val chooseRole: String, val roleDirector: String, val rolePsychologist: String,
    val roleStudent: String, val roleDirectorDesc: String, val rolePsychDesc: String,
    val roleStudentDesc: String, val orgName: String, val fullName: String,
    val createOrg: String, val specialCode: String, val orgCode: String,
    val ageGroupLabel: String, val inviteCodeStudent: String, val inviteCodePsych: String,
    val welcomeHome: String, val logoutBtn: String, val yourOrgCodes: String, val language: String,
    // ── Director Dashboard ────────────────────────────────────────────────────
    val dashboardTitle: String,
    val directorPanelSubtitle: String,
    val sectionInviteCodes: String, val copyCode: String, val shareCode: String,
    val sectionAnalytics: String, val kpiBurnout: String, val kpiStress: String,
    val kpiEngagement: String, val chartTitle: String, val chartLegendStress: String,
    val chartLegendBurnout: String, val chartDaysLabel: String,
    val sectionMembers: String, val searchHint: String, val noMembers: String,
    val viewDetails: String, val memberRole: String, val memberAge: String,
    val sectionCriticalAlerts: String, val noCriticalAlerts: String,
    val contactPsychologist: String, val psychologistContacted: String,
    val noMatchingMembers: String,
    // ── Member Detail ─────────────────────────────────────────────────────────
    val sectionTestHistory: String, val noTestHistory: String,
    val testScore: String, val testAiResult: String, val testDate: String,
    val sectionCourseProgress: String, val noCourseProgress: String,
    val memberDetailTitle: String,
    // ── AI Status labels ──────────────────────────────────────────────────────
    val statusNormal: String, val statusStress: String, val statusCritical: String,
    val statusUnknown: String,
    // ── Generic home ─────────────────────────────────────────────────────────
    val genericHomeTitle: String, val refresh: String,
    // ── Contact dialog ────────────────────────────────────────────────────────
    val contactDialogTitle: String, val contactDialogNoPs: String, val contactDialogEmail: String,
    val close: String,
    // ── NEW: Tab navigation ───────────────────────────────────────────────────
    val tabDashboard: String, val tabAnalytics: String,
    val tabManagement: String, val tabContent: String,
    // ── NEW: AI Assistant ─────────────────────────────────────────────────────
    val aiAssistantTitle: String, val aiInsightLoading: String, val aiInsightEmpty: String,
    // ── NEW: Metrics ──────────────────────────────────────────────────────────
    val metricBurnout: String, val metricEmotion: String,
    val metricMotivation: String, val metricAnxiety: String,
    val metricStress: String,       // short label for the avg-stress bar
    // ── NEW: Invitation flow ──────────────────────────────────────────────────
    val inviteUser: String, val inviteSheetTitle: String,
    val psychAccess: String, val psychAccessDesc: String,
    val studentAccess: String, val studentAccessDesc: String,
    // ── NEW: Member management ────────────────────────────────────────────────
    val changeRole: String, val blockUser: String, val unblockUser: String,
    val userManagement: String,
    // ── NEW: Filters ──────────────────────────────────────────────────────────
    val filterAll: String, val filterJunior: String, val filterMiddle: String,
    val filterSenior: String, val filterStaff: String,
    // ── NEW: Content management ───────────────────────────────────────────────
    val testsTitle: String, val coursesTitle: String,
    val viewStats: String, val aiRecommended: String,
    // ── NEW: Feedback messages ────────────────────────────────────────────────
    val roleChangedMsg: String, val userBlockedMsg: String, val userUnblockedMsg: String,
    // ── NEW: Header ───────────────────────────────────────────────────────────
    val welcomeDirector: String, val orgHealthTitle: String,
    // ── NEW: Role short labels ────────────────────────────────────────────────
    val roleDirectorShort: String, val rolePsychShort: String, val roleStudentShort: String,
    // ── NEW: Matte redesign strings ───────────────────────────────────────────
    val inviteToOrg: String,       // "Пригласить в организацию" — big button on Dashboard
    val aiAnalysisTitle: String,   // "AI Анализ" — clean AI card title
    val filterSchool: String,      // "Школа" — combined school filter chip
    // ── Psychologist navigation tabs ──────────────────────────────────────────
    val psychTabStudents: String,  // "Студенты"
    val psychTabHelp: String,      // "Помощь"
    // ── Student navigation tabs ───────────────────────────────────────────────
    val tabHome: String,           // "Главная"
    val tabHelp: String,           // "Помощь"
    val tabCourses: String,        // "Курсы"
    val tabProfile: String,        // "Профиль"
    // ── StudentDatabaseTab / Psychologist analytics ───────────────────────────
    val dbTitle: String,           // "База студентов"
    val dbAnalytics: String,       // "Аналитика психолога"
    val dbShown: String,           // "Показано:"
    val dbNoStudents: String,      // "Студентов не найдено"
    val dbChangeFilter: String,    // "Измените фильтр для просмотра студентов"
    val dbViewProfile: String,     // "Подробный профиль →"
    val dbStudentProfile: String,  // "Профиль студента"
    val dbWriteRec: String,        // "Написать рекомендацию"
    val dbUpdateRec: String,       // "Обновить рекомендацию"
    val dbPsychSection: String,    // "ПСИХОЛОГИЧЕСКИЙ ПРОФИЛЬ"
    val dbMyRec: String,           // "МОЯ РЕКОМЕНДАЦИЯ"
    val dbPriority: String,        // "Приоритет:"
    val dbCourse: String,          // "Курс:"
    val priorityHigh: String,      // "Высокий"
    val priorityMedium: String,    // "Средний"
    val priorityLow: String,       // "Низкий"
    val priorityNone: String,      // "Не указан"
    // ── PatientOverviewTab ────────────────────────────────────────────────────
    val goodDay: String,           // "Добрый день,"
    val requiresAttention: String, // "требуют внимания"
    val allNormal: String,         // "Все показатели в норме ✓"
    // ── Status full labels (student profile) ─────────────────────────────────
    val statusCriticalFull: String,   // "Критическое состояние"
    val statusStressFull: String,     // "Повышенный стресс"
    val statusNormalFull: String,     // "Состояние в норме"
    val statusNoData: String,         // "Нет данных"
    // ── Student / Psychologist profile ───────────────────────────────────────
    val profileInfoTitle: String,     // "ИНФОРМАЦИЯ"
    val profileRole: String,          // "Роль"
    val profileGroup: String,         // "Группа"
    val profileTestsDone: String,     // "Пройдено тестов"
    val profileCourseProgress: String,// "Прогресс курсов"
    val profileNotSpecified: String,  // "Не указана"
    val profileLogout: String,        // "Выйти из аккаунта"
    val profileAbout: String,         // "О приложении"
    val scoreLabel: String,           // "Балл:"
)

fun getStrings(lang: AppLanguage): Strings = when (lang) {
    AppLanguage.KZ -> Strings(
        login = "Кіру", register = "Тіркелу", email = "Электрондық пошта",
        password = "Құпия сөз", noAccount = "Тіркелгіңіз жоқ па? Тіркелу",
        haveAccount = "Тіркелгіңіз бар ма? Кіру", loading = "Жүктелуде...", back = "Артқа",
        appTitle = "AI Physical", appSubtitle = "Денсаулық & Психологиялық әл-ауқат",
        chooseRole = "Рөлді таңдаңыз", roleDirector = "Ұйым өкілі (Директор)",
        rolePsychologist = "Психолог", roleStudent = "Студент / Қызметкер",
        roleDirectorDesc = "Жаңа ұйым құру және қосылу кодтарын алу",
        rolePsychDesc = "Директордан шақыру кодымен тіркелу",
        roleStudentDesc = "Ұйым кодымен қосылу", orgName = "Ұйым атауы",
        fullName = "Толық аты-жөні", createOrg = "Ұйым құру",
        specialCode = "Арнайы шақыру коды", orgCode = "Ұйым коды",
        ageGroupLabel = "Жас тобы", inviteCodeStudent = "Студенттер коды",
        inviteCodePsych = "Психолог коды", welcomeHome = "Қош келдіңіз!",
        logoutBtn = "Шығу", yourOrgCodes = "Ұйым кодтарыңыз", language = "Тіл",
        dashboardTitle = "Директор тақтасы",
        directorPanelSubtitle = "Директор тақтасы",
        sectionInviteCodes = "Шақыру кодтары", copyCode = "Көшіру", shareCode = "Бөлісу",
        sectionAnalytics = "Аналитика", kpiBurnout = "Шаршау индексі",
        kpiStress = "Орт. стресс", kpiEngagement = "Курс белсенділігі",
        chartTitle = "30 күндік эмоционалдық тренд", chartLegendStress = "Стресс",
        chartLegendBurnout = "Шаршау", chartDaysLabel = "Күндер",
        sectionMembers = "Мүшелер", searchHint = "Мүше іздеу...", noMembers = "Мүшелер жоқ",
        viewDetails = "Толығырақ", memberRole = "Рөл", memberAge = "Жас тобы",
        sectionCriticalAlerts = "🔴 Маңызды ескертулер", noCriticalAlerts = "Маңызды оқиғалар жоқ",
        contactPsychologist = "Психологпен байланысу", psychologistContacted = "Байланыс ашылды",
        noMatchingMembers = "Іздеу нәтижесі жоқ",
        sectionTestHistory = "Тест тарихы", noTestHistory = "Тест тарихы жоқ",
        testScore = "Балл", testAiResult = "AI бағасы", testDate = "Күні",
        sectionCourseProgress = "Курс барысы", noCourseProgress = "Курс деректері жоқ",
        memberDetailTitle = "Қолданушы профилі",
        statusNormal = "Қалыпты", statusStress = "Стресс", statusCritical = "Маңызды",
        statusUnknown = "Белгісіз", genericHomeTitle = "Қош келдіңіз!", refresh = "Жаңарту",
        contactDialogTitle = "Психологпен байланысу", contactDialogNoPs = "Психологтер жоқ",
        contactDialogEmail = "Email жіберу", close = "Жабу",
        tabDashboard = "Басты бет", tabAnalytics = "Аналитика",
        tabManagement = "Басқару", tabContent = "Контент",
        aiAssistantTitle = "AI-Ассистент", aiInsightLoading = "Деректер талдануда...",
        aiInsightEmpty = "Талдау деректері жоқ",
        metricBurnout = "Шаршау", metricEmotion = "Жағдай",
        metricMotivation = "Мотивация", metricAnxiety = "Алаңдаушылық",
        metricStress = "Стресс",
        inviteUser = "Пайдаланушыны шақыру", inviteSheetTitle = "Шақыру түрін таңдаңыз",
        psychAccess = "Психолог рұқсаты", psychAccessDesc = "Толық аналитика",
        studentAccess = "Студент рұқсаты", studentAccessDesc = "Тесттер мен курстар",
        changeRole = "Рөлді өзгерту", blockUser = "Бұғаттау", unblockUser = "Бұғатты алу",
        userManagement = "Пайдаланушыларды басқару",
        filterAll = "Барлығы", filterJunior = "Кіші", filterMiddle = "Орта",
        filterSenior = "Жоғары", filterStaff = "Қызметкерлер",
        testsTitle = "Тесттер", coursesTitle = "Курстар",
        viewStats = "Статистика", aiRecommended = "AI Ұсынады",
        roleChangedMsg = "Рөл өзгертілді", userBlockedMsg = "Пайдаланушы бұғатталды",
        userUnblockedMsg = "Бұғат алынды",
        welcomeDirector = "Қош келдіңіз, ", orgHealthTitle = "KASU Психикалық денсаулық",
        roleDirectorShort = "Директор", rolePsychShort = "Психолог", roleStudentShort = "Студент",
        inviteToOrg = "Ұйымға шақыру",
        aiAnalysisTitle = "AI Талдауы",
        filterSchool = "Мектеп",
        psychTabStudents = "Студенттер", psychTabHelp = "Көмек",
        tabHome = "Басты бет", tabHelp = "Көмек", tabCourses = "Курстар", tabProfile = "Профиль",
        dbTitle = "Студент базасы", dbAnalytics = "Психолог аналитикасы",
        dbShown = "Көрсетілген:", dbNoStudents = "Студенттер табылмады",
        dbChangeFilter = "Студенттерді қарау үшін сүзгіні өзгертіңіз",
        dbViewProfile = "Толық профиль →", dbStudentProfile = "Студент профилі",
        dbWriteRec = "Ұсыным жазу", dbUpdateRec = "Ұсынымды жаңарту",
        dbPsychSection = "ПСИХОЛОГИЯЛЫҚ ПРОФИЛЬ", dbMyRec = "МЕНІҢ ҰСЫНЫМЫМ",
        dbPriority = "Басымдық:", dbCourse = "Курс:",
        priorityHigh = "Жоғары", priorityMedium = "Орта",
        priorityLow = "Төмен", priorityNone = "Белгісіз",
        goodDay = "Қайырлы күн,", requiresAttention = "назар аударуды қажет ет.",
        allNormal = "Барлық көрсеткіштер қалыпты ✓",
        statusCriticalFull = "Маңызды жағдай", statusStressFull = "Жоғарылаған стресс",
        statusNormalFull = "Жағдай қалыпты", statusNoData = "Деректер жоқ",
        profileInfoTitle = "АҚПАРАТ", profileRole = "Рөл", profileGroup = "Топ",
        profileTestsDone = "Өткен тесттер", profileCourseProgress = "Курс барысы",
        profileNotSpecified = "Белгісіз", profileLogout = "Аккаунттан шығу",
        profileAbout = "Қолданба туралы", scoreLabel = "Балл:"
    )
    AppLanguage.RU -> Strings(
        login = "Войти", register = "Зарегистрироваться", email = "Электронная почта",
        password = "Пароль", noAccount = "Нет аккаунта? Зарегистрируйтесь",
        haveAccount = "Уже есть аккаунт? Войти", loading = "Загрузка...", back = "Назад",
        appTitle = "AI Physical", appSubtitle = "Здоровье & Психологическое благополучие",
        chooseRole = "Выберите вашу роль", roleDirector = "Представитель организации (Директор)",
        rolePsychologist = "Психолог", roleStudent = "Студент / Сотрудник",
        roleDirectorDesc = "Создать новую организацию и получить коды для приглашений",
        rolePsychDesc = "Регистрация по коду от директора",
        roleStudentDesc = "Присоединиться по коду организации", orgName = "Название организации",
        fullName = "Полное имя", createOrg = "Создать организацию",
        specialCode = "Специальный код приглашения", orgCode = "Код организации",
        ageGroupLabel = "Возрастная группа", inviteCodeStudent = "Код для студентов",
        inviteCodePsych = "Код для психологов", welcomeHome = "Добро пожаловать!",
        logoutBtn = "Выйти", yourOrgCodes = "Коды вашей организации", language = "Язык",
        dashboardTitle = "Панель директора",
        directorPanelSubtitle = "Панель директора",
        sectionInviteCodes = "Коды приглашений", copyCode = "Копировать", shareCode = "Поделиться",
        sectionAnalytics = "Аналитика организации", kpiBurnout = "Индекс выгорания",
        kpiStress = "Ср. уровень стресса", kpiEngagement = "Вовлечённость в курсы",
        chartTitle = "Эмоциональный тренд за 30 дней", chartLegendStress = "Стресс",
        chartLegendBurnout = "Выгорание", chartDaysLabel = "Дни",
        sectionMembers = "Участники организации", searchHint = "Поиск участника...",
        noMembers = "Участников пока нет", viewDetails = "Подробнее", memberRole = "Роль",
        memberAge = "Возр. группа", sectionCriticalAlerts = "🔴 Критические оповещения",
        noCriticalAlerts = "Критических ситуаций нет",
        contactPsychologist = "Связаться с психологом",
        psychologistContacted = "Ссылка для связи открыта", noMatchingMembers = "Ничего не найдено",
        sectionTestHistory = "История тестов", noTestHistory = "Тесты ещё не проходились",
        testScore = "Балл", testAiResult = "AI-оценка", testDate = "Дата",
        sectionCourseProgress = "Прогресс по курсам", noCourseProgress = "Данных по курсам нет",
        memberDetailTitle = "Профиль участника",
        statusNormal = "Норма", statusStress = "Стресс", statusCritical = "Критично",
        statusUnknown = "Неизвестно", genericHomeTitle = "Добро пожаловать!", refresh = "Обновить",
        contactDialogTitle = "Связаться с психологом", contactDialogNoPs = "Психологов нет",
        contactDialogEmail = "Написать на Email", close = "Закрыть",
        tabDashboard = "Главная", tabAnalytics = "Аналитика",
        tabManagement = "Управление", tabContent = "Контент",
        aiAssistantTitle = "AI-Ассистент", aiInsightLoading = "Анализируем данные...",
        aiInsightEmpty = "Нет данных для анализа",
        metricBurnout = "Выгорание", metricEmotion = "Состояние",
        metricMotivation = "Мотивация", metricAnxiety = "Тревога",
        metricStress = "Стресс",
        inviteUser = "Пригласить пользователя", inviteSheetTitle = "Выберите тип приглашения",
        psychAccess = "Доступ Психолога", psychAccessDesc = "Полная аналитика",
        studentAccess = "Доступ Студента", studentAccessDesc = "Тесты и курсы",
        changeRole = "Изменить роль", blockUser = "Заблокировать", unblockUser = "Разблокировать",
        userManagement = "Управление участниками",
        filterAll = "Все", filterJunior = "Начальная", filterMiddle = "Средняя",
        filterSenior = "Старшая", filterStaff = "Персонал",
        testsTitle = "Тесты", coursesTitle = "Курсы",
        viewStats = "Статистика", aiRecommended = "AI Рекомендует",
        roleChangedMsg = "Роль изменена", userBlockedMsg = "Пользователь заблокирован",
        userUnblockedMsg = "Пользователь разблокирован",
        welcomeDirector = "Добро пожаловать, ", orgHealthTitle = "Ментальное здоровье KASU",
        roleDirectorShort = "Директор", rolePsychShort = "Психолог", roleStudentShort = "Студент",
        inviteToOrg = "Пригласить в организацию",
        aiAnalysisTitle = "AI Анализ",
        filterSchool = "Школа",
        psychTabStudents = "Студенты", psychTabHelp = "Помощь",
        tabHome = "Главная", tabHelp = "Помощь", tabCourses = "Курсы", tabProfile = "Профиль",
        dbTitle = "База студентов", dbAnalytics = "Аналитика психолога",
        dbShown = "Показано:", dbNoStudents = "Студентов не найдено",
        dbChangeFilter = "Измените фильтр для просмотра студентов",
        dbViewProfile = "Подробный профиль →", dbStudentProfile = "Профиль студента",
        dbWriteRec = "Написать рекомендацию", dbUpdateRec = "Обновить рекомендацию",
        dbPsychSection = "ПСИХОЛОГИЧЕСКИЙ ПРОФИЛЬ", dbMyRec = "МОЯ РЕКОМЕНДАЦИЯ",
        dbPriority = "Приоритет:", dbCourse = "Курс:",
        priorityHigh = "Высокий", priorityMedium = "Средний",
        priorityLow = "Низкий", priorityNone = "Не указан",
        goodDay = "Добрый день,", requiresAttention = "требуют внимания",
        allNormal = "Все показатели в норме ✓",
        statusCriticalFull = "Критическое состояние", statusStressFull = "Повышенный стресс",
        statusNormalFull = "Состояние в норме", statusNoData = "Нет данных",
        profileInfoTitle = "ИНФОРМАЦИЯ", profileRole = "Роль", profileGroup = "Группа",
        profileTestsDone = "Пройдено тестов", profileCourseProgress = "Прогресс курсов",
        profileNotSpecified = "Не указана", profileLogout = "Выйти из аккаунта",
        profileAbout = "О приложении", scoreLabel = "Балл:"
    )
    AppLanguage.EN -> Strings(
        login = "Login", register = "Register", email = "Email", password = "Password",
        noAccount = "Don't have an account? Register here",
        haveAccount = "Already have an account? Login", loading = "Loading...", back = "Back",
        appTitle = "AI Physical", appSubtitle = "Health & Mental Well-being",
        chooseRole = "Choose your role", roleDirector = "Organization Representative (Director)",
        rolePsychologist = "Psychologist", roleStudent = "Student / Employee",
        roleDirectorDesc = "Create a new organization and get invitation codes",
        rolePsychDesc = "Register using invite code from Director",
        roleStudentDesc = "Join with organization code", orgName = "Organization Name",
        fullName = "Full Name", createOrg = "Create Organization",
        specialCode = "Special Invite Code", orgCode = "Organization Code",
        ageGroupLabel = "Age Group", inviteCodeStudent = "Student Code",
        inviteCodePsych = "Psychologist Code", welcomeHome = "Welcome!",
        logoutBtn = "Logout", yourOrgCodes = "Your Organization Codes", language = "Language",
        dashboardTitle = "Director's Panel",
        directorPanelSubtitle = "Director's Panel",
        sectionInviteCodes = "Invite Codes", copyCode = "Copy", shareCode = "Share",
        sectionAnalytics = "Organization Analytics", kpiBurnout = "Burnout Index",
        kpiStress = "Avg. Stress Level", kpiEngagement = "Course Engagement",
        chartTitle = "Emotional Trend — 30 Days", chartLegendStress = "Stress",
        chartLegendBurnout = "Burnout", chartDaysLabel = "Days",
        sectionMembers = "Organization Members", searchHint = "Search member...",
        noMembers = "No members yet", viewDetails = "View Details", memberRole = "Role",
        memberAge = "Age Group", sectionCriticalAlerts = "🔴 Critical Alerts",
        noCriticalAlerts = "No critical situations detected",
        contactPsychologist = "Contact Psychologist",
        psychologistContacted = "Contact link opened", noMatchingMembers = "No results found",
        sectionTestHistory = "Test History", noTestHistory = "No tests taken yet",
        testScore = "Score", testAiResult = "AI Assessment", testDate = "Date",
        sectionCourseProgress = "Course Progress", noCourseProgress = "No course data",
        memberDetailTitle = "Member Profile",
        statusNormal = "Normal", statusStress = "Stress", statusCritical = "Critical",
        statusUnknown = "Unknown", genericHomeTitle = "Welcome!", refresh = "Refresh",
        contactDialogTitle = "Contact Psychologist", contactDialogNoPs = "No psychologists in org",
        contactDialogEmail = "Send Email", close = "Close",
        tabDashboard = "Dashboard", tabAnalytics = "Analytics",
        tabManagement = "Management", tabContent = "Content",
        aiAssistantTitle = "AI Assistant", aiInsightLoading = "Analyzing data...",
        aiInsightEmpty = "No data to analyze",
        metricBurnout = "Burnout", metricEmotion = "Condition",
        metricMotivation = "Motivation", metricAnxiety = "Anxiety",
        metricStress = "Stress",
        inviteUser = "Invite User", inviteSheetTitle = "Choose Invitation Type",
        psychAccess = "Psychologist Access", psychAccessDesc = "Full analytics",
        studentAccess = "Student Access", studentAccessDesc = "Tests and courses",
        changeRole = "Change Role", blockUser = "Block", unblockUser = "Unblock",
        userManagement = "User Management",
        filterAll = "All", filterJunior = "Junior", filterMiddle = "Middle",
        filterSenior = "Senior", filterStaff = "Staff",
        testsTitle = "Tests", coursesTitle = "Courses",
        viewStats = "Stats", aiRecommended = "AI Recommended",
        roleChangedMsg = "Role changed", userBlockedMsg = "User blocked",
        userUnblockedMsg = "User unblocked",
        welcomeDirector = "Welcome, ", orgHealthTitle = "KASU Mental Health",
        roleDirectorShort = "Director", rolePsychShort = "Psychologist", roleStudentShort = "Student",
        inviteToOrg = "Invite to Organization",
        aiAnalysisTitle = "AI Analysis",
        filterSchool = "School",
        psychTabStudents = "Students", psychTabHelp = "Help",
        tabHome = "Home", tabHelp = "Help", tabCourses = "Courses", tabProfile = "Profile",
        dbTitle = "Student Database", dbAnalytics = "Psychologist Analytics",
        dbShown = "Shown:", dbNoStudents = "No students found",
        dbChangeFilter = "Change filter to view students",
        dbViewProfile = "Full Profile →", dbStudentProfile = "Student Profile",
        dbWriteRec = "Write Recommendation", dbUpdateRec = "Update Recommendation",
        dbPsychSection = "PSYCHOLOGICAL PROFILE", dbMyRec = "MY RECOMMENDATION",
        dbPriority = "Priority:", dbCourse = "Course:",
        priorityHigh = "High", priorityMedium = "Medium",
        priorityLow = "Low", priorityNone = "Not specified",
        goodDay = "Good day,", requiresAttention = "require attention",
        allNormal = "All indicators normal ✓",
        statusCriticalFull = "Critical Condition", statusStressFull = "Elevated Stress",
        statusNormalFull = "Normal Condition", statusNoData = "No data",
        profileInfoTitle = "INFORMATION", profileRole = "Role", profileGroup = "Group",
        profileTestsDone = "Tests Completed", profileCourseProgress = "Course Progress",
        profileNotSpecified = "Not specified", profileLogout = "Log out of account",
        profileAbout = "About App", scoreLabel = "Score:"
    )
}
