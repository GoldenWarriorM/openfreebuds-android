package org.openfreebuds.se.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openfreebuds.se.model.BatteryLevels
import org.openfreebuds.se.ui.BudIcons

/**
 * Material 3 expressive battery card: one row per earbud and the case,
 * each showing a progress bar, percentage and charging state.
 */
@Composable
fun BatteryCard(
    battery: BatteryLevels,
    modifier: Modifier = Modifier,
    connected: Boolean = true,
) {
    var lastLeft by remember { mutableStateOf<Int?>(null) }
    var lastRight by remember { mutableStateOf<Int?>(null) }
    var lastCase by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(battery.left) { battery.left?.let { lastLeft = it } }
    LaunchedEffect(battery.right) { battery.right?.let { lastRight = it } }
    LaunchedEffect(battery.case) { battery.case?.let { lastCase = it } }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            BatteryRow(
                icon = BudIcons.LeftEarbud,
                label = "Left bud",
                level = battery.left,
                displayLevel = battery.left ?: lastLeft,
                charging = battery.chargingLeft,
                connected = connected,
            )
            BatteryRow(
                icon = BudIcons.RightEarbud,
                label = "Right bud",
                level = battery.right,
                displayLevel = battery.right ?: lastRight,
                charging = battery.chargingRight,
                connected = connected,
            )
            BatteryRow(
                icon = BudIcons.Case,
                label = "Case",
                level = battery.case,
                displayLevel = battery.case ?: lastCase,
                charging = battery.chargingCase,
                connected = connected,
            )
        }
    }
}

@Composable
private fun BatteryRow(
    icon: ImageVector,
    label: String,
    level: Int?,
    displayLevel: Int?,
    charging: Boolean,
    connected: Boolean,
) {
    val available = connected && level != null
    val levelColor = if (available) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outline
    }
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp),
                    tint = if (available) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (available) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.weight(1f),
            )
            if (charging && available) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = "Charging",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = if (displayLevel != null) "$displayLevel%" else "—",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = levelColor,
            )
        }
        Spacer(Modifier.height(10.dp))
        BatteryProgressBar(level = displayLevel, available = available)
    }
}

@Composable
private fun BatteryProgressBar(level: Int?, available: Boolean) {
    LinearProgressIndicator(
        progress = { if (level != null) level / 100f else 0f },
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp),
        color = when {
            level == null -> MaterialTheme.colorScheme.outline
            !available -> MaterialTheme.colorScheme.onSurfaceVariant
            level <= 20 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        },
        trackColor = if (available) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
    )
}
