package com.journal.features.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

private val PrimaryText = Color(0xFF223268)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthRoleEntryPoint {
    fun roleSession(): com.journal.core.common.config.RoleSession
}

@Composable
fun AuthRoute(onContinue: (String) -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("teacher") }

    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        AuthRoleEntryPoint::class.java
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.78f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Логотип",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 190.dp, height = 74.dp),
                    contentScale = ContentScale.Fit
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoleChip("teacher", selectedRole == "teacher") { selectedRole = "teacher" }
                    RoleChip("methodologist", selectedRole == "methodologist") { selectedRole = "methodologist" }
                }

                Button(
                    onClick = {
                        // Debug backend reads role from X-Debug-Role.
                        entryPoint.roleSession().setRole(selectedRole)
                        onContinue(selectedRole)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(min = 130.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryText)
                ) {
                    Text("Войти", color = Color.White)
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
private fun RoleChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .background(
                if (selected) PrimaryText else Color(0xFFD3D7E1),
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        color = if (selected) Color.White else PrimaryText,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold
    )
}

