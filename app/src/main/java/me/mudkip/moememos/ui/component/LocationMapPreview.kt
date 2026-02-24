package me.mudkip.moememos.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.MemoLocation
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh

private data class TileInfo(
    val tileX: Int,
    val tileY: Int,
    val zoom: Int,
    val offsetXPx: Float,
    val offsetYPx: Float
)

private fun computeTile(lat: Double, lng: Double, zoom: Int): TileInfo {
    val n = 2.0.pow(zoom)
    val xExact = (lng + 180.0) / 360.0 * n
    val latRad = lat * PI / 180.0
    val yExact = (1.0 - ln(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / PI) / 2.0 * n
    val tileX = floor(xExact).toInt()
    val tileY = floor(yExact).toInt()
    val offsetXPx = ((xExact - tileX) * 256).toFloat()
    val offsetYPx = ((yExact - tileY) * 256).toFloat()
    return TileInfo(tileX, tileY, zoom, offsetXPx, offsetYPx)
}

private fun tileUrl(zoom: Int, x: Int, y: Int): String {
    return "https://tile.openstreetmap.org/$zoom/$x/$y.png"
}

private fun openMapsIntent(location: MemoLocation): Intent {
    val label = location.placeholder.ifEmpty { "Location" }
    val uri = Uri.parse(
        "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}(${Uri.encode(label)})"
    )
    return Intent(Intent.ACTION_VIEW, uri)
}

@Composable
private fun TileMapBox(
    location: MemoLocation,
    zoom: Int,
    widthDp: Dp,
    heightDp: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val tile = remember(location.latitude, location.longitude, zoom) {
        computeTile(location.latitude, location.longitude, zoom)
    }

    val widthPx = with(density) { widthDp.toPx() }
    val heightPx = with(density) { heightDp.toPx() }
    val tileSizePx = 256f
    // How many tiles we need in each direction to fill the box
    val tilesX = (widthPx / tileSizePx).toInt() + 2
    val tilesY = (heightPx / tileSizePx).toInt() + 2
    // Offset to center the point in the box
    val panX = widthPx / 2f - tile.offsetXPx
    val panY = heightPx / 2f - tile.offsetYPx
    val tileSizeDp = with(density) { tileSizePx.toDp() }

    Box(
        modifier = modifier
            .size(widthDp, heightDp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE8E8E8))
    ) {
        // Render grid of tiles
        for (dy in -(tilesY / 2)..(tilesY / 2)) {
            for (dx in -(tilesX / 2)..(tilesX / 2)) {
                val tx = tile.tileX + dx
                val ty = tile.tileY + dy
                val offsetX = panX + dx * tileSizePx
                val offsetY = panY + dy * tileSizePx

                AsyncImage(
                    model = tileUrl(tile.zoom, tx, ty),
                    contentDescription = null,
                    modifier = Modifier
                        .size(tileSizeDp)
                        .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) },
                    contentScale = ContentScale.FillBounds
                )
            }
        }

        // Pin marker centered
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-12).dp)
                .size(32.dp),
            tint = Color(0xFFE53935)
        )
    }
}

@Composable
fun LocationMapPreview(
    location: MemoLocation,
    modifier: Modifier = Modifier,
    mapHeight: Dp = 140.dp,
    showRemoveButton: Boolean = false,
    onRemove: () -> Unit = {}
) {
    val context = LocalContext.current
    val label = location.placeholder.ifEmpty {
        "%.4f, %.4f".format(location.latitude, location.longitude)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight)
                .clickable {
                    try {
                        context.startActivity(openMapsIntent(location))
                    } catch (_: Exception) { }
                }
        ) {
            TileMapBox(
                location = location,
                zoom = 15,
                widthDp = 400.dp,
                heightDp = mapHeight,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                maxLines = 1
            )
            if (showRemoveButton) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.remove_location),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LocationMapPreviewCompact(
    location: MemoLocation,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val label = location.placeholder.ifEmpty {
        "%.4f, %.4f".format(location.latitude, location.longitude)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                try {
                    context.startActivity(openMapsIntent(location))
                } catch (_: Exception) { }
            }
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TileMapBox(
            location = location,
            zoom = 14,
            widthDp = 90.dp,
            heightDp = 60.dp,
            modifier = Modifier.clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
        )

        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(14.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
            maxLines = 1
        )
    }
}
