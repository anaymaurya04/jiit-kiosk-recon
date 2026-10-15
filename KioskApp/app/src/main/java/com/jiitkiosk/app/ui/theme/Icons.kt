package com.jiitkiosk.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The nav and chevron icons, built from the mockups' own path data. They are
 * stroked in black and tinted at the call site, which is what the design's
 * `stroke="currentColor"` amounts to.
 */
private fun strokeIcon(
    name: String,
    size: Dp,
    vararg paths: String,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = size,
    defaultHeight = size,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    paths.forEach { d ->
        addPath(
            pathData = addPathNodes(d),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()

/** <circle cx=12 cy=12 r=8/> + <path d="M12 8v4l3 2"/> */
val IconAttendance = strokeIcon(
    "Attendance", 24.dp,
    "M4 12 A8 8 0 1 1 20 12 A8 8 0 1 1 4 12",
    "M12 8v4l3 2",
)

/** <path d="M5 4h14v16H5zM9 9h6M9 13h6"/> */
val IconExams = strokeIcon(
    "Exams", 24.dp,
    "M5 4h14v16H5z",
    "M9 9h6",
    "M9 13h6",
)

/** <path d="M4 20V10M10 20V4M16 20v-8M22 20H2"/> */
val IconGrades = strokeIcon(
    "Grades", 24.dp,
    "M4 20V10",
    "M10 20V4",
    "M16 20v-8",
    "M22 20H2",
)

/** <circle cx=12 cy=8 r=4/> + <path d="M4 21c1-5 15-5 16 0"/> */
val IconMe = strokeIcon(
    "Me", 24.dp,
    "M8 8 A4 4 0 1 1 16 8 A4 4 0 1 1 8 8",
    "M4 21c1-5 15-5 16 0",
)

/** <path d="M15 5l-7 7 7 7"/> */
val IconBack = strokeIcon("Back", 28.dp, "M15 5l-7 7 7 7")

/** <path d="M9 5l7 7-7 7"/> */
val IconNext = strokeIcon("Next", 24.dp, "M9 5l7 7-7 7")
