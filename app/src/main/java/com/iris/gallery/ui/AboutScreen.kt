package com.iris.gallery.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iris.gallery.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    padding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var eggClicks by remember { mutableIntStateOf(0) }
    var activeToast by remember { mutableStateOf<Toast?>(null) }
    var showEasterEggSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // App Header & Interactive Easter Egg Trigger with actual App Logo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF9D34F5),
                                    Color(0xFF4F16D8)
                                )
                            )
                        )
                        .clickable {
                            eggClicks++
                            activeToast?.cancel()
                            if (eggClicks >= 5) {
                                eggClicks = 0
                                activeToast = Toast.makeText(context, context.getString(R.string.toast_optics_unlocked), Toast.LENGTH_SHORT).apply { show() }
                                showEasterEggSheet = true
                            } else {
                                val remaining = 5 - eggClicks
                                val message = if (remaining == 1) context.getString(R.string.toast_easter_egg_hint_one)
                                else context.getString(R.string.toast_easter_egg_hint, remaining)
                                activeToast = Toast.makeText(context, message, Toast.LENGTH_SHORT).apply { show() }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = "Iris Gallery Logo",
                        modifier = Modifier.size(76.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    androidx.compose.ui.res.stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    androidx.compose.ui.res.stringResource(R.string.version_format, "0.1.0", 1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    androidx.compose.ui.res.stringResource(R.string.app_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item {
            Text(
                androidx.compose.ui.res.stringResource(R.string.about_developer_project),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 8.dp)
            )
        }

        item {
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AboutLinkRow(
                        icon = Icons.Outlined.Psychology,
                        title = androidx.compose.ui.res.stringResource(R.string.about_developer_label),
                        subtitle = androidx.compose.ui.res.stringResource(R.string.about_developer_name)
                    ) { openUrl("https://bn3di.is-a.dev") }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    AboutLinkRow(
                        icon = Icons.Outlined.Language,
                        title = androidx.compose.ui.res.stringResource(R.string.about_website_label),
                        subtitle = "https://bn3di.is-a.dev"
                    ) { openUrl("https://bn3di.is-a.dev") }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    AboutLinkRow(
                        icon = Icons.Outlined.Code,
                        title = androidx.compose.ui.res.stringResource(R.string.about_source_label),
                        subtitle = androidx.compose.ui.res.stringResource(R.string.about_source_url)
                    ) { openUrl("https://github.com/MohamadOday/iris-gallery") }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    AboutLinkRow(
                        icon = Icons.Outlined.VerifiedUser,
                        title = androidx.compose.ui.res.stringResource(R.string.about_license_label),
                        subtitle = androidx.compose.ui.res.stringResource(R.string.about_license_name)
                    ) { openUrl("https://github.com/MohamadOday/iris-gallery/blob/main/LICENSE") }
                }
            }
        }

        item {
            Text(
                androidx.compose.ui.res.stringResource(R.string.about_general_info),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 8.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppInfoItem(
                        androidx.compose.ui.res.stringResource(R.string.about_arch_title),
                        androidx.compose.ui.res.stringResource(R.string.about_arch_desc)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    AppInfoItem(
                        androidx.compose.ui.res.stringResource(R.string.about_ui_title),
                        androidx.compose.ui.res.stringResource(R.string.about_ui_desc)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    AppInfoItem(
                        androidx.compose.ui.res.stringResource(R.string.about_img_engine_title),
                        androidx.compose.ui.res.stringResource(R.string.about_img_engine_desc)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    AppInfoItem(
                        androidx.compose.ui.res.stringResource(R.string.about_vid_engine_title),
                        androidx.compose.ui.res.stringResource(R.string.about_vid_engine_desc)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    AppInfoItem(
                        androidx.compose.ui.res.stringResource(R.string.about_features_title),
                        androidx.compose.ui.res.stringResource(R.string.about_features_desc)
                    )
                }
            }
        }
    }

    if (showEasterEggSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEasterEggSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MesopotamiaOpticsSheet(onClose = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { showEasterEggSheet = false }
            })
        }
    }
}

@Composable
private fun AppInfoItem(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AboutLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MesopotamiaOpticsSheet(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Ancient Cuneiform Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D2538),
                            Color(0xFF1E3A5F),
                            Color(0xFF2A1B0A)
                        )
                    )
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "𒀭 𒈹 𒌓 𒂗 𒆠",
                    fontSize = 24.sp,
                    color = Color(0xFFFFD700),
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "AL-HASAN IBN AL-HAYTHAM",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    "The Invention of the Camera & Optics",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFE082),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Feature: Al-Hasan Ibn al-Haytham (Alhazen)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Outlined.Camera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            "Al-Hasan Ibn al-Haytham (Alhazen)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Born in Basra, Iraq • c. 965 – 1040 CE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    "Long before the modern digital sensor and smartphone camera, the foundational principles of photography were invented by the Iraqi polymath, mathematician, and physicist Al-Hasan Ibn al-Haytham (الحسن ابن الهيثم), born in Basra during the Islamic Golden Age.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // The Invention of Camera Obscura
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "1. The Invention of the Camera Obscura",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Ibn al-Haytham was the first to systematically construct and explain the Camera Obscura (which he termed 'Al-Bayt Al-Muthlim' / البيت المظلم — The Darkened Room). He demonstrated that light passing through a tiny pinhole aperture travels in straight rays and projects an inverted, true-to-life image of the outside world onto a flat surface inside.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "The word 'Camera' itself originates directly from this concept (from the Arabic 'Qamara' / قمرة and Latin 'camera' meaning a dark chamber).",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Debunking Ancient Greek Emission Theory
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "2. Revolutionizing the Science of Vision",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "For over a thousand years, ancient Greek philosophers (including Euclid and Ptolemy) believed vision occurred because our eyes emitted invisible rays that touched objects. Ibn al-Haytham refuted this mathematically and experimentally, proving that light reflects from objects and enters the pupil of the eye, correctly identifying the lens and retina's image-forming role.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Father of Modern Scientific Method
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "3. Kitāb al-Manāẓir & The Scientific Method",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "His landmark 7-volume treatise 'Kitāb al-Manāẓir' (Book of Optics) established modern empirical science. He insisted that scientific truth cannot be accepted by authority or intuition alone, but must be proven through reproducible, controlled experiments and mathematical proofs — establishing the scientific method 600 years before European scientists.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Mesopotamian Legacy
        OutlinedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Mesopotamian Heritage & Optics Legacy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "From the earliest mathematics, astronomy, and cuneiform writing in ancient Sumer, Babylon, and Mesopotamia, to the groundbreaking optics of Basra that created the Camera: Iris Gallery honors this timeless heritage of preserving visual memories.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        FilledTonalButton(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            Text(androidx.compose.ui.res.stringResource(R.string.action_close))
        }
    }
}
