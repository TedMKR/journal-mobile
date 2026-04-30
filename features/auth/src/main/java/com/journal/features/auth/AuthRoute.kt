package com.journal.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.journal.core.common.config.RoleSession
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthRoleEntryPoint {
    fun roleSession(): RoleSession
}

@Composable
fun AuthRoute(onContinue: () -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("teacher") }

    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, AuthRoleEntryPoint::class.java)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEEED))
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Электронный\nЖурнал",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF223268)
        )

        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Логин") }
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Пароль") }
        )

        OutlinedTextField(
            value = selectedRole,
            onValueChange = { selectedRole = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Роль debug (teacher/student/admin/dean/methodologist)") }
        )

        Button(
            onClick = {
                entryPoint.roleSession().setRole(selectedRole.trim().ifBlank { "teacher" })
                onContinue()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF223268))
        ) {
            Text("Войти")
        }

        Text(
            text = "Забыли пароль?",
            color = Color(0xFF223268),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
