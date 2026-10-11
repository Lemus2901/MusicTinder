package com.example.musictinder.media

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import com.example.musictinder.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Escanea una carpeta elegida con Storage Access Framework y devuelve los
 * archivos de audio de la carpeta y sus subcarpetas. No necesita permisos
 * de almacenamiento: usa el permiso persistente del árbol de documentos.
 */
class MusicScanner {

    private class FoundAudio(val uri: Uri, val displayName: String)

    suspend fun scan(context: Context, treeUri: Uri): List<Track> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val documents = mutableListOf<FoundAudio>()
        try {
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            collectDocuments(resolver, treeUri, treeDocId, documents)
        } catch (e: Exception) {
            // Si el árbol no responde documento a documento, vacío.
        }
        documents.mapNotNull { audio ->
            readMetadata(resolver, audio)
        }
    }

    private fun collectDocuments(
        resolver: ContentResolver,
        treeUri: Uri,
        docId: String,
        out: MutableList<FoundAudio>
    ) {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val childId = cursor.getString(idColumn)
                val mime = if (mimeColumn >= 0) cursor.getString(mimeColumn) else null
                val name = if (nameColumn >= 0) cursor.getString(nameColumn) ?: "" else ""
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    collectDocuments(resolver, treeUri, childId, out)
                } else if (isAudio(mime, name)) {
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                    out.add(FoundAudio(childUri, name))
                }
            }
        }
    }

    private fun isAudio(mime: String?, displayName: String): Boolean {
        if (mime == null) return hasAudioExtension(displayName)
        if (mime.startsWith("audio/")) return true
        // Algunos proveedores no reportan bien el tipo; usamos la extensión.
        return hasAudioExtension(displayName)
    }

    private val audioExtensions = setOf(
        "mp3", "m4a", "m4b", "aac", "ogg", "oga", "opus", "flac", "wav", "wave",
        "wma", "amr", "aiff", "aif", "mp2", "mpga", "3gp", "m4p", "awb", "caf", "mid", "midi"
    )

    private fun hasAudioExtension(displayName: String): Boolean {
        val ext = displayName.substringAfterLast('.', "").lowercase()
        return ext in audioExtensions
    }

    private fun readMetadata(resolver: ContentResolver, audio: FoundAudio): Track? {
        val retriever = MediaMetadataRetriever()
        return try {
            val fd = resolver.openFileDescriptor(audio.uri, "r") ?: return null
            fd.use {
                try {
                    retriever.setDataSource(it.fileDescriptor)
                } catch (e: Exception) {
                    return null
                }
            }
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: audio.displayName.substringBeforeLast('.')
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durationText = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationText?.toLongOrNull() ?: 0L
            Track(
                uri = audio.uri.toString(),
                title = title.ifBlank { audio.displayName.substringBeforeLast('.') },
                artist = artist?.ifBlank { null }?.takeIf { it != "<unknown>" },
                album = album?.ifBlank { null }?.takeIf { it != "<unknown>" },
                durationMs = durationMs
            )
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}