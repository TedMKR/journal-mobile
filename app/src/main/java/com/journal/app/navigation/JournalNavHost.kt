package com.journal.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
import com.journal.shared.navigation.Routes

@Composable
fun JournalNavHost(journalApi: JournalApi) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.AUTH) {
        composable(Routes.AUTH) {
            AuthRoute { role ->
                navController.navigate(if (role == "methodologist") Routes.METHODIST_JOURNALS else Routes.TEACHER_HOME)
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
                },
                onOpenDashboard = { navController.navigate(Routes.TEACHER_DASHBOARD) }
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
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId").orEmpty()
            val disciplineId = backStackEntry.arguments?.getString("disciplineId").orEmpty()
            val periodId = backStackEntry.arguments?.getString("periodId").orEmpty()
            TeacherJournalRoute(
                groupId = groupId,
                disciplineId = disciplineId,
                periodId = periodId,
                lessonType = backStackEntry.arguments?.getString("lessonType").orEmpty(),
                journalApi = journalApi,
                onOpenStudentCard = { studentId ->
                    navController.navigate(
                        Routes.teacherStudentCard(
                            groupId = groupId,
                            disciplineId = disciplineId,
                            periodId = periodId,
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
                navArgument("studentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            TeacherStudentCardRoute(
                groupId = backStackEntry.arguments?.getString("groupId").orEmpty(),
                disciplineId = backStackEntry.arguments?.getString("disciplineId").orEmpty(),
                periodId = backStackEntry.arguments?.getString("periodId").orEmpty(),
                studentId = backStackEntry.arguments?.getString("studentId").orEmpty(),
                journalApi = journalApi,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
