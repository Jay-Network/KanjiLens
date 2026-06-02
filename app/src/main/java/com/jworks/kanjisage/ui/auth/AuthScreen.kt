package com.jworks.kanjisage.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.kanjisage.data.auth.AuthRepository
import com.jworks.kanjisage.ui.theme.KanjiSageColors
import androidx.compose.ui.res.stringResource
import com.jworks.kanjisage.R
import com.jworks.kanjisage.data.auth.AuthState
import kotlinx.coroutines.launch
import com.jworks.kanjisage.ui.theme.KanjiSageTypography
import com.jworks.kanjisage.ui.theme.KanjiSageShapes

@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    onBackClick: () -> Unit = {},
    onSignedIn: () -> Unit
) {
    val authState by authRepository.authState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var signInRequested by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                authRepository.handleSignInResult(result.data)
            }
        }
    }

    // Only auto-continue after an explicit sign-in attempt from this screen.
    LaunchedEffect(authState, signInRequested) {
        if (signInRequested && authState is AuthState.SignedIn) {
            signInRequested = false
            onSignedIn()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        KanjiSageColors.DarkBg,
                        KanjiSageColors.HeaderBg
                    )
                )
            )
    ) {
        // Back button
        TextButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.auth_back),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = KanjiSageTypography.Body
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = stringResource(R.string.auth_title),
                fontSize = KanjiSageTypography.TitleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.auth_subtitle),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontSize = KanjiSageTypography.BodyMedium,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy note
            Text(
                text = stringResource(R.string.auth_privacy_note),
                color = KanjiSageColors.PrimaryAction.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontSize = KanjiSageTypography.Label,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Google Sign-In button
            Button(
                onClick = {
                    signInRequested = true
                    authRepository.initGoogleSignIn(context)
                    val intent = authRepository.getSignInIntent()
                    if (intent != null) {
                        signInLauncher.launch(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = KanjiSageShapes.Pill,
                enabled = authState !is AuthState.Loading
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.auth_google_letter),
                            fontSize = KanjiSageTypography.TitleSmall,
                            fontWeight = FontWeight.Bold,
                            color = KanjiSageColors.GoogleBlue
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.auth_continue_google),
                            color = KanjiSageColors.DarkGreyText,
                            fontSize = KanjiSageTypography.Body,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Error message
            if (authState is AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (authState as AuthState.Error).message,
                    color = KanjiSageColors.TimerWarning,
                    fontSize = KanjiSageTypography.BodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Benefits of linking
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.auth_why_link),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = KanjiSageTypography.Label,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                BenefitItem(stringResource(R.string.auth_benefit_coins))
                BenefitItem(stringResource(R.string.auth_benefit_sync))
                BenefitItem(stringResource(R.string.auth_benefit_recover))
                BenefitItem(stringResource(R.string.auth_benefit_journey))
            }
        }
    }
}

@Composable
private fun BenefitItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\u2713",
            color = KanjiSageColors.PrimaryAction,
            fontSize = KanjiSageTypography.BodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = KanjiSageTypography.Label
        )
    }
}
