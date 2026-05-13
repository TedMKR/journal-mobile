package com.journal.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.journal.core.network.api.JournalApi
import com.journal.features.auth.AuthRoute
import com.journal.features.methodist.templates.MethodistJournalCreateRoute
import com.journal.features.methodist.templates.MethodistJournalsRoute
import com.journal.features.methodist.templates.MethodistTemplatesRoute
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
fun JournalNavHost(journalApi: JournalApi) {
    val navController = rememberNavController()
    var role by remember { mutableStateOf<String?>(null) }
    var isMenuOpen by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showMenu = currentRoute != null && currentRoute != Routes.AUTH && role != null

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.AUTH) {
            composable(Routes.AUTH) {
                AuthRoute { selectedRole ->
                    role = selectedRole
                    navController.navigate(if (selectedRole == "methodologist") Routes.METHODIST_JOURNALS else Routes.TEACHER_HOME)
                }
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
                    onBack = { navController.popBackStack() },
                    onOpenJournal = { target ->
                        navController.navigate(
                            Routes.teacherJournal(
                                groupId = target.groupId,
                                disciplineId = target.disciplineId,
                                periodId = target.periodId,
                                lessonType = target.lessonType
                            )
                        )
                    }
                )
            }
            composable(Routes.TEACHER_VED) {
                TeacherVedRoute(onBack = { navController.popBackStack() })
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
                                lessonType = target.lessonType
                            )
                        )
                    },
                    onCreateJournal = { navController.navigate(Routes.METHODIST_JOURNAL_CREATE) },
                    onOpenTemplates = { navController.navigate(Routes.METHODIST_TEMPLATES) }
                )
            }
            composable(Routes.METHODIST_TEMPLATES) {
                MethodistTemplatesRoute(
                    journalApi = journalApi,
                    onOpenJournals = { navController.navigate(Routes.METHODIST_JOURNALS) }
                )
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
                                lessonType = target.lessonType
                            )
                        )
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.TEACHER_JOURNAL,
                arguments = listOf(
                    navArgument("groupId") { type = NavType.StringType },
                    navArgument("disciplineId") { type = NavType.StringType },
                    navArgument("periodId") { type = NavType.StringType },
                    navArgument("lessonType") { type = NavType.StringType }
                )
            ) { entry ->
                val groupId = entry.arguments?.getString("groupId").orEmpty()
                val disciplineId = entry.arguments?.getString("disciplineId").orEmpty()
                val periodId = entry.arguments?.getString("periodId").orEmpty()
                TeacherJournalRoute(
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = entry.arguments?.getString("lessonType").orEmpty(),
                    journalApi = journalApi,
                    onOpenStudentCard = { studentId ->
                        navController.navigate(Routes.teacherStudentCard(groupId, disciplineId, periodId, studentId))
                    },
                    onBack = { navController.popBackStack() }
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
                    journalApi = journalApi,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (showMenu) {
            MenuButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp),
                onClick = { isMenuOpen = true }
            )
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
                }
            )
        }
    }
}

@Composable
private fun MenuButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text = "☰",
        modifier = modifier
            .background(MenuBackground, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        color = MenuPrimary,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RightSideMenu(
    role: String,
    currentRoute: String,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Меню", color = MenuPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(if (role == "methodologist") "Методист" else "Преподаватель", color = MenuPrimary.copy(alpha = 0.65f))
                }
                Text(
                    text = "×",
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(8.dp),
                    color = MenuPrimary,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            val items = if (role == "methodologist") {
                listOf(
                    MenuItem("Журналы", Routes.METHODIST_JOURNALS),
                    MenuItem("КТП", Routes.METHODIST_TEMPLATES)
                )
            } else {
                listOf(
                    MenuItem("Главная", Routes.TEACHER_HOME),
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
        }
    }
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

private data class MenuItem(
    val title: String,
    val route: String
)
