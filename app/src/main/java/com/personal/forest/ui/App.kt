package com.personal.forest.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.forest.R
import com.personal.forest.data.SpeciesRepo
import com.personal.forest.data.Store
import com.personal.forest.session.Focus
import com.personal.forest.session.FocusService
import com.personal.forest.ui.screens.FocusScreen
import com.personal.forest.ui.screens.ForestScreen
import com.personal.forest.ui.screens.HomeScreen
import com.personal.forest.ui.screens.ProfileScreen
import com.personal.forest.ui.screens.ResultScreen
import com.personal.forest.ui.screens.StatsScreen
import com.personal.forest.ui.theme.Cream
import com.personal.forest.ui.theme.CreamDeep
import com.personal.forest.ui.theme.Forest
import com.personal.forest.ui.theme.Muted
import com.personal.forest.ui.theme.SageLight

@Composable
fun ForestAppRoot() {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val speciesList = remember { SpeciesRepo.get(context) }
    val data by store.data.collectAsStateWithLifecycle()
    val running by Focus.running.collectAsStateWithLifecycle()
    val result by Focus.result.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    val activeRun = running
    val activeResult = result

    when {
        activeRun != null -> FocusScreen(activeRun)
        activeResult != null -> ResultScreen(activeResult) { goForest ->
            Focus.result.value = null
            tab = if (goForest) 1 else 0
        }
        else -> Scaffold(
            containerColor = Cream,
            bottomBar = { BottomBar(tab) { tab = it } },
        ) { inner ->
            Box(Modifier.padding(inner)) {
                when (tab) {
                    0 -> HomeScreen(store, data, speciesList) { species, minutes, tag ->
                        FocusService.start(context, species, minutes, tag)
                    }
                    1 -> ForestScreen(store, data, speciesList)
                    2 -> StatsScreen(data)
                    else -> ProfileScreen(store, data, speciesList)
                }
            }
        }
    }
}

@Composable
private fun BottomBar(tab: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = CreamDeep, tonalElevation = 0.dp) {
        val items = listOf(
            Triple("种树", R.drawable.ic_tab_tree, 0),
            Triple("林地", R.drawable.ic_tab_forest, 1),
            Triple("统计", R.drawable.ic_tab_stats, 2),
            Triple("我的", R.drawable.ic_tab_profile, 3),
        )
        items.forEach { (label, icon, index) ->
            NavigationBarItem(
                selected = tab == index,
                onClick = { onSelect(index) },
                icon = { Icon(painterResource(icon), contentDescription = label) },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Forest,
                    selectedTextColor = Forest,
                    indicatorColor = SageLight,
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted,
                ),
            )
        }
    }
}
