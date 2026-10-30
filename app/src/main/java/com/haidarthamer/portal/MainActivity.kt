package com.haidarthamer.portal

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PortalScreen { uri -> inspect(uri) } }
    }

    private fun inspect(uri: Uri): String {
        val name = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "Selected file"
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(4096).also { buffer ->
                    stream.read(buffer)
                }
                when {
                    header.size >= 2 && header[0] == 0x4D.toByte() && header[1] == 0x5A.toByte() ->
                        inspectPe(name, header)
                    header.size >= 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() ->
                        inspectZip(name, uri)
                    else -> "Unknown file format\n\n$name\n\nNo supported signature detected."
                }
            } ?: "Could not open the selected file."
        } catch (e: Exception) {
            "Inspection failed safely: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    private fun inspectPe(name: String, h: ByteArray): String {
        if (h.size < 64) return "PE file: $name\nTruncated DOS header."
        val b = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN)
        val peOffset = b.getInt(0x3C)
        if (peOffset < 0 || peOffset > 1_048_576) return "PE file: $name\nInvalid PE header offset: $peOffset"
        if (peOffset + 24 > h.size) return "PE file: $name\nDOS header found. PE header lies beyond the initial 4 KB preview; deeper streaming inspection is planned."
        if (b.getInt(peOffset) != 0x00004550) return "PE file: $name\nMZ signature found, but PE signature is invalid."
        val machine = b.getShort(peOffset + 4).toInt() and 0xffff
        val sectionCount = b.getShort(peOffset + 6).toInt() and 0xffff
        val optSize = b.getShort(peOffset + 20).toInt() and 0xffff
        val magic = if (peOffset + 26 <= h.size) b.getShort(peOffset + 24).toInt() and 0xffff else 0
        val arch = when (machine) {
            0x014c -> "x86 (32-bit)"
            0x8664 -> "x86-64"
            0xAA64 -> "ARM64"
            0x01c4 -> "ARM/Thumb-2"
            else -> "Unknown (0x${machine.toString(16)})"
        }
        val format = when (magic) { 0x10b -> "PE32"; 0x20b -> "PE32+"; else -> "Unknown / not in preview" }
        return "WINDOWS PE\n\n$name\n\nArchitecture: $arch\nOptional header: $format\nSections: $sectionCount\nOptional header size: $optSize\n\nStatic header inspection only. No code is executed."
    }

    private fun inspectZip(name: String, uri: Uri): String {
        val entries = mutableListOf<String>()
        val abis = linkedSetOf<String>()
        var hasManifest = false
        var hasPayloadApp = false
        var machO = false
        var inflatedBytes = 0L
        contentResolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(raw).use { zip ->
                var entry = zip.nextEntry
                val scratch = ByteArray(8192)
                while (entry != null && entries.size < 1000 && inflatedBytes < 32L * 1024 * 1024) {
                    val path = entry.name
                    if (!entry.isDirectory) {
                        entries += path
                        if (path == "AndroidManifest.xml") hasManifest = true
                        Regex("lib/([^/]+)/[^/]+\\.so").find(path)?.groupValues?.get(1)?.let { abis += it }
                        if (path.startsWith("Payload/") && path.contains(".app/")) hasPayloadApp = true
                        if (path.endsWith(".dylib") || path.endsWith(".framework/")) machO = true
                    }
                    var n = zip.read(scratch)
                    while (n > 0) {
                        inflatedBytes += n
                        if (inflatedBytes >= 32L * 1024 * 1024) break
                        n = zip.read(scratch)
                    }
                    if (inflatedBytes >= 32L * 1024 * 1024) break
                    entry = zip.nextEntry
                }
            }
        }
        val isIpa = hasPayloadApp || name.endsWith(".ipa", true)
        val title = if (isIpa) "IOS IPA ARCHIVE" else "ANDROID APK / ZIP"
        return buildString {
            append("$title\n\n$name\n\nFiles inspected: ${entries.size}")
            if (isIpa) {
                append("\nPayload app bundle: ${if (hasPayloadApp) "found" else "not confirmed"}")
                append("\nMach-O-related files: ${if (machO) "found" else "not confirmed in filename scan"}")
                append("\n\nIPA is not directly convertible to APK.")
            } else {
                append("\nAndroidManifest.xml: ${if (hasManifest) "found" else "not found"}")
                append("\nNative library ABIs: ${if (abis.isEmpty()) "none found" else abis.joinToString()}")
            }
            append("\n\nArchive inventory preview:")
            entries.take(24).forEach { append("\n• $it") }
            if (entries.size > 24) append("\n… and more")
            append("\n\nArchive metadata inspection only. Files are not installed or executed.")
        }
    }
}

@Composable
private fun PortalScreen(onInspect: (Uri) -> String) {
    var result by remember { mutableStateOf("Choose a Windows EXE/DLL, Android APK, or iOS IPA to inspect it locally.") }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            result = onInspect(uri)
            busy = false
        }
    }
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
                Text("PORTAL", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Text("LOCAL FILE INTELLIGENCE", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(28.dp))
                Text("Inspect. Understand. Port.", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Offline-first analysis foundation. No root, account, or API key required.")
                Spacer(Modifier.height(24.dp))
                Button(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("SELECT FILE")
                }
                Spacer(Modifier.height(20.dp))
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Card(Modifier.fillMaxWidth()) {
                    Text(result, Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(20.dp))
                Text("v0.1 • Local static inspection • No execution", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
