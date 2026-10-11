package com.example.musictinder.ui.swipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musictinder.Category
import com.example.musictinder.MusicTinderApplication
import com.example.musictinder.TinderUiState
import com.example.musictinder.formatDuration

@Composable
fun SwipeScreen(
    state: TinderUiState,
    folderUri: String?,
    onSelectFolder: () -> Unit,
    onSwipe: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playback = remember {
        (context.applicationContext as MusicTinderApplication).container.playback
    }
    var autoPlay by rememberSaveable { mutableStateOf(true) }

    val isPlaying by playback.isPlaying.collectAsStateWithLifecycle()
    val positionMs by playback.positionMs.collectAsStateWithLifecycle()
    val durationMs by playback.durationMs.collectAsStateWithLifecycle()
    val isBuffering by playback.isBuffering.collectAsStateWithLifecycle()

    val current = state.current

    // Reproduce automáticamente la tarjeta que está en pantalla.
    LaunchedEffect(current?.uri, autoPlay) {
        val uri = current?.uri
        if (uri != null) playback.load(uri, autoPlay) else playback.pause()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Autoplay y pista actual
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (current != null) "Evaluando lista" else "Tu música",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = { autoPlay = !autoPlay }) {
                Text(
                    text = if (autoPlay) "Autoplay: SÍ" else "Autoplay: NO",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        when {
            folderUri == null -> {
                NoFolderState(onSelectFolder, Modifier.weight(1f))
            }

            state.songs.isEmpty() -> {
                EmptyPoolState(onSelectFolder, Modifier.weight(1f))
            }

            current == null -> {
                AllSuperlikedState(
                    onSelectFolder = onSelectFolder,
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {
                TinderCard(
                    track = current,
                    modifier = Modifier.weight(1f),
                    onReject = { onSwipe(Category.REJECT) },
                    onLike = { onSwipe(Category.LIKE) },
                    onSuperlike = { onSwipe(Category.SUPERLIKE) }
                )
            }
        }

        // Barra de progreso
        val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
        Slider(
            value = progress,
            onValueChange = { fraction ->
                playback.seekTo((fraction * durationMs).toLong())
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Controles de transporte
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { playback.seekBackward30() }, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Default.Replay30,
                    contentDescription = "Retroceder 30 segundos",
                    modifier = Modifier.size(36.dp)
                )
            }

            Surface(
                onClick = { playback.togglePlayPause() },
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    when {
                        isBuffering -> LinearProgressIndicator(modifier = Modifier.size(28.dp))
                        isPlaying -> Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pausar",
                            modifier = Modifier.size(32.dp)
                        )
                        else -> Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            IconButton(onClick = { playback.seekForward30() }, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Default.Forward30,
                    contentDescription = "Adelantar 30 segundos",
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Text(
            text = "Desliza: ← No   ·   Me gusta →   ·   ↑ Super",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NoFolderState(onSelectFolder: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Elige una carpeta de música",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Solo se reproducirá el audio que esté en esa carpeta (y sus subcarpetas).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSelectFolder) {
            Text("Elegir carpeta")
        }
    }
}

@Composable
private fun EmptyPoolState(onSelectFolder: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No se encontraron archivos de audio",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Puedes elegir otra carpeta o volver a intentarlo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSelectFolder) {
            Text("Cambiar carpeta")
        }
    }
}

@Composable
private fun AllSuperlikedState(onSelectFolder: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Dejaste de evaluar esta lista",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Todas las canciones disponibles tienen un superlike.\nRevisa la lista de Superlikes para quitar algunas y que vuelvan a aparecer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSelectFolder) {
            Text("Cambiar carpeta")
        }
    }
}