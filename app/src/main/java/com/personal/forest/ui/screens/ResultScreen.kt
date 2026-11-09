package com.personal.forest.ui.screens

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.forest.data.Achievements
import com.personal.forest.data.SpeciesRepo
import com.personal.forest.session.FinishedResult
import com.personal.forest.ui.components.Scenery
import com.personal.forest.ui.components.TreeArt
import com.personal.forest.ui.theme.Cream
import com.personal.forest.ui.theme.CreamDeep
import com.personal.forest.ui.theme.Forest
import com.personal.forest.ui.theme.Muted
import com.personal.forest.ui.theme.Withered

@Composable
fun ResultScreen(result: FinishedResult, onDone: (goForest: Boolean) -> Unit) {
    val context = LocalContext.current
    val species = remember(result.session.speciesId) {
        SpeciesRepo.byId(context, result.session.speciesId)
    }
    val success = result.session.status == "success"

    Scenery(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.7f))
            if (species != null) {
                TreeArt(
                    species,
                    stage = 3,
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .aspectRatio(0.75f),
                    withered = !success,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (success) "树长大了" else "树枯萎了",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (success) Forest else Withered,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (success) {
                    "专注 ${result.session.plannedMin} 分钟 · +${result.session.coins} 金币"
                } else {
                    val minutes = (result.session.actualSec + 59) / 60
                    if (result.session.actualSec < 30) "还没开始就离开了"
                    else "在第 $minutes 分钟离开，这棵树不会带来金币"
                },
                fontSize = 14.sp,
                color = Muted,
            )

            if (result.achievements.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Surface(color = CreamDeep, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(14.dp)) {
                        result.achievements.forEach { id ->
                            Text(
                                "成就解锁 · ${Achievements.name(id)}",
                                fontSize = 13.sp,
                                color = Forest,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onDone(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = Cream),
            ) {
                Text("回到林地", fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { onDone(false) }) {
                Text("再种一棵", color = Forest)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
