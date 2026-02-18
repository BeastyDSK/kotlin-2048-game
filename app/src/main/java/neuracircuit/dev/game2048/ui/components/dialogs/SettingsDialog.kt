package neuracircuit.dev.game2048.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import neuracircuit.dev.game2048.ui.theme.GameColors
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import neuracircuit.dev.game2048.R
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.widget.Toast
import neuracircuit.dev.game2048.data.ConsentManager

@Composable
fun SettingsDialog(
    volume: Float,
    isHapticsEnabled: Boolean,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: () -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val activity = context as Activity
    val consentManager = remember { ConsentManager(activity) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF8EF)), // Using Game Background Color
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.title_settings), 
                    fontSize = 28.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = GameColors.TextDark
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // --- VOLUME CONTROL ---
                Text(
                    text = stringResource(R.string.label_volume), 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = GameColors.TextDark, 
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        onValueChangeFinished = onVolumeChangeFinished,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = GameColors.TextDark,
                            activeTrackColor = GameColors.TextDark,
                            inactiveTrackColor = GameColors.GridBackground
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${(volume * 100).roundToInt()}%",
                        color = GameColors.TextDark,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- HAPTICS CONTROL ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_vibration), 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = GameColors.TextDark
                    )
                    Switch(
                        checked = isHapticsEnabled,
                        onCheckedChange = onHapticsChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GameColors.TextDark,
                            uncheckedThumbColor = GameColors.TextDark,
                            uncheckedTrackColor = GameColors.GridBackground
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- LEGAL SECTION ---
                HorizontalDivider(color = GameColors.GridBackground, thickness = 2.dp)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Privacy Policy",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GameColors.TextDark,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://beastydsk.github.io/2048/privacy-policy.html")
                        }
                    )
                    
                    Text(
                        text = "Terms of Service",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GameColors.TextDark,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://beastydsk.github.io/2048/tos.html")
                        }
                    )
                }

                if (consentManager.isPrivacyOptionsRequired) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Ad and data preferences",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GameColors.TextDark,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            activity?.let { act ->
                                consentManager.showPrivacyOptionsForm(act) {
                                    // Form dismissed
                                    Toast.makeText(context, "Preferences updated.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GameColors.TextDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.btn_close), color = Color.White)
                }
            }
        }
    }
}
