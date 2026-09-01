package com.iris.gallery.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.iris.gallery.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

fun isBiometricHardwareAvailable(context: Context): Boolean {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bm = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
            bm?.canAuthenticate(
                android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
            ) == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            val fm = context.getSystemService(android.hardware.fingerprint.FingerprintManager::class.java)
            fm?.isHardwareDetected == true && fm.hasEnrolledFingerprints()
        } else {
            false
        }
    }.getOrDefault(false)
}

fun triggerBiometricPrompt(
    activity: Activity,
    title: String? = null,
    subtitle: String? = null,
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {}
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        runCatching {
            val executor = ContextCompat.getMainExecutor(activity)
            val promptTitle = title ?: activity.getString(R.string.app_lock_title)
            val promptSubtitle = subtitle ?: activity.getString(R.string.app_lock_scan_fingerprint)
            val prompt = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                .setTitle(promptTitle)
                .setSubtitle(promptSubtitle)
                .setNegativeButton(activity.getString(R.string.pin_keypad_use_pin), executor) { _, _ -> }
                .build()
            val cancellationSignal = CancellationSignal()
            prompt.authenticate(
                cancellationSignal,
                executor,
                object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        onError(errString?.toString() ?: activity.getString(R.string.app_lock_auth_cancelled))
                    }
                }
            )
        }.onFailure { error ->
            onError(error.message ?: activity.getString(R.string.app_lock_biometrics_unavailable))
        }
    }
}

@Composable
fun AppLockScreen(
    isPicker: Boolean = false,
    biometricsEnabled: Boolean = true,
    onVerifyPin: (String) -> Boolean,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val haptic = LocalHapticFeedback.current
    val hasBiometrics = remember(context) { isBiometricHardwareAvailable(context) && biometricsEnabled }
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUnlockedSuccess by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun performUnlock() {
        if (isUnlockedSuccess) return
        isUnlockedSuccess = true
        scope.launch {
            delay(160) // Visual celebration with green dots pop
            onUnlocked()
        }
    }

    fun triggerBiometrics() {
        if (activity != null && hasBiometrics && !activity.isFinishing && !activity.isDestroyed && !isUnlockedSuccess) {
            triggerBiometricPrompt(
                activity = activity,
                title = if (isPicker) context.getString(R.string.app_lock_picker_title) else context.getString(R.string.app_lock_title),
                subtitle = context.getString(R.string.app_lock_scan_fingerprint),
                onSuccess = ::performUnlock,
                onError = { /* stay on PIN screen gracefully */ }
            )
        }
    }

    // Auto-prompt fingerprint safely after window initialization
    LaunchedEffect(Unit) {
        if (hasBiometrics) {
            delay(300)
            triggerBiometrics()
        }
    }

    fun triggerWrongPinAnimation() {
        errorMessage = context.getString(R.string.app_lock_wrong_pin)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 450
                    -30f at 40 using FastOutSlowInEasing
                    30f at 90 using FastOutSlowInEasing
                    -24f at 140 using FastOutSlowInEasing
                    24f at 190 using FastOutSlowInEasing
                    -16f at 240 using FastOutSlowInEasing
                    16f at 290 using FastOutSlowInEasing
                    -8f at 340 using FastOutSlowInEasing
                    8f at 390 using FastOutSlowInEasing
                    0f at 450
                }
            )
            enteredPin = ""
        }
    }

    fun handleDigit(digit: String) {
        if (isUnlockedSuccess) return
        if (enteredPin.length < 6) {
            val nextPin = enteredPin + digit
            enteredPin = nextPin
            errorMessage = null
            if (nextPin.length >= 4) {
                if (onVerifyPin(nextPin)) {
                    performUnlock()
                } else if (nextPin.length == 6) {
                    triggerWrongPinAnimation()
                }
            }
        }
    }

    fun handleBackspace() {
        if (isUnlockedSuccess) return
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Botanical App Icon with Pulsing Halo on Unlock
                val logoScale by animateFloatAsState(
                    targetValue = if (isUnlockedSuccess) 1.08f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "logoScale"
                )

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .graphicsLayer {
                            scaleX = logoScale
                            scaleY = logoScale
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (isUnlockedSuccess) listOf(Color(0xFF00C853), Color(0xFF00E676))
                                else listOf(Color(0xFF9D34F5), Color(0xFF4F16D8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = if (isUnlockedSuccess) stringResource(R.string.app_lock_welcome_back)
                    else if (isPicker) stringResource(R.string.app_lock_picker_title)
                    else stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = if (isUnlockedSuccess) stringResource(R.string.app_lock_unlocking)
                    else if (isPicker) stringResource(R.string.app_lock_picker_subtitle)
                    else stringResource(R.string.app_lock_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(20.dp))

                // PIN Dots Indicator
                PinDotsIndicator(
                    count = maxOf(4, enteredPin.length),
                    filledCount = enteredPin.length,
                    isError = errorMessage != null,
                    isSuccess = isUnlockedSuccess,
                    shakeOffset = shakeOffset.value
                )

                // Error message container
                Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.Center) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Keypad Grid
            PinKeypad(
                hasBiometrics = hasBiometrics,
                onDigit = ::handleDigit,
                onBackspace = ::handleBackspace,
                onBiometric = ::triggerBiometrics
            )
        }
    }
}

@Composable
fun PinDotsIndicator(
    count: Int,
    filledCount: Int,
    isError: Boolean,
    isSuccess: Boolean = false,
    shakeOffset: Float = 0f
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.offset { IntOffset(shakeOffset.roundToInt(), 0) }
    ) {
        for (i in 0 until count) {
            val isFilled = i < filledCount || isSuccess
            val dotColor by animateColorAsState(
                targetValue = when {
                    isError -> MaterialTheme.colorScheme.error
                    isSuccess -> Color(0xFF00C853)
                    isFilled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                },
                animationSpec = tween(180),
                label = "dotColor"
            )
            val dotScale by animateFloatAsState(
                targetValue = if (isSuccess) 1.25f else if (isFilled) 1.15f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "dotScale"
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer(scaleX = dotScale, scaleY = dotScale)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
fun PinKeypad(
    hasBiometrics: Boolean = false,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        ).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    KeypadButton(text = digit, onClick = { onDigit(digit) })
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasBiometrics) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 36.dp),
                            onClick = onBiometric
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Fingerprint,
                        contentDescription = stringResource(R.string.settings_biometric_unlock_title),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else {
                Spacer(Modifier.size(72.dp))
            }

            KeypadButton(text = "0", onClick = { onDigit("0") })

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 36.dp),
                        onClick = onBackspace
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = stringResource(R.string.action_backspace),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 36.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// -------------------------------------------------------------
// Beautiful Bottom Sheets for PIN Setup, Change, and Disable
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupBottomSheet(
    onDismiss: () -> Unit,
    onPinConfirmed: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    var step by remember { mutableStateOf(1) } // 1: Enter, 2: Confirm
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun triggerShake(msg: String) {
        errorMessage = msg
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 450
                    -30f at 40 using FastOutSlowInEasing
                    30f at 90 using FastOutSlowInEasing
                    -24f at 140 using FastOutSlowInEasing
                    24f at 190 using FastOutSlowInEasing
                    -16f at 240 using FastOutSlowInEasing
                    16f at 290 using FastOutSlowInEasing
                    -8f at 340 using FastOutSlowInEasing
                    8f at 390 using FastOutSlowInEasing
                    0f at 450
                }
            )
        }
    }

    fun handleDigit(digit: String) {
        errorMessage = null
        if (step == 1) {
            if (firstPin.length < 6) {
                val next = firstPin + digit
                firstPin = next
                if (next.length == 6) {
                    step = 2
                }
            }
        } else {
            if (confirmPin.length < 6) {
                val next = confirmPin + digit
                confirmPin = next
                if (next.length == firstPin.length) {
                    if (next == firstPin) {
                        isSuccess = true
                        scope.launch {
                            delay(160)
                            onPinConfirmed(firstPin)
                        }
                    } else {
                        triggerShake(context.getString(R.string.pin_setup_mismatch))
                        confirmPin = ""
                        firstPin = ""
                        step = 1
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        errorMessage = null
        if (step == 1) {
            if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
        } else {
            if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
            else {
                step = 1
                firstPin = firstPin.dropLast(1)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = if (step == 1) androidx.compose.ui.res.stringResource(R.string.pin_setup_create_title)
                else androidx.compose.ui.res.stringResource(R.string.pin_setup_confirm_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (step == 1) androidx.compose.ui.res.stringResource(R.string.pin_setup_create_subtitle)
                else androidx.compose.ui.res.stringResource(R.string.pin_setup_confirm_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(20.dp))

            val currentPin = if (step == 1) firstPin else confirmPin
            PinDotsIndicator(
                count = maxOf(4, if (step == 2) firstPin.length else currentPin.length),
                filledCount = currentPin.length,
                isError = errorMessage != null,
                isSuccess = isSuccess,
                shakeOffset = shakeOffset.value
            )

            Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.Center) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (step == 1 && firstPin.length in 4..5) {
                FilledTonalButton(
                    onClick = { step = 2 },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(androidx.compose.ui.res.stringResource(R.string.pin_continue_with_digits, firstPin.length))
                }
            }

            PinKeypad(
                hasBiometrics = false,
                onDigit = ::handleDigit,
                onBackspace = ::handleBackspace
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinChangeBottomSheet(
    onDismiss: () -> Unit,
    onVerifyOldPin: (String) -> Boolean,
    onNewPinConfirmed: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    var step by remember { mutableIntStateOf(1) } // 1: Old PIN, 2: New PIN, 3: Confirm New PIN
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun triggerShake(msg: String) {
        errorMessage = msg
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 450
                    -30f at 40 using FastOutSlowInEasing
                    30f at 90 using FastOutSlowInEasing
                    -24f at 140 using FastOutSlowInEasing
                    24f at 190 using FastOutSlowInEasing
                    -16f at 240 using FastOutSlowInEasing
                    16f at 290 using FastOutSlowInEasing
                    -8f at 340 using FastOutSlowInEasing
                    8f at 390 using FastOutSlowInEasing
                    0f at 450
                }
            )
        }
    }

    fun handleDigit(digit: String) {
        errorMessage = null
        when (step) {
            1 -> {
                if (oldPin.length < 6) {
                    val next = oldPin + digit
                    oldPin = next
                    if (next.length >= 4 && onVerifyOldPin(next)) {
                        step = 2
                    } else if (next.length == 6) {
                        triggerShake(context.getString(R.string.pin_incorrect_current))
                        oldPin = ""
                    }
                }
            }
            2 -> {
                if (newPin.length < 6) {
                    val next = newPin + digit
                    newPin = next
                    if (next.length == 6) step = 3
                }
            }
            3 -> {
                if (confirmNewPin.length < 6) {
                    val next = confirmNewPin + digit
                    confirmNewPin = next
                    if (next.length == newPin.length) {
                        if (next == newPin) {
                            isSuccess = true
                            scope.launch {
                                delay(160)
                                onNewPinConfirmed(newPin)
                            }
                        } else {
                            triggerShake(context.getString(R.string.pin_mismatch_new))
                            confirmNewPin = ""
                            newPin = ""
                            step = 2
                        }
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        errorMessage = null
        when (step) {
            1 -> if (oldPin.isNotEmpty()) oldPin = oldPin.dropLast(1)
            2 -> if (newPin.isNotEmpty()) newPin = newPin.dropLast(1)
            3 -> if (confirmNewPin.isNotEmpty()) confirmNewPin = confirmNewPin.dropLast(1) else step = 2
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.LockReset,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = when (step) {
                    1 -> androidx.compose.ui.res.stringResource(R.string.pin_change_verify_title)
                    2 -> androidx.compose.ui.res.stringResource(R.string.pin_change_new_title)
                    else -> androidx.compose.ui.res.stringResource(R.string.pin_change_confirm_title)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = when (step) {
                    1 -> androidx.compose.ui.res.stringResource(R.string.pin_change_verify_subtitle)
                    2 -> androidx.compose.ui.res.stringResource(R.string.pin_change_new_subtitle)
                    else -> androidx.compose.ui.res.stringResource(R.string.pin_change_confirm_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(20.dp))

            val currentPin = when (step) {
                1 -> oldPin
                2 -> newPin
                else -> confirmNewPin
            }
            val dotCount = when (step) {
                1 -> maxOf(4, oldPin.length)
                2 -> maxOf(4, newPin.length)
                else -> maxOf(4, newPin.length)
            }

            PinDotsIndicator(
                count = dotCount,
                filledCount = currentPin.length,
                isError = errorMessage != null,
                isSuccess = isSuccess,
                shakeOffset = shakeOffset.value
            )

            Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.Center) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (step == 2 && newPin.length in 4..5) {
                FilledTonalButton(
                    onClick = { step = 3 },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(androidx.compose.ui.res.stringResource(R.string.pin_continue_with_digits, newPin.length))
                }
            }

            PinKeypad(
                hasBiometrics = false,
                onDigit = ::handleDigit,
                onBackspace = ::handleBackspace
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinDisableBottomSheet(
    onDismiss: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun triggerShake(msg: String) {
        errorMessage = msg
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 450
                    -30f at 40 using FastOutSlowInEasing
                    30f at 90 using FastOutSlowInEasing
                    -24f at 140 using FastOutSlowInEasing
                    24f at 190 using FastOutSlowInEasing
                    -16f at 240 using FastOutSlowInEasing
                    16f at 290 using FastOutSlowInEasing
                    -8f at 340 using FastOutSlowInEasing
                    8f at 390 using FastOutSlowInEasing
                    0f at 450
                }
            )
        }
    }

    fun handleDigit(digit: String) {
        errorMessage = null
        if (pin.length < 6) {
            val next = pin + digit
            pin = next
            if (next.length >= 4 && onVerifyPin(next)) {
                isSuccess = true
                scope.launch {
                    delay(160)
                    onSuccess()
                }
            } else if (next.length == 6) {
                triggerShake(context.getString(R.string.pin_incorrect))
                pin = ""
            }
        }
    }

    fun handleBackspace() {
        errorMessage = null
        if (pin.isNotEmpty()) pin = pin.dropLast(1)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = androidx.compose.ui.res.stringResource(R.string.pin_disable_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = androidx.compose.ui.res.stringResource(R.string.pin_disable_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(20.dp))

            PinDotsIndicator(
                count = maxOf(4, pin.length),
                filledCount = pin.length,
                isError = errorMessage != null,
                isSuccess = isSuccess,
                shakeOffset = shakeOffset.value
            )

            Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.Center) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            PinKeypad(
                hasBiometrics = false,
                onDigit = ::handleDigit,
                onBackspace = ::handleBackspace
            )
        }
    }
}
