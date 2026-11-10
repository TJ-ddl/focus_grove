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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.personal.forest.data.Store
import com.personal.forest.ui.components.CoinPill
import com.personal.forest.ui.components.Scenery
import com.personal.forest.ui.components.TreeArt
import com.personal.forest.ui.theme.Clay
import com.personal.forest.ui.theme.Cream
import com.personal.forest.ui.theme.CreamDeep
import com.personal.forest.ui.theme.Forest
import com.personal.forest.ui.theme.Ink
import com.personal.forest.ui.theme.Muted
import com.personal.forest.util.dayLabel
import com.personal.forest.util.formatMinutes
import com.personal.forest.util.timeLabel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForestScreen(store: Store, data: GameData, speciesList: List<Species>) {
    val sessions = data.sessions
    val today = LocalDate.now()
    val days = remember(sessions) {
        val now = LocalDate.now()
        val earliest = sessions.minOfOrNull { Economy.dayOf(it.startAt) } ?: now
        generateSequence(now) { it.minusDays(1) }
            .takeWhile { !it.isBefore(earliest) }
            .toList()
    }
    val byDay = remember(sessions) { sessions.groupBy { Economy.dayOf(it.startAt) } }
    val pagerState = rememberPagerState(pageCount = { days.size })
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Session?>(null) }
    var calendarOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("林地", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text("每一天，都留下一片林地", fontSize = 13.sp, color = Muted)
            }
            Text(
                "日历",
                fontSize = 13.sp,
                color = Forest,
                modifier = Modifier
                    .clickable { calendarOpen = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            CoinPill(data.coins)
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val day = days[page]
            DayForest(
                day = day,
                daySessions = byDay[day].orEmpty(),
                speciesList = speciesList,
                today = today,
                onSelect = { selected = it },
            )
        }
    }

    if (calendarOpen) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = days[pagerState.currentPage].toEpochDay() * 86_400_000L,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(ZoneOffset.UTC).toLocalDate()
                    return !date.isAfter(today) && !date.isBefore(days.last())
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { calendarOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    calendarOpen = false
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                        val index = days.indexOf(date)
                        if (index >= 0) scope.launch { pagerState.scrollToPage(index) }
                    }
                }) { Text("查看") }
            },
            dismissButton = {
                TextButton(onClick = { calendarOpen = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }

    val sel = selected
    if (sel != null) {
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = Cream) {
            SessionDetail(
                session = sel,
                speciesList = speciesList,
                coins = data.coins,
                onClear = {
                    if (store.clearWithered(sel.id)) selected = null
                },
            )
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
    val visible = remember(daySessions) { daySessions.filter { !it.cleared } }
    val placed = remember(visible) { placeTrees(visible) }

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
                if (visible.isEmpty()) {
                    Text(
                        if (daySessions.isEmpty()) "这一天还没有树" else "枯树已清理",
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
private fun SessionDetail(
    session: Session,
    speciesList: List<Species>,
    coins: Int,
    onClear: () -> Unit,
) {
    val species = speciesList.firstOrNull { it.id == session.speciesId }
    val success = session.status == "success"
    var confirmClear by remember { mutableStateOf(false) }
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
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { confirmClear = true },
                enabled = coins >= Economy.CLEAR_WITHERED_COST,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Clay,
                    contentColor = Cream,
                ),
            ) {
                Text(
                    if (coins >= Economy.CLEAR_WITHERED_COST) {
                        "铲除枯树 · ${Economy.CLEAR_WITHERED_COST} 金币"
                    } else {
                        "铲除枯树 · 金币不足"
                    },
                    fontSize = 14.sp,
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("铲除这棵枯树？") },
            text = {
                Text(
                    "花费 ${Economy.CLEAR_WITHERED_COST} 金币。" +
                        "枯树会从林地消失，失败记录和成功率仍会保留。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClear()
                }) { Text("铲除", color = Clay) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            },
            containerColor = Cream,
        )
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
