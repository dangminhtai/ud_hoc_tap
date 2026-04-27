package com.duong.udhoctap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.duong.udhoctap.core.data.repository.DeckRepository
import com.duong.udhoctap.core.data.repository.SettingsRepository
import com.duong.udhoctap.core.ui.navigation.AppNavigation
import com.duong.udhoctap.core.ui.theme.UdHocTapTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var deckRepository: DeckRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkTheme by rememberSaveable { mutableStateOf(settingsRepository.darkTheme) }
            UdHocTapTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        darkTheme = darkTheme,
                        onThemeChanged = { darkTheme = it },
                        deckRepository = deckRepository
                    )
                }
            }
        }
    }
}
