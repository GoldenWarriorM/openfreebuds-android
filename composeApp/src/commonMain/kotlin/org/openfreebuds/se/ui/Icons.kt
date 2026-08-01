package org.openfreebuds.se.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Custom earbud icons, ported from FreeBuddy's `FreebuddyIcons` font
 * (`lib/gen/freebuddy_icons.dart`).
 */
object BudIcons {

    private fun icon(name: String, pathData: String): ImageVector {
        val nodes = PathParser().parsePathString(pathData).toNodes()
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(nodes, fill = SolidColor(Color.Black))
        }.build()
    }

    val LeftEarbud = icon(
        "LeftEarbud",
        "m13,3c2,0 3,2 3,3v5c0,1 -1,3 -3,3 -0.61,0 -1.32,-0.28 -2,-0.73V20c0,0.55 -0.45,1 -1,1H9C8.45,21 8,20.55 8,20V8C8,6 11,3 13,3m-3,7.23 l2.09,1.37c0.51,0.33 0.83,0.4 0.91,0.4 0.7,0 1,-0.92 1,-1V6.03C14,5.92 13.7,5 13,5 12.1,5 10,7.1 10,8v2.23",
    )

    val RightEarbud = icon(
        "RightEarbud",
        "M11,3C9,3 8,5 8,6v5c0,1 1,3 3,3 0.61,0 1.32,-0.28 2,-0.73V20c0,0.55 0.45,1 1,1h1c0.55,0 1,-0.45 1,-1V8C16,6 13,3 11,3m3,7.23 l-2.09,1.37C11.4,11.93 11.08,12 11,12 10.3,12 10,11.08 10,11V6.03C10,5.92 10.3,5 11,5c0.9,0 3,2.1 3,3v2.23",
    )

    val Case = icon(
        "Case",
        "M12,20A8,8 0,0 1,4 12A8,8 0,0 1,12 4A8,8 0,0 1,20 12A8,8 0,0 1,12 20M12,2A10,10 0,0 0,2 12A10,10 0,0 0,12 22A10,10 0,0 0,22 12A10,10 0,0 0,12 2Z M20.038,8.135H4.199L3.518,10.228H21.166Z",
    )
}
