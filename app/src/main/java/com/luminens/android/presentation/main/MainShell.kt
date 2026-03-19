package com.luminens.android.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.luminens.android.R
import com.luminens.android.presentation.account.AccountScreen
import com.luminens.android.presentation.account.OrderHistoryScreen
import com.luminens.android.presentation.albums.AlbumsScreen
import com.luminens.android.presentation.gallery.GalleryScreen
import com.luminens.android.presentation.generate.GenerateScreen
import com.luminens.android.presentation.navigation.Screen

data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Gallery, R.string.nav_gallery, Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    BottomNavItem(Screen.Generate, R.string.nav_generate, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    BottomNavItem(Screen.Albums, R.string.nav_albums, Icons.Filled.Collections, Icons.Outlined.Collections),
    BottomNavItem(Screen.Account, R.string.nav_account, Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle),
)

@Composable
fun MainShell(
    onOpenPhoto: (String) -> Unit,
    onEditPhoto: (String, String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToPrint: () -> Unit,
    onSignedOut: () -> Unit,
    capturedImageUri: String? = null,
    onCapturedImageConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy
                        ?.any { it.route == item.screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = stringResource(item.labelRes),
                            )
                        },
                        label = { Text(stringResource(item.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Gallery.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Gallery.route) {
                GalleryScreen(
                    onPhotoClick = onOpenPhoto,
                    onEditPhoto = onEditPhoto,
                    onCartClick = onNavigateToPrint,
                )
            }
            composable(Screen.Generate.route) {
                GenerateScreen(
                    onNavigateToCamera = onNavigateToCamera,
                    onGenerationComplete = {
                        navController.navigate(Screen.Gallery.route) {
                            popUpTo(Screen.Gallery.route) { inclusive = true }
                        }
                    },
                    onNavigateToBuyCredits = { /* handled in AccountScreen */ },
                    capturedImageUri = capturedImageUri,
                    onCapturedImageConsumed = onCapturedImageConsumed,
                )
            }
            composable(Screen.Albums.route) {
                AlbumsScreen(
                    onAlbumClick = onOpenAlbum,
                )
            }
            composable(Screen.Account.route) {
                AccountScreen(
                    onManageSubscription = { /* open billing/WebView */ },
                    onSignOut = onSignedOut,
                    onPrintOrder = onNavigateToPrint,
                    onOrderHistory = { navController.navigate(Screen.OrderHistory.route) },
                )
            }
            composable(Screen.OrderHistory.route) {
                OrderHistoryScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
