package com.journal.features.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.journal.core.ui.AppTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

private val PrimaryText: Color
    @Composable get() = AppTheme.colors.primary
private val ErrorText: Color
    @Composable get() = AppTheme.colors.danger

@Composable
fun AuthRoute(
    onContinue: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
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
            modifier = Modifier.fillMaxWidth(),
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = { launcher.launch(viewModel.buildAuthIntent()) },
            enabled = state !is AuthUiState.Loading,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(min = 160.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.primary,
                contentColor = AppTheme.colors.onPrimary
            )
        ) {
            if (state is AuthUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = AppTheme.colors.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Войти", color = AppTheme.colors.onPrimary)
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
private fun AuthBackgroundCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = AppTheme.colors
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
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
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
