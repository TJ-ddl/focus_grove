package com.personal.forest.ui.screens

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.forest.data.Species
import com.personal.forest.data.SpeciesRepo
import com.personal.forest.session.FocusService
import com.personal.forest.session.RunningSession
import com.personal.forest.ui.components.Scenery
import com.personal.forest.ui.components.treeDrawableId
import com.personal.forest.ui.theme.Clay
import com.personal.forest.ui.theme.Cream
import com.personal.forest.ui.theme.CreamDeep
import com.personal.forest.ui.theme.Forest
import com.personal.forest.ui.theme.Ink
import com.personal.forest.ui.theme.Muted
import com.personal.forest.ui.theme.Sage
import com.personal.forest.ui.theme.SageLight
import com.personal.forest.util.formatClock
import kotlinx.coroutines.delay

@Composable
fun FocusScreen(r: RunningSession) {
    val context = LocalContext.current
    val species = remember(r.speciesId) { SpeciesRepo.byId(context, r.speciesId) }
    val totalMs = r.plannedMin * 60_000L
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(r.id) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay(500)
        }
    }
    val elapsedMs = (now - r.startElapsed).coerceIn(0L, totalMs)
    val progress = elapsedMs.toFloat() / totalMs
    val remainingSec = ((totalMs - elapsedMs) / 1000L).toInt()
    var confirm by remember { mutableStateOf(false) }

    Scenery(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("专注中", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(
                        if (r.tag.isBlank()) "别离开，树会枯萎" else "${r.tag} · 别离开，树会枯萎",
                        fontSize = 13.sp,
                        color = Muted,
                    )
                }
                Surface(shape = RoundedCornerShape(50), color = CreamDeep) {
                    Text(
                        "${r.plannedMin} 分钟",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = Forest,
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            if (species != null) {
                GrowthTree(species, progress, Modifier.fillMaxWidth(0.82f).aspectRatio(0.75f))
            }
            Spacer(Modifier.height(8.dp))
            Text(formatClock(remainingSec), fontSize = 46.sp, fontWeight = FontWeight.Light, color = Forest)
            Text("剩余时间", fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Sage,
                trackColor = SageLight,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { confirm = true }) {
                Text("放弃这棵树", color = Muted)
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("放弃这棵树？") },
            text = { Text("它会在林地里枯萎，这次专注也不会有金币。") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    FocusService.instance?.abandonFromApp()
                }) { Text("放弃", color = Clay) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("继续专注") }
            },
            containerColor = Cream,
        )
    }
}

@Composable
private fun GrowthTree(species: Species, progress: Float, modifier: Modifier) {
    val stage = when {
        progress < 0.34f -> 1
        progress < 0.67f -> 2
        else -> 3
    }
    val scale by animateFloatAsState(
        targetValue = 0.78f + 0.22f * progress,
        animationSpec = tween(900),
        label = "treeScale",
    )
    val transition = rememberInfiniteTransition(label = "sway")
    val angle by transition.animateFloat(
        initialValue = -1.1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "swayAngle",
    )
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Crossfade(targetState = stage, animationSpec = tween(800), label = "treeStage") { s ->
            Image(
                painter = painterResource(treeDrawableId(species.art, s)),
                contentDescription = species.name,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = angle
                    },
                contentScale = ContentScale.Fit,
            )
        }
    }
}
