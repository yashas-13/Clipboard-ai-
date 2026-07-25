package com.example.presentation.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.*

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 5

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step progress header
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step ${currentStep + 1} of $totalSteps",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPurple
            )
            TextButton(onClick = onFinish) {
                Text("Skip Setup", fontSize = 12.sp, color = Slate400)
            }
        }

        LinearProgressIndicator(
            progress = { (currentStep + 1) / totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = PrimaryPurple,
            trackColor = Slate200
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            when (currentStep) {
                0 -> WelcomeOverviewStep { currentStep++ }
                1 -> NotificationPermissionStep { currentStep++ }
                2 -> OverlayPermissionStep { currentStep++ }
                3 -> KeyboardSetupStep { currentStep++ }
                4 -> PrebuiltTemplatesStep { onFinish() }
            }
        }
    }
}

@Composable
fun WelcomeOverviewStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = PrimaryPurple.copy(alpha = 0.12f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Welcome to Clipboard AI",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your smart background clipboard engine powered by AI classification, floating overlay, and custom keyboard.",
            fontSize = 14.sp,
            color = Slate600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Feature Highlights Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FeatureRow(
                    icon = Icons.Filled.ContentCopy,
                    title = "Real-time Clipboard Capture",
                    description = "Captures copied text, links, and code snippets automatically in background."
                )
                HorizontalDivider(color = Slate200)
                FeatureRow(
                    icon = Icons.Filled.Psychology,
                    title = "AI Auto-Classification & Tags",
                    description = "Detects OTPs, URLs, Credentials, and Code with instant neural summaries."
                )
                HorizontalDivider(color = Slate200)
                FeatureRow(
                    icon = Icons.Filled.Keyboard,
                    title = "Clipboard AI Keyboard",
                    description = "Paste smart snippets and AI rewrites directly inside any application."
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Begin Guided Setup", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun NotificationPermissionStep(onNext: () -> Unit) {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = granted
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = CircleShape,
            color = (if (isGranted) EmeraldAccent else SunsetOrange).copy(alpha = 0.12f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = if (isGranted) EmeraldAccent else SunsetOrange,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Foreground Service & Notifications",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Android requires Notification permission for our background service to continuously capture clipboard updates safely.",
            fontSize = 14.sp,
            color = Slate600,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Guided Instructions Box
        GuidedInstructionCard(
            step1 = "Tap 'Grant Notification Permission' below.",
            step2 = "Select 'Allow' on the system pop-up dialog.",
            step3 = "Ensures background clipboard listener stays active reliably."
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Status Banner
        StatusBadge(isGranted = isGranted, grantedText = "Notification Permission Active", pendingText = "Notification Permission Pending")

        Spacer(modifier = Modifier.height(24.dp))

        if (!isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Button(
                onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Notification Permission", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) PrimaryPurple else Slate200,
                contentColor = if (isGranted) Color.White else Slate700
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isGranted) "Continue to Next Step" else "Skip / Continue Anyway")
        }
    }
}

@Composable
fun OverlayPermissionStep(onNext: () -> Unit) {
    val context = LocalContext.current
    var hasOverlayPermission by remember { mutableStateOf(false) }

    val checkPermission = {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    LaunchedEffect(Unit) {
        checkPermission()
    }

    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermission()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = CircleShape,
            color = (if (hasOverlayPermission) EmeraldAccent else PrimaryPurple).copy(alpha = 0.12f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (hasOverlayPermission) Icons.Filled.CheckCircle else Icons.Filled.Layers,
                    contentDescription = null,
                    tint = if (hasOverlayPermission) EmeraldAccent else PrimaryPurple,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Display Over Other Apps",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enables the floating assistant bubble on screen so you can access AI summaries and 1-tap pasting from any app.",
            fontSize = 14.sp,
            color = Slate600,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        GuidedInstructionCard(
            step1 = "1. Tap 'Open Display Over Apps Settings' below.",
            step2 = "2. Find 'Clipboard AI' in the app list.",
            step3 = "3. Turn ON 'Allow display over other apps'."
        )

        Spacer(modifier = Modifier.height(20.dp))

        StatusBadge(isGranted = hasOverlayPermission, grantedText = "Floating Overlay Enabled", pendingText = "Overlay Permission Not Granted")

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (hasOverlayPermission) {
                    onNext()
                } else {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    overlayLauncher.launch(intent)
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (hasOverlayPermission) "Continue to Keyboard Setup" else "Open Overlay Settings", fontWeight = FontWeight.SemiBold)
        }

        if (!hasOverlayPermission) {
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = onNext) {
                Text("Skip overlay setup for now", color = Slate600)
            }
        }
    }
}

@Composable
fun KeyboardSetupStep(onNext: () -> Unit) {
    val context = LocalContext.current
    var isKeyboardEnabled by remember { mutableStateOf(false) }

    val checkKeyboardStatus = {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val enabledMethods = imm?.enabledInputMethodList ?: emptyList()
            isKeyboardEnabled = enabledMethods.any { it.packageName == context.packageName }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) {
        checkKeyboardStatus()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = CircleShape,
            color = (if (isKeyboardEnabled) EmeraldAccent else OceanBlue).copy(alpha = 0.12f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isKeyboardEnabled) Icons.Filled.CheckCircle else Icons.Filled.Keyboard,
                    contentDescription = null,
                    tint = if (isKeyboardEnabled) EmeraldAccent else OceanBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Enable AI Clipboard Keyboard",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Access your saved clips, AI rewriters, and templates directly inside any text field in any app.",
            fontSize = 14.sp,
            color = Slate600,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        GuidedInstructionCard(
            step1 = "Step 1: Tap '1. Enable Keyboard in Settings' and toggle Clipboard AI ON.",
            step2 = "Step 2: Tap '2. Switch Active Input Method' and choose Clipboard AI Keyboard.",
            step3 = "Step 3: Enjoy instant 1-tap pasting from your virtual keyboard!"
        )

        Spacer(modifier = Modifier.height(20.dp))

        StatusBadge(isGranted = isKeyboardEnabled, grantedText = "Clipboard AI Keyboard Active", pendingText = "Keyboard Not Enabled Yet")

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("1. System Settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    try {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("2. Switch IME", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continue to Pre-built Templates", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PrebuiltTemplatesStep(onFinish: () -> Unit) {
    var includePrebuiltData by remember { mutableStateOf(true) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = CircleShape,
            color = PrimaryPurple.copy(alpha = 0.12f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.FolderSpecial,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Pre-built Items & Smart Folders",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We have prepared pre-built smart folder rules, code snippet templates, and sample clips so your workspace is ready immediately.",
            fontSize = 14.sp,
            color = Slate600,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Pre-built items preview list
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Included Sample Smart Rules:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                
                PrebuiltChipRow(icon = Icons.Filled.Code, label = "Dev & Code", detail = "Auto-tag #kotlin & snippet previews")
                PrebuiltChipRow(icon = Icons.Filled.VpnKey, label = "Security & Secrets", detail = "Auto-mask API keys & bearer tokens")
                PrebuiltChipRow(icon = Icons.Filled.Sms, label = "Verification OTPs", detail = "Detect 6-digit OTP codes & auto-copy")
                PrebuiltChipRow(icon = Icons.Filled.Psychology, label = "AI Prompts", detail = "Saved LLM templates & system instructions")

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Slate200)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = includePrebuiltData,
                        onCheckedChange = { includePrebuiltData = it },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load pre-built samples & smart rules now", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.RocketLaunch, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Complete Setup & Start", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = icon, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(description, fontSize = 12.sp, color = Slate600)
        }
    }
}

@Composable
fun GuidedInstructionCard(step1: String, step2: String, step3: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Guided Steps:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
            Text("• $step1", fontSize = 12.sp, color = Slate700)
            Text("• $step2", fontSize = 12.sp, color = Slate700)
            Text("• $step3", fontSize = 12.sp, color = Slate700)
        }
    }
}

@Composable
fun StatusBadge(isGranted: Boolean, grantedText: String, pendingText: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = (if (isGranted) EmeraldAccent else SunsetOrange).copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            (if (isGranted) EmeraldAccent else SunsetOrange).copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Info,
                contentDescription = null,
                tint = if (isGranted) EmeraldAccent else SunsetOrange,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isGranted) grantedText else pendingText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) EmeraldAccent else SunsetOrange
            )
        }
    }
}

@Composable
fun PrebuiltChipRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = icon, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.width(6.dp))
        Text("• $detail", fontSize = 11.sp, color = Slate400)
    }
}
