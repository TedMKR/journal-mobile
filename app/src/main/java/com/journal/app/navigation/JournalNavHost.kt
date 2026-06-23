package com.journal.app.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.journal.core.common.config.AppConfig
import com.journal.core.common.config.JwtUtils
import com.journal.core.common.config.TokenSession
import com.journal.core.network.api.JournalApi
import com.journal.features.admin.access.AdminAccessRoute
import com.journal.features.admin.audit.AdminAuditRoute
import com.journal.features.admin.dashboard.AdminDashboardRoute
import com.journal.features.admin.documents.AdminDocumentsRoute
import com.journal.features.admin.imports.AdminImportsRoute
import com.journal.features.admin.journals.AdminJournalsRoute
import com.journal.features.admin.periods.AdminPeriodsRoute
import com.journal.features.admin.problemstudents.AdminProblemStudentsRoute
import com.journal.features.admin.system.AdminSystemRoute
import com.journal.features.admin.users.AdminUsersRoute
import com.journal.features.auth.AuthRoute
import com.journal.features.methodist.dashboard.MethodistDashboardRoute
import com.journal.features.methodist.journalcreate.MethodistJournalCreateRoute
import com.journal.features.methodist.journals.MethodistJournalsRoute
import com.journal.features.methodist.templates.MethodistTemplatesRoute
import com.journal.features.student.home.StudentDashboardRoute
import com.journal.features.student.home.StudentScheduleRoute
import com.journal.features.student.journal.StudentJournalRoute
import com.journal.features.teacher.archive.TeacherArchiveRoute
import com.journal.features.teacher.dashboard.TeacherDashboardRoute
import com.journal.features.teacher.home.TeacherHomeRoute
import com.journal.features.teacher.journal.TeacherJournalRoute
import com.journal.features.teacher.studentcard.TeacherStudentCardRoute
import com.journal.features.teacher.ved.TeacherVedRoute
import com.journal.shared.navigation.Routes

private val MenuBackground = Color.White
private val MenuPrimary = Color(0xFF223268)
private val MenuOverlay = Color.Black.copy(alpha = 0.28f)
private val MenuItemBackground = Color(0xFFD3D7E1)
private val MenuDanger = Color(0xFFB91C1C)
private val MenuDangerBackground = Color(0xFFFFE4E6)

@Composable
fun JournalNavHost(
    journalApi: JournalApi,
    appConfig: AppConfig,
    tokenSession: TokenSession,
    initialRole: String?,
    gradeNotificationsEnabled: Boolean,
    onGradeNotificationsEnabledChange: (Boolean) -> Unit,
    onClearSession: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var role by remember { mutableStateOf(initialRole) }
    var isMenuOpen by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showMenu = currentRoute != null && currentRoute != Routes.AUTH && role != null

    val accessToken by tokenSession.accessToken.collectAsState()
    val jwtFullName: String? = remember(accessToken) {
        accessToken?.let { JwtUtils.extractFullName(it) }
    }
    val jwtUserId: String? = remember(accessToken) {
        accessToken?.let { JwtUtils.extractSubject(it) }
    }
    val startDestination = if (initialRole != null) roleStartRoute(initialRole) else Routes.AUTH

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showMenu) {
                AppHeader(
                    title = screenTitle(currentRoute.orEmpty()),
                    canNavigateBack = canNavigateBack(role = role, currentRoute = currentRoute),
                    isMenuOpen = isMenuOpen,
                    onBack = { navController.popBackStack() },
                    onMenu = { isMenuOpen = !isMenuOpen }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                NavHost(navController = navController, startDestination = startDestination) {
                    composable(Routes.AUTH) {
                        AuthRoute(onContinue = { selectedRole ->
                            role = selectedRole
                            navController.navigate(roleStartRoute(selectedRole))
                        })
                    }
                    composable(Routes.ADMIN_DASHBOARD) {
                        AdminDashboardRoute(
                            userId = jwtUserId,
                            onOpenUsers = { navController.navigate(Routes.ADMIN_USERS) },
                            onOpenAudit = { navController.navigate(Routes.ADMIN_AUDIT) },
                            onOpenJournals = { navController.navigate(Routes.ADMIN_JOURNALS) },
                            onOpenPeriods = { navController.navigate(Routes.ADMIN_PERIODS) },
                            onOpenAccess = { navController.navigate(Routes.ADMIN_ACCESS) },
                            onOpenDocuments = { navController.navigate(Routes.ADMIN_DOCUMENTS) },
                            onOpenImports = { navController.navigate(Routes.ADMIN_IMPORTS) },
                            onOpenSystem = { navController.navigate(Routes.ADMIN_SYSTEM) },
                            onOpenProblemStudents = { navController.navigate(Routes.ADMIN_PROBLEM_STUDENTS) }
                        )
                    }
                    composable(Routes.ADMIN_USERS) {
                        AdminUsersRoute(journalApi = journalApi)
                    }
                    composable(Routes.ADMIN_AUDIT) {
                        AdminAuditRoute(journalApi = journalApi)
                    }
                    composable(Routes.ADMIN_JOURNALS) {
                        AdminJournalsRoute(journalApi = journalApi)
                    }
                    composable(Routes.ADMIN_PERIODS) {
                        AdminPeriodsRoute(journalApi = journalApi)
                    }
                    composable(Routes.ADMIN_ACCESS) {
                        AdminAccessRoute(journalApi = journalApi)
                    }
                    composable(Routes.ADMIN_DOCUMENTS) {
                        AdminDocumentsRoute(journalApi = journalApi)
                    }
                    composable(Routes.ADMIN_IMPORTS) {
                        AdminImportsRoute(journalApi = journalApi)
                    }
                    composable(Routes.ADMIN_SYSTEM) {
                        AdminSystemRoute(journalApi = journalApi)
                    }
                    composable(Routes.ADMIN_PROBLEM_STUDENTS) {
                        AdminProblemStudentsRoute(journalApi = journalApi)
                    }
                    composable(Routes.TEACHER_HOME) {
                        TeacherHomeRoute(
                            onOpenLesson = { lesson ->
                                val groupId = lesson.groupId.orEmpty()
                                val disciplineId = lesson.disciplineId.orEmpty()
                                val periodId = lesson.periodId.orEmpty()
                                if (groupId.isNotBlank() && disciplineId.isNotBlank() && periodId.isNotBlank()) {
                                    navController.navigate(
                                        Routes.teacherJournal(
                                            groupId = groupId,
                                            disciplineId = disciplineId,
                                            periodId = periodId,
                                            lessonType = lesson.lessonType
                                        )
                                    )
                                }
                            }
                        )
                    }
                    composable(Routes.TEACHER_DASHBOARD) {
                        TeacherDashboardRoute(
                            jwtName = jwtFullName,
                            userId = jwtUserId,
                            onOpenJournal = { target ->
                                navController.navigate(
                                    Routes.teacherJournal(
                                        groupId = target.groupId,
                                        disciplineId = target.disciplineId,
                                        periodId = target.periodId,
                                        lessonType = target.lessonType,
                                        teacherId = target.teacherId
                                    )
                                )
                            }
                        )
                    }
                    composable(Routes.TEACHER_VED) {
                        TeacherVedRoute()
                    }
                    composable(Routes.TEACHER_ARCHIVE) {
                        TeacherArchiveRoute(
                            journalApi = journalApi,
                            onOpenJournal = { target ->
                                navController.navigate(
                                    Routes.teacherJournal(
                                        groupId = target.groupId,
                                        disciplineId = target.disciplineId,
                                        periodId = target.periodId,
                                        lessonType = target.lessonType,
                                        teacherId = target.teacherId
                                    )
                                )
                            }
                        )
                    }
                    composable(Routes.STUDENT_SCHEDULE) {
                        StudentScheduleRoute(
                            onOpenLesson = { disciplineId, periodId, groupId ->
                                navController.navigate(Routes.studentJournal(disciplineId, periodId, groupId))
                            }
                        )
                    }
                    composable(
                        route = Routes.STUDENT_JOURNAL,
                        arguments = listOf(
                            navArgument("disciplineId") { type = NavType.StringType },
                            navArgument("periodId") { type = NavType.StringType },
                            navArgument("groupId") { type = NavType.StringType }
                        )
                    ) { entry ->
                        StudentJournalRoute(
                            disciplineId = entry.arguments?.getString("disciplineId").orEmpty(),
                            periodId = entry.arguments?.getString("periodId").orEmpty(),
                            groupId = entry.arguments?.getString("groupId").orEmpty()
                        )
                    }
                    composable(Routes.STUDENT_DASHBOARD) {
                        StudentDashboardRoute(jwtName = jwtFullName)
                    }
                    composable(Routes.METHODIST_DASHBOARD) {
                        MethodistDashboardRoute(
                            jwtName = jwtFullName,
                            userId = jwtUserId
                        )
                    }
                    composable(Routes.METHODIST_JOURNALS) {
                        MethodistJournalsRoute(
                            onOpenJournal = { target ->
                                navController.navigate(
                                    Routes.teacherJournal(
                                        groupId = target.groupId,
                                        disciplineId = target.disciplineId,
                                        periodId = target.periodId,
                                        lessonType = target.lessonType,
                                        teacherId = target.teacherId
                                    )
                                )
                            },
                            onCreateJournal = { navController.navigate(Routes.METHODIST_JOURNAL_CREATE) }
                        )
                    }
                    composable(Routes.METHODIST_TEMPLATES) {
                        MethodistTemplatesRoute(journalApi = journalApi)
                    }
                    composable(Routes.METHODIST_JOURNAL_CREATE) {
                        MethodistJournalCreateRoute(
                            journalApi = journalApi,
                            onOpenJournal = { target ->
                                navController.navigate(
                                    Routes.teacherJournal(
                                        groupId = target.groupId,
                                        disciplineId = target.disciplineId,
                                        periodId = target.periodId,
                                        lessonType = target.lessonType,
                                        teacherId = target.teacherId
                                    )
                                )
                            }
                        )
                    }
                    composable(
                        route = Routes.TEACHER_JOURNAL,
                        arguments = listOf(
                            navArgument("groupId") { type = NavType.StringType },
                            navArgument("disciplineId") { type = NavType.StringType },
                            navArgument("periodId") { type = NavType.StringType },
                            navArgument("lessonType") { type = NavType.StringType },
                            navArgument("teacherId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { entry ->
                        val groupId = entry.arguments?.getString("groupId").orEmpty()
                        val disciplineId = entry.arguments?.getString("disciplineId").orEmpty()
                        val periodId = entry.arguments?.getString("periodId").orEmpty()
                        val lessonType = entry.arguments?.getString("lessonType").orEmpty()
                        TeacherJournalRoute(
                            onOpenStudentCard = { studentId ->
                                navController.navigate(
                                    Routes.teacherStudentCard(
                                        groupId = groupId,
                                        disciplineId = disciplineId,
                                        periodId = periodId,
                                        lessonType = lessonType,
                                        studentId = studentId
                                    )
                                )
                            }
                        )
                    }
                    composable(
                        route = Routes.TEACHER_STUDENT_CARD,
                        arguments = listOf(
                            navArgument("groupId") { type = NavType.StringType },
                            navArgument("disciplineId") { type = NavType.StringType },
                            navArgument("periodId") { type = NavType.StringType },
                            navArgument("lessonType") { type = NavType.StringType },
                            navArgument("studentId") { type = NavType.StringType }
                        )
                    ) {
                        TeacherStudentCardRoute()
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isMenuOpen && role != null,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            RightSideMenu(
                role = role.orEmpty(),
                currentRoute = currentRoute.orEmpty(),
                onDismiss = { isMenuOpen = false },
                onNavigate = { route ->
                    isMenuOpen = false
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                gradeNotificationsEnabled = gradeNotificationsEnabled,
                onGradeNotificationsEnabledChange = onGradeNotificationsEnabledChange,
                onLogout = {
                    isMenuOpen = false
                    openKeycloakLogout(
                        context = context,
                        appConfig = appConfig,
                        idToken = tokenSession.idToken.value
                    )
                    onClearSession()
                    role = null
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
private fun AppHeader(
    title: String,
    canNavigateBack: Boolean,
    isMenuOpen: Boolean,
    onBack: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MenuBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (canNavigateBack) {
            HeaderIconButton(
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = onBack
            ) {
                BackIcon()
            }
        } else {
            Spacer(modifier = Modifier.align(Alignment.CenterStart).width(48.dp))
        }

        Text(
            text = title,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 64.dp),
            color = MenuPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        HeaderIconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = onMenu
        ) {
            if (isMenuOpen) {
                CloseIcon()
            } else {
                MenuIcon()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedVisibilityScope.RightSideMenu(
    role: String,
    currentRoute: String,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    gradeNotificationsEnabled: Boolean,
    onGradeNotificationsEnabledChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val animScope = this
    var showSettings by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .animateEnterExit(
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            )
            .background(MenuOverlay)
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = with(animScope) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(284.dp)
                    .animateEnterExit(
                        enter = slideInHorizontally(animationSpec = tween(300)) { it },
                        exit = slideOutHorizontally(animationSpec = tween(300)) { it }
                    )
            }
                .background(MenuBackground)
                .clickable(enabled = false) {}
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showSettings) {
                SettingsMenuContent(
                    gradeNotificationsEnabled = gradeNotificationsEnabled,
                    onNotificationsEnabledChange = onGradeNotificationsEnabledChange,
                    onLogout = onLogout
                )
            } else {
                MainMenuContent(
                    role = role,
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    onOpenSettings = { showSettings = true }
                )
            }
        }

        HeaderIconButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 16.dp),
            onClick = onDismiss
        ) {
            CloseIcon()
        }
    }
}

@Composable
private fun ColumnScope.MainMenuContent(
    role: String,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    Column {
        Text(
            text = "Меню",
            color = MenuPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = roleTitle(role),
            color = MenuPrimary.copy(alpha = 0.65f)
        )
    }

    menuItems(role).forEach { item ->
        MenuRow(
            item = item,
            selected = currentRoute == item.route,
            onClick = { onNavigate(item.route) }
        )
    }

    Spacer(modifier = Modifier.weight(1f))
    SettingsRow(onClick = onOpenSettings)
}

@Composable
private fun ColumnScope.SettingsMenuContent(
    gradeNotificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    Column {
        Text(
            text = "Настройки",
            color = MenuPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }

    NotificationSwitchRow(
        checked = gradeNotificationsEnabled,
        onCheckedChange = onNotificationsEnabledChange
    )

    Text(
        text = if (gradeNotificationsEnabled) {
            "Системные уведомления об оценках включены."
        } else {
            "Системные уведомления об оценках отключены."
        },
        color = MenuPrimary.copy(alpha = 0.65f),
        style = MaterialTheme.typography.bodySmall
    )

    Spacer(modifier = Modifier.weight(1f))
    LogoutRow(onClick = onLogout)
}

@Composable
private fun NotificationSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MenuItemBackground, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Уведомления",
            color = MenuPrimary,
            fontWeight = FontWeight.SemiBold
        )
        ProjectSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ProjectSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val shape = RoundedCornerShape(30.dp)
    val trackColor by animateColorAsState(
        targetValue = if (checked) MenuPrimary else Color.White,
        animationSpec = tween(240),
        label = "notificationSwitchTrackColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) MenuPrimary else MenuItemBackground,
        animationSpec = tween(240),
        label = "notificationSwitchBorderColor"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) Color.White else MenuItemBackground,
        animationSpec = tween(240),
        label = "notificationSwitchThumbColor"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 30.dp else 4.dp,
        animationSpec = tween(240),
        label = "notificationSwitchThumbOffset"
    )

    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 32.dp)
            .background(trackColor, shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = thumbOffset)
                .size(22.dp)
                .background(thumbColor, RoundedCornerShape(20.dp))
        )
    }
}

@Composable
private fun SettingsRow(onClick: () -> Unit) {
    MenuActionRow(
        text = "Настройки",
        onClick = onClick
    )
}

@Composable
private fun MenuActionRow(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .background(MenuItemBackground, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        color = MenuPrimary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun LogoutRow(onClick: () -> Unit) {
    Text(
        text = "Выйти из аккаунта",
        modifier = Modifier
            .fillMaxWidth()
            .background(MenuDangerBackground, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        color = MenuDanger,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun MenuRow(item: MenuItem, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = item.title,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MenuPrimary else MenuItemBackground, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        color = if (selected) Color.White else MenuPrimary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun HeaderIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun BackIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val strokeWidth = 2.5.dp.toPx()
        drawLine(
            color = MenuPrimary,
            start = Offset(size.width * 0.72f, size.height * 0.2f),
            end = Offset(size.width * 0.3f, size.height * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = MenuPrimary,
            start = Offset(size.width * 0.3f, size.height * 0.5f),
            end = Offset(size.width * 0.72f, size.height * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = MenuPrimary,
            start = Offset(size.width * 0.32f, size.height * 0.5f),
            end = Offset(size.width * 0.88f, size.height * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun MenuIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val strokeWidth = 2.5.dp.toPx()
        listOf(0.26f, 0.5f, 0.74f).forEach { y ->
            drawLine(
                color = MenuPrimary,
                start = Offset(size.width * 0.2f, size.height * y),
                end = Offset(size.width * 0.8f, size.height * y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun CloseIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val strokeWidth = 2.5.dp.toPx()
        drawLine(
            color = MenuPrimary,
            start = Offset(size.width * 0.25f, size.height * 0.25f),
            end = Offset(size.width * 0.75f, size.height * 0.75f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = MenuPrimary,
            start = Offset(size.width * 0.75f, size.height * 0.25f),
            end = Offset(size.width * 0.25f, size.height * 0.75f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun openKeycloakLogout(
    context: android.content.Context,
    appConfig: AppConfig,
    idToken: String?
) {
    val logoutUri = Uri.parse(
        "${appConfig.keycloakBaseUrl}/realms/${appConfig.keycloakRealm}/protocol/openid-connect/logout"
    ).buildUpon()
        .appendQueryParameter("client_id", appConfig.keycloakClientId)
        .apply {
            if (!idToken.isNullOrBlank()) {
                appendQueryParameter("id_token_hint", idToken)
            }
        }
        .build()

    val intent = Intent(Intent.ACTION_VIEW, logoutUri)
    context.startActivity(intent)
}

private fun canNavigateBack(role: String?, currentRoute: String?): Boolean {
    if (role == null || currentRoute == null || currentRoute == Routes.AUTH) return false
    return currentRoute !in setOf(
        Routes.TEACHER_HOME,
        Routes.STUDENT_SCHEDULE,
        Routes.METHODIST_DASHBOARD,
        Routes.ADMIN_DASHBOARD
    )
}

private fun screenTitle(route: String): String = when {
    route == Routes.TEACHER_HOME -> "Расписание занятий"
    route == Routes.TEACHER_DASHBOARD -> "Личный кабинет"
    route == Routes.TEACHER_VED -> "Ведомости"
    route == Routes.TEACHER_ARCHIVE -> "Архив журналов"
    route.startsWith("teacher_journal") -> "Журнал занятий"
    route.startsWith("teacher_student_card") -> "Карточка студента"
    route == Routes.STUDENT_SCHEDULE -> "Расписание занятий"
    route == Routes.STUDENT_DASHBOARD -> "Личный кабинет"
    route.startsWith("student_journal") -> "Журнал занятий"
    route == Routes.METHODIST_DASHBOARD -> "Личный кабинет"
    route == Routes.METHODIST_JOURNALS -> "Журналы"
    route == Routes.METHODIST_TEMPLATES -> "КТП"
    route == Routes.METHODIST_JOURNAL_CREATE -> "Создание журнала"
    route == Routes.ADMIN_DASHBOARD -> "Личный кабинет"
    route == Routes.ADMIN_USERS -> "Пользователи"
    route == Routes.ADMIN_AUDIT -> "Аудит"
    route == Routes.ADMIN_JOURNALS -> "Журналы"
    route == Routes.ADMIN_PERIODS -> "Учебные периоды"
    route == Routes.ADMIN_ACCESS -> "Доступы"
    route == Routes.ADMIN_DOCUMENTS -> "Документы"
    route == Routes.ADMIN_IMPORTS -> "Импорт"
    route == Routes.ADMIN_SYSTEM -> "Система"
    route == Routes.ADMIN_PROBLEM_STUDENTS -> "Проблемные студенты"
    else -> "Электронный журнал"
}

private fun roleTitle(role: String): String = when (role) {
    "methodologist" -> "Методист"
    "student" -> "Студент"
    "admin" -> "Администратор"
    else -> "Преподаватель"
}

private fun menuItems(role: String): List<MenuItem> = when (role) {
    "methodologist" -> listOf(
        MenuItem("Личный кабинет", Routes.METHODIST_DASHBOARD),
        MenuItem("Журналы", Routes.METHODIST_JOURNALS),
        MenuItem("КТП", Routes.METHODIST_TEMPLATES)
    )
    "student" -> listOf(
        MenuItem("Расписание", Routes.STUDENT_SCHEDULE),
        MenuItem("Личный кабинет", Routes.STUDENT_DASHBOARD)
    )
    "admin" -> listOf(
        MenuItem("Личный кабинет", Routes.ADMIN_DASHBOARD),
        MenuItem("Пользователи", Routes.ADMIN_USERS),
        MenuItem("Аудит", Routes.ADMIN_AUDIT),
        MenuItem("Журналы", Routes.ADMIN_JOURNALS),
        MenuItem("Периоды", Routes.ADMIN_PERIODS),
        MenuItem("Доступы", Routes.ADMIN_ACCESS),
        MenuItem("Документы", Routes.ADMIN_DOCUMENTS),
        MenuItem("Импорт", Routes.ADMIN_IMPORTS),
        MenuItem("Система", Routes.ADMIN_SYSTEM),
        MenuItem("Проблемные студенты", Routes.ADMIN_PROBLEM_STUDENTS)
    )
    else -> listOf(
        MenuItem("Расписание", Routes.TEACHER_HOME),
        MenuItem("Личный кабинет", Routes.TEACHER_DASHBOARD),
        MenuItem("Ведомости", Routes.TEACHER_VED),
        MenuItem("Архив", Routes.TEACHER_ARCHIVE)
    )
}

private data class MenuItem(
    val title: String,
    val route: String
)

private fun roleStartRoute(role: String): String = when (role) {
    "methodologist" -> Routes.METHODIST_DASHBOARD
    "student" -> Routes.STUDENT_SCHEDULE
    "admin" -> Routes.ADMIN_DASHBOARD
    else -> Routes.TEACHER_HOME
}
