package com.pgedeon.portalcounters.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pgedeon.portalcounters.R
import com.pgedeon.portalcounters.audio.SoundManager
import com.pgedeon.portalcounters.ui.theme.*
import kotlin.random.Random

@Composable
fun ControlBar(
    elapsedSeconds: Int,
    onUndo: () -> Unit,
    canUndo: Boolean,
    onNewGame: () -> Unit,
    onMenu: () -> Unit,
    soundManager: SoundManager,
    modifier: Modifier = Modifier,
) {
    var showDiceDialog by remember { mutableStateOf<DiceType?>(null) }
    var isMuted by remember { mutableStateOf(soundManager.isMuted) }

    Surface(
        color = ControlBarBg,
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        ) {
            // Timer
            val minutes = elapsedSeconds / 60
            val seconds = elapsedSeconds % 60
            Text(
                text = String.format(java.util.Locale.ROOT, "⏱ %d:%02d", minutes, seconds),
                fontSize = 16.sp,
                color = ContentOnDark,
            )

            // Dice buttons
            Button(
                onClick = { showDiceDialog = DiceType.D6 },
                modifier = Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ControlBarButton,
                    contentColor = ContentOnDark,
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(stringResource(R.string.btn_dice_d6), fontSize = 14.sp)
            }

            Button(
                onClick = { showDiceDialog = DiceType.D20 },
                modifier = Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ControlBarButton,
                    contentColor = ContentOnDark,
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(stringResource(R.string.btn_dice_d20), fontSize = 14.sp)
            }

            // Undo
            Button(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ControlBarButton,
                    contentColor = ContentOnDark,
                    disabledContainerColor = ControlBarBg,
                    disabledContentColor = ContentOnDark.copy(alpha = 0.3f),
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(stringResource(R.string.btn_undo), fontSize = 14.sp)
            }

            // Sound toggle
            Button(
                onClick = {
                    isMuted = !isMuted
                    soundManager.isMuted = isMuted
                },
                modifier = Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMuted) MtgRed.copy(alpha = 0.3f) else ControlBarButton,
                    contentColor = ContentOnDark,
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(if (isMuted) "🔇" else "🔊", fontSize = 14.sp)
            }

            // New Game
            Button(
                onClick = onNewGame,
                modifier = Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ControlBarButton,
                    contentColor = ContentOnDark,
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(stringResource(R.string.btn_new_game), fontSize = 14.sp)
            }

            // Menu
            Button(
                onClick = onMenu,
                modifier = Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ControlBarButton,
                    contentColor = ContentOnDark,
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(stringResource(R.string.btn_menu), fontSize = 14.sp)
            }
        }
    }

    showDiceDialog?.let { diceType ->
        DiceRollerDialog(
            diceType = diceType,
            onDismiss = { showDiceDialog = null },
        )
    }
}

private enum class DiceType { D6, D20 }

@Composable
private fun DiceRollerDialog(
    diceType: DiceType,
    onDismiss: () -> Unit,
) {
    val sides = if (diceType == DiceType.D6) 6 else 20
    val result = remember { Random.nextInt(1, sides + 1) }
    val label = if (diceType == DiceType.D6) "D6" else "D20"

    AlertDialog(
        containerColor = SurfaceDark,
        onDismissRequest = onDismiss,
        title = { Text("🎲 $label", color = ContentOnDark) },
        text = {
            Text(
                text = "$result",
                fontSize = 64.sp,
                color = MetaBlue,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_ok), color = MetaBlue)
            }
        },
    )
}
