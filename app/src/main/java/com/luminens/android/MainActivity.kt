package com.luminens.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.luminens.android.presentation.auth.AuthViewModel
import com.luminens.android.presentation.navigation.NavGraph
import com.luminens.android.presentation.theme.LuminensTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingAlbumId: String? = null
    private var pendingPhotoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        parseIntent(intent)
        setContent {
            LuminensTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                NavGraph(
                    navController = navController,
                    authViewModel = authViewModel,
                    deepLinkAlbumId = pendingAlbumId,
                    deepLinkPhotoId = pendingPhotoId,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIntent(intent)
        val data = intent.data?.toString() ?: return
        when {
            data.startsWith("luminens://auth/callback") -> {
                // Supabase Auth module handles this internally via the registered scheme
            }
        }
    }

    private fun parseIntent(intent: Intent?) {
        val data = intent?.data ?: return
        val pathSegments = data.pathSegments
        when {
            data.host == "shared-album" || (pathSegments.size >= 2 && pathSegments[0] == "shared-album") -> {
                pendingAlbumId = data.lastPathSegment
            }
            data.host == "shared-photo" || (pathSegments.size >= 2 && pathSegments[0] == "shared-photo") -> {
                pendingPhotoId = data.lastPathSegment
            }
        }
    }
}
