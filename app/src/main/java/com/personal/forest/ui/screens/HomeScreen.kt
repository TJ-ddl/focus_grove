package com.personal.forest.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.forest.data.Economy
import com.personal.forest.data.GameData
import com.personal.forest.data.Species
import com.personal.forest.data.Store
import com.personal.forest.data.TAGS
import com.personal.forest.data.rarityLabel
import com.personal.forest.ui.components.CoinPill
import com.personal.forest.ui.components.Scenery
import com.personal.forest.ui.components.SoftCard
import com.personal.forest.ui.components.StatCard
import com.personal.forest.ui.components.TreeArt
import com.personal.forest.ui.theme.Clay
import com.personal.forest.ui.theme.Cream
import com.personal.forest.ui.theme.Forest
import com.personal.forest.ui.theme.Ink
import com.personal.forest.ui.theme.Muted
import com.personal.forest.ui.theme.SageLight
import com.personal.forest.util.dayLabel
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    store: Store,
    data: GameData,
    speciesList: List<Species>,
    onStart: (Species, Int, String) -> Unit,
) {
    var minutes by remember { mutableIntStateOf(data.settings.lastMinutes.coerceIn(10, 120)) }
    var speciesId by remember { mutableStateOf(data.settings.lastSpeciesId) }
    var tag by remember { mutableStateOf("") }
    var pickerOpen by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    val selected = speciesList.firstOrNull { it.id == speciesId && it.id in data.unlocked }
        ?: speciesList.firstOrNull { it.id in data.unlocked }
        ?: speciesList.first()

    Scenery(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("林地", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(dayLabel(today), fontSize = 13.sp, color = Muted)
                }
                CoinPill(data.coins)
            }
            Spacer(Modifier.height(22.dp))

            SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("专注时长", fontSize = 13.sp, color = Muted)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$minutes", fontSize = 56.sp, fontWeight = FontWeight.Light, color = Forest)
                        Text(
                            " 分钟",
                            fontSize = 15.sp,
                            color = Muted,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    Slider(
                        value = minutes.toFloat(),
                        onValueChange = {
                            minutes = ((it / 5f).roundToInt() * 5).coerceIn(10, 120)
                        },
                        valueRange = 10f..120f,
                        steps = 21,
                        colors = SliderDefaults.colors(
                            thumbColor = Forest,
                            activeTrackColor = Forest,
                            inactiveTrackColor = SageLight,
                        ),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("10 分钟", fontSize = 11.sp, color = Muted)
                        Text("120 分钟", fontSize = 11.sp, color = Muted)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            SoftCard(Modifier.fillMaxWidth(), onClick = { pickerOpen = true }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    TreeArt(selected, 3, Modifier.size(52.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(selected.name, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Ink)
                        Text(
                            "${rarityLabel(selected.rarity)} · ${selected.desc}",
                            fontSize = 11.sp,
                            color = Muted,
                            maxLines = 1,
                        )
                    }
                    Text("更换 ›", fontSize = 13.sp, color = Forest)
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("标签", fontSize = 12.sp, color = Muted, modifier = Modifier.padding(bottom = 6.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = tag.isEmpty(),
                    onClick = { tag = "" },
                    label = { Text("无标签", fontSize = 12.sp) },
                )
                TAGS.forEach { t ->
                    FilterChip(
                        selected = tag == t,
                        onClick = { tag = if (tag == t) "" else t },
                        label = { Text(t, fontSize = 12.sp) },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    store.setSettings { it.copy(lastSpeciesId = selected.id, lastMinutes = minutes) }
                    onStart(selected, minutes, tag)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = Cream),
            ) {
                Text("种下一棵树", fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("今日", "${Economy.minutesOn(data.sessions, today)} 分钟", Modifier.weight(1f))
                StatCard("树木", "${Economy.treesOn(data.sessions, today)} 棵", Modifier.weight(1f))
                StatCard("连续", "${Economy.streak(data.sessions)} 天", Modifier.weight(1f))
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (pickerOpen) {
        ModalBottomSheet(onDismissRequest = { pickerOpen = false }, containerColor = Cream) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text("选择树种", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.height(8.dp))
                speciesList.forEach { sp ->
                    val unlocked = sp.id in data.unlocked
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(enabled = unlocked) {
                                speciesId = sp.id
                                pickerOpen = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TreeArt(sp, 3, Modifier.size(44.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(sp.name, fontSize = 15.sp, color = if (unlocked) Ink else Muted)
                            Text(
                                if (unlocked) rarityLabel(sp.rarity)
                                else "${rarityLabel(sp.rarity)} · 未解锁",
                                fontSize = 11.sp,
                                color = Muted,
                            )
                        }
                        if (sp.id == selected.id) {
                            Text("当前", fontSize = 12.sp, color = Forest)
                        } else if (!unlocked) {
                            Text("去解锁", fontSize = 12.sp, color = Clay)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("更多树种可以在「我的」里用金币解锁", fontSize = 11.sp, color = Muted)
            }
        }
    }
}
