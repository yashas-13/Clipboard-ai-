package com.example.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.presentation.overlay.ClipboardOverlayService

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentStep by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (currentStep) {
            0 -> WelcomeStep { currentStep++ }
            1 -> PermissionsStep { currentStep++ }
            2 -> AiProviderStep { currentStep++ }
            3 -> PrivacyStep { onFinish() }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Welcome to Clipboard AI",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your smart clipboard manager powered by AI.",
            fontSize = 16.sp,
            color = Slate600,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Get Started")
        }
    }
}

@Composable
fun PermissionsStep(onNext: () -> Unit) {
    val context = LocalContext.current
    var hasOverlayPermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            hasOverlayPermission = Settings.canDrawOverlays(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Permissions",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "We need overlay permission to display the floating clipboard bubble.",
            fontSize = 16.sp,
            color = Slate600,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (hasOverlayPermission) {
                    onNext()
                } else {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    overlayPermissionLauncher.launch(intent)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (hasOverlayPermission) "Continue" else "Grant Overlay Permission")
        }
        
        if (!hasOverlayPermission) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNext) {
                Text("Skip for now", color = Slate600)
            }
        }
        
        if (hasOverlayPermission) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNext) {
                Text("Permission Granted! Tap to continue.", color = PrimaryPurple)
            }
        }
    }
}

@Composable
fun AiProviderStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Settings, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Setup AI Provider",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Choose your preferred AI to power smart actions like summarization and translation.",
            fontSize = 16.sp,
            color = Slate600,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = true, onClick = {})
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini (Recommended)", fontWeight = FontWeight.Medium)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = false, onClick = {})
                Spacer(modifier = Modifier.width(8.dp))
                Text("OpenAI", fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun PrivacyStep(onFinish: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Security, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Privacy First",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your data stays on your device. We never upload your clipboard without your explicit consent.",
            fontSize = 16.sp,
            color = Slate600,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Start Using App")
        }
    }
}
