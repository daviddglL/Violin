package com.violinmaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold

import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.ui.screens.HomeScreen
import com.violinmaster.app.ui.screens.LessonsScreen
import com.violinmaster.app.ui.screens.MetronomeScreen
import com.violinmaster.app.ui.screens.StatsScreen
import com.violinmaster.app.ui.screens.SettingsScreen
import com.violinmaster.app.ui.screens.TunerScreen
import com.violinmaster.app.ui.screens.AuthenticationScreen
import com.violinmaster.app.ui.theme.DarkNavBarBg
import com.violinmaster.app.ui.theme.MyApplicationTheme
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.AuthViewModel
import com.violinmaster.app.ui.viewmodel.PracticeViewModel
import com.violinmaster.app.ui.viewmodel.TunerViewModel
import com.violinmaster.app.ui.viewmodel.MetronomeViewModel
import com.violinmaster.app.ui.viewmodel.AssignmentViewModel
import com.violinmaster.app.ui.viewmodel.ChatViewModel
import com.violinmaster.app.data.auth.IGoogleAuthRepository
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.NavigationManager
import com.violinmaster.app.di.UserPreferencesManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var userPreferencesManager: UserPreferencesManager
  @Inject lateinit var authManager: AuthManager
  @Inject lateinit var navigationManager: NavigationManager
  @Inject lateinit var googleAuthRepository: IGoogleAuthRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainLayout(userPreferencesManager, authManager, navigationManager, googleAuthRepository)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
  userPreferencesManager: UserPreferencesManager,
  authManager: AuthManager,
  navigationManager: NavigationManager,
  googleAuthRepository: IGoogleAuthRepository
) {
  val currentUser by authManager.currentUser.collectAsState()
  val lang by userPreferencesManager.appLanguage.collectAsState()
  val currentTab by navigationManager.currentTab.collectAsState()
  val activeOverlay by navigationManager.currentOverlay.collectAsState()
  val authViewModel: AuthViewModel = hiltViewModel()
  val practiceVM: PracticeViewModel = hiltViewModel()
  val tunerVM: TunerViewModel = hiltViewModel()
  val metronomeVM: MetronomeViewModel = hiltViewModel()
  val assignmentVM: AssignmentViewModel = hiltViewModel()
  val chatViewModel: ChatViewModel = hiltViewModel()

  if (currentUser == null) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "VIOLIN STUDIO PRO",
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Serif
            )
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
          ),
          modifier = Modifier.statusBarsPadding()
        )
      }
    ) { innerPadding ->
      AuthenticationScreen(
        authViewModel = authViewModel,
        googleAuthRepository = googleAuthRepository,
        authManager = authManager,
        appLanguage = lang,
        modifier = Modifier.padding(innerPadding)
      )
    }
  } else {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        TopAppBar(
          title = {
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (activeOverlay != null) {
                  IconButton(
                    onClick = { navigationManager.showOverlay(null) },
                  modifier = Modifier.testTag("overlay_back_button")
                ) {
                  Icon(
                     imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                     contentDescription = "Back",
                     tint = Color.White
                  )
                 }
                 Spacer(modifier = Modifier.width(8.dp))
                 Text(
                   text = if (activeOverlay == "tuner") Localization.get("smart_tuner", lang) else Localization.get("harmonic_metronome", lang),
                   style = MaterialTheme.typography.titleLarge,
                   color = Color.White,
                   fontFamily = FontFamily.Serif
                 )
               } else {
                 Box(
                   modifier = Modifier
                     .size(36.dp)
                     .clip(CircleShape)
                     .background(Color(0xFF49454F))
                     .border(width = 1.dp, color = Color(0xFF938F99), shape = CircleShape),
                   contentAlignment = Alignment.Center
                 ) {
                   Text(
                     text = (currentUser?.username?.firstOrNull()?.toString() ?: "V").uppercase(),
                     color = Color(0xFFD0BCFF),
                     fontWeight = FontWeight.Bold,
                     fontSize = 16.sp
                   )
                 }
                 Spacer(modifier = Modifier.width(12.dp))
                 Text(
                   text = Localization.get("app_name", lang),
                   style = MaterialTheme.typography.titleLarge,
                   color = Color.White,
                   fontWeight = FontWeight.Medium,
                   fontFamily = FontFamily.Serif
                 )
               }
             }
           },
           actions = {
              IconButton(onClick = { authViewModel.logout() }) {
               Icon(
                 imageVector = Icons.Default.Person,
                 contentDescription = "Log Out",
                 tint = MaterialTheme.colorScheme.primary
               )
             }
           },
           colors = TopAppBarDefaults.topAppBarColors(
             containerColor = MaterialTheme.colorScheme.background
           ),
           modifier = Modifier.statusBarsPadding()
         )
       },
       bottomBar = {
         if (activeOverlay == null) {
           NavigationBar(
             containerColor = DarkNavBarBg,
             tonalElevation = 0.dp,
             modifier = Modifier.navigationBarsPadding()
           ) {
             NavigationBarItem(
                selected = currentTab == 0,
                onClick = { navigationManager.selectTab(0) },
               icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
               label = { Text(Localization.get("tab_home", lang), fontSize = 11.sp) },
               colors = NavigationBarItemColors(),
               modifier = Modifier.testTag("tab_home")
             )
             NavigationBarItem(
                selected = currentTab == 1,
                onClick = { navigationManager.selectTab(1) },
               icon = { Text("🎻", fontSize = 20.sp) },
               label = { Text(Localization.get("tab_lessons", lang), fontSize = 11.sp) },
               colors = NavigationBarItemColors(),
               modifier = Modifier.testTag("tab_lessons")
             )
             NavigationBarItem(
                selected = currentTab == 2,
                onClick = { navigationManager.selectTab(2) },
               icon = { Text("📈", fontSize = 20.sp) },
               label = { Text(Localization.get("tab_stats", lang), fontSize = 11.sp) },
               colors = NavigationBarItemColors(),
               modifier = Modifier.testTag("tab_stats")
             )
             NavigationBarItem(
                selected = currentTab == 3,
                onClick = { navigationManager.selectTab(3) },
               icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
               label = { Text(Localization.get("tab_settings", lang), fontSize = 11.sp) },
               colors = NavigationBarItemColors(),
               modifier = Modifier.testTag("tab_settings")
             )
           }
         }
       }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .background(MaterialTheme.colorScheme.background)
      ) {
        if (activeOverlay != null) {
          when (activeOverlay) {
            "tuner" -> TunerScreen(viewModel = tunerVM, appLanguage = lang)
            "metronome" -> MetronomeScreen(viewModel = metronomeVM, appLanguage = lang)
          }
        } else {
          when (currentTab) {
            0 -> HomeScreen(
              practiceVM = practiceVM,
              authManager = authManager,
              userPreferencesManager = userPreferencesManager,
              navigationManager = navigationManager,
              modifier = Modifier.clickable(enabled = false) {}
            )
            1 -> LessonsScreen(
              practiceVM = practiceVM,
              tunerVM = tunerVM,
              authVM = authViewModel,
              assignmentVM = assignmentVM,
              userPreferencesManager = userPreferencesManager,
              authManager = authManager,
              navigationManager = navigationManager,
              chatViewModel = chatViewModel
            )
            2 -> StatsScreen(
              practiceVM = practiceVM,
              assignmentVM = assignmentVM,
              userPreferencesManager = userPreferencesManager,
              authManager = authManager
            )
            3 -> SettingsScreen(
              practiceVM = practiceVM,
              authVM = authViewModel,
              tunerVM = tunerVM,
              userPreferencesManager = userPreferencesManager,
              authManager = authManager
            )
          }
        }
      }
    }
  }
}

@Composable
fun NavigationBarItemColors() = NavigationBarItemDefaults.colors(
  selectedIconColor = Color(0xFF21005D),
  selectedTextColor = Color.White,
  indicatorColor = Color(0xFFE8DEF8),
  unselectedIconColor = Color.White.copy(alpha = 0.6f),
  unselectedTextColor = Color.White.copy(alpha = 0.6f)
)
