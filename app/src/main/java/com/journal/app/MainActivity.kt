package com.journal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.journal.app.navigation.JournalNavHost
import com.journal.app.ui.theme.JournalTheme
import com.journal.core.network.api.JournalApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var journalApi: JournalApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JournalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JournalNavHost(journalApi = journalApi)
                }
            }
        }
    }
}
