package org.openfreebuds.se.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openfreebuds.se.model.TapAction
import org.openfreebuds.se.ui.BudIcons

private val TapAction.label: String
    get() = when (this) {
        TapAction.OFF -> "Off"
        TapAction.VOICE_ASSISTANT -> "Voice"
        TapAction.PLAY_PAUSE -> "Play"
        TapAction.NEXT -> "Next"
        TapAction.PREVIOUS -> "Prev"
    }

/**
 * Double tap gesture configuration for the left and right bud.
 */
@Composable
fun DoubleTapPanel(
    left: TapAction,
    right: TapAction,
    enabled: Boolean,
    onLeft: (TapAction) -> Unit,
    onRight: (TapAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isSystemInDarkTheme()) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Double tap",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.width(0.dp).height(4.dp))
            Text(
                text = "Choose what happens when you double tap each bud.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(0.dp).height(16.dp))

            TapActionSelector(
                label = "Left bud",
                icon = BudIcons.LeftEarbud,
                selected = left,
                enabled = enabled,
                onSelect = onLeft,
            )
            Spacer(Modifier.width(0.dp).height(16.dp))
            TapActionSelector(
                label = "Right bud",
                icon = BudIcons.RightEarbud,
                selected = right,
                enabled = enabled,
                onSelect = onRight,
            )
        }
    }
}

@Composable
private fun TapActionSelector(
    label: String,
    icon: ImageVector,
    selected: TapAction,
    enabled: Boolean,
    onSelect: (TapAction) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.width(0.dp).height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val ordered = listOf(
                TapAction.OFF,
                TapAction.PREVIOUS,
                TapAction.PLAY_PAUSE,
                TapAction.NEXT,
                TapAction.VOICE_ASSISTANT,
            )
            ordered.forEachIndexed { index, action ->
                val tapColors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    activeBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
                )
                SegmentedButton(
                    selected = selected == action,
                    onClick = { onSelect(action) },
                    enabled = enabled,
                    icon = {},
                    colors = tapColors,
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ordered.size,
                    ),
                    label = { Text(action.label, fontSize = MaterialTheme.typography.labelLarge.fontSize) },
                )
            }
        }
    }
}
