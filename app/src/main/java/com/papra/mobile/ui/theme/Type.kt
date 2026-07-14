package com.papra.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Deliberate pairing for a document-archive subject: a serif display face
// for titles (document names, headers) evokes typeset/printed paper, while
// a clean sans face keeps lists and metadata legible. Both are system fonts
// (no bundled font files needed) but the pairing itself is the choice.
private val DisplayFace = FontFamily.Serif
private val BodyFace = FontFamily.SansSerif

val PapraTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = DisplayFace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFace,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = DisplayFace,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFace,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFace,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.2.sp,
    ),
)
