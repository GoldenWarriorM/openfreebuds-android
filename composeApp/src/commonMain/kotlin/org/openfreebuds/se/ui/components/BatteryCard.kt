package org.openfreebuds.se.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openfreebuds.se.model.BatteryLevels
import org.openfreebuds.se.ui.BudIcons
import org.openfreebuds.se.ui.platformAccentTone

/**
 * Battery card styled after the home-screen widget: one rounded "pill" cell per
 * earbud and the case, each filled with a progress bar that spans the whole cell
 * while showing the icon, label and percentage on top.
 */
@Composable
fun BatteryCard(
    battery: BatteryLevels,
    modifier: Modifier = Modifier,
    connected: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BatteryPill(
            icon = BudIcons.LeftEarbud,
            label = "Left bud",
            level = battery.left,
            charging = battery.chargingLeft,
            connected = connected,
        )
        BatteryPill(
            icon = BudIcons.RightEarbud,
            label = "Right bud",
            level = battery.right,
            charging = battery.chargingRight,
            connected = connected,
        )
        BatteryPill(
            icon = BudIcons.Case,
            label = "Case",
            level = battery.case,
            charging = battery.chargingCase,
            connected = connected,
        )
    }
}

@Composable
private fun BatteryPill(
    icon: ImageVector,
    label: String,
    level: Int?,
    charging: Boolean,
    connected: Boolean,
) {
    val available = connected && level != null
    val shape = RoundedCornerShape(18.dp)

    val dark = isSystemInDarkTheme()
    val fillColor = when {
        level == null -> MaterialTheme.colorScheme.outline
        !available -> MaterialTheme.colorScheme.onSurfaceVariant
        level <= 20 -> MaterialTheme.colorScheme.errorContainer
        else -> platformAccentTone(if (dark) 700 else 100)
    }
    val trackColor = if (available) {
        platformAccentTone(if (dark) 800 else 200)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (available) {
        platformAccentTone(if (dark) 100 else 800)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val fraction = animateFloatAsState(
        targetValue = if (level != null) (level / 100f).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "pillFill",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(color = trackColor, shape = shape),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.value)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(fillColor, fillColor.copy(alpha = 0.85f)),
                    ),
                    shape = shape,
                ),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = if (available) contentColor
                else MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                modifier = Modifier.weight(1f),
            )
            if (charging && available) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = "Charging",
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = if (level != null) "$level%" else "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = contentColor,
            )
        }
    }
}