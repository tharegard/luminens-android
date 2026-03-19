package com.luminens.android.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.luminens.android.R
import com.luminens.android.presentation.albums.AlbumDetailScreen
import com.luminens.android.presentation.albums.AlbumsScreen
import com.luminens.android.presentation.auth.AuthViewModel
import com.luminens.android.presentation.auth.LoginScreen
import com.luminens.android.presentation.auth.RegisterScreen
import com.luminens.android.presentation.auth.SplashScreen
import com.luminens.android.presentation.camera.CameraScreen
import com.luminens.android.presentation.editor.EditorScreen
import com.luminens.android.presentation.gallery.GalleryScreen
import com.luminens.android.presentation.gallery.PhotoViewerScreen
import com.luminens.android.presentation.generate.GenerateScreen
import com.luminens.android.presentation.main.MainShell
import com.luminens.android.presentation.print.CartViewModel
import com.luminens.android.presentation.print.PrintOrderScreen
import com.luminens.android.presentation.shared.SharedAlbumScreen
import com.luminens.android.presentation.shared.SharedPhotoScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route,
    deepLinkAlbumId: String? = null,
    deepLinkPhotoId: String? = null,
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    // Handle deep links on startup
    LaunchedEffect(deepLinkAlbumId, deepLinkPhotoId) {
        deepLinkAlbumId?.let {
            navController.navigate(Screen.SharedAlbum.createRoute(it)) {
                launchSingleTop = true
            }
        }
        deepLinkPhotoId?.let {
            navController.navigate(Screen.SharedPhoto.createRoute(it)) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onAuthenticated = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onUnauthenticated = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onAuthSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onAuthSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Main.route) { backStackEntry ->
            val capturedUri by backStackEntry.savedStateHandle
                .getStateFlow<String?>("captured_uri", null)
                .collectAsState()

            MainShell(
                onOpenPhoto = { photoId ->
                    navController.navigate(Screen.PhotoViewer.createRoute(photoId))
                },
                onEditPhoto = { photoId, uri ->
                    navController.navigate(Screen.Editor.createRoute(photoId, uri))
                },
                onOpenAlbum = { albumId ->
                    navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                },
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToPrint = { navController.navigate(Screen.PrintOrder.route) },
                onSignedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                capturedImageUri = capturedUri,
                onCapturedImageConsumed = {
                    backStackEntry.savedStateHandle.set("captured_uri", null)
                },
            )
        }

        composable(
            route = Screen.PhotoViewer.route,
            arguments = listOf(navArgument("photoId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId") ?: return@composable
            PhotoViewerScreen(
                initialPhotoId = photoId,
                onBack = { navController.popBackStack() },
                onEdit = { id, uri ->
                    navController.navigate(Screen.Editor.createRoute(id, uri))
                },
                onPrint = { navController.navigate(Screen.PrintOrder.route) },
            )
        }

        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
            AlbumDetailScreen(
                albumId = albumId,
                onBack = { navController.popBackStack() },
                onEditPhoto = { photoId, uri ->
                    navController.navigate(Screen.Editor.createRoute(photoId, uri))
                },
                onPrintAlbum = { navController.navigate(Screen.PrintOrder.route) },
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("photoId") { type = NavType.StringType },
                navArgument("photoUri") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId") ?: return@composable
            val encodedUri = backStackEntry.arguments?.getString("photoUri") ?: return@composable
            val photoUri = URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.name())
            EditorScreen(
                photoId = photoId,
                photoUri = photoUri,
                onBack = { navController.popBackStack() },
                onCropRequested = { uri ->
                    // uCrop is launched as an Activity from EditorScreen directly
                },
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onBack = { navController.popBackStack() },
                onPhotoCaptured = { uri ->
                    // After capture, go back to generate screen with the URI
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("captured_uri", uri.toString())
                    navController.popBackStack()
                },
            )
        }

        composable(Screen.PrintOrder.route) {
            PrintOrderScreen(
                onBack = { navController.popBackStack() },
                onOrderSuccess = {
                    navController.navigate(Screen.OrderSuccess.route) {
                        popUpTo(Screen.PrintOrder.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.OrderSuccess.route) {
            OrderSuccessScreen(onDone = {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.OrderSuccess.route) { inclusive = true }
                }
            })
        }

        // Public screens — no auth required
        composable(
            route = Screen.SharedAlbum.route,
            arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
            SharedAlbumScreen(
                albumId = albumId,
                onBack = { navController.popBackStack() },
                onSignUpCta = {
                    navController.navigate(Screen.Register.route)
                },
            )
        }

        composable(
            route = Screen.SharedPhoto.route,
            arguments = listOf(navArgument("photoId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId") ?: return@composable
            SharedPhotoScreen(
                photoId = photoId,
                onBack = { navController.popBackStack() },
                onSignUpCta = { navController.navigate(Screen.Register.route) },
            )
        }
    }
}

@Composable
private fun OrderSuccessScreen(onDone: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("🎉", style = MaterialTheme.typography.displayLarge)
            Text(
                stringResource(R.string.order_success),
                style = MaterialTheme.typography.headlineSmall,
            )
            Button(onClick = onDone) {
                Text(stringResource(R.string.done))
            }
        }
    }
}
