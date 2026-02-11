package neuracircuit.dev.game2048.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

object GameColors {
    val Background = Color(0xFFFAF8EF)
    val GridBackground = Color(0xFFBBADA0)
    val EmptySlot = Color(0xFFCDC1B4)
    val TextDark = Color(0xFF776E65)
    val TextLight = Color(0xFFF9F6F2)

    fun tileColor(value: Int): Color = when (value) {
        2 -> Color(0xFFEEE4DA)
        4 -> Color(0xFFEDE0C8)
        8 -> Color(0xFFF2B179)
        16 -> Color(0xFFF59563)
        32 -> Color(0xFFF67C5F)
        64 -> Color(0xFFF65E3B)
        128 -> Color(0xFFEDCF72)
        256 -> Color(0xFFEDCC61)
        512 -> Color(0xFFEDC850)
        1024 -> Color(0xFFEDC53F)
        2048 -> Color(0xFFEDC22E)
        4096 -> Color(0xFF3E3933) // Darker Red/Blackish (Official 4096 style)
        8192 -> Color(0xFF2C2A26) // Even darker, nearly black
        else -> Color.Black       // Fallback for > 8192 (Super Dark)
    }

    fun textColor(value: Int): Color = 
        if (value <= 4) TextDark else TextLight
}

fun getFontSize(value: Int): TextUnit = when {
    value < 100 -> 32.sp
    value < 1000 -> 26.sp
    value < 10000 -> 20.sp
    value < 100000 -> 16.sp
    value < 1000000 -> 14.sp
    else -> 24.sp
}
