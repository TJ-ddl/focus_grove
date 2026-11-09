package com.personal.forest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import com.personal.forest.data.Species
import com.personal.forest.ui.theme.Cream
import com.personal.forest.ui.theme.CreamDeep
import com.personal.forest.ui.theme.Forest
import com.personal.forest.ui.theme.Ink
import com.personal.forest.ui.theme.Muted

@Composable
fun Scenery(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(Color(0xFFEAF0E1), Cream))
        )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = Color(0x33E8C75A),
                radius = w * 0.13f,
                center = Offset(w * 0.82f, h * 0.16f),
            )
            val far = Path().apply {
                moveTo(0f, h * 0.70f)
                quadraticBezierTo(w * 0.28f, h * 0.62f, w * 0.52f, h * 0.71f)
                quadraticBezierTo(w * 0.78f, h * 0.80f, w, h * 0.68f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(far, Color(0xFFDCE7D6))
            val near = Path().apply {
                moveTo(0f, h * 0.82f)
                quadraticBezierTo(w * 0.30f, h * 0.74f, w * 0.60f, h * 0.84f)
                quadraticBezierTo(w * 0.85f, h * 0.91f, w, h * 0.82f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(near, Color(0xFFCFDEC5))
        }
        content()
    }
}

/** Resources are looked up by name so adding a species only needs a JSON entry + art files. */
@Composable
fun treeDrawableId(art: String, stage: Int): Int {
    val context = LocalContext.current
    val s = stage.coerceIn(1, 3)
    return remember(art, s) {
        val id = context.resources.getIdentifier("${art}_$s", "drawable", context.packageName)
        if (id != 0) id
        else context.resources.getIdentifier("tree_sapling_$s", "drawable", context.packageName)
    }
}

@Composable
fun TreeArt(
    species: Species,
    stage: Int,
    modifier: Modifier = Modifier,
    withered: Boolean = false,
) {
    val res = treeDrawableId(species.art, stage)
    if (res == 0) return
    val filter = if (withered) {
        remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    } else null
    Image(
        painter = painterResource(res),
        contentDescription = species.name,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = filter,
    )
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.55f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = color,
        shape = MaterialTheme.shapes.large,
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
    ) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun CoinPill(coins: Int) {
    Surface(shape = RoundedCornerShape(50), color = CreamDeep) {
        Text(
            "金币 $coins",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Forest,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    SoftCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (hint != null) Text(hint, color = Muted, fontSize = 11.sp)
        }
    }
}
