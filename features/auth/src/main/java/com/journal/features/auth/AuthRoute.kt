package com.journal.features.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

private val BgColor = Color(0xFFEDEEED)
private val PrimaryText = Color(0xFF223268)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthRoleEntryPoint {
    fun roleSession(): com.journal.core.common.config.RoleSession
}

@Composable
fun AuthRoute(onContinue: () -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        AuthRoleEntryPoint::class.java
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        DecorativeLines(modifier = Modifier.align(Alignment.TopCenter))
        DecorativeLines(modifier = Modifier.align(Alignment.BottomCenter))

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.78f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Электронный\nЖурнал",
                    color = PrimaryText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    label = { Text("Логин") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    label = { Text("Пароль") },
                    singleLine = true
                )

                Button(
                    onClick = {
                        // Keep debug flow stable for backend test mode.
                        entryPoint.roleSession().setRole("teacher")
                        onContinue()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(min = 130.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryText)
                ) {
                    Text("Войти")
                }

                Text(
                    text = "Забыли пароль?",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DecorativeLines(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val stroke = 1f
        val color = PrimaryText.copy(alpha = 0.35f)
        var y = -size.height * 0.3f
        while (y < size.height * 1.3f) {
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(size.width, y + size.height * 0.5f),
                strokeWidth = stroke
            )
            y += 6f
        }
    }
}
