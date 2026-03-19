package com.luminens.android.presentation.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Main : Screen("main")

    // Main tabs (nested in MainShell)
    data object Gallery : Screen("gallery")
    data object Generate : Screen("generate")
    data object Albums : Screen("albums")
    data object Account : Screen("account")

    // Detail screens
    data object PhotoViewer : Screen("photo_viewer/{photoId}") {
        fun createRoute(photoId: String) = "photo_viewer/$photoId"
    }
    data object AlbumDetail : Screen("album_detail/{albumId}") {
        fun createRoute(albumId: String) = "album_detail/$albumId"
    }
    data object Editor : Screen("editor/{photoId}/{photoUri}") {
        fun createRoute(photoId: String, photoUri: String) =
            "editor/$photoId/${Uri.encode(photoUri)}"
    }
    data object Camera : Screen("camera")
    data object PrintOrder : Screen("print_order")
    data object OrderSuccess : Screen("order_success")

    // Cart
    data object Cart : Screen("cart")

    // Public (no auth)
    data object SharedPhoto : Screen("shared_photo/{photoId}") {
        fun createRoute(photoId: String) = "shared_photo/$photoId"
    }
    data object SharedAlbum : Screen("shared_album/{albumId}") {
        fun createRoute(albumId: String) = "shared_album/$albumId"
    }
}
