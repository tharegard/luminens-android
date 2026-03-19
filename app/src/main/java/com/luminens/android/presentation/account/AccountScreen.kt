package com.luminens.android.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.luminens.android.R
import com.luminens.android.presentation.theme.PlanEditorColor
import com.luminens.android.presentation.theme.PlanFreeColor
import com.luminens.android.presentation.theme.PlanProColor
import com.luminens.android.presentation.theme.PlanStarterColor
import com.luminens.android.presentation.theme.OnSurfaceVariant
import com.luminens.android.presentation.theme.SurfaceBorder
import com.luminens.android.presentation.theme.SurfaceCard
import com.luminens.android.presentation.theme.SurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onSignOut: () -> Unit,
    onManageSubscription: () -> Unit,
    onPrintOrder: () -> Unit,
    onOrderHistory: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.sign_out)) },
            text = { Text("Sei sicuro di voler uscire?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.signOut(onSignOut) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.sign_out)) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        buildAnnotatedString {
                            pushStyle(SpanStyle(color = Color.White))
                            append("Lumi")
                            pop()
                            pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary))
                            append("nens")
                            pop()
                            pushStyle(SpanStyle(color = OnSurfaceVariant))
                            append("  •  ")
                            append(stringResource(R.string.account_title))
                            pop()
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceElevated),
            )
        },
    ) { padding ->
        if (isLoading && profile == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            profile?.let { p ->
                // Language selector
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.language_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        val currentLanguage = remember {
                            mutableStateOf(
                                AppCompatDelegate.getApplicationLocales()[0]?.language ?: "system"
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            LanguageOptionChip(
                                label = stringResource(R.string.language_system),
                                selected = currentLanguage.value == "system",
                                onClick = {
                                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                                    currentLanguage.value = "system"
                                },
                            )
                            LanguageOptionChip(
                                label = stringResource(R.string.language_italian),
                                selected = currentLanguage.value == "it",
                                onClick = {
                                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("it"))
                                    currentLanguage.value = "it"
                                },
                            )
                            LanguageOptionChip(
                                label = stringResource(R.string.language_english),
                                selected = currentLanguage.value == "en",
                                onClick = {
                                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                                    currentLanguage.value = "en"
                                },
                            )
                        }
                    }
                }

                // Plan card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = p.displayName ?: "Utente",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            PlanChip(plan = p.plan)
                        }

                        // Credits progress
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    stringResource(R.string.credits_remaining, p.creditsRemaining),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    stringResource(R.string.credits_of, p.creditsMonthlyLimit),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            LinearProgressIndicator(
                                progress = {
                                    (p.creditsRemaining.toFloat() / p.creditsMonthlyLimit.coerceAtLeast(1)).coerceIn(0f, 1f)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                // Subscription management
                if (p.isPremium) {
                    OutlinedButton(
                        onClick = onManageSubscription,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SurfaceBorder),
                    ) {
                        Text(stringResource(R.string.manage_subscription))
                    }
                } else {
                    Button(
                        onClick = onManageSubscription,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.upgrade_to_pro))
                    }
                }

                // New print order
                OutlinedButton(
                    onClick = onPrintOrder,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SurfaceBorder),
                ) {
                    Text(stringResource(R.string.print_order_title))
                }

                // Order history button
                OutlinedButton(
                    onClick = onOrderHistory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SurfaceBorder),
                ) {
                    Text(stringResource(R.string.order_history))
                }

                Spacer(Modifier.height(16.dp))

                // Sign out
                OutlinedButton(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.sign_out))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LanguageOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label) },
        colors = if (selected) {
            SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                labelColor = Color.White,
            )
        } else {
            SuggestionChipDefaults.suggestionChipColors(
                containerColor = SurfaceElevated,
                labelColor = OnSurfaceVariant,
            )
        },
    )
}

@Composable
private fun PlanChip(plan: String) {
    val (label, color) = when (plan) {
        "starter" -> stringResource(R.string.plan_starter) to PlanStarterColor
        "pro" -> stringResource(R.string.plan_pro) to PlanProColor
        "editor" -> stringResource(R.string.plan_editor) to PlanEditorColor
        else -> stringResource(R.string.plan_free) to PlanFreeColor
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = color),
    )
}
