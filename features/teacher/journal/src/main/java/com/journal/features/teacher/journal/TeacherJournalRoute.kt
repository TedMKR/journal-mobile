package com.journal.features.teacher.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val BgColor = Color(0xFFEDEEED)
private val PrimaryText = Color(0xFF223268)

@Composable
fun TeacherJournalRoute(onOpenStudentCard: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Журнал", color = PrimaryText, style = MaterialTheme.typography.headlineSmall)

        Text(
            text = "Организация процессов разработки\nпрограммного обеспечения",
            color = PrimaryText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag("2022-ФГиИБ-ИСиТ-2б")
            Tag("Практическое занятие")
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(10.dp))
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Студент", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text("01.04", color = PrimaryText)
                    Text("08.04", color = PrimaryText)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Иванова Анна",
                    color = PrimaryText,
                    modifier = Modifier.clickable(onClick = onOpenStudentCard)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(52.dp)) {
                    Text("5", color = PrimaryText)
                    Text("Н", color = PrimaryText)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.End)
                .background(Color(0xFFD3D7E1), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Редактировать", color = PrimaryText)
        }
    }
}

@Composable
private fun Tag(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFD3D7E1), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = PrimaryText, style = MaterialTheme.typography.bodySmall)
    }
}
