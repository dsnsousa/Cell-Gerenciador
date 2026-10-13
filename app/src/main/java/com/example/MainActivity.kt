package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseInitializer
import com.example.data.repository.EcellRepository
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.LoginScreen
import com.example.ui.navigation.MainAppScreen
import com.example.ui.theme.ECellTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: EcellRepository
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getInstance(this)
        repository = EcellRepository(database)
        authViewModel = AuthViewModel(repository)

        // Seed initial database demo records if fresh installation
        lifecycleScope.launch {
            DatabaseInitializer.populateInitialDataIfEmpty(database)
        }

        setContent {
            ECellTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authState by authViewModel.uiState.collectAsState()

                    if (authState.currentUser == null) {
                        LoginScreen(viewModel = authViewModel)
                    } else {
                        MainAppScreen(
                            repository = repository,
                            currentUser = authState.currentUser!!,
                            onLogout = authViewModel::logout
                        )
                    }
                }
            }
        }
    }
}
