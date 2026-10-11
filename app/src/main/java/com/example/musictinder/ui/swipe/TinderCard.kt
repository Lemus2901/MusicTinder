package com.example.musictinder.ui.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musictinder.Category
import com.example.musictinder.Track
import com.example.musictinder.formatDuration
import com.example.musictinder.ui.theme.LikeGreen
import com.example.musictinder.ui.theme.PinkPrimary
import com.example.musictinder.ui.theme.RejectRed
import com.example.musictinder.ui.theme.StarYellow
import kotlinx.coroutines.launch

private val CardPalettes = listOf(
    listOf(Color(0xFF1B2A4A), Color(0xFF4A1B33)),
    listOf(Color(0xFF2A1F33), Color(0xFF1B3322)),
    listOf(Color(0xFF331F1F), Color(0xFF1F2A33)),
    listOf(Color(0xFF1F3326), Color(0xFF33241F)),
    listOf(Color(0xFF262033), Color(0xFF331F2A)),
    listOf(Color(0xFF30301A), Color(0xFF1F2E33))
)

@Composable
fun TinderCard(
    track: Track,
    modifier: Modifier = Modifier,
    onReject: () -> Unit,
    onLike: () -> Unit,
    onSuperlike: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var retired by remember(track.uri) { mutableStateOf(false) }
    val animX = remember(track.uri) { Animatable(0f) }
    val animY = remember(track.uri) { Animatable(0f) }

    fun exit(category: Category) {
        if (retired) return
        retired = true
        scope.launch {
            val px = with(density) { 280.dp.toPx() }
            val dirX = if (category == Category.LIKE) 1f else if (category == Category.REJECT) -1f else 0f
            val dirY = if (category == Category.SUPERLIKE) -1f else 0f
            animX.snapTo(animX.value)
            launch {
                animX.animateTo(dirX * px, tween(300, easing = FastOutLinearInEasing))
            }
            launch {
                animY.animateTo(dirY * px, tween(300, easing = FastOutLinearInEasing))
            }
            when (category) {
                Category.REJECT -> onReject()
                Category.LIKE -> onLike()
                Category.SUPERLIKE -> onSuperlike()
            }
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 2.dp)) {
            val cardWidthPx = with(density) { maxWidth.toPx() }
            val cardHeightPx = with(density) { maxHeight.toPx() }
            val likeAlpha = (animX.value / (cardWidthPx * 0.5f)).coerceIn(0f, 1f)
            val rejectAlpha = (-animX.value / (cardWidthPx * 0.5f)).coerceIn(0f, 1f)
            val superlikeAlpha = (-animY.value / (cardHeightPx * 0.4f)).coerceIn(0f, 1f)
            val palette = CardPalettes[track.uri.hashCode().mod(CardPalettes.size)]

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = animX.value
                        translationY = animY.value
                        rotationZ = animX.value * 0.006f
                    }
                    .pointerInput(track.uri) {
                        detectDragGestures(
                            onDrag = { change, amount ->
                                change.consume()
                                if (!retired) {
                                    scope.launch {
                                        animX.snapTo(animX.value + amount.x)
                                        animY.snapTo(animY.value + amount.y)
                                    }
                                }
                            },
                            onDragEnd = {
                                if (retired) return@detectDragGestures
                                val x = animX.value
                                val y = animY.value
                                val category = when {
                                    x > cardWidthPx * 0.35f -> Category.LIKE
                                    x < -cardWidthPx * 0.35f -> Category.REJECT
                                    y < -cardHeightPx * 0.35f -> Category.SUPERLIKE
                                    else -> null
                                }
                                if (category != null) {
                                    exit(category)
                                } else {
                                    scope.launch {
                                        launch {
                                            animX.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                        launch {
                                            animY.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    },
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(palette))
                ) {
                    // Nota de fondo
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(140.dp)
                            .align(Alignment.Center)
                    )

                    // Nombre e info de la canción
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = track.artist ?: "Artista desconocido",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (track.album != null) {
                            Text(
                                text = "${track.album} · ${formatDuration(track.durationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = formatDuration(track.durationMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Sellos de decisión
                    Sello(
                        text = "ME GUSTA",
                        color = LikeGreen,
                        alpha = likeAlpha,
                        modifier = Modifier.align(Alignment.TopStart).padding(top = 18.dp, start = 18.dp),
                        rotate = -12
                    )
                    Sello(
                        text = "NO",
                        color = RejectRed,
                        alpha = rejectAlpha,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 18.dp),
                        rotate = 12
                    )
                    Sello(
                        text = "SUPERLIKE",
                        color = StarYellow,
                        alpha = superlikeAlpha,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
                        rotate = 0
                    )
                }
            }
        }

        // Botones de acción
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(
                color = RejectRed,
                icon = Icons.Default.Close,
                label = "Rechazar"
            ) { exit(Category.REJECT) }

            ActionButton(
                color = StarYellow,
                icon = Icons.Default.Star,
                label = "Superlike"
            ) { exit(Category.SUPERLIKE) }

            ActionButton(
                color = LikeGreen,
                icon = Icons.Default.Favorite,
                label = "Me gusta"
            ) { exit(Category.LIKE) }
        }
    }
}

@Composable
private fun ActionButton(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = color)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White)
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun Sello(
    text: String,
    color: Color,
    alpha: Float,
    modifier: Modifier = Modifier,
    rotate: Int
) {
    Box(
        modifier = modifier.graphicsLayer {
            val rotation = rotate * alpha
            rotationZ = rotation
            this.alpha = alpha
        },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(3.dp, color)
        ) {
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}