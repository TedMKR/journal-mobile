package com.journal.shared.navigation

object Routes {
    const val AUTH = "auth"
    const val TEACHER_HOME = "teacher_home"
    const val TEACHER_DASHBOARD = "teacher_dashboard"
    const val TEACHER_VED = "teacher_ved"
    const val TEACHER_JOURNAL = "teacher_journal/{groupId}/{disciplineId}/{periodId}/{lessonType}?teacherId={teacherId}"
    const val TEACHER_STUDENT_CARD = "teacher_student_card/{groupId}/{disciplineId}/{periodId}/{studentId}"
    const val STUDENT_SCHEDULE = "student_schedule"
    const val STUDENT_DASHBOARD = "student_dashboard"
    const val METHODIST_DASHBOARD = "methodist_dashboard"
    const val METHODIST_JOURNALS = "methodist_journals"
    const val METHODIST_TEMPLATES = "methodist_templates"
    const val METHODIST_JOURNAL_CREATE = "methodist_journal_create"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_AUDIT = "admin_audit"
    const val ADMIN_JOURNALS = "admin_journals"
    const val ADMIN_PERIODS = "admin_periods"
    const val ADMIN_ACCESS = "admin_access"

    fun teacherJournal(
        groupId: String,
        disciplineId: String,
        periodId: String,
        lessonType: String,
        teacherId: String? = null
    ): String {
        val base = "teacher_journal/$groupId/$disciplineId/$periodId/$lessonType"
        return if (teacherId.isNullOrBlank()) base else "$base?teacherId=$teacherId"
    }

    fun teacherStudentCard(groupId: String, disciplineId: String, periodId: String, studentId: String): String =
        "teacher_student_card/$groupId/$disciplineId/$periodId/$studentId"
}
