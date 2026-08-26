package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.audio.AudioPlayerScreen
import com.example.ui.editor.PhotoEditorScreen
import com.example.ui.gallery.GalleryHomeScreen
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.navigation.NavRoutes
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.player.VideoPlayerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.SultanGalleryTheme
import com.example.ui.tools.SultanToolsScreen
import com.example.ui.trash.TrashScreen
import com.example.ui.vault.SecretVaultScreen
import com.example.ui.viewer.PhotoViewerScreen

@androidx.compose.material3.ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SultanGalleryApp()
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun SultanGalleryApp(
    galleryViewModel: GalleryViewModel = viewModel()
) {
    val state by galleryViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    SultanGalleryTheme(
        darkTheme = state.isDark,
        isAmoled = state.isAmoled
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = NavRoutes.GALLERY_HOME
            ) {
                // Onboarding Screen
                composable(NavRoutes.ONBOARDING) {
                    OnboardingScreen(
                        onFinishOnboarding = {
                            navController.navigate(NavRoutes.GALLERY_HOME) {
                                popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }

                // Gallery Home Screen
                composable(NavRoutes.GALLERY_HOME) {
                    GalleryHomeScreen(
                        viewModel = galleryViewModel,
                        onNavigateToViewer = { mediaId ->
                            navController.navigate(NavRoutes.photoViewer(mediaId))
                        },
                        onNavigateToPlayer = { mediaId ->
                            navController.navigate(NavRoutes.videoPlayer(mediaId))
                        },
                        onNavigateToAudio = { mediaId ->
                            navController.navigate(NavRoutes.audioPlayer(mediaId))
                        },
                        onNavigateToTools = {
                            navController.navigate(NavRoutes.SULTAN_TOOLS)
                        },
                        onNavigateToVault = {
                            navController.navigate(NavRoutes.SECRET_VAULT)
                        },
                        onNavigateToTrash = {
                            navController.navigate(NavRoutes.TRASH)
                        },
                        onNavigateToSettings = {
                            navController.navigate(NavRoutes.SETTINGS)
                        }
                    )
                }

                // Photo Viewer Screen
                composable(
                    route = NavRoutes.PHOTO_VIEWER,
                    arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
                    PhotoViewerScreen(
                        initialMediaId = mediaId,
                        viewModel = galleryViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEditor = { uri ->
                            navController.navigate(NavRoutes.photoEditor(uri))
                        }
                    )
                }

                // Video Player Screen
                composable(
                    route = NavRoutes.VIDEO_PLAYER,
                    arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
                    VideoPlayerScreen(
                        mediaId = mediaId,
                        viewModel = galleryViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToVideo = { nextId ->
                            navController.navigate(NavRoutes.videoPlayer(nextId)) {
                                popUpTo(NavRoutes.VIDEO_PLAYER) { inclusive = true }
                            }
                        }
                    )
                }

                // Audio Player Screen
                composable(
                    route = NavRoutes.AUDIO_PLAYER,
                    arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
                    AudioPlayerScreen(
                        mediaId = mediaId,
                        viewModel = galleryViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Photo Editor Screen
                composable(
                    route = NavRoutes.PHOTO_EDITOR,
                    arguments = listOf(navArgument("uri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val uriStr = backStackEntry.arguments?.getString("uri") ?: ""
                    val uri = Uri.parse(Uri.decode(uriStr))
                    PhotoEditorScreen(
                        imageUri = uri,
                        galleryViewModel = galleryViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Sultan Tools Screen
                composable(NavRoutes.SULTAN_TOOLS) {
                    SultanToolsScreen(
                        galleryViewModel = galleryViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Secret Vault Screen
                composable(NavRoutes.SECRET_VAULT) {
                    SecretVaultScreen(
                        galleryViewModel = galleryViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) }
                    )
                }

                // Trash / Recycle Bin Screen
                composable(NavRoutes.TRASH) {
                    TrashScreen(
                        galleryViewModel = galleryViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Settings & Developer Screen
                composable(NavRoutes.SETTINGS) {
                    SettingsScreen(
                        galleryViewModel = galleryViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
