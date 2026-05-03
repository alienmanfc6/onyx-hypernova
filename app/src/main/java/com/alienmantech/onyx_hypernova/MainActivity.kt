package com.alienmantech.onyx_hypernova

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.alienmantech.onyx_hypernova.ui.RankItNavGraph
import com.alienmantech.onyx_hypernova.ui.theme.RankItTheme
import com.alienmantech.onyx_hypernova.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val homeViewModel: HomeViewModel = hiltViewModel()
            val state by homeViewModel.uiState.collectAsState()

            RankItTheme(darkTheme = state.isDarkMode) {
                RankItNavGraph()
            }
        }
    }
}
