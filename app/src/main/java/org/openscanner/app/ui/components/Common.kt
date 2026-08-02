package org.openscanner.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PortableWifiOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Wifi1Bar
import androidx.compose.material.icons.rounded.Wifi2Bar
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openscanner.app.ChannelGroupUiModel
import org.openscanner.app.R
import org.openscanner.app.ui.displayLabel
import org.openscanner.app.ui.displaySelectorLabel
import org.openscanner.app.ui.theme.ScannerAmber
import org.openscanner.app.ui.theme.ScannerBorder
import org.openscanner.app.ui.theme.ScannerCyan
import org.openscanner.app.ui.theme.ScannerGreen
import org.openscanner.app.ui.theme.ScannerIconWell
import org.openscanner.app.ui.theme.ScannerMuted
import org.openscanner.app.ui.theme.ScannerOnAmber
import org.openscanner.app.ui.theme.ScannerOnCyan
import org.openscanner.app.ui.theme.ScannerOrange
import org.openscanner.app.ui.theme.ScannerPositiveBorder
import org.openscanner.app.ui.theme.ScannerPositiveSurface
import org.openscanner.app.ui.theme.ScannerSpacing
import org.openscanner.app.ui.theme.ScannerSurface
import org.openscanner.app.ui.theme.ScannerText
import org.openscanner.core.domain.Freshness
import org.openscanner.core.model.ScannerPhase
import org.openscanner.core.model.WifiChannelGroup

@Composable
fun AppHeader(
    phase: ScannerPhase,
    freshness: Freshness?,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ScannerSpacing.MinTouchTarget + 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.common_app_name),
                color = ScannerText,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
            )
            if (eyebrow != null) {
                Text(
                    text = eyebrow,
                    color = ScannerMuted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        StatusBadge(phase = phase, freshness = freshness)
    }
}

@Composable
fun StatusBadge(
    phase: ScannerPhase,
    freshness: Freshness?,
    modifier: Modifier = Modifier,
) {
    val label = when (phase) {
        ScannerPhase.PAUSED -> stringResource(R.string.common_status_paused)
        ScannerPhase.LIVE -> when (freshness) {
            Freshness.AGING -> stringResource(R.string.common_status_aging)
            Freshness.STALE -> stringResource(R.string.common_status_stale)
            else -> stringResource(R.string.common_status_live)
        }
        ScannerPhase.CHECKING -> stringResource(R.string.common_status_check)
        else -> stringResource(R.string.common_status_offline)
    }
    val color = when {
        phase == ScannerPhase.LIVE && freshness != Freshness.AGING && freshness != Freshness.STALE -> ScannerCyan
        phase == ScannerPhase.PAUSED ||
            (phase == ScannerPhase.LIVE && freshness == Freshness.AGING) -> ScannerAmber
        phase == ScannerPhase.LIVE && freshness == Freshness.STALE -> ScannerOrange
        else -> ScannerMuted
    }
    val statusDescription = stringResource(R.string.common_status_description, label)
    Box(
        modifier = modifier
            .heightIn(min = 32.dp)
            .border(1.5.dp, color, MaterialTheme.shapes.small)
            .padding(horizontal = ScannerSpacing.Md, vertical = ScannerSpacing.Sm)
            .semantics { contentDescription = statusDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
fun ChannelGroupSelector(
    groups: List<ChannelGroupUiModel>,
    selectedGroup: WifiChannelGroup,
    onSelect: (WifiChannelGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val selectedIndex = groups.indexOfFirst { it.group == selectedGroup }
    val selectorShape = MaterialTheme.shapes.small
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.scrollToItem(selectedIndex)
    }
    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .height(ScannerSpacing.MinTouchTarget)
            .border(1.dp, ScannerBorder, selectorShape)
            .clip(selectorShape),
    ) {
        itemsIndexed(groups, key = { _, option -> option.group }) { index, option ->
            val selected = option.group == selectedGroup
            val interactionSource = remember { MutableInteractionSource() }
            val selectedSuffix = stringResource(R.string.common_cd_selected)
            val unavailableSuffix = stringResource(
                R.string.common_cd_unavailable,
                stringResource(R.string.common_channel_group_unavailable_reason),
            )
            val groupLabel = option.group.displayLabel()
            Box(
                modifier = Modifier
                    .widthIn(min = 88.dp)
                    .fillMaxHeight()
                    .background(if (selected) ScannerCyan else ScannerSurface)
                    .clickable(
                        enabled = option.enabled,
                        role = Role.RadioButton,
                        interactionSource = interactionSource,
                        indication = null,
                    ) { onSelect(option.group) }
                    .semantics {
                        this.selected = selected
                        role = Role.RadioButton
                        contentDescription = buildString {
                            append(groupLabel)
                            if (selected) append(selectedSuffix)
                            if (!option.enabled) append(unavailableSuffix)
                        }
                        if (!option.enabled) disabled()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.group.displaySelectorLabel(),
                    color = when {
                        selected -> ScannerOnCyan
                        option.enabled -> ScannerMuted
                        else -> ScannerMuted.copy(alpha = 0.45f)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
            if (index != groups.lastIndex) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(ScannerBorder))
            }
        }
    }
}

@Composable
fun PrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = ScannerSpacing.MinTouchTarget + 6.dp),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = ScannerAmber,
            contentColor = ScannerOnAmber,
            disabledContainerColor = ScannerBorder,
            disabledContentColor = ScannerMuted,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SignalGlyph(signalDbm: Int, modifier: Modifier = Modifier) {
    val icon = when {
        signalDbm >= -60 -> Icons.Rounded.Wifi
        signalDbm >= -72 -> Icons.Rounded.Wifi2Bar
        else -> Icons.Rounded.Wifi1Bar
    }
    Box(
        modifier = modifier
            .size(ScannerSpacing.MinTouchTarget)
            .background(ScannerIconWell, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = ScannerCyan, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun CurrentWifiBadge(modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.extraSmall
    Box(
        modifier = modifier
            .background(ScannerGreen, shape)
            .border(1.dp, ScannerGreen, shape)
            .padding(horizontal = ScannerSpacing.Sm, vertical = ScannerSpacing.Xs),
    ) {
        Text(
            text = stringResource(R.string.scan_badge_current_wifi),
            color = ScannerSurface,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
fun InformationBanner(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    positive: Boolean = false,
) {
    val border = if (positive) ScannerPositiveBorder else ScannerBorder
    val background = if (positive) ScannerPositiveSurface else ScannerSurface
    val foreground = if (positive) ScannerGreen else ScannerMuted
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, border, MaterialTheme.shapes.small)
            .background(background)
            .padding(ScannerSpacing.Md + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Md - 2.dp),
    ) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(21.dp))
        Text(text, color = foreground, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ScannerUnavailable(
    phase: ScannerPhase,
    safeErrorCode: String?,
    onRequestPermission: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = when (phase) {
        ScannerPhase.PERMISSION_REQUIRED -> Triple(
            Icons.Rounded.Lock,
            stringResource(R.string.common_unavailable_permission_title),
            stringResource(R.string.common_unavailable_permission_body),
        )
        ScannerPhase.WIFI_DISABLED -> Triple(
            Icons.Rounded.WifiOff,
            stringResource(R.string.common_unavailable_wifi_off_title),
            stringResource(R.string.common_unavailable_wifi_off_body),
        )
        ScannerPhase.LOCATION_DISABLED -> Triple(
            Icons.Rounded.LocationOff,
            stringResource(R.string.common_unavailable_location_off_title),
            stringResource(R.string.common_unavailable_location_off_body),
        )
        ScannerPhase.UNSUPPORTED -> Triple(
            Icons.Rounded.PortableWifiOff,
            stringResource(R.string.common_unavailable_unsupported_title),
            stringResource(R.string.common_unavailable_unsupported_body),
        )
        ScannerPhase.ERROR -> Triple(
            Icons.Rounded.PortableWifiOff,
            stringResource(R.string.common_unavailable_error_title),
            stringResource(R.string.common_unavailable_error_body, safeErrorCode ?: "UNKNOWN"),
        )
        else -> Triple(
            Icons.Rounded.Wifi,
            stringResource(R.string.common_unavailable_checking_title),
            stringResource(R.string.common_unavailable_checking_body),
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = ScannerSpacing.Xxl + 16.dp, start = ScannerSpacing.Md, end = ScannerSpacing.Md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Lg),
    ) {
        Box(
            modifier = Modifier.size(80.dp).background(ScannerIconWell, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(content.first, contentDescription = null, tint = ScannerCyan, modifier = Modifier.size(40.dp))
        }
        Text(content.second, color = ScannerText, style = MaterialTheme.typography.headlineSmall)
        Text(
            content.third,
            color = ScannerMuted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(ScannerSpacing.Xs))
        when (phase) {
            ScannerPhase.PERMISSION_REQUIRED -> PrimaryAction(stringResource(R.string.common_action_grant_permission), onRequestPermission)
            ScannerPhase.WIFI_DISABLED -> PrimaryAction(stringResource(R.string.common_action_open_wifi_settings), onOpenWifiSettings)
            ScannerPhase.LOCATION_DISABLED -> PrimaryAction(stringResource(R.string.common_action_open_location_settings), onOpenLocationSettings)
            ScannerPhase.ERROR -> PrimaryAction(stringResource(R.string.common_action_retry), onRetry)
            else -> Unit
        }
    }
}
