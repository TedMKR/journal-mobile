package com.journal.core.common.config

object OperationToScreenMatrix {
    val teacher = mapOf(
        "listLessons" to "TeacherHomeScreen",
        "getGroupJournalGrid" to "TeacherJournalScreen",
        "markAttendance" to "TeacherJournalEditAttendance",
        "bulkMarkAttendance" to "TeacherJournalBulkEdit",
        "createGrade" to "TeacherJournalCreateGrade",
        "updateGrade" to "TeacherJournalUpdateGrade",
        "listJournalAccessGrants" to "TeacherDashboardAccessSection"
    )
}
