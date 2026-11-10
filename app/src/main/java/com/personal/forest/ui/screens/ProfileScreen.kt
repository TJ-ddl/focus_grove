package com.personal.forest.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.personal.forest.data.Achievements
import com.personal.forest.data.GameData
import com.personal.forest.data.Species
import com.personal.forest.data.Store
import com.personal.forest.ui.components.CoinPill
import com.personal.forest.ui.components.SoftCard
import com.personal.forest.ui.components.TreeArt
import com.personal.forest.ui.theme.Clay
import com.personal.forest.ui.theme.Cream
import com.personal.forest.ui.theme.CreamDeep
import com.personal.forest.ui.theme.Forest
import com.personal.forest.ui.theme.Ink
import com.personal.forest.ui.theme.Muted
import com.personal.forest.ui.theme.Sage
import com.personal.forest.util.hasUsageAccess

@Composable
fun ProfileScreen(store: Store, data: GameData, speciesList: List<Species>) {
    val context = LocalContext.current
    var unlockTarget by remember { mutableStateOf<Species?>(null) }
    var usageGranted by remember { mutableStateOf(hasUsageAccess(context)) }
    var whitelistOpen by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) usageGranted = hasUsageAccess(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val ok = store.export(uri)
            Toast.makeText(context, if (ok) "已导出" else "导出失败", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val ok = store.import(uri)
            Toast.makeText(context, if (ok) "已导入" else "导入失败：文件格式不对", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("我的", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text("林地和收藏都在这台手机里", fontSize = 13.sp, color = Muted)
            }
            CoinPill(data.coins)
        }

        SectionTitle("树种收藏", "点击已解锁的树可设为默认")
        speciesList.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { species ->
                    val unlocked = species.id in data.unlocked
                    SoftCard(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (unlocked) {
                                store.setSettings { it.copy(lastSpeciesId = species.id) }
                                Toast.makeText(context, "已设为默认树种", Toast.LENGTH_SHORT).show()
                            } else {
                                unlockTarget = species
                            }
                        },
                    ) {
                        Column(
                            Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TreeArt(
                                species,
                                3,
                                Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    .alpha(if (unlocked) 1f else 0.35f),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(species.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
                            Text(
                                if (unlocked) "已解锁" else "${species.price} 金币",
                                fontSize = 11.sp,
                                color = if (unlocked) Sage else Clay,
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        SectionTitle("成就")
        Achievements.all.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { achievement ->
                    val earned = achievement.id in data.achievements
                    SoftCard(
                        modifier = Modifier.weight(1f),
                        color = if (earned) CreamDeep else Color.White.copy(alpha = 0.35f),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                achievement.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (earned) Ink else Muted,
                            )
                            Text(achievement.desc, fontSize = 11.sp, color = Muted)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (earned) "已达成" else "未达成",
                                fontSize = 10.sp,
                                color = if (earned) Sage else Muted,
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }

        SectionTitle("设置")
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("严格模式", fontSize = 14.sp, color = Ink)
                        Text(
                            if (usageGranted) "切到其他 App 超过几秒就判枯"
                            else "需要先授予「使用情况访问」权限",
                            fontSize = 11.sp,
                            color = Muted,
                        )
                    }
                    Switch(
                        checked = data.settings.strictMode,
                        onCheckedChange = { checked ->
                            if (checked && !hasUsageAccess(context)) {
                                usageGranted = false
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                Toast.makeText(
                                    context,
                                    "请先允许「使用情况访问」，再回来打开严格模式",
                                    Toast.LENGTH_LONG,
                                ).show()
                            } else {
                                store.setSettings { it.copy(strictMode = checked) }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Cream,
                            checkedTrackColor = Forest,
                        ),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("forest-backup.json") },
                        modifier = Modifier.weight(1f),
                    ) { Text("导出数据", fontSize = 13.sp) }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                    ) { Text("导入数据", fontSize = 13.sp) }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "所有数据只保存在本机。建议每周导出一次备份，导入会覆盖当前数据。",
                    fontSize = 11.sp,
                    color = Muted,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "桌面小组件：长按桌面 → 小组件 → 林地",
                    fontSize = 11.sp,
                    color = Muted,
                )
                Spacer(Modifier.height(8.dp))
                Text("林地 1.1 · 本地版", fontSize = 11.sp, color = Muted)
            }
        }

        SectionTitle("专注白名单", "切到这些 App 不判枯")
        SoftCard(Modifier.fillMaxWidth(), onClick = { whitelistOpen = true }) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "已添加 ${data.settings.whitelist.size} 个 App",
                        fontSize = 14.sp,
                        color = Ink,
                    )
                    Text(
                        if (usageGranted) "切到列表外的 App 仍会判枯"
                        else "需要「使用情况访问」权限才能识别当前 App",
                        fontSize = 11.sp,
                        color = Muted,
                    )
                }
                Text("管理 ›", fontSize = 13.sp, color = Forest)
            }
        }
        Spacer(Modifier.height(28.dp))
    }

    val target = unlockTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { unlockTarget = null },
            title = { Text("解锁「${target.name}」？") },
            text = { Text("需要 ${target.price} 金币，当前有 ${data.coins} 金币。") },
            confirmButton = {
                TextButton(onClick = {
                    val ok = store.unlock(target)
                    if (ok) {
                        Toast.makeText(context, "解锁成功", Toast.LENGTH_SHORT).show()
                        unlockTarget = null
                    } else {
                        Toast.makeText(context, "金币不够", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(
                        if (data.coins >= target.price) "解锁" else "金币不够",
                        color = if (data.coins >= target.price) Forest else Muted,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { unlockTarget = null }) { Text("再想想") }
            },
            containerColor = Cream,
        )
    }

    if (whitelistOpen) {
        WhitelistSheet(
            context = context,
            whitelist = data.settings.whitelist,
            usageGranted = usageGranted,
            onToggle = { pkg ->
                store.setSettings { s ->
                    s.copy(
                        whitelist = if (pkg in s.whitelist) s.whitelist - pkg
                        else s.whitelist + pkg,
                    )
                }
            },
            onGrant = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            },
            onDismiss = { whitelistOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
private fun WhitelistSheet(
    context: Context,
    whitelist: List<String>,
    usageGranted: Boolean,
    onToggle: (String) -> Unit,
    onGrant: () -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val apps = remember {
        context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            0,
        ).map {
            it.activityInfo.packageName to it.loadLabel(context.packageManager).toString()
        }.filter { it.first != context.packageName }
            .distinctBy { it.first }
            .sortedBy { it.second }
    }
    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter {
            it.second.contains(query, ignoreCase = true) ||
                it.first.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cream) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("专注白名单", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                if (usageGranted) "打开这些 App 不算离开；切到列表外的 App 仍会判枯。"
                else "需要先授予「使用情况访问」权限，否则无法识别你切到了哪个 App。",
                fontSize = 11.sp,
                color = Muted,
            )
            if (!usageGranted) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onGrant) { Text("去授权", fontSize = 13.sp) }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜索应用", fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                items(filtered, key = { it.first }) { (pkg, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(pkg) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = pkg in whitelist, onCheckedChange = null)
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(label, fontSize = 14.sp, color = Ink, maxLines = 1)
                            Text(pkg, fontSize = 10.sp, color = Muted, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, hint: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Ink)
        if (hint != null) {
            Spacer(Modifier.width(8.dp))
            Text(hint, fontSize = 11.sp, color = Muted)
        }
    }
}
