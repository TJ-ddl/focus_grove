package com.personal.forest.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.forest.data.Economy
import com.personal.forest.data.GameData
import com.personal.forest.data.Session
import com.personal.forest.data.Species
import com.personal.forest.ui.components.CoinPill
import com.personal.forest.ui.components.Scenery
import com.personal.forest.ui.components.TreeArt
import com.personal.forest.ui.theme.Cream
import com.personal.forest.ui.theme.CreamDeep
import com.personal.forest.ui.theme.Forest
import com.personal.forest.ui.theme.Ink
import com.personal.forest.ui.theme.Muted
import com.personal.forest.util.dayLabel
import com.personal.forest.util.formatMinutes
import com.personal.forest.util.timeLabel
import java.time.LocalDate
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForestScreen(data: GameData, speciesList: List<Species>) {
    val sessions = data.sessions
    val today = LocalDate.now()
    val days = remember(sessions) {
        val now = LocalDate.now()
        val earliest = sessions.minOfOrNull { Economy.dayOf(it.startAt) } ?: now
        val start = maxOf(earliest, now.minusDays(89))
        generateSequence(now) { it.minusDays(1) }
            .takeWhile { !it.isBefore(start) }
            .toList()
    }
    val pagerState = rememberPagerState(pageCount = { days.size })
    var selected by remember { mutableStateOf<Session?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("林地", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text("每一天，都留下一片林地", fontSize = 13.sp, color = Muted)
            }
            CoinPill(data.coins)
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val day = days[page]
            DayForest(
                day = day,
                daySessions = sessions.filter { Economy.dayOf(it.startAt) == day },
                speciesList = speciesList,
                today = today,
                onSelect = { selected = it },
            )
        }
    }

    val sel = selected
    if (sel != null) {
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = Cream) {
            SessionDetail(sel, speciesList)
        }
    }
}

private data class PlacedTree(
    val session: Session,
    val xFrac: Float,
    val yFrac: Float,
    val widthFrac: Float,
)

private fun placeTrees(sessions: List<Session>): List<PlacedTree> {
    val count = sessions.size.coerceAtLeast(1)
    return sessions.mapIndexed { index, session ->
        val seed = if (session.seed != 0) session.seed else session.id.hashCode()
        val random = Random(seed)
        val depth = random.nextFloat()
        val baseX = 0.10f + (index + 0.5f) / count * 0.80f
        val jitter = (random.nextFloat() - 0.5f) * 0.10f
        PlacedTree(
            session = session,
            xFrac = (baseX + jitter).coerceIn(0.10f, 0.90f),
            yFrac = 0.20f - depth * 0.16f,
            widthFrac = 0.24f + depth * 0.20f,
        )
    }.sortedByDescending { it.yFrac }
}

@Composable
private fun DayForest(
    day: LocalDate,
    daySessions: List<Session>,
    speciesList: List<Species>,
    today: LocalDate,
    onSelect: (Session) -> Unit,
) {
    val successCount = daySessions.count { it.status == "success" }
    val minutes = daySessions.filter { it.status == "success" }.sumOf { it.plannedMin }
    val placed = remember(daySessions) { placeTrees(daySessions) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        Scenery(Modifier.fillMaxSize()) {}
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (day == today) "今天" else dayLabel(day),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                Text("$minutes 分钟 · $successCount 棵树", fontSize = 12.sp, color = Muted)
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                if (daySessions.isEmpty()) {
                    Text(
                        "这一天还没有树",
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                placed.forEach { tree ->
                    val species = speciesList.firstOrNull { it.id == tree.session.speciesId }
                        ?: return@forEach
                    val treeWidth = maxWidth * tree.widthFrac
                    TreeArt(
                        species = species,
                        stage = 3,
                        withered = tree.session.status != "success",
                        modifier = Modifier
                            .width(treeWidth)
                            .height(treeWidth * 1.333f)
                            .align(Alignment.BottomStart)
                            .offset(
                                x = maxWidth * tree.xFrac - treeWidth / 2,
                                y = -maxHeight * tree.yFrac,
                            )
                            .clickable { onSelect(tree.session) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionDetail(session: Session, speciesList: List<Species>) {
    val species = speciesList.firstOrNull { it.id == session.speciesId }
    val success = session.status == "success"
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (species != null) {
                TreeArt(species, 3, Modifier.size(56.dp), withered = !success)
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(
                    species?.name ?: session.speciesId,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink,
                )
                Text(
                    "${dayLabel(Economy.dayOf(session.startAt))} · ${timeLabel(session.startAt)}",
                    fontSize = 12.sp,
                    color = Muted,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailCell("时长", formatMinutes(session.plannedMin), Modifier.weight(1f))
            DetailCell("状态", if (success) "成林" else "枯萎", Modifier.weight(1f))
            DetailCell("金币", if (success) "+${session.coins}" else "0", Modifier.weight(1f))
        }
        if (session.tag.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text("标签 · ${session.tag}", fontSize = 13.sp, color = Muted)
        }
        if (!success) {
            Spacer(Modifier.height(10.dp))
            Text("这棵树因为中途离开而枯萎了", fontSize = 12.sp, color = Muted)
        }
    }
}

@Composable
private fun DetailCell(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = CreamDeep, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = Muted)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Forest)
        }
    }
}
