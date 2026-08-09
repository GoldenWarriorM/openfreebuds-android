package org.openfreebuds.se.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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

    private fun caseIcon(
        name: String,
        pathData: String,
        scaleX: Float,
        translationX: Float,
        translationY: Float,
    ): ImageVector {
        val nodes = PathParser().parsePathString(pathData).toNodes()
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addGroup(
                scaleX = scaleX,
                scaleY = scaleX,
                translationX = translationX,
                translationY = translationY,
            )
            addPath(
                pathData = nodes,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            clearGroup()
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

    val Case = caseIcon(
        "Case",
        "M5.05469 3.02539C5.34231 3.02539 5.53231 3.0868 5.67969 3.17773C5.8282 3.2694 5.93492 3.39135 6.05762 3.51758C6.17971 3.64317 6.31612 3.77055 6.52051 3.86621C6.7251 3.96187 6.99712 4.02539 7.38867 4.02539H13.6104C14.0017 4.02534 14.273 3.96184 14.4775 3.86621C14.682 3.77054 14.8183 3.64318 14.9404 3.51758C15.0632 3.39133 15.1698 3.26941 15.3184 3.17773C15.429 3.10948 15.5633 3.05768 15.7451 3.03613L15.9434 3.02539H20.9736C20.9736 4.51365 20.9721 5.62781 20.8096 6.46387C20.6465 7.30265 20.3218 7.85814 19.6738 8.22852C19.0231 8.6004 18.0435 8.78712 16.5674 8.88086C15.0919 8.97455 13.1241 8.97461 10.499 8.97461C7.87394 8.97461 5.9061 8.97455 4.43066 8.88086C2.95489 8.78714 1.97592 8.60024 1.3252 8.22852C0.677095 7.85814 0.351585 7.30275 0.188477 6.46387C0.0259462 5.62781 0.0254058 4.51363 0.0253906 3.02539H5.05469ZM15.5459 3.02539C15.4483 3.05295 15.3653 3.08956 15.292 3.13477C15.1368 3.23056 15.0246 3.35871 14.9043 3.48242C14.7834 3.60674 14.6524 3.72948 14.4561 3.82129C14.2597 3.91309 13.9963 3.97456 13.6104 3.97461H7.38867C7.00252 3.97461 6.73841 3.91313 6.54199 3.82129C6.34567 3.72948 6.21459 3.60673 6.09375 3.48242C5.97351 3.35872 5.86124 3.23055 5.70605 3.13477C5.63278 3.08957 5.54971 3.05295 5.45215 3.02539H15.5459ZM10.8877 0.0253906C13.5121 0.0253906 15.4556 0.0879901 16.8945 0.212891C18.3339 0.337844 19.2671 0.524929 19.8711 0.773438C20.4733 1.02132 20.7464 1.32895 20.8711 1.69531C20.9949 2.05904 20.9744 2.48275 20.9736 2.97461H0.0253906C0.0262534 2.48523 0.0531968 2.06183 0.24707 1.69922C0.443669 1.33167 0.814483 1.02215 1.51562 0.773438C2.21735 0.524595 3.24789 0.337819 4.75977 0.212891C6.27159 0.087979 8.2635 0.0253943 10.8877 0.0253906Z",
        scaleX = 0.9524f,
        translationX = 2f,
        translationY = 7.714f,
    )
}
