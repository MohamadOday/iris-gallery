@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.iris.gallery

import android.app.Activity
import android.content.Intent
import android.content.ClipData
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iris.gallery.data.SettingsPreferences
import com.iris.gallery.data.ThemeMode
import com.iris.gallery.ui.theme.IrisTheme
import com.iris.gallery.ui.GalleryApp

class MainActivity : ComponentActivity() {
    private val currentIntentState = mutableStateOf<Intent?>(null)
    private val intentGenerationState = mutableStateOf(0L)

    override fun attachBaseContext(newBase: android.content.Context) {
        val savedLanguage = newBase.getSharedPreferences("iris_gallery_settings", android.content.Context.MODE_PRIVATE)
            .getString("app_language", "")
            .orEmpty()
        super.attachBaseContext(com.iris.gallery.ui.wrapContextWithLanguage(newBase, savedLanguage))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentIntentState.value = intent
        IrisPhotoWidget.refreshAll(this)
        enableEdgeToEdge()
        val settingsPreferences = SettingsPreferences(this)
        val initialSettings = settingsPreferences.state.value
        val isDark = when (initialSettings.themeMode) {
            com.iris.gallery.data.ThemeMode.LIGHT -> false
            com.iris.gallery.data.ThemeMode.DARK -> true
            com.iris.gallery.data.ThemeMode.SYSTEM -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        val bgColor = when {
            isDark && initialSettings.amoledBlack -> android.graphics.Color.BLACK
            isDark -> 0xFF141218.toInt()
            else -> 0xFFFFF8FF.toInt()
        }
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bgColor))
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getSystemService(android.app.LocaleManager::class.java)
            val systemAppLocales = localeManager?.applicationLocales
            if (systemAppLocales != null && !systemAppLocales.isEmpty) {
                val activeTag = systemAppLocales[0]?.toLanguageTag().orEmpty()
                if (activeTag.isNotEmpty() && activeTag != initialSettings.language) {
                    val normalizedTag = if (activeTag.startsWith("zh", ignoreCase = true)) "zh" else activeTag.substringBefore("-")
                    val matched = com.iris.gallery.data.SUPPORTED_LANGUAGES.firstOrNull {
                        it.code.equals(activeTag, ignoreCase = true) || it.code.equals(normalizedTag, ignoreCase = true)
                    }
                    if (matched != null) {
                        settingsPreferences.setLanguage(matched.code)
                    }
                }
            } else if (initialSettings.language.isNotEmpty()) {
                com.iris.gallery.ui.setAppLanguage(this, initialSettings.language, recreateIfNeeded = false)
            }
        }
        setContent {
            val activeIntent = currentIntentState.value ?: intent
            val pickerMode = activeIntent.action == Intent.ACTION_PICK || activeIntent.action == Intent.ACTION_GET_CONTENT
            val isCameraReviewAction = activeIntent.action == "com.android.camera.action.REVIEW" ||
                activeIntent.action == "android.provider.MediaStore.ACTION_REVIEW"
            val isViewAction = activeIntent.action == Intent.ACTION_VIEW ||
                activeIntent.action == Intent.ACTION_EDIT ||
                isCameraReviewAction
            val requestedType = activeIntent.type
            val rawViewUri = activeIntent.data ?: activeIntent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            val viewUri = rawViewUri.takeIf { isViewAction }
            val isEditAction = activeIntent.action == Intent.ACTION_EDIT
            val shouldOpenLatest = isCameraReviewAction && rawViewUri == null
            val intentGeneration = intentGenerationState.value

            val settings by settingsPreferences.state.collectAsStateWithLifecycle()
            IrisTheme(
                themeMode = settings.themeMode,
                amoledBlack = settings.amoledBlack,
                accentColor = settings.accentColor,
            ) {
                GalleryApp(
                    settings = settings,
                    settingsPreferences = settingsPreferences,
                    requestedType = requestedType.takeIf { pickerMode },
                    initialViewUri = viewUri,
                    initialEditMode = isEditAction,
                    initialMemories = activeIntent.getBooleanExtra("open_memories", false),
                    initialOpenLatest = shouldOpenLatest,
                    initialIntentGeneration = intentGeneration,
                    onPick = if (pickerMode) {{ media ->
                        val result = Intent().apply {
                            data = media.uri
                            clipData = ClipData.newUri(contentResolver, media.name, media.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        setResult(Activity.RESULT_OK, result)
                        finish()
                    }} else null,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntentState.value = intent
        intentGenerationState.value += 1
    }
}
