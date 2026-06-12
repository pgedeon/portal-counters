package com.pgedeon.portalcounters.ui

import com.pgedeon.portalcounters.audio.SoundManager

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pgedeon.portalcounters.R
import com.pgedeon.portalcounters.model.GameAction
import com.pgedeon.portalcounters.model.GameMode
import com.pgedeon.portalcounters.model.GameState
import com.pgedeon.portalcounters.model.PlayerState
import com.pgedeon.portalcounters.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
private fun rememberPressCounter(): Pair<(String) -> Unit, State<Map<String, Pair<Int, Float>>>> {
    val scope = rememberCoroutineScope()
    val pressCounts = remember { mutableStateMapOf<String, Pair<Int, Long>>() }
    val visibilityMap = remember { derivedStateOf {
        val now = System.currentTimeMillis()
        pressCounts.mapValues { (_, pair) ->
            val elapsed = now - pair.second
            val fade = if (elapsed > 4000) ((5000f - elapsed) / 1000f).coerceIn(0f, 1f) else 1f
            pair.first to fade
        }.filterValues { it.first > 0 }
    }}
    val press: (String) -> Unit = { key ->
        scope.launch {
            val current = pressCounts[key]?.first ?: 0
            pressCounts[key] = (current + 1) to System.currentTimeMillis()
            delay(5000)
            val entry = pressCounts[key]
            if (entry != null && System.currentTimeMillis() - entry.second >= 4900) {
                pressCounts.remove(key)
            }
        }
    }
    return press to visibilityMap
}

private data class FloatingText(
    val id: Int, val text: String, val color: Color, val startTime: Long, val offsetX: Float,
)

@Composable
fun PlayerZone(
    playerIndex: Int,
    player: PlayerState,
    allPlayers: List<PlayerState>,
    gameMode: com.pgedeon.portalcounters.model.GameMode,
    isInverted: Boolean,
    onLifeChange: (Int, Int) -> Unit,
    soundManager: SoundManager,
    onCommanderDamageChange: (Int, Int, Int) -> Unit,
    onPoisonChange: (Int, Int) -> Unit,
    onEnergyChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDead = player.isDead
    val (press, pressVisibility) = rememberPressCounter()
    val scope = rememberCoroutineScope()

    val lifeNow = player.life
    var prevLife by remember { mutableIntStateOf(lifeNow) }
    val lastDeltaColor = remember { mutableStateOf(MtgRed) }

    val shakeX = remember { Animatable(0f) }
    val shakeY = remember { Animatable(0f) }
    val scalePulse = remember { Animatable(1f) }
    val flashAlpha = remember { Animatable(0f) }
    var flashColor by remember { mutableStateOf(Color.Transparent) }

    // Trigger animations on life change
    LaunchedEffect(lifeNow) {
        if (prevLife != lifeNow) {
            val delta = lifeNow - prevLife
            val isDamage = delta < 0

            if (isDamage) soundManager.playDamage() else soundManager.playHeal()

            lastDeltaColor.value = if (isDamage) MtgRed else MtgGreen
            flashColor = if (isDamage) MtgRed.copy(alpha = 0.5f) else MtgGreen.copy(alpha = 0.35f)
            prevLife = lifeNow

            scalePulse.snapTo(1.35f)
            launch {
                scalePulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
            }

            if (isDamage) {
                launch {
                    val a = 14f
                    shakeX.snapTo(a)
                    shakeX.animateTo(0f, keyframes { durationMillis = 350; a at 0; -a*0.7f at 40; a*0.5f at 80; -a*0.3f at 130; a*0.15f at 190; 0f at 350 })
                }
                launch {
                    val a = 10f
                    shakeY.snapTo(-a)
                    shakeY.animateTo(0f, keyframes { durationMillis = 350; -a at 0; a*0.6f at 50; -a*0.4f at 100; a*0.2f at 160; 0f at 350 })
                }
            }

            flashAlpha.snapTo(1f)
            launch {
                flashAlpha.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
            }
        }
    }

    val lifeColor by animateColorAsState(
        targetValue = when {
            player.life <= 0 -> MtgRed
            player.life <= 10 -> Color(0xFFFFA726)
            else -> ContentOnDark
        },
        animationSpec = tween(400), label = "lifeColor",
    )

    val floatingTexts = remember { mutableStateListOf<FloatingText>() }
    var floatId by remember { mutableIntStateOf(0) }

    fun spawnFloatingText(text: String, color: Color) {
        val id = floatId++
        val x = Random.nextFloat() * 160f - 80f
        floatingTexts.add(FloatingText(id, text, color, System.currentTimeMillis(), x))
        scope.launch { delay(2500); floatingTexts.removeAll { it.id == id } }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .graphicsLayer { translationX = shakeX.value; translationY = shakeY.value }
    ) {
        // Flash overlay
        if (flashAlpha.value > 0.01f) {
            Box(modifier = Modifier.fillMaxSize().background(flashColor.copy(alpha = flashAlpha.value * 0.3f)))
        }

        // Player name header
        Box(modifier = Modifier
            .align(if (isInverted) Alignment.BottomCenter else Alignment.TopCenter)
            .padding(top = if (isInverted) 0.dp else 8.dp, bottom = if (isInverted) 8.dp else 0.dp)
        ) { PlayerHeader(player = player) }

        // GIANT LIFE COUNTER
        Box(modifier = Modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
            val glowStrength = ((scalePulse.value - 1f) * 3f).coerceIn(0f, 1f)
            if (glowStrength > 0.01f) {
                Box(modifier = Modifier.size(200.dp).graphicsLayer {
                    scaleX = scalePulse.value * 1.3f; scaleY = scalePulse.value * 1.3f; alpha = glowStrength * 0.5f
                }.background(Brush.radialGradient(
                    colors = listOf(lastDeltaColor.value, lastDeltaColor.value.copy(alpha = 0.3f), Color.Transparent),
                    radius = 350f,
                ), CircleShape))
            }

            Text(
                text = "$lifeNow",
                style = LifeTotalStyle.copy(shadow = Shadow(
                    color = when { player.life <= 0 -> MtgRed.copy(alpha = 0.8f); player.life <= 10 -> Color(0xFFFFA726).copy(alpha = 0.5f); else -> Color.Transparent },
                    offset = Offset(0f, 0f), blurRadius = 30f,
                )),
                color = lifeColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { scaleX = scalePulse.value; scaleY = scalePulse.value },
            )
        }

        // Floating damage/heal numbers
        floatingTexts.forEach { ft ->
            val age = (System.currentTimeMillis() - ft.startTime).toFloat()
            val progress = (age / 2500f).coerceIn(0f, 1f)
            val offsetY = -progress * 250f
            val alpha = if (progress < 0.1f) progress * 10f else (1f - ((progress - 0.1f) / 0.9f))
            if (alpha > 0.01f) {
                Text(
                    text = ft.text, fontSize = 52.sp, color = ft.color, textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).offset(x = ft.offsetX.dp, y = offsetY.dp)
                        .alpha(alpha.coerceIn(0f, 1f))
                        .graphicsLayer { scaleX = 1f + progress * 0.4f; scaleY = 1f + progress * 0.4f },
                )
            }
        }

        // Cumulative delta
        val vis = pressVisibility.value
        val totalDelta = vis.entries.sumOf { (key, pair) ->
            val count = pair.first
            when (key) { "m5" -> -5 * count; "m1" -> -1 * count; "p1" -> +1 * count; "p5" -> +5 * count; else -> 0 }
        }
        if (vis.isNotEmpty() && totalDelta != 0) {
            val minFade = vis.values.minOf { it.second }
            if (minFade > 0f) {
                val sign = if (totalDelta > 0) "+" else ""
                Box(modifier = Modifier.align(Alignment.Center).offset(y = (-110).dp).alpha(minFade)) {
                    Text("$sign$totalDelta", fontSize = 36.sp,
                        color = if (totalDelta > 0) MtgGreen else MtgRed, textAlign = TextAlign.Center)
                }
            }
        }

        // LIFE BUTTONS
        Box(modifier = Modifier
            .align(if (isInverted) Alignment.TopCenter else Alignment.BottomCenter)
            .padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()
        ) {
            LifeButtons(
                onMinusOne = { press("m1"); onLifeChange(playerIndex, -1); spawnFloatingText("-1", MtgRed) },
                onMinusFive = { press("m5"); onLifeChange(playerIndex, -5); spawnFloatingText("-5", MtgRed) },
                onPlusFive = { press("p5"); onLifeChange(playerIndex, +5); spawnFloatingText("+5", MtgGreen) },
                onPlusOne = { press("p1"); onLifeChange(playerIndex, +1); spawnFloatingText("+1", MtgGreen) },
                enabled = true, pressVisibility = vis,
            )
        }

        // COUNTERS ROW
        Box(modifier = Modifier
            .align(if (isInverted) Alignment.BottomCenter else Alignment.TopCenter)
            .padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(32.dp))
                CountersRow(
                    poison = player.poisonCounters, energy = player.energyCounters,
                    onPoisonPlus = { onPoisonChange(playerIndex, 1) }, onPoisonMinus = { onPoisonChange(playerIndex, -1) },
                    onEnergyPlus = { onEnergyChange(playerIndex, 1) }, onEnergyMinus = { onEnergyChange(playerIndex, -1) },
                    enabled = true,
                )
                if (gameMode == com.pgedeon.portalcounters.model.GameMode.COMMANDER) {
                    Spacer(modifier = Modifier.height(4.dp))
                    CommanderDamageRow(playerIndex, player, allPlayers, onCommanderDamageChange, !isDead)
                }
            }
        }
    }
}

@Composable
private fun PlayerHeader(player: PlayerState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(modifier = Modifier.size(14.dp).background(player.color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(player.name, style = MaterialTheme.typography.labelLarge, color = ContentOnDark, maxLines = 1)
    }
}

@Composable
private fun LifeButtons(
    onMinusOne: () -> Unit, onMinusFive: () -> Unit, onPlusFive: () -> Unit, onPlusOne: () -> Unit,
    enabled: Boolean, pressVisibility: Map<String, Pair<Int, Float>>,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        LifeButton("-5", { if (enabled) onMinusFive() }, enabled, false, pressVisibility["m5"], Modifier.weight(1f))
        LifeButton("-1", { if (enabled) onMinusOne() }, enabled, false, pressVisibility["m1"], Modifier.weight(1f))
        LifeButton("+1", { if (enabled) onPlusOne() }, enabled, true, pressVisibility["p1"], Modifier.weight(1f))
        LifeButton("+5", { if (enabled) onPlusFive() }, enabled, true, pressVisibility["p5"], Modifier.weight(1f))
    }
}

@Composable
private fun LifeButton(
    label: String, onClick: () -> Unit, enabled: Boolean, isPlus: Boolean,
    pressCount: Pair<Int, Float>?, modifier: Modifier = Modifier,
) {
    val count = pressCount?.first ?: 0
    val fade = pressCount?.second ?: 0f
    var pressed by remember { mutableStateOf(false) }
    val btnScale by animateFloatAsState(
        targetValue = if (pressed) 0.82f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh), label = "btn",
    )
    LaunchedEffect(pressed) { if (pressed) { delay(120); pressed = false } }

    Box(modifier = modifier.height(64.dp)) {
        Button(
            onClick = { if (enabled) { pressed = true; onClick() } },
            enabled = enabled,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = btnScale; scaleY = btnScale },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlus) ButtonPlusAccent else ButtonMinusAccent,
                contentColor = ContentOnDark,
                disabledContainerColor = ButtonDark,
                disabledContentColor = ContentOnDark.copy(alpha = 0.3f),
            ),
            shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(0.dp),
        ) { Text(label, fontSize = 22.sp) }

        if (count > 0 && fade > 0f) {
            Surface(modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-6).dp).alpha(fade),
                shape = CircleShape, color = if (isPlus) MtgGreen else MtgRed) {
                Text("×$count", fontSize = 14.sp, color = ContentOnDark, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun CommanderDamageRow(
    playerIndex: Int, player: PlayerState, allPlayers: List<PlayerState>,
    onCommanderDamageChange: (Int, Int, Int) -> Unit, enabled: Boolean,
) {
    val others = allPlayers.filterIndexed { idx, _ -> idx != playerIndex }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        others.forEach { other ->
            val otherIdx = allPlayers.indexOf(other)
            val dmg = player.commanderDamage.getOrDefault(otherIdx, 0)
            Surface(modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(6.dp),
                color = SurfaceMid, border = BorderStroke(1.dp, other.color)) {
                Row(modifier = Modifier.fillMaxSize().clickable(enabled = enabled) {
                    onCommanderDamageChange(playerIndex, otherIdx, 1)
                }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(other.color, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${other.name.take(3)}:$dmg", fontSize = 13.sp, color = ContentOnDark)
                }
            }
        }
    }
}

/**
 * Small counter button with a minimum 40dp touch target.
 */
@Composable
private fun CounterButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(6.dp),
        color = SurfaceMid,
        contentColor = color,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text, fontSize = 20.sp, color = color, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CountersRow(
    poison: Int, energy: Int,
    onPoisonPlus: () -> Unit, onPoisonMinus: () -> Unit,
    onEnergyPlus: () -> Unit, onEnergyMinus: () -> Unit, enabled: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Poison counter
        Surface(
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(6.dp),
            color = if (poison > 0) MtgGreen.copy(alpha = 0.25f) else SurfaceMid,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text("☠", fontSize = 16.sp, color = MtgGreen)
                Spacer(modifier = Modifier.width(4.dp))
                Text("$poison", fontSize = 16.sp, color = ContentOnDark)
                if (enabled) {
                    Spacer(modifier = Modifier.width(4.dp))
                    CounterButton("+", onPoisonPlus, enabled, MetaBlue)
                    Spacer(modifier = Modifier.width(2.dp))
                    CounterButton("−", onPoisonMinus, enabled && poison > 0, MtgRed)
                }
            }
        }

        // Energy counter
        Surface(
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(6.dp),
            color = if (energy > 0) MtgBlue.copy(alpha = 0.25f) else SurfaceMid,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text("⚡", fontSize = 16.sp, color = MtgBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Text("$energy", fontSize = 16.sp, color = ContentOnDark)
                if (enabled) {
                    Spacer(modifier = Modifier.width(4.dp))
                    CounterButton("+", onEnergyPlus, enabled, MetaBlue)
                    Spacer(modifier = Modifier.width(2.dp))
                    CounterButton("−", onEnergyMinus, enabled && energy > 0, MtgRed)
                }
            }
        }
    }
}
