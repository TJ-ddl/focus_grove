package com.personal.forest.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.forest.data.Economy
import com.personal.forest.data.GameData
import com.personal.forest.ui.components.SoftCard
import com.personal.forest.ui.components.StatCard
import com.personal.forest.ui.theme.Forest
import com.personal.forest.ui.theme.Ink
import com.personal.forest.ui.theme.Muted
import com.personal.forest.ui.theme.Sage
import com.personal.forest.ui.theme.SageLight
import com.personal.forest.util.formatMinutes
import com.personal.forest.util.weekdayCn
import java.time.LocalDate

@Composable
fun StatsScreen(data: GameData) {
    val sessions = data.sessions
    val today = LocalDate.now()
    val weekly = remember(sessions) { Economy.weeklyMinutes(sessions, today) }
    val labels = remember(today) {
        (6 downTo 0).map { weekdayCn(today.minusDays(it.toLong())).removePrefix("周") }
    }
    val tags = remember(sessions) { Economy.tagMinutes(sessions) }
    val success = Economy.successful(sessions)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("统计", fontSize = 26.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = Ink)
        Text("专注的每一天都算数", fontSize = 13.sp, color = Muted)
        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("累计专注", formatMinutes(Economy.totalMinutes(sessions)), Modifier.weight(1f))
            StatCard("连续天数", "${Economy.streak(sessions, today)} 天", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("成功的树", "${success.size} 棵", Modifier.weight(1f))
            StatCard("成功率", "${(Economy.successRate(sessions) * 100).toInt()}%", Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("最近 7 天", fontSize = 13.sp, color = Muted)
                Spacer(Modifier.height(12.dp))
                WeeklyChart(weekly)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    labels.forEach { label ->
                        Text(
                            label,
                            modifier = Modifier.weight(1f),
                            fontSize = 11.sp,
                            color = Muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("标签分布", fontSize = 13.sp, color = Muted)
                if (tags.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("还没有带标签的专注记录", fontSize = 12.sp, color = Muted)
                } else {
                    val max = tags.maxOf { it.second }.coerceAtLeast(1)
                    tags.forEach { (tag, minutes) ->
                        Column(Modifier.padding(top = 12.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text(tag, fontSize = 12.sp, color = Ink)
                                Spacer(Modifier.weight(1f))
                                Text(formatMinutes(minutes), fontSize = 12.sp, color = Muted)
                            }
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(SageLight)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(minutes.toFloat() / max)
                                        .fillMaxHeight()
                                        .background(Sage)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("累计 ${sessions.size} 条专注记录", fontSize = 11.sp, color = Muted)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun WeeklyChart(values: List<Int>) {
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val maxValue = values.max().coerceAtLeast(1)
        val slot = size.width / values.size
        values.forEachIndexed { index, value ->
            val barWidth = slot * 0.44f
            val barHeight = size.height * 0.86f * value / maxValue
            val left = slot * index + (slot - barWidth) / 2f
            drawRoundRect(
                color = if (index == values.lastIndex) Forest else Sage,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
