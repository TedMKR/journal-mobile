package com.journal.features.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.journal.core.ui.AppPrimary
import com.journal.core.ui.appFieldColors

private val PrimaryText = AppPrimary
private val ErrorText = Color(0xFFB91C1C)

@Composable
fun AuthRoute(
    onContinue: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    if (viewModel.isDebugRoleEnabled) {
        DebugAuthContent(
            onContinue = { selectedRole ->
                viewModel.setDebugRole(selectedRole)
                onContinue(selectedRole)
            }
        )
    } else {
        KeycloakAuthContent(
            viewModel = viewModel,
            onContinue = onContinue
        )
    }
}

@Composable
private fun KeycloakAuthContent(
    viewModel: AuthViewModel,
    onContinue: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleAuthResult(result.data, onContinue)
    }

    AuthBackgroundCard {
        Text(
            text = "Войдите через корпоративную учётную запись",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Button(
            onClick = { launcher.launch(viewModel.buildAuthIntent()) },
            enabled = state !is AuthUiState.Loading,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(min = 160.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryText)
        ) {
            if (state is AuthUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Войти", color = Color.White)
            }
        }

        val error = (state as? AuthUiState.Error)?.message
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = ErrorText,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DebugAuthContent(onContinue: (String) -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("teacher") }

    AuthBackgroundCard {
        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            label = { Text("Логин") },
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            colors = appFieldColors(),
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            label = { Text("Пароль") },
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            visualTransformation = PasswordVisualTransformation(),
            colors = appFieldColors(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoleChip("teacher", selectedRole == "teacher") { selectedRole = "teacher" }
            RoleChip("student", selectedRole == "student") { selectedRole = "student" }
            RoleChip("methodologist", selectedRole == "methodologist") { selectedRole = "methodologist" }
            RoleChip("admin", selectedRole == "admin") { selectedRole = "admin" }
        }

        Button(
            onClick = { onContinue(selectedRole) },
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

@Composable
private fun AuthBackgroundCard(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.auth_white_section_background),
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.auth_logo),
                    contentDescription = "Логотип",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 190.dp, height = 74.dp),
                    contentScale = ContentScale.Fit
                )

                content()
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
