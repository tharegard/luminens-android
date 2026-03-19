package com.luminens.android.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.luminens.android.presentation.theme.OnSurfaceVariant
import com.luminens.android.presentation.theme.SurfaceElevated

data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Gallery, R.string.nav_gallery, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Generate, R.string.nav_generate, Icons.Filled.AutoFixHigh, Icons.Outlined.AutoFixHigh),
    BottomNavItem(Screen.Albums, R.string.nav_albums, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
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
            NavigationBar(
                containerColor = SurfaceElevated,
                tonalElevation = 0.dp,
            ) {
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
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = OnSurfaceVariant,
                            unselectedTextColor = OnSurfaceVariant,
                            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        ),
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
