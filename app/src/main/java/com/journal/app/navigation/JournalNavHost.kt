package com.journal.app.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.journal.features.auth.AuthRoute
import com.journal.features.methodist.templates.MethodistDashboardRoute
import com.journal.features.methodist.templates.MethodistJournalCreateRoute
import com.journal.features.methodist.templates.MethodistJournalsRoute
import com.journal.features.methodist.templates.MethodistTemplatesRoute
import com.journal.features.student.home.StudentDashboardRoute
import com.journal.features.student.home.StudentScheduleRoute
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

@Composable
fun JournalNavHost(
    journalApi: JournalApi,
    appConfig: AppConfig,
    tokenSession: TokenSession,
    initialRole: String?,
    onClearSession: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var role by remember { mutableStateOf(initialRole) }
    var isMenuOpen by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showMenu = currentRoute != null && currentRoute != Routes.AUTH && role != null

    // Extract first name from JWT for profile screens (teacher & student dashboards).
    // Falls back gracefully to null when running in debug/stub mode (no real JWT).
    val accessToken by tokenSession.accessToken.collectAsState()
    val jwtFirstName: String? = remember(accessToken) {
        accessToken?.let { JwtUtils.extractFirstName(it) }
    }

    val startDestination = if (initialRole != null) roleStartRoute(initialRole) else Routes.AUTH

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showMenu) {
                AppHeader(
                    title = screenTitle(currentRoute.orEmpty()),
                    canNavigateBack = canNavigateBack(role = role, currentRoute = currentRoute),
                    onBack = { navController.popBackStack() },
                    onMenu = { isMenuOpen = !isMenuOpen }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                NavHost(navController = navController, startDestination = startDestination) {
            composable(Routes.AUTH) {
                AuthRoute(onContinue = { selectedRole ->
                    role = selectedRole
                    val startRoute = when (selectedRole) {
                        "methodologist" -> Routes.METHODIST_DASHBOARD
                        "student" -> Routes.STUDENT_SCHEDULE
                        else -> Routes.TEACHER_HOME
                    }
                    navController.navigate(startRoute)
                })
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
                    journalApi = journalApi,
                    jwtName = jwtFirstName,
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
                TeacherVedRoute(journalApi = journalApi)
            }
            composable(Routes.STUDENT_SCHEDULE) {
                StudentScheduleRoute(journalApi = journalApi)
            }
            composable(Routes.STUDENT_DASHBOARD) {
                StudentDashboardRoute(journalApi = journalApi, jwtFirstName = jwtFirstName)
            }
            composable(Routes.METHODIST_DASHBOARD) {
                MethodistDashboardRoute(journalApi = journalApi)
            }
            composable(Routes.METHODIST_JOURNALS) {
                MethodistJournalsRoute(
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
                TeacherJournalRoute(
                    onOpenStudentCard = { studentId ->
                        navController.navigate(Routes.teacherStudentCard(groupId, disciplineId, periodId, studentId))
                    }
                )
            }
            composable(
                route = Routes.TEACHER_STUDENT_CARD,
                arguments = listOf(
                    navArgument("groupId") { type = NavType.StringType },
                    navArgument("disciplineId") { type = NavType.StringType },
                    navArgument("periodId") { type = NavType.StringType },
                    navArgument("studentId") { type = NavType.StringType }
                )
            ) { entry ->
                TeacherStudentCardRoute(
                    groupId = entry.arguments?.getString("groupId").orEmpty(),
                    disciplineId = entry.arguments?.getString("disciplineId").orEmpty(),
                    periodId = entry.arguments?.getString("periodId").orEmpty(),
                    studentId = entry.arguments?.getString("studentId").orEmpty(),
                    journalApi = journalApi
                )
            }
                }
            }
        }

        if (isMenuOpen && role != null) {
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
            Text(
                text = "←",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                color = MenuPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        } else {
            Spacer(modifier = Modifier.align(Alignment.CenterStart).width(48.dp))
        }

        Text(
            text = title,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            color = MenuPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "☰",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(onClick = onMenu)
                .padding(horizontal = 13.dp, vertical = 8.dp),
            color = MenuPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RightSideMenu(
    role: String,
    currentRoute: String,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MenuOverlay)
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(284.dp)
                .background(MenuBackground)
                .clickable(enabled = false) {}
                .padding(horizontal = 18.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("Меню", color = MenuPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        when (role) {
                            "methodologist" -> "Методист"
                            "student" -> "Студент"
                            else -> "Преподаватель"
                        },
                        color = MenuPrimary.copy(alpha = 0.65f)
                    )
                }
                Text(
                    text = "×",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable(onClick = onDismiss)
                        .padding(8.dp),
                    color = MenuPrimary,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            val items = when (role) {
                "methodologist" -> listOf(
                    MenuItem("Личный кабинет", Routes.METHODIST_DASHBOARD),
                    MenuItem("Журналы", Routes.METHODIST_JOURNALS),
                    MenuItem("КТП", Routes.METHODIST_TEMPLATES)
                )
                "student" -> listOf(
                    MenuItem("Расписание", Routes.STUDENT_SCHEDULE),
                    MenuItem("Личный кабинет", Routes.STUDENT_DASHBOARD)
                )
                else -> listOf(
                    MenuItem("Расписание", Routes.TEACHER_HOME),
                    MenuItem("Личный кабинет", Routes.TEACHER_DASHBOARD),
                    MenuItem("Ведомости", Routes.TEACHER_VED)
                )
            }

            items.forEach { item ->
                MenuRow(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            LogoutRow(onClick = onLogout)
        }
    }
}

@Composable
private fun LogoutRow(onClick: () -> Unit) {
    Text(
        text = "Выйти из аккаунта",
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFE4E6), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        color = Color(0xFFB91C1C),
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
        Routes.METHODIST_DASHBOARD
    )
}

private fun screenTitle(route: String): String = when {
    route == Routes.TEACHER_HOME -> "Расписание занятий"
    route == Routes.TEACHER_DASHBOARD -> "Личный кабинет"
    route == Routes.TEACHER_VED -> "Ведомости"
    route.startsWith("teacher_journal") -> "Журнал занятий"
    route.startsWith("teacher_student_card") -> "Карточка студента"
    route == Routes.STUDENT_SCHEDULE -> "Расписание занятий"
    route == Routes.STUDENT_DASHBOARD -> "Личный кабинет"
    route == Routes.METHODIST_DASHBOARD -> "Личный кабинет"
    route == Routes.METHODIST_JOURNALS -> "Журналы"
    route == Routes.METHODIST_TEMPLATES -> "КТП"
    route == Routes.METHODIST_JOURNAL_CREATE -> "Создание журнала"
    else -> "Электронный журнал"
}

private data class MenuItem(
    val title: String,
    val route: String
)

private fun roleStartRoute(role: String): String = when (role) {
    "methodologist" -> Routes.METHODIST_DASHBOARD
    "student" -> Routes.STUDENT_SCHEDULE
    else -> Routes.TEACHER_HOME
}
