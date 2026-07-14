package com.papra.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// Papra's palette is grounded in its actual subject: paper, ink, and the
// stamped seals found on real archived documents (see the legal paperwork
// scans this app was built to manage) -- not a generic Material blue.

// Ink: deep desaturated navy, the primary brand color.
val Ink = Color(0xFF2E3A55)
val InkLight = Color(0xFF4A5A7D)
val InkContainer = Color(0xFFDCE3F0)
val OnInkContainer = Color(0xFF17233D)

// Seal: brick-red accent, used sparingly for one signature touch (file-type
// badges, selected states) -- deliberately not the AI-cliché warm terracotta.
val Seal = Color(0xFFA13E2B)
val SealLight = Color(0xFFD68F7D)
val SealContainer = Color(0xFFF3DCD5)
val OnSealContainer = Color(0xFF4A150A)

// Paper: warm off-white / near-black surfaces instead of cold Material grey.
val PaperLight = Color(0xFFFAF7F1)
val PaperDark = Color(0xFF1B1A17)
val PaperVariantLight = Color(0xFFF0EAE0)
val PaperVariantDark = Color(0xFF2A2823)

val OnPaperLight = Color(0xFF1F1C16)
val OnPaperDark = Color(0xFFEAE5DA)

// Parchment: warm border/divider tone.
val ParchmentLight = Color(0xFFDDD4C0)
val ParchmentDark = Color(0xFF48443A)

// File-type accent colors -- kept distinct from the brand palette since
// they carry semantic meaning (file type), not brand identity.
val FileColorPdf = Color(0xFFB23A2F)
val FileColorImage = Color(0xFF3E7D5C)
val FileColorSpreadsheet = Color(0xFF2E7D57)
val FileColorDoc = Color(0xFF3958A5)
val FileColorArchive = Color(0xFF8A8578)
val FileColorGeneric = Color(0xFF6B6656)
