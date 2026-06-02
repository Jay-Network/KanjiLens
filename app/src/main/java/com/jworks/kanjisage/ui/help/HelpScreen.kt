package com.jworks.kanjisage.ui.help

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.kanjisage.BuildConfig
import androidx.compose.ui.res.stringResource
import com.jworks.kanjisage.R
import com.jworks.kanjisage.ui.theme.GlassCard
import com.jworks.kanjisage.ui.theme.KanjiSageColors
import com.jworks.kanjisage.ui.theme.focusBorder
import com.jworks.kanjisage.ui.theme.KanjiSageTypography
import com.jworks.kanjisage.ui.theme.KanjiSageShapes

private val AccentBlue = KanjiSageColors.PrimaryAction
private val AccentTeal = KanjiSageColors.AccentTeal
private val AccentOrange = KanjiSageColors.AccentOrange
private val BadgeBg = KanjiSageColors.CardBg
private val RowLabelBg = KanjiSageColors.RowLabelBg

@Composable
fun HelpScreen(
    onBackClick: () -> Unit,
    onFeedbackClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KanjiSageColors.DarkBg)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.help_back),
                modifier = Modifier
                    .size(24.dp)
                    .focusBorder(CircleShape)
                    .clickable { onBackClick() },
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.help_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = KanjiSageTypography.TitleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // About Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = KanjiSageShapes.Card,
                contentPadding = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "\u6F22\u5B57",  // 漢字
                        fontSize = KanjiSageTypography.HeadlineLarge,
                        color = KanjiSageColors.PrimaryAction,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.help_app_name),
                        fontSize = KanjiSageTypography.TitleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.help_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.help_by_jworks),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // User Guide header
            Text(
                text = stringResource(R.string.help_user_guide),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Button Grid Guide
            GuideSection(stringResource(R.string.help_camera_buttons), stringResource(R.string.help_icon_keyboard)) {
                RowLabel(stringResource(R.string.help_top_row))
                ButtonTip(stringResource(R.string.help_btn_full_focus), stringResource(R.string.help_btn_full_focus_desc))
                ButtonTip(stringResource(R.string.help_btn_tate_yoko), stringResource(R.string.help_btn_tate_yoko_desc))
                ButtonTip(stringResource(R.string.help_btn_play_pause), stringResource(R.string.help_btn_play_pause_desc))
                Spacer(modifier = Modifier.height(10.dp))
                RowLabel(stringResource(R.string.help_middle_row))
                ButtonTip(stringResource(R.string.help_btn_flash), stringResource(R.string.help_btn_flash_desc))
                ButtonTip(stringResource(R.string.help_btn_settings), stringResource(R.string.help_btn_settings_desc))
                ButtonTip(stringResource(R.string.help_btn_profile), stringResource(R.string.help_btn_profile_desc))
                Spacer(modifier = Modifier.height(10.dp))
                RowLabel(stringResource(R.string.help_bottom_row))
                ButtonTip(stringResource(R.string.help_btn_bookmarks), stringResource(R.string.help_btn_bookmarks_desc))
                ButtonTip(stringResource(R.string.help_btn_jcoin), stringResource(R.string.help_btn_jcoin_desc))
                ButtonTip(stringResource(R.string.help_btn_feedback), stringResource(R.string.help_btn_feedback_desc))
            }

            // Jukugo & Dictionary Guide
            GuideSection(stringResource(R.string.help_word_list_title), stringResource(R.string.help_icon_book)) {
                StepItem(1, stringResource(R.string.help_wl_step1_pre), stringResource(R.string.help_wl_step1_hl), stringResource(R.string.help_wl_step1_suf))
                StepItem(2, stringResource(R.string.help_wl_step2_pre), stringResource(R.string.help_wl_step2_hl), stringResource(R.string.help_wl_step2_suf))
                StepItem(3, stringResource(R.string.help_wl_step3_pre), stringResource(R.string.help_wl_step3_hl), stringResource(R.string.help_wl_step3_suf))
                StepItem(4, stringResource(R.string.help_wl_step4_pre), stringResource(R.string.help_wl_step4_hl), stringResource(R.string.help_wl_step4_suf))
                StepItem(5, stringResource(R.string.help_wl_step5_pre), stringResource(R.string.help_wl_step5_hl), stringResource(R.string.help_wl_step5_suf))
            }

            // Vertical Mode Guide
            GuideSection(stringResource(R.string.help_vertical_mode_title), stringResource(R.string.help_icon_vertical)) {
                StepItem(1, stringResource(R.string.help_vm_step1_pre), stringResource(R.string.help_vm_step1_hl), stringResource(R.string.help_vm_step1_suf))
                StepItem(2, stringResource(R.string.help_vm_step2_pre), stringResource(R.string.help_vm_step2_hl), stringResource(R.string.help_vm_step2_suf))
                StepItem(3, stringResource(R.string.help_vm_step3_pre), stringResource(R.string.help_vm_step3_hl), stringResource(R.string.help_vm_step3_suf))
            }

            // Scan Challenge Guide
            GuideSection(stringResource(R.string.help_scan_challenge_title), stringResource(R.string.help_icon_target)) {
                StepItem(1, stringResource(R.string.help_sc_step1_pre), stringResource(R.string.help_sc_step1_hl), stringResource(R.string.help_sc_step1_suf))
                StepItem(2, stringResource(R.string.help_sc_step2_pre), stringResource(R.string.help_sc_step2_hl), stringResource(R.string.help_sc_step2_suf))
                StepItem(3, stringResource(R.string.help_sc_step3_pre), stringResource(R.string.help_sc_step3_hl), stringResource(R.string.help_sc_step3_suf))
            }

            // Links Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = KanjiSageShapes.Card,
                contentPadding = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.help_links),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LinkRow(
                        label = stringResource(R.string.help_privacy_policy),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://jworks-ai.com/apps/kanjisage/privacy"))
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LinkRow(
                        label = stringResource(R.string.help_rate_play),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.jworks.kanjisage"))
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LinkRow(
                        label = stringResource(R.string.help_send_feedback),
                        onClick = onFeedbackClick
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LinkRow(
                        label = stringResource(R.string.help_jworks_ai),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://jworks-ai.com"))
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LinkRow(
                        label = stringResource(R.string.help_creator),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://jayismocking.com"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Credits Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = KanjiSageShapes.Card,
                contentPadding = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.help_credits),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    CreditItem(stringResource(R.string.help_credit_mlkit), stringResource(R.string.help_credit_mlkit_desc))
                    CreditItem(stringResource(R.string.help_credit_jmdict), stringResource(R.string.help_credit_jmdict_desc))
                    CreditItem(stringResource(R.string.help_credit_kuromoji), stringResource(R.string.help_credit_kuromoji_desc))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GuideSection(title: String, icon: String = "", content: @Composable () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = KanjiSageShapes.Card,
        contentPadding = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon.isNotEmpty()) {
                    Text(
                        text = icon,
                        fontSize = KanjiSageTypography.BodyLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun RowLabel(label: String) {
    Box(
        modifier = Modifier
            .clip(KanjiSageShapes.Small)
            .background(RowLabelBg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = KanjiSageTypography.LabelSmall,
            fontWeight = FontWeight.Bold,
            color = AccentBlue
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun ButtonTip(buttonName: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .clip(KanjiSageShapes.Small)
                .background(BadgeBg)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = buttonName,
                fontSize = KanjiSageTypography.LabelSmall,
                fontWeight = FontWeight.Bold,
                color = AccentBlue
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StepItem(number: Int, prefix: String, highlight: String, suffix: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(AccentTeal),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = KanjiSageTypography.LabelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = buildAnnotatedString {
                append(prefix)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AccentBlue)) {
                    append(highlight)
                }
                append(suffix)
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusBorder()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = ">",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CreditItem(name: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
