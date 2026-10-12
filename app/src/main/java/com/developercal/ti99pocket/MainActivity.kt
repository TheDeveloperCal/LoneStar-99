// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

package com.developercal.ti99pocket

import android.content.Intent
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Slider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.SideEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

private val Ink = Color(0xFF171A1E)
private val Silver = Color(0xFFC7C9C9)
private val SilverLight = Color(0xFFE8E8E5)
private val Panel = Color(0xFF23262A)
private val Muted = Color(0xFF8A9198)
private val Teal = Color(0xFF65D5C7)

private data class UiPalette(val background: Color, val panel: Color, val text: Color, val muted: Color, val accent: Color)
private val LocalUiPalette = compositionLocalOf { UiPalette(Ink, Panel, Color.White, Muted, Teal) }
private fun paletteColors(name: String, dark: Boolean): UiPalette {
    val accent = when (name) {
        "Lone Star Blue" -> Color(0xFF0877EE)
        "Classic TI-99" -> Color(0xFF66CDBE)
        "TI Rainbow" -> Color(0xFF3E75E8)
        "Midnight Console" -> Color(0xFF55D7ED)
        "Warm Retro" -> Color(0xFF597F9D)
        else -> Color(0xFF777777)
    }
    val base = when (name) {
        "Warm Retro" -> if (dark) Color(0xFF211F1A) else Color(0xFFF0E8D7)
        "Monochrome" -> if (dark) Color(0xFF090909) else Color(0xFFF4F4F4)
        "Lone Star Blue" -> if (dark) Color(0xFF111B2B) else Color(0xFFEAF2FC)
        "Classic TI-99" -> if (dark) Color(0xFF181A1B) else Color(0xFFECEBE6)
        "TI Rainbow" -> if (dark) Color(0xFF141A2B) else Color(0xFFEAEFF8)
        "Midnight Console" -> if (dark) Color(0xFF080E17) else Color(0xFFE6EBF0)
        else -> if (dark) Ink else Color(0xFFE9ECEE)
    }
    val surface = when (name) {
        "Warm Retro" -> if (dark) Color(0xFF302C24) else Color(0xFFE1D7C4)
        "Monochrome" -> if (dark) Color(0xFF1B1B1B) else Color.White
        "Lone Star Blue" -> if (dark) Color(0xFF1A2A42) else Color(0xFFD9E7F7)
        "Classic TI-99" -> if (dark) Color(0xFF292C2E) else Color(0xFFDAD9D3)
        "TI Rainbow" -> if (dark) Color(0xFF222E49) else Color(0xFFD5E0F2)
        "Midnight Console" -> if (dark) Color(0xFF151D29) else Color(0xFFD4DCE4)
        else -> if (dark) Panel else Color(0xFFD9DDDF)
    }
    return UiPalette(base, surface, if (dark) Color.White else Color(0xFF171A1E), if (dark) Color(0xFFADB5BC) else Color(0xFF555B60), accent)
}

private fun sourceDirectory(context: android.content.Context) = File(context.filesDir, "programming-lab").apply { mkdirs() }
private fun sourceFiles(context: android.content.Context) = sourceDirectory(context).listFiles()?.filter { it.isFile }?.sortedBy { it.name.lowercase() }.orEmpty()
private fun newSourceFile(context: android.content.Context): File {
    val dir = sourceDirectory(context)
    var n = 1
    while (File(dir, "program-$n.tipy").exists()) n++
    return File(dir, "program-$n.tipy").apply { writeText("PRINT(\"HELLO\")\n2+3*4\n") }
}
internal fun renameSource(file: File, requested: String): File {
    val safe = requested.trim().replace(Regex("[^A-Za-z0-9._-]"), "_").trim('.').ifBlank { "program.tipy" }
    val target = File(file.parentFile, if (safe.contains('.')) safe else "$safe.tipy")
    require(target == file || !target.exists()) { "A file with that name already exists." }
    require(file.renameTo(target)) { "The file could not be renamed." }
    return target
}
internal fun duplicateSource(file: File): File {
    var n = 2
    var target = File(file.parentFile, "${file.nameWithoutExtension} copy.${file.extension}")
    while (target.exists()) { target = File(file.parentFile, "${file.nameWithoutExtension} copy $n.${file.extension}"); n++ }
    file.copyTo(target)
    return target
}

@Composable
private fun ProgrammingLabDialog(files: List<File>, active: File?, text: String, onText: (String) -> Unit,
    onNew: () -> Unit, onOpen: (File) -> Unit, onSave: (File) -> String, onRename: (File, String) -> Unit,
    onDuplicate: (File) -> Unit, onDelete: (File) -> Unit, onImport: () -> Unit, onExport: (File) -> Unit,
    onRun: (String) -> String?, onDismiss: () -> Unit) {
    var renameFile by remember { mutableStateOf<File?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var deleteFile by remember { mutableStateOf<File?>(null) }
    var message by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Programming Lab") },
        text = { Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Local TI PYTHON source files run in the clean-room firmware's built-in REPL. This is a small Python-like language, not Python 3 or authentic TI BASIC. One input line may contain at most 28 characters.", color = LocalUiPalette.current.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onNew) { Text("NEW") }; TextButton(onClick = onImport) { Text("IMPORT") }
                if (active != null) TextButton(onClick = { message = onSave(active) }) { Text("SAVE") }
            }
            if (active != null) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onExport(active) }) { Text("EXPORT") }
                TextButton(onClick = { onRun(text)?.let { message = it } ?: onDismiss() }) { Text("RUN ON CONSOLE") }
            }
            if (files.isEmpty()) Text("No source files yet.") else files.forEach { file ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(file.name, Modifier.weight(1f).clickable { onOpen(file) }.padding(vertical = 5.dp), color = if (active == file) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = { renameFile = file; renameValue = file.name }) { Text("RENAME") }
                    TextButton(onClick = { onDuplicate(file) }) { Text("COPY") }
                    TextButton(onClick = { deleteFile = file }) { Text("DELETE") }
                }
            }
            if (active != null) {
                Text("Editing: ${active.name} · stored locally")
                OutlinedTextField(value = text, onValueChange = onText, modifier = Modifier.fillMaxWidth().height(250.dp), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp), label = { Text("TI PYTHON source") })
                Text("TI PYTHON REPL · expressions, assignments, PRINT(), and # comments. Each source line is entered and executed in order.", fontSize = 12.sp)
                TextButton(onClick = { onText("PRINT(\"HELLO\")\n2+3*4\n") }) { Text("INSERT WORKING EXAMPLE") }
            }
            if (message.isNotBlank()) Text(message)
        } }, confirmButton = { TextButton(onClick = onDismiss) { Text("DONE") } })
    renameFile?.let { file -> AlertDialog(onDismissRequest = { renameFile = null }, title = { Text("Rename source") }, text = { OutlinedTextField(renameValue, { renameValue = it }, label = { Text("File name") }) }, confirmButton = { TextButton(onClick = { runCatching { onRename(file, renameValue) }.onFailure { message = "Rename failed: ${it.message}" }; renameFile = null }) { Text("RENAME") } }, dismissButton = { TextButton(onClick = { renameFile = null }) { Text("CANCEL") } }) }
    deleteFile?.let { file -> AlertDialog(onDismissRequest = { deleteFile = null }, title = { Text("Delete ${file.name}?") }, text = { Text("This removes the local source file.") }, confirmButton = { TextButton(onClick = { onDelete(file); deleteFile = null }) { Text("DELETE") } }, dismissButton = { TextButton(onClick = { deleteFile = null }) { Text("CANCEL") } }) }
}

/** Maps the documented TI PYTHON character set to the real console key matrix. */
internal fun tiPythonKeys(character: Char): List<Int>? {
    val c = character.uppercaseChar()
    val plain = when (c) {
        in 'A'..'Z' -> c.code - 'A'.code
        in '0'..'9' -> 26 + c.digitToInt()
        ' ' -> 41
        '=' -> 36
        '/' -> 40
        ',' -> 38
        '.' -> 37
        else -> null
    }
    if (plain != null) return listOf(plain)
    return when (c) {
        '+' -> listOf(44, 36)
        '-' -> listOf(44, 40)
        '*' -> listOf(44, 34)
        '%' -> listOf(44, 31)
        '(' -> listOf(44, 35)
        ')' -> listOf(44, 26)
        '#' -> listOf(44, 29)
        '"' -> listOf(43, 15) // FCTN+P
        '\'' -> listOf(43, 14) // FCTN+O
        '_' -> listOf(43, 20) // FCTN+U
        else -> null
    }
}

internal fun validateTiPythonSource(source: String): String? {
    val lines = source.split('\n').map { it.trimEnd('\r') }
    if (lines.size > 200) return "A source file may contain at most 200 lines."
    for ((index, line) in lines.withIndex()) {
        if (line.length > 28) return "Line ${index + 1} exceeds the 28-character console input limit."
        if (line.any { tiPythonKeys(it) == null }) return "Line ${index + 1} contains a character this console keyboard cannot type."
    }
    return null
}

private suspend fun sendTiKey(key: Int, frames: Int = 3) {
    NativeCore.nativeSetKey(NativeCore.handle, key, true)
    try { delay(frames * 17L) } finally { NativeCore.nativeSetKey(NativeCore.handle, key, false) }
    delay(3 * 17L)
}

private fun openPdfRenderer(context: android.content.Context, uri: Uri): Pair<ParcelFileDescriptor, PdfRenderer> {
    val descriptor = if (uri.scheme == "file") {
        ParcelFileDescriptor.open(File(requireNotNull(uri.path)), ParcelFileDescriptor.MODE_READ_ONLY)
    } else context.contentResolver.openFileDescriptor(uri, "r") ?: error("Cannot open PDF")
    return descriptor to PdfRenderer(descriptor)
}

private fun importPdfForOfflineUse(context: android.content.Context, sourceUri: Uri): Uri {
    val name = (sourceUri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "manual.pdf")
        .replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "manual.pdf" }
    val directory = File(context.filesDir, "manuals").apply { check(isDirectory || mkdirs()) { "Cannot create the offline manuals folder." } }
    val id = Integer.toHexString(sourceUri.toString().hashCode())
    val destination = File(directory, "$id-$name")
    val temporary = File(directory, "$id-$name.importing")
    val source = context.contentResolver.openInputStream(sourceUri) ?: error("The selected PDF could not be opened.")
    try {
        source.use { input ->
            val header = ByteArray(5)
            var headerSize = 0
            while (headerSize < header.size) {
                val count = input.read(header, headerSize, header.size - headerSize)
                if (count < 0) break
                headerSize += count
            }
            require(headerSize == header.size && header.decodeToString() == "%PDF-") { "The selected file is not a PDF document." }
            FileOutputStream(temporary).use { output ->
                output.write(header)
                val buffer = ByteArray(8192)
                var total = header.size
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(total + count <= 64 * 1024 * 1024) { "PDFs are limited to 64 MB for offline reading." }
                    output.write(buffer, 0, count)
                    total += count
                }
                output.fd.sync()
            }
        }
    } catch (failure: Throwable) {
        temporary.delete()
        throw failure
    }
    check(temporary.renameTo(destination)) { "Could not store the PDF for offline reading." }
    return Uri.fromFile(destination)
}

@Composable
private fun PdfReaderDialog(uri: Uri, page: Int, zoom: Float, onPage: (Int) -> Unit, onZoom: (Float) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val rendererPair = remember(uri) { runCatching { openPdfRenderer(context, uri) }.getOrNull() }
    DisposableEffect(rendererPair) { onDispose { runCatching { rendererPair?.second?.close(); rendererPair?.first?.close() } } }
    val count = rendererPair?.second?.pageCount ?: 0
    val bitmap = remember(rendererPair, page) { runCatching {
        if (rendererPair == null || count == 0) null else rendererPair.second.openPage(page.coerceIn(0, count - 1)).use { pdfPage ->
            val scale = minOf(1200f / pdfPage.width, 1700f / pdfPage.height)
            val width = (pdfPage.width * scale).toInt().coerceAtLeast(1)
            val height = (pdfPage.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { pdfPage.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
        }
    }.getOrNull() }
    DisposableEffect(bitmap) { onDispose { if (bitmap != null && !bitmap.isRecycled) bitmap.recycle() } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("PDF manual · ${if (count == 0) "unavailable" else "${page + 1} / $count"}") },
        text = { Column(Modifier.heightIn(max = 560.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (bitmap != null) Image(bitmap.asImageBitmap(), contentDescription = "PDF page ${page + 1}", modifier = Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()).graphicsLayer(scaleX = zoom, scaleY = zoom))
            else Text("This PDF could not be rendered. Choose a readable PDF file.")
            if (count > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) { TextButton(enabled = page > 0, onClick = { onPage(page - 1) }) { Text("PREVIOUS") }; Text("Page ${page + 1} of $count"); TextButton(enabled = page < count - 1, onClick = { onPage(page + 1) }) { Text("NEXT") } }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Zoom"); Slider(value = zoom, onValueChange = onZoom, valueRange = 1f..3f, modifier = Modifier.weight(1f)); Text("${(zoom * 100).toInt()}%") }
            }
        } }, confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } })
}

data class SoftwareFile(val uri: Uri?, val name: String, val type: String, val assetName: String? = null)

private val builtInPrograms = listOf(
    SoftwareFile(null, "The Texas Trail", "CTG", "games/texas-trail.ctg"),
    SoftwareFile(null, "Titris", "CTG", "games/titris.ctg"),
    SoftwareFile(null, "Sokoban", "CTG", "games/sokoban.ctg"),
)

private fun InputDevice.isExternalPhysicalKeyboard(): Boolean {
    if (isVirtual || (sources and InputDevice.SOURCE_KEYBOARD) != InputDevice.SOURCE_KEYBOARD) return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) isExternal else true
}

class MainActivity : ComponentActivity() {
    private var immersiveEnabled = true
    private val inputRouter = TiInputRouter()
    private val heldHardwareKeys = mutableMapOf<Pair<Int, Int>, List<Int>>()
    private lateinit var inputManager: InputManager
    private var inputListenerRegistered = false
    private var externalKeyboardConnected by mutableStateOf(false)
    private val inputDeviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = updateExternalKeyboardState()
        override fun onInputDeviceChanged(deviceId: Int) = updateExternalKeyboardState()
        override fun onInputDeviceRemoved(deviceId: Int) {
            releasePhysicalDevice(deviceId)
            updateExternalKeyboardState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputManager = getSystemService(INPUT_SERVICE) as InputManager
        immersiveEnabled = getSharedPreferences("software-library", MODE_PRIVATE).getBoolean("immersive", true)
        applyImmersiveMode(immersiveEnabled)
        updateExternalKeyboardState()
        setContent { PocketApp(::persistUriPermission, ::setImmersiveMode, externalKeyboardConnected, inputRouter) }
    }

    private fun setImmersiveMode(enabled: Boolean) {
        immersiveEnabled = enabled
        applyImmersiveMode(enabled)
    }

    private fun applyImmersiveMode(enabled: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, !enabled)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            if (enabled) {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            } else show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode(immersiveEnabled) else inputRouter.releaseAll()
    }

    private fun persistUriPermission(uri: Uri) {
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    override fun onPause() {
        NativeCore.appForeground.value = false
        EmulatorDisplayView.pauseAll()
        inputRouter.releaseAll()
        NativeCore.releaseAllKeys()
        heldHardwareKeys.clear()
        if (inputListenerRegistered) {
            inputManager.unregisterInputDeviceListener(inputDeviceListener)
            inputListenerRegistered = false
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        NativeCore.appForeground.value = true
        if (!inputListenerRegistered) {
            inputManager.registerInputDeviceListener(inputDeviceListener, Handler(Looper.getMainLooper()))
            inputListenerRegistered = true
        }
        updateExternalKeyboardState()
        applyImmersiveMode(immersiveEnabled)
        EmulatorDisplayView.resumeVisibleViews()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val evt = event ?: return super.onKeyDown(keyCode, event)
        val prefs = getSharedPreferences("software-library", MODE_PRIVATE)
        if (!prefs.getBoolean("accept_physical_keyboard", true) || !isExternalKeyboardEvent(evt)) {
            return super.onKeyDown(keyCode, event)
        }
        if (evt.repeatCount > 0) return true
        val keys = mapPhysicalKeyToTiKeys(evt.keyCode, evt.unicodeChar) ?: return super.onKeyDown(keyCode, event)
        val source = evt.deviceId to keyCode
        heldHardwareKeys[source] = keys
        keys.forEach { inputRouter.set("physical:${evt.deviceId}:$keyCode", it, true) }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val evt = event ?: return super.onKeyUp(keyCode, event)
        val keys = heldHardwareKeys.remove(evt.deviceId to keyCode)
            ?: return super.onKeyUp(keyCode, event)
        keys.forEach { inputRouter.set("physical:${evt.deviceId}:$keyCode", it, false) }
        return true
    }

    private fun isExternalKeyboardEvent(event: KeyEvent): Boolean {
        val device = inputManager.getInputDevice(event.deviceId) ?: return false
        return device.isExternalPhysicalKeyboard()
    }

    private fun updateExternalKeyboardState() {
        externalKeyboardConnected = inputManager.inputDeviceIds.any { id ->
            val device = inputManager.getInputDevice(id)
            device?.isExternalPhysicalKeyboard() == true
        }
    }

    private fun releasePhysicalDevice(deviceId: Int) {
        val keys = heldHardwareKeys.filterKeys { it.first == deviceId }.toList()
        keys.forEach { (physical, codes) ->
            val source = "physical:${physical.first}:${physical.second}"
            codes.forEach { inputRouter.set(source, it, false) }
            heldHardwareKeys.remove(physical)
        }
    }
}

internal class TiInputRouter(
    private val emit: (key: Int, down: Boolean) -> Unit = { key, down -> NativeCore.nativeSetKey(NativeCore.handle, key, down) },
) {
    private val ownersByKey = mutableMapOf<Int, MutableSet<String>>()

    @Synchronized
    fun set(source: String, key: Int, down: Boolean) {
        if (down) {
            val owners = ownersByKey.getOrPut(key) { mutableSetOf() }
            if (owners.add(source) && owners.size == 1) emit(key, true)
        } else {
            val owners = ownersByKey[key] ?: return
            if (!owners.remove(source) || owners.isNotEmpty()) return
            ownersByKey.remove(key)
            emit(key, false)
        }
    }

    @Synchronized
    fun releaseAll() {
        ownersByKey.keys.toList().forEach { emit(it, false) }
        ownersByKey.clear()
    }
}

internal fun mapPhysicalKeyToTiKeys(code: Int, unicode: Int): List<Int>? {
    when (code) {
        KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> return listOf(44)
        KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> return listOf(45)
        KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> return listOf(43)
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> return listOf(42)
        KeyEvent.KEYCODE_SPACE -> return listOf(41)
        KeyEvent.KEYCODE_DPAD_LEFT -> return listOf(43, 18) // FCTN+S
        KeyEvent.KEYCODE_DPAD_RIGHT -> return listOf(43, 3) // FCTN+D
        KeyEvent.KEYCODE_DPAD_UP -> return listOf(43, 4) // FCTN+E
        KeyEvent.KEYCODE_DPAD_DOWN -> return listOf(43, 23) // FCTN+X
        KeyEvent.KEYCODE_DEL -> return listOf(43, 27) // FCTN+1 (delete)
        KeyEvent.KEYCODE_FORWARD_DEL -> return listOf(43, 29) // FCTN+3 (erase)
        KeyEvent.KEYCODE_ESCAPE -> return listOf(43, 36) // FCTN+= (QUIT)
    }
    if (unicode != 0) {
        val character = unicode.toChar().lowercaseChar()
        val byCharacter = when (character) {
            in 'a'..'z' -> character.code - 'a'.code
            in '0'..'9' -> 26 + character.digitToInt()
            '=', '+' -> 36
            '.', '>' -> 37
            ',', '<' -> 38
            ';', ':' -> 39
            '/', '-', '?' -> 40
            ' ' -> 41
            '\n', '\r' -> 42
            '!' -> 27; '@' -> 28; '#' -> 29; '$' -> 30; '%' -> 31
            '^' -> 32; '&' -> 33; '*' -> 34; ')' -> 26; '(' -> 35
            else -> null
        }
        if (byCharacter != null) return listOf(byCharacter)
    }
    // Some control-modified key events have no printable Unicode value. Use
    // Android's logical letter/digit key code as a fallback for those events.
    return when (code) {
        in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> listOf(code - KeyEvent.KEYCODE_A)
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> listOf(26 + (code - KeyEvent.KEYCODE_0))
        KeyEvent.KEYCODE_EQUALS -> listOf(36)
        KeyEvent.KEYCODE_PERIOD -> listOf(37)
        KeyEvent.KEYCODE_COMMA -> listOf(38)
        KeyEvent.KEYCODE_SEMICOLON -> listOf(39)
        KeyEvent.KEYCODE_SLASH, KeyEvent.KEYCODE_MINUS -> listOf(40)
        else -> null
    }
}

private val touchSourceIds = AtomicLong()

@Composable
private fun PocketApp(onKeepPermission: (Uri) -> Unit, onImmersiveChanged: (Boolean) -> Unit, externalKeyboardConnected: Boolean, inputRouter: TiInputRouter) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("software-library", android.content.Context.MODE_PRIVATE) }
    var library by remember {
        val imported = preferences.getStringSet("entries", emptySet()).orEmpty().mapNotNull { entry ->
            val uriText = entry.substringBefore('\n')
            val name = entry.substringAfter('\n', "Imported software")
            runCatching { SoftwareFile(Uri.parse(uriText), name, name.substringAfterLast('.', "unknown").uppercase()) }.getOrNull()
        }.sortedBy { it.name.lowercase() }
        mutableStateOf(builtInPrograms + imported)
    }
    var recentIds by remember {
        mutableStateOf(preferences.getString("recent_order", "").orEmpty().split('\n').filter(String::isNotBlank).take(5))
    }
    fun SoftwareFile.libraryId(): String = assetName ?: uri?.toString() ?: name
    val orderedLibrary = remember(library, recentIds) {
        val ranks = recentIds.withIndex().associate { it.value to it.index }
        library.sortedWith(compareBy<SoftwareFile>({ ranks[it.libraryId()] ?: Int.MAX_VALUE }, { it.name.lowercase() }))
    }
    var selected by remember {
        mutableStateOf(recentIds.firstOrNull()?.let { id -> library.firstOrNull { it.libraryId() == id } })
    }
    var mountedCartridgeId by remember { mutableStateOf<String?>(null) }
    var mountedDiskId by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("LoneStar 99 console ready") }
    var paused by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(preferences.getBoolean("muted", false)) }
    var showLibrary by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var infoPage by remember { mutableStateOf("ABOUT") }
    var showSettings by remember { mutableStateOf(false) }
    var keyboardVisible by remember { mutableStateOf(true) }
    var themeMode by remember { mutableStateOf(preferences.getString("theme_mode", "system") ?: "system") }
    var colorPalette by remember { mutableStateOf(preferences.getString("color_palette", "Lone Star Blue") ?: "Lone Star Blue") }
    var hardwareSkin by remember { mutableStateOf(preferences.getString("hardware_skin", "Classic TI-99/4A") ?: "Classic TI-99/4A") }
    var immersive by remember { mutableStateOf(preferences.getBoolean("immersive", true)) }
    var commandRowVisible by remember { mutableStateOf(preferences.getBoolean("command_row", true)) }
    var keyFeedback by remember { mutableStateOf(preferences.getBoolean("key_feedback", true)) }
    var acceptPhysicalKeyboard by remember { mutableStateOf(preferences.getBoolean("accept_physical_keyboard", true)) }
    var showVirtualWithPhysical by remember { mutableStateOf(preferences.getBoolean("show_virtual_with_physical", true)) }
    var keyboardSize by remember { mutableStateOf(preferences.getString("keyboard_size", "Standard") ?: "Standard") }
    var displayScale by remember { mutableFloatStateOf(preferences.getFloat("display_scale", 1f).coerceIn(.65f, 1f)) }
    var pixelFiltering by remember { mutableStateOf(preferences.getBoolean("pixel_filtering", false)) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var audioEnabled by remember { mutableStateOf(preferences.getBoolean("audio_enabled", true)) }
    var audioVolume by remember { mutableFloatStateOf(preferences.getFloat("audio_volume", 1f)) }
    var showLab by remember { mutableStateOf(false) }
    var showPdf by remember { mutableStateOf(false) }
    var labFiles by remember { mutableStateOf(sourceFiles(context)) }
    var activeSource by remember { mutableStateOf<File?>(null) }
    var sourceText by remember { mutableStateOf("") }
    var runTiPython by remember { mutableStateOf<String?>(null) }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfPage by remember { mutableIntStateOf(0) }
    var pdfZoom by remember { mutableFloatStateOf(1f) }
    var pendingExport by remember { mutableStateOf<File?>(null) }
    val io = remember { Executors.newSingleThreadExecutor() }
    val main = remember { Handler(Looper.getMainLooper()) }
    DisposableEffect(Unit) { onDispose { io.shutdownNow() } }
    val mountFile: (SoftwareFile) -> Unit = { file ->
        notice = "Reading ${file.name}…"
        io.execute {
            val result = runCatching {
                val source = file.assetName?.let(context.assets::open)
                    ?: file.uri?.let(context.contentResolver::openInputStream)
                    ?: error("This software entry has no readable source.")
                source.use { stream ->
                    val limit = if (file.type == "DSK") 4 * 1024 * 1024 else 16 * 1024 * 1024
                    readBounded(stream, limit)
                }
            }.mapCatching { bytes ->
                when (file.type) {
                    "DSK" -> require(NativeCore.nativeLoadDisk(NativeCore.handle, 0, bytes, file.libraryId())) { "The disk image could not be mounted." }
                    else -> require(NativeCore.nativeLoadCartridge(NativeCore.handle, bytes)) { "The cartridge is malformed or uses an unsupported format." }
                }
            }
            main.post {
                notice = result.fold({
                    val id = file.libraryId()
                    if (file.type == "DSK") mountedDiskId = id else mountedCartridgeId = id
                    recentIds = (listOf(id) + recentIds).distinct().take(5)
                    preferences.edit().putString("recent_order", recentIds.joinToString("\n")).apply()
                    "${file.name} loaded"
                }, { "Could not load ${file.name}: ${it.message ?: "invalid file"}" })
            }
        }
    }
    val ejectSoftware: (SoftwareFile) -> Unit = { file ->
        val result = runCatching {
            if (file.type == "DSK") {
                require(NativeCore.nativeEjectDisk(NativeCore.handle, 0)) { "The disk could not be ejected." }
                mountedDiskId = null
                notice = "${file.name} ejected. Its writes remain in session memory; reinsert and export to keep them."
            } else {
                require(NativeCore.nativeEjectCartridge(NativeCore.handle)) { "The cartridge could not be ejected." }
                inputRouter.releaseAll()
                paused = false
                EmulatorDisplayView.setUserPaused(false)
                mountedCartridgeId = null
                notice = "${file.name} ejected; console restarted."
            }
        }
        result.onFailure { notice = "Eject failed: ${it.message ?: "the media could not be removed"}" }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Imported software"
            val type = name.substringAfterLast('.', "unknown").uppercase()
            if (type.lowercase() !in setOf("bin", "ctg", "dsk")) {
                notice = "Unsupported extension .$type. Supported formats: BIN, CTG, DSK."
            } else {
                onKeepPermission(uri)
                val file = SoftwareFile(uri, name, type)
                library = (library.filterNot { it.uri == uri } + file)
                preferences.edit().putStringSet("entries", library.filter { it.uri != null }.map { "${it.uri}\n${it.name}" }.toSet()).apply()
                selected = file
                mountFile(file)
            }
        }
    }
    val diskExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) {
            notice = "Disk export cancelled"
        } else {
            val result = runCatching {
                val bytes = NativeCore.nativeGetDiskImage(NativeCore.handle, 0) ?: error("No disk is mounted in DSK1.")
                context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes); it.flush() }
                    ?: error("Android could not create the exported disk file.")
            }
            notice = result.fold({ "Disk image exported" }, { "Disk export failed: ${it.message ?: "write error"}" })
        }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val offlineUri = importPdfForOfflineUse(context, uri)
            pdfUri = offlineUri
            pdfPage = preferences.getInt("pdf_page_${offlineUri}", 0)
            pdfZoom = 1f
            showPdf = true
        }.onFailure { notice = "PDF import failed: ${it.message ?: "the file could not be read"}" }
    }
    val textPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val name = (uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "imported.tipy").replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File(File(context.filesDir, "programming-lab").apply { mkdirs() }, name).let { if (it.extension.isBlank()) File(it.parentFile, "${it.name}.tipy") else it }
            context.contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use(input::copyTo) }
            onKeepPermission(uri); labFiles = sourceFiles(context); activeSource = file; sourceText = file.readText()
        }.onFailure { notice = "Import failed: ${it.message}" }
    }
    val sourceExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val file = pendingExport
        if (uri != null && file != null) runCatching { context.contentResolver.openOutputStream(uri)!!.bufferedWriter().use { it.write(file.readText()) } }.onFailure { notice = "Export failed: ${it.message}" }
        pendingExport = null
    }
    val wide = LocalConfiguration.current.screenWidthDp > 600 || LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val landscapeLayout = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val showVirtualKeyboard = keyboardVisible && (!externalKeyboardConnected || showVirtualWithPhysical)
    val appForeground by NativeCore.appForeground.collectAsState()

    LaunchedEffect(audioEnabled, audioVolume, muted) { EmulatorDisplayView.setAudioEnabled(audioEnabled); EmulatorDisplayView.setVolume(audioVolume); EmulatorDisplayView.setMuted(muted) }
    LaunchedEffect(runTiPython, appForeground) {
        val source = runTiPython ?: return@LaunchedEffect
        if (!appForeground) return@LaunchedEffect
        val problem = validateTiPythonSource(source)
        if (problem != null) {
            notice = problem
            runTiPython = null
            return@LaunchedEffect
        }
        try {
            val lines = source.split('\n').map { it.trimEnd('\r') }.filter { it.isNotBlank() }
            notice = "Starting TI PYTHON…"
            paused = false
            EmulatorDisplayView.setUserPaused(false)
            inputRouter.releaseAll()
            NativeCore.releaseAllKeys()
            NativeCore.nativeReset(NativeCore.handle)
            delay(40 * 17L)
            sendTiKey(41) // dismiss the clean-room startup screen
            delay(220 * 17L) // wait for the console's input menu
            sendTiKey(27, 20) // menu item 1: TI PYTHON
            delay(40 * 17L)
            for (line in lines) {
                for (character in line) {
                    val keys = tiPythonKeys(character) ?: error("The source changed after validation.")
                    try {
                        keys.forEach { NativeCore.nativeSetKey(NativeCore.handle, it, true) }
                        delay(3 * 17L)
                    } finally {
                        keys.asReversed().forEach { NativeCore.nativeSetKey(NativeCore.handle, it, false) }
                    }
                    delay(3 * 17L)
                }
                sendTiKey(42) // Enter submits this REPL statement
                delay(40 * 17L)
            }
            notice = "Ran ${lines.size} source lines in TI PYTHON; results are on the console."
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            runTiPython = null
            notice = "TI PYTHON run stopped when the app paused. Tap RUN ON CONSOLE to start it again."
            throw cancelled
        } finally {
            inputRouter.releaseAll()
            NativeCore.releaseAllKeys()
        }
        runTiPython = null
    }
    val dark = when (themeMode) { "light" -> false; "dark" -> true; else -> isSystemInDarkTheme() }
    val palette = paletteColors(colorPalette, dark)
    BackHandler(enabled = showSettings) { showSettings = false; onImmersiveChanged(immersive) }
    SideEffect {
        (context as? ComponentActivity)?.window?.let { window ->
            val bars = WindowInsetsControllerCompat(window, window.decorView)
            bars.isAppearanceLightStatusBars = !dark
            bars.isAppearanceLightNavigationBars = !dark
            if (!immersive) {
                window.statusBarColor = if (dark) android.graphics.Color.rgb(17, 27, 43) else android.graphics.Color.rgb(234, 242, 252)
                window.navigationBarColor = if (dark) android.graphics.Color.rgb(17, 27, 43) else android.graphics.Color.rgb(234, 242, 252)
            }
        }
    }
    val scheme = if (dark) darkColorScheme(background = palette.background, surface = palette.panel, primary = palette.accent, onSurface = palette.text, secondary = palette.accent, surfaceVariant = palette.panel, onSurfaceVariant = palette.muted) else lightColorScheme(background = palette.background, surface = palette.panel, primary = palette.accent, onSurface = palette.text, secondary = palette.accent, surfaceVariant = palette.panel, onSurfaceVariant = palette.muted)
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalUiPalette provides palette) {
        Surface(Modifier.fillMaxSize(), color = palette.background) {
            if (showSettings) {
                SettingsScreen(
                    themeMode, { themeMode = it; preferences.edit().putString("theme_mode", it).apply() },
                    colorPalette, { colorPalette = it; preferences.edit().putString("color_palette", it).apply() },
                    hardwareSkin, { hardwareSkin = it; if (it == "Minimal Console") keyboardVisible = false else keyboardVisible = true; preferences.edit().putString("hardware_skin", it).apply() },
                    immersive, { immersive = it; preferences.edit().putBoolean("immersive", it).apply(); onImmersiveChanged(it) },
                    commandRowVisible, { commandRowVisible = it; preferences.edit().putBoolean("command_row", it).apply() },
                    keyFeedback, { keyFeedback = it; preferences.edit().putBoolean("key_feedback", it).apply() },
                    acceptPhysicalKeyboard, { enabled ->
                        acceptPhysicalKeyboard = enabled
                        preferences.edit().putBoolean("accept_physical_keyboard", enabled).apply()
                        if (!enabled) { inputRouter.releaseAll() }
                    },
                    showVirtualWithPhysical, { show -> showVirtualWithPhysical = show; preferences.edit().putBoolean("show_virtual_with_physical", show).apply() },
                    externalKeyboardConnected,
                    keyboardSize, { size -> keyboardSize = size; preferences.edit().putString("keyboard_size", size).apply() },
                    displayScale, { scale -> displayScale = scale; preferences.edit().putFloat("display_scale", scale).apply(); EmulatorDisplayView.setDisplayScale(scale) },
                    pixelFiltering, { enabled -> pixelFiltering = enabled; preferences.edit().putBoolean("pixel_filtering", enabled).apply(); EmulatorDisplayView.setPixelFiltering(enabled) },
                    audioEnabled, { audioEnabled = it; preferences.edit().putBoolean("audio_enabled", it).apply() },
                    audioVolume, { audioVolume = it; preferences.edit().putFloat("audio_volume", it).apply(); EmulatorDisplayView.setVolume(it) },
                    onBack = { showSettings = false; onImmersiveChanged(immersive) },
                )
            } else Column(Modifier.fillMaxSize().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing).padding(horizontal = if (wide) 22.dp else 12.dp, vertical = 8.dp)) {
                Header(onInfo = { showInfo = true }, onSettings = { showSettings = true; onImmersiveChanged(false) })
                Spacer(Modifier.height(if (wide) 5.dp else 10.dp))
                if (wide) {
                    Column(Modifier.weight(if (showVirtualKeyboard) .60f else 1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MachineScreen(Modifier.weight(1.15f), notice, hardwareSkin, displayScale.coerceAtLeast(.95f), pixelFiltering, landscape = landscapeLayout)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                CommandBar(onLoad = { selected?.let(mountFile) ?: picker.launch(arrayOf("application/octet-stream", "*/*")) }, onExportDisk = { diskExporter.launch("lonestar99-dsk1.dsk") }, onLibrary = { showLibrary = true }, onRun = { paused = false; EmulatorDisplayView.setUserPaused(false) }, onPause = { paused = !paused; EmulatorDisplayView.setUserPaused(paused) }, onReset = { showResetConfirm = true }, onSave = { saveSnapshot(context.filesDir, notice = { notice = it }) }, onRestore = { restoreSnapshot(context.filesDir, notice = { notice = it }) }, onMute = { muted = !muted; preferences.edit().putBoolean("muted", muted).apply(); EmulatorDisplayView.setMuted(muted) }, muted = muted, paused = paused, visible = commandRowVisible)
                                if (hardwareSkin == "Minimal Console") KeyboardVisibilityControl(keyboardVisible, { keyboardVisible = it }, palette)
                                LibrarySummary(library, { showLibrary = true }, Modifier.weight(1f))
                            }
                        }
                    }
                    if (showVirtualKeyboard) {
                        KeyboardPanel(Modifier.weight(.40f), onKey = { source, key, down -> inputRouter.set(source, key, down) }, keyFeedback = keyFeedback, skin = hardwareSkin, landscape = true, size = keyboardSize)
                    }
                } else {
                    MachineScreen(Modifier.fillMaxWidth().height((LocalConfiguration.current.screenHeightDp * if (hardwareSkin == "Minimal Console") .43f else .34f).coerceIn(220f, 340f).dp), notice, hardwareSkin, displayScale, pixelFiltering)
                    Spacer(Modifier.height(9.dp))
                    CommandBar(onLoad = { selected?.let(mountFile) ?: picker.launch(arrayOf("application/octet-stream", "*/*")) }, onExportDisk = { diskExporter.launch("lonestar99-dsk1.dsk") }, onLibrary = { showLibrary = true }, onRun = { paused = false; EmulatorDisplayView.setUserPaused(false) }, onPause = { paused = !paused; EmulatorDisplayView.setUserPaused(paused) }, onReset = { showResetConfirm = true }, onSave = { saveSnapshot(context.filesDir, notice = { notice = it }) }, onRestore = { restoreSnapshot(context.filesDir, notice = { notice = it }) }, onMute = { muted = !muted; preferences.edit().putBoolean("muted", muted).apply(); EmulatorDisplayView.setMuted(muted) }, muted = muted, paused = paused, visible = commandRowVisible)
                    Spacer(Modifier.height(8.dp))
                    if (hardwareSkin == "Minimal Console") KeyboardVisibilityControl(keyboardVisible, { keyboardVisible = it }, palette)
                    if (showVirtualKeyboard) KeyboardPanel(Modifier.fillMaxWidth(), onKey = { source, key, down -> inputRouter.set(source, key, down) }, keyFeedback = keyFeedback, skin = hardwareSkin, landscape = false, size = keyboardSize)
                    Spacer(Modifier.height(6.dp))
                    LibrarySummary(library, onLibrary = { showLibrary = true })
                }
                if (!wide) Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(6.dp))
                Text("UNOFFICIAL EMULATOR • NOT AFFILIATED WITH TEXAS INSTRUMENTS", Modifier.fillMaxWidth(), color = palette.muted, fontSize = 9.sp, textAlign = TextAlign.Center, letterSpacing = 1.sp)
            }
            }
        }
        }

    if (showLibrary) AlertDialog(
        onDismissRequest = { showLibrary = false },
        title = { Text("Software library") },
        text = { if (library.isEmpty()) Text("Your imported software appears here. Import a file you have permission to use.") else LazyColumn { items(orderedLibrary) { f ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val isMounted = f.libraryId() == mountedCartridgeId || f.libraryId() == mountedDiskId
                Text("${f.name}  ·  ${f.type}${if (isMounted) " · mounted" else ""}", Modifier.weight(1f).clickable { selected = f; showLibrary = false; notice = "${f.name} selected. Use LOAD to mount it." }.padding(vertical = 10.dp))
                if (isMounted) {
                    TextButton(onClick = { ejectSoftware(f) }) { Text("EJECT") }
                }
            }
        } } },
        confirmButton = { TextButton(onClick = { showLibrary = false; picker.launch(arrayOf("application/octet-stream", "*/*")) }) { Text("IMPORT FILE") } },
        dismissButton = { TextButton(onClick = { showLibrary = false }) { Text("DONE") } }
    )
    if (showResetConfirm) AlertDialog(
        onDismissRequest = { showResetConfirm = false },
        title = { Text("Reset the TI-99/4A?") },
        text = { Text("This restarts the emulated console and clears its current unsaved session. Save a machine state first if you want to return to it.") },
        confirmButton = { TextButton(onClick = {
            showResetConfirm = false
            inputRouter.releaseAll()
            NativeCore.releaseAllKeys()
            NativeCore.nativeReset(NativeCore.handle)
            paused = false
            EmulatorDisplayView.setUserPaused(false)
            notice = "Console reset"
        }) { Text("RESET") } },
        dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("CANCEL") } },
    )
    if (showInfo) AlertDialog(
        onDismissRequest = { showInfo = false },
        title = { Text(when (infoPage) { "HOW_TO" -> "How to run software"; "HISTORY" -> "TI-99/4A history"; else -> "About LoneStar 99" }) },
        text = {
            val scroll = rememberScrollState()
            Column(Modifier.heightIn(max = 500.dp).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (infoPage) {
                    "HOW_TO" -> {
                        Text("Use Software Library to choose an included cartridge or import a file you have permission to use. Supported imports are .CTG and raw .BIN cartridges plus .DSK disk images. RPK packages and cassette images are not supported.")
                        Text("After selecting a program, tap LOAD. At the console menu, press 2 to start the cartridge. The Texas Trail shows its instructions before the journey; its numbered choices travel, rest, repair, and trade. Use PAUSE/RESUME without resetting the machine. RESET restarts the emulated console.")
                        Text("Open Software Library to eject mounted media. Cartridge eject returns to the console menu. Disk eject retains writes in this app session; reinsert that same image and use EXPORT DSK to save a separate edited copy. Save a machine state if you need to preserve the in-memory disk shelf across app restarts.")
                        Text("Disk software requires a compatible disk image mounted as DSK1. Authentic TI BASIC and commercial firmware are not bundled. The built-in clean-room firmware runs the included software and the documented compatible programs; compatibility is not guaranteed for every cartridge or disk.")
                        TextButton(onClick = { showInfo = false; mountFile(builtInPrograms.first()) }) { Text("LOAD THE TEXAS TRAIL") }
                    }
                    "HISTORY" -> {
                        Text("Texas Instruments grew from an electronics and semiconductor business into a maker of calculators, instruments, and computers. The TI-99/4 arrived in 1979; the revised TI-99/4A followed in 1981 with a full-travel keyboard and updated video hardware. TI ended production in 1984 as the home-computer market changed rapidly.")
                        Text("The console is unusual: its TMS9900 is a 16-bit CPU related to TI's 990 minicomputer architecture, while the complete system also relies on dedicated video, sound, keyboard, and memory chips. GROM stores code and data; the console ROM interprets GPL, the Graphics Programming Language used by much of the built-in software. Cartridges and the side expansion connector provided programs and peripherals, including disk controllers.")
                        Text("TI BASIC made programming accessible through an interactive language. Cartridges, books, user groups, and later homebrew projects helped people learn and preserve the platform. Emulation keeps software and hardware behavior available on modern devices, while source preservation and clear licensing help the community understand what is being shared.")
                        Text("Sources: Smithsonian National Museum of American History, TI-99/4A collection record; IEEE Spectrum, ‘The Texas Instruments TI-99/4: World’s First 16-Bit Home Computer’; TI-99/4A technical pages by Thierry Nouspikel.", color = LocalUiPalette.current.muted, fontSize = 12.sp)
                        TextButton(onClick = { openExternal(context, "https://americanhistory.si.edu/collections/object/nmah_1064832") }) { Text("SMITHSONIAN COLLECTION RECORD") }
                        TextButton(onClick = { openExternal(context, "https://spectrum.ieee.org/the-texas-instruments-994-worlds-first-16bit-computer") }) { Text("IEEE SPECTRUM ARTICLE") }
                        TextButton(onClick = { openExternal(context, "https://www.unige.ch/medecine/nouspikel/ti99/titechpages.htm") }) { Text("TI-99/4A TECHNICAL PAGES") }
                    }
                    else -> {
                        Text("LoneStar 99 / LS-99 by DeveloperCal\nUnofficial TI-99/4A emulator\nVersion 0.1.0 (1) · © 2026 DeveloperCal")
                        Text("Built in Texas for retro-computing fans.", color = LocalUiPalette.current.accent, fontWeight = FontWeight.SemiBold)
                        Text("Why I made LoneStar 99\n\nI'm a Texan who loves retro Texas computer history. I've always wanted a TI-99/4A emulator for Android that feels like using the original computer while making it easier for a new generation to experience its software.\n\nLoneStar 99 is my independent project to preserve and celebrate that history through accessible emulation, original homebrew projects, and a modern mobile interface.\n\nMy goal is to make vintage computing approachable, encourage people to explore and write their own programs, and share my appreciation for this part of Texas technology history.")
                        Text("This unofficial project is not affiliated with or endorsed by Texas Instruments. The Android app includes original DeveloperCal work and third-party emulator, firmware, and game components under their respective licenses. The integrated application is mixed-license; some third-party terms restrict commercial distribution.", color = LocalUiPalette.current.muted)
                        TextButton(onClick = { infoPage = "HOW_TO" }) { Text("HOW TO RUN GAMES") }
                        TextButton(onClick = { infoPage = "HISTORY" }) { Text("HISTORY OF THE TI-99/4A") }
                        TextButton(onClick = { showInfo = false; showLab = true; labFiles = sourceFiles(context); activeSource = null; sourceText = "" }) { Text("PROGRAMMING LAB") }
                        Text("Manual PDFs are copied into app-private storage for offline reading (up to 64 MB per file). Import only a document you are allowed to keep.", color = LocalUiPalette.current.muted, fontSize = 12.sp)
                        TextButton(onClick = { showInfo = false; pdfPicker.launch(arrayOf("application/pdf")) }) { Text("OPEN A PDF MANUAL") }
                        TextButton(onClick = { openExternal(context, "https://www.99er.net/retrofiles/category.php?id=12&page=1&sort=popular") }) { Text("COMMUNITY MANUAL ARCHIVE") }
                        TextButton(onClick = { showInfo = false; showSettings = true; onImmersiveChanged(false) }) { Text("OPEN SOURCE LICENSES & NOTICES") }
                        TextButton(onClick = { openExternal(context, "https://liberapay.com/DeveloperCal/") }) { Text("SUPPORT LONESTAR 99 ON LIBERAPAY") }
                    }
                }
            }
        },
        dismissButton = { if (infoPage != "ABOUT") TextButton(onClick = { infoPage = "ABOUT" }) { Text("ABOUT") } },
        confirmButton = { TextButton(onClick = { showInfo = false }) { Text("CLOSE") } }
    )

    if (showLab) ProgrammingLabDialog(
        files = labFiles,
        active = activeSource,
        text = sourceText,
        onText = { sourceText = it },
        onNew = { val file = newSourceFile(context); labFiles = sourceFiles(context); activeSource = file; sourceText = file.readText() },
        onOpen = { file -> activeSource = file; sourceText = file.readText() },
        onSave = { file -> runCatching { file.writeText(sourceText); labFiles = sourceFiles(context); "Saved ${file.name}" }.getOrElse { "Save failed: ${it.message}" } },
        onRename = { file, name -> renameSource(file, name).also { labFiles = sourceFiles(context); if (activeSource == file) activeSource = it } },
        onDuplicate = { file -> duplicateSource(file).also { labFiles = sourceFiles(context) } },
        onDelete = { file -> file.delete(); labFiles = sourceFiles(context); if (activeSource == file) { activeSource = null; sourceText = "" } },
        onImport = { textPicker.launch(arrayOf("text/plain", "text/*")) },
        onExport = { file -> pendingExport = file; sourceExporter.launch(file.name) },
        onRun = { source ->
            val problem = validateTiPythonSource(source)
            if (problem != null) problem
            else {
                activeSource?.writeText(source)
                labFiles = sourceFiles(context)
                showLab = false
                runTiPython = source
                null
            }
        },
        onDismiss = { showLab = false }
    )
    if (showPdf && pdfUri != null) PdfReaderDialog(
        uri = pdfUri!!, page = pdfPage, zoom = pdfZoom,
        onPage = { pdfPage = it; preferences.edit().putInt("pdf_page_${pdfUri}", it).apply() }, onZoom = { pdfZoom = it },
        onDismiss = { showPdf = false }
    )
}

@Composable
private fun SettingsScreen(
    theme: String, setTheme: (String) -> Unit,
    palette: String, setPalette: (String) -> Unit,
    skin: String, setSkin: (String) -> Unit,
    immersive: Boolean, setImmersive: (Boolean) -> Unit,
    commands: Boolean, setCommands: (Boolean) -> Unit,
    feedback: Boolean, setFeedback: (Boolean) -> Unit,
    acceptPhysical: Boolean, setAcceptPhysical: (Boolean) -> Unit,
    showVirtualWithPhysical: Boolean, setShowVirtualWithPhysical: (Boolean) -> Unit,
    externalKeyboardConnected: Boolean,
    keyboardSize: String, setKeyboardSize: (String) -> Unit,
    displayScale: Float, setDisplayScale: (Float) -> Unit,
    pixelFiltering: Boolean, setPixelFiltering: (Boolean) -> Unit,
    audio: Boolean, setAudio: (Boolean) -> Unit,
    volume: Float, setVolume: (Float) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalUiPalette.current
    val context = LocalContext.current
    var showLicenses by remember { mutableStateOf(false) }
    val licenseText = remember {
        listOf("LICENSE-SCOPE.md", "THIRD_PARTY_NOTICES.md", "LICENSE.txt").joinToString("\n\n\n") { file ->
            runCatching { context.assets.open("notices/$file").bufferedReader().use { it.readText() } }
                .getOrDefault("$file could not be loaded.")
        }
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹  EMULATOR", color = colors.accent) }
            Text("SETTINGS", Modifier.weight(1f), color = colors.text, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.width(78.dp))
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            item { SettingGroup("APPEARANCE", colors) {
                Text("Theme mode", color = colors.text, fontWeight = FontWeight.SemiBold)
                ChoiceRow(listOf("System default", "Light", "Dark"), when (theme) { "light" -> "Light"; "dark" -> "Dark"; else -> "System default" }, { setTheme(when (it) { "Light" -> "light"; "Dark" -> "dark"; else -> "system" }) }, colors)
                Spacer(Modifier.height(8.dp)); Text("Color palette", color = colors.text, fontWeight = FontWeight.SemiBold)
                ChoiceRow(listOf("Lone Star Blue", "Classic TI-99", "TI Rainbow", "Midnight Console", "Warm Retro", "Monochrome"), palette, setPalette, colors)
                Text("Theme mode sets light or dark surfaces. The palette supplies the accent and supporting hardware colors.", color = colors.muted, fontSize = 12.sp)
            } }
            item { SettingGroup("EMULATOR APPEARANCE", colors) {
                Text("Hardware skin", color = colors.text, fontWeight = FontWeight.SemiBold)
                ChoiceRow(listOf("Classic TI-99/4A", "Lone Star Blue", "Pocket Calculator", "Minimal Console"), skin, setSkin, colors)
            } }
            item { SettingGroup("EMULATOR", colors) {
                PreferenceToggle("Immersive fullscreen", "Hide system bars; swipe from an edge to reveal them.", immersive, setImmersive, colors)
                PreferenceToggle("Show command row", "Display reset, load, save, run, pause, restore, and disk export controls.", commands, setCommands, colors)
            } }
            item { SettingGroup("INPUT", colors) {
                PreferenceToggle("Key press feedback", "Show the pressed state on virtual keyboard keys.", feedback, setFeedback, colors)
                Text("External keyboard", color = colors.text, fontWeight = FontWeight.SemiBold)
                PreferenceToggle("Accept physical keyboard input", "Route supported Bluetooth and USB HID keyboard events through the TI key matrix.", acceptPhysical, setAcceptPhysical, colors)
                PreferenceToggle("Show on-screen keyboard when connected", "Keep the touch keyboard visible while a physical keyboard is attached.", showVirtualWithPhysical, setShowVirtualWithPhysical, colors)
                Text("Physical keyboard status: ${if (externalKeyboardConnected) "Connected" else "Not connected"}", color = if (externalKeyboardConnected) colors.accent else colors.muted, fontSize = 13.sp)
                Text("Pair a Bluetooth keyboard in Android Settings → Connected devices. Connect a standard USB HID keyboard with a compatible USB-C adapter. No Bluetooth permission is needed. Android reports ordinary keyboard events to this app; unsupported keys are ignored. Game controllers and USB serial adapters are not keyboards.", color = colors.muted, fontSize = 12.sp)
                Text("Arrow keys send the TI's FCTN+S/D/E/X cursor-key combinations. Backspace/Delete map to FCTN+1/3. Shift, Ctrl, and Alt-as-FCTN are passed as held matrix keys. Function keys without TI matrix equivalents are ignored.", color = colors.muted, fontSize = 12.sp)
                Spacer(Modifier.height(5.dp))
                Text("On-screen keyboard size", color = colors.text, fontWeight = FontWeight.SemiBold)
                ChoiceRow(listOf("Compact", "Standard", "Large"), keyboardSize, setKeyboardSize, colors)
                Text("Keyboard size changes apply to the touch keys and directional pad without resetting the emulated machine.", color = colors.muted, fontSize = 12.sp)
            } }
            item { SettingGroup("DISPLAY", colors) {
                Text("Display size", color = colors.text, fontWeight = FontWeight.SemiBold)
                Slider(value = displayScale, onValueChange = setDisplayScale, valueRange = .65f..1f)
                Text("${(displayScale * 100).toInt()}% · preserves the full 4:3 image and centers it within the display bezel.", color = colors.muted, fontSize = 12.sp)
                Text("Pixel filtering", color = colors.text, fontWeight = FontWeight.SemiBold)
                ChoiceRow(listOf("Nearest neighbor", "Bilinear"), if (pixelFiltering) "Bilinear" else "Nearest neighbor", { setPixelFiltering(it == "Bilinear") }, colors)
                Text("Nearest neighbor keeps the original sharp pixels; bilinear softens enlarged pixels.", color = colors.muted, fontSize = 12.sp)
            } }
            item { SettingGroup("AUDIO", colors) {
                PreferenceToggle("Emulator sound", "Use Android audio focus and play sound generated by the emulator.", audio, { setAudio(it); EmulatorDisplayView.setAudioEnabled(it) }, colors)
                Text("Master volume", color = colors.text, fontWeight = FontWeight.SemiBold)
                Slider(value = volume, onValueChange = setVolume, enabled = audio, valueRange = 0f..1f)
            } }
            item { SettingGroup("SOFTWARE LIBRARY", colors) {
                Text("Import CTG or raw BIN cartridges, or DSK disk images through the Android document picker. Modified DSK1 media can be exported. RPK and cassette/tape formats are unsupported.", color = colors.text, fontSize = 13.sp)
                Text("TI BASIC requires authentic user-provided firmware. The bundled clean-room firmware provides the console environment, not TI BASIC.", color = colors.muted, fontSize = 12.sp)
            } }
            item { SettingGroup("ABOUT & SUPPORT", colors) {
                Text("LoneStar 99 · LS-99\nUnofficial TI-99/4A Emulator\nDeveloperCal · version 0.1.0 (1)\n© 2026 DeveloperCal", color = colors.text, fontSize = 13.sp, lineHeight = 19.sp)
                Text("Not affiliated with or endorsed by Texas Instruments. This app combines components under different licenses; see LICENSE-SCOPE.md and THIRD_PARTY_NOTICES.md for full license terms and third-party attribution.", color = colors.muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { openExternal(context, "https://liberapay.com/DeveloperCal/") }) { Text("SUPPORT ON LIBERAPAY", color = colors.accent) }
                    TextButton(onClick = { showLicenses = true }) { Text("LICENSES & NOTICES", color = colors.accent) }
                }
            } }
        }
    }
    if (showLicenses) AlertDialog(
        onDismissRequest = { showLicenses = false },
        title = { Text("Open-source licenses") },
        text = { LazyColumn(Modifier.heightIn(max = 520.dp)) { item { Text(licenseText, fontSize = 12.sp, lineHeight = 17.sp) } } },
        confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("CLOSE") } },
    )
}

@Composable
private fun SettingGroup(title: String, colors: UiPalette, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(colors.panel, RoundedCornerShape(10.dp)).border(1.dp, colors.muted.copy(alpha = .25f), RoundedCornerShape(10.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        content()
    }
}

@Composable
private fun ChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit, colors: UiPalette) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        options.forEach { choice ->
                Row(Modifier.fillMaxWidth().background(if (choice == selected) colors.accent.copy(alpha = .16f) else Color.Transparent, RoundedCornerShape(6.dp)).selectable(selected = choice == selected, onClick = { onSelect(choice) }, role = Role.RadioButton).padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (choice == selected) "●" else "○", color = colors.accent, modifier = Modifier.padding(end = 10.dp))
                Text(choice, color = colors.text, fontSize = 13.sp)
                if (choice in setOf("Lone Star Blue", "Classic TI-99", "TI Rainbow", "Midnight Console", "Warm Retro", "Monochrome")) {
                    Spacer(Modifier.weight(1f))
                    val preview = paletteColors(choice, true)
                    Row(Modifier.width(48.dp).height(12.dp).border(1.dp, colors.muted.copy(alpha = .35f), RoundedCornerShape(4.dp))) {
                        if (choice == "TI Rainbow") {
                            listOf(Color(0xFFE63A36), Color(0xFF41B95A), Color(0xFFE4C13A), Color(0xFFC238A6), Color(0xFF3554D6), Color(0xFF39B8C6)).forEach { color ->
                                Box(Modifier.weight(1f).fillMaxHeight().background(color))
                            }
                        } else {
                            Box(Modifier.weight(1f).fillMaxHeight().background(preview.background))
                            Box(Modifier.weight(1f).fillMaxHeight().background(preview.panel))
                            Box(Modifier.weight(1f).fillMaxHeight().background(preview.accent))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceToggle(title: String, detail: String, checked: Boolean, onChanged: (Boolean) -> Unit, colors: UiPalette) {
    Row(Modifier.fillMaxWidth().clickable { onChanged(!checked) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(detail, color = colors.muted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

private fun openExternal(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { Toast.makeText(context, "No browser is available to open this link.", Toast.LENGTH_LONG).show() }
}

@Composable
private fun KeyboardVisibilityControl(visible: Boolean, setVisible: (Boolean) -> Unit, colors: UiPalette) {
    TextButton(onClick = { setVisible(!visible) }, modifier = Modifier.fillMaxWidth()) {
        Text(if (visible) "HIDE KEYBOARD" else "SHOW KEYBOARD", color = colors.accent, letterSpacing = 1.sp)
    }
}

@Composable
private fun Header(onInfo: () -> Unit, onSettings: () -> Unit) {
    val palette = LocalUiPalette.current
    Row(Modifier.fillMaxWidth().height(52.dp).background(Brush.verticalGradient(listOf(palette.panel, Color(0xFFB8BEC2), Color(0xFF969DA1))), RoundedCornerShape(9.dp)).border(1.dp, Color.White.copy(alpha = .45f), RoundedCornerShape(9.dp)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ls99_brand),
            contentDescription = "LS-99 Lone Star emblem",
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("LONESTAR 99", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("UNOFFICIAL TI-99/4A EMULATOR", color = Color(0xFF555B60), fontSize = 8.sp, letterSpacing = .8.sp)
        }
        Text("TI-99", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text("/", color = Color(0xFFE3342F), fontSize = 19.sp, fontWeight = FontWeight.Black)
        Text("4A", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(7.dp))
        Box(Modifier.size(40.dp).semantics { contentDescription = "Settings" }.clickable(role = Role.Button, onClick = onSettings), contentAlignment = Alignment.Center) {
            Text("⚙", color = Ink, fontSize = 17.sp)
        }
        Box(Modifier.size(40.dp).semantics { contentDescription = "About LoneStar 99" }.clickable(role = Role.Button, onClick = onInfo), contentAlignment = Alignment.Center) {
            Text("ⓘ", color = Ink, fontSize = 16.sp)
        }
    }
}

@Composable
private fun MachineScreen(modifier: Modifier, notice: String, skin: String, displayScale: Float, pixelFiltering: Boolean, landscape: Boolean = false) {
    val palette = LocalUiPalette.current
    val skinCase = if (skin == "Lone Star Blue") listOf(Color(0xFF97A8B7), Color(0xFF254F87), Color(0xFF70889B), Color(0xFF16355B)) else if (skin == "Minimal Console") listOf(Color(0xFFE3E4E1), Color(0xFFB7B9B7), Silver, Color(0xFF8A8D8D)) else listOf(Color(0xFFE0E1DF), Color(0xFF9DA0A0), Silver, Color(0xFF85898A))
    Column(modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(if (skin == "Minimal Console") 4.dp else 13.dp)).background(Brush.verticalGradient(skinCase), RoundedCornerShape(if (skin == "Minimal Console") 4.dp else 13.dp)).padding(horizontal = if (landscape) 5.dp else 9.dp, vertical = if (landscape) 4.dp else 9.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().border(2.dp, Color(0xFF626667), RoundedCornerShape(12.dp)).background(Color(0xFF050606), RoundedCornerShape(12.dp)).padding(8.dp)) {
            AndroidView(factory = { EmulatorDisplayView(it).apply { setDisplayScale(displayScale); setPixelFiltering(pixelFiltering) } }, update = { it.setDisplayScale(displayScale); it.setPixelFiltering(pixelFiltering) }, modifier = Modifier.fillMaxSize())
        }
        if (!landscape) {
            Text(notice, Modifier.fillMaxWidth().padding(vertical = 3.dp), color = Color(0xFF292D30), fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            ColorStrip()
        }
    }
}

@Composable
private fun ColorStrip() {
    val colors = listOf(Color(0xFFEF3631), Color(0xFF35B84B), Color(0xFF090909), Color(0xFFE6C531), Color(0xFF188E39), Color(0xFFC8229D), SilverLight, Color(0xFF243CDB), Color(0xFF138945), Color(0xFF8F2B9D), Color(0xFFEF3631), Silver, Color(0xFF3848DB), Color(0xFFE04D55), Color(0xFFE6C531), Color(0xFFEF3631))
    Row(Modifier.fillMaxWidth().height(16.dp).border(1.dp, Color.White.copy(alpha = .55f), RoundedCornerShape(5.dp)), horizontalArrangement = Arrangement.spacedBy(1.dp)) { colors.forEach { Box(Modifier.weight(1f).fillMaxHeight().background(it)) } }
}

@Composable
private fun CommandBar(onLoad: () -> Unit, onExportDisk: () -> Unit, onLibrary: () -> Unit, onRun: () -> Unit, onPause: () -> Unit, onReset: () -> Unit, onSave: () -> Unit, onRestore: () -> Unit, onMute: () -> Unit, muted: Boolean, paused: Boolean, visible: Boolean = true) {
    if (!visible) return
    val palette = LocalUiPalette.current
    Column(Modifier.fillMaxWidth().background(palette.panel, RoundedCornerShape(10.dp)).border(1.dp, Color.White.copy(alpha = .1f), RoundedCornerShape(10.dp)).padding(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Command("RESET", onReset, true, Modifier.weight(1f))
            Command("LOAD", onLoad, true, Modifier.weight(1f))
            Command("SAVE", onSave, true, Modifier.weight(1f))
            Command("RUN", onRun, true, Modifier.weight(1f))
            Command(if (paused) "RESUME" else "PAUSE", onPause, true, Modifier.weight(1f))
            Command("RESTORE", onRestore, true, Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("POWER", color = palette.muted, fontSize = 8.sp, letterSpacing = 1.sp)
            Spacer(Modifier.width(5.dp))
            Box(Modifier.size(6.dp).background(Color(0xFFDA684F), RoundedCornerShape(50)))
            Spacer(Modifier.width(8.dp))
            Text(if (muted) "SOUND OFF" else "SOUND ON", Modifier.clickable(onClick = onMute).padding(4.dp), color = if (muted) palette.muted else palette.accent, fontSize = 8.sp, letterSpacing = .5.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onExportDisk, contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp)) { Text("EXPORT DSK", color = palette.accent, fontSize = 8.sp, letterSpacing = .5.sp) }
            TextButton(onClick = onLibrary, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("SOFTWARE LIBRARY  ›", color = palette.accent, fontSize = 9.sp, letterSpacing = .8.sp) }
        }
    }
}

private data class TouchKey(val label: String, val codes: List<Int>, val grow: Float = 1f)

@Composable
private fun KeyboardPanel(
    modifier: Modifier,
    onKey: (String, Int, Boolean) -> Unit,
    keyFeedback: Boolean,
    skin: String,
    landscape: Boolean,
    size: String,
) {
    val palette = LocalUiPalette.current
    val compact = skin == "Pocket Calculator"
    val keyScale = when (size) { "Compact" -> .82f; "Large" -> 1.18f; else -> 1f }
    val keyRows = listOf(
        listOf(TouchKey("FCTN", listOf(43), 1.45f)) + (1..9).map { n -> TouchKey("$n", listOf(26 + n)) } + TouchKey("0", listOf(26), 1.2f),
        listOf(TouchKey("CTRL", listOf(45), 1.4f)) + "QWERTYUIOP".map { letterKey(it) },
        listOf(TouchKey("SHIFT", listOf(44), 1.55f)) + "ASDFGHJKL".map { letterKey(it) } + TouchKey("ENTER", listOf(42), 1.45f),
        "ZXCVBNM".map { letterKey(it) },
        listOf(TouchKey("=", listOf(36)), TouchKey(",", listOf(38)), TouchKey(".", listOf(37)), TouchKey("SPACE", listOf(41), 4f), TouchKey("/", listOf(40)), TouchKey(";", listOf(39))),
    )
    Column(
        modifier.fillMaxWidth().background(palette.panel, RoundedCornerShape(10.dp))
            .border(1.dp, palette.muted.copy(alpha = .2f), RoundedCornerShape(10.dp)).padding(5.dp),
        verticalArrangement = if (landscape) Arrangement.Center else Arrangement.spacedBy(3.dp * keyScale),
    ) {
        if (landscape) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                KeyRows(keyRows, onKey, keyFeedback, compact, keyScale, Modifier.weight(1f), keyHeight = (if (compact) 20.dp else 21.dp) * keyScale, rowGap = 3.dp)
                DirectionPad(onKey, keyFeedback, compact, buttonSize = (if (compact) 25.dp else 28.dp) * keyScale, modifier = Modifier.padding(start = 6.dp))
            }
        } else {
                KeyRows(keyRows.take(3), onKey, keyFeedback, compact, keyScale, Modifier.fillMaxWidth(), keyHeight = (if (compact) 34.dp else 38.dp) * keyScale, rowGap = 3.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                KeyRows(keyRows.drop(3), onKey, keyFeedback, compact, keyScale, Modifier.weight(1f), keyHeight = (if (compact) 37.dp else 42.dp) * keyScale, rowGap = 3.dp)
                DirectionPad(onKey, keyFeedback, compact, buttonSize = (if (compact) 25.dp else 28.dp) * keyScale)
            }
        }
    }
}

@Composable
private fun KeyRows(
    keyRows: List<List<TouchKey>>,
    onKey: (String, Int, Boolean) -> Unit,
    keyFeedback: Boolean,
    compact: Boolean,
    keyScale: Float,
    modifier: Modifier,
    keyHeight: androidx.compose.ui.unit.Dp,
    rowGap: androidx.compose.ui.unit.Dp,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(rowGap)) {
        keyRows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp * keyScale)) {
                row.forEach { key -> TouchKeyButton(key, onKey, Modifier.weight(key.grow), keyFeedback, compact, keyHeight) }
            }
        }
    }
}

@Composable
private fun DirectionPad(
    onKey: (String, Int, Boolean) -> Unit,
    feedback: Boolean,
    compact: Boolean,
    buttonSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val gap = if (compact) 2.dp else 3.dp
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(buttonSize))
            TouchKeyButton(TouchKey("↑", listOf(43, 4)), onKey, Modifier.size(buttonSize), feedback, compact, buttonSize)
            Spacer(Modifier.size(buttonSize))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
            TouchKeyButton(TouchKey("←", listOf(43, 18)), onKey, Modifier.size(buttonSize), feedback, compact, buttonSize)
            Spacer(Modifier.size(buttonSize))
            TouchKeyButton(TouchKey("→", listOf(43, 3)), onKey, Modifier.size(buttonSize), feedback, compact, buttonSize)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(buttonSize))
            TouchKeyButton(TouchKey("↓", listOf(43, 23)), onKey, Modifier.size(buttonSize), feedback, compact, buttonSize)
            Spacer(Modifier.size(buttonSize))
        }
    }
}

private fun letterKey(letter: Char) = TouchKey(letter.toString(), listOf(letter.uppercaseChar().code - 'A'.code))

@Composable
private fun TouchKeyButton(
    key: TouchKey,
    onKey: (String, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    feedback: Boolean = true,
    compact: Boolean = false,
    height: androidx.compose.ui.unit.Dp = if (compact) 24.dp else 30.dp,
) {
    val palette = LocalUiPalette.current
    val source = remember { "touch:${touchSourceIds.incrementAndGet()}" }
    var pressed by remember { mutableStateOf(false) }
    val shade = if (pressed) Color(0xFF4B555B) else Color(0xFF101316)
    Box(
        modifier.height(height).shadow(2.dp, RoundedCornerShape(if (compact) 3.dp else 5.dp)).background(if (feedback) shade else Color(0xFF101316), RoundedCornerShape(if (compact) 3.dp else 5.dp))
            .border(1.dp, Color.White.copy(alpha = if (pressed) .4f else .12f), RoundedCornerShape(5.dp))
            .pointerInput(key.codes, source) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    key.codes.forEach { onKey(source, it, true) }
                    try { waitForUpOrCancellation() } finally {
                        key.codes.asReversed().forEach { onKey(source, it, false) }
                        pressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(key.label, color = if (feedback && pressed) palette.accent else Color(0xFFE2E4E5), fontWeight = FontWeight.SemiBold, fontSize = if (compact || key.label.length > 3) 7.sp else 12.sp, maxLines = 1)
    }
}

private fun readBounded(input: java.io.InputStream, limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        require(output.size() + count <= limit) { "File is larger than the supported size limit." }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private fun saveSnapshot(directory: File, notice: (String) -> Unit) {
    runCatching {
        val bytes = NativeCore.nativeSaveState(NativeCore.handle) ?: error("The core could not save the machine state.")
        val destination = File(directory, "quick-save.ti99state")
        val temporary = File(directory, "quick-save.ti99state.tmp")
        FileOutputStream(temporary).use { it.write(bytes); it.fd.sync() }
        if (!temporary.renameTo(destination)) {
            destination.delete()
            check(temporary.renameTo(destination)) { "Could not replace the state file." }
        }
    }.fold({ notice("Machine state saved") }, { notice("Save failed: ${it.message ?: "storage error"}") })
}

private fun restoreSnapshot(directory: File, notice: (String) -> Unit) {
    runCatching {
        val source = File(directory, "quick-save.ti99state")
        require(source.isFile) { "No quick-save exists yet." }
        val bytes = source.inputStream().use { readBounded(it, 16 * 1024 * 1024) }
        require(NativeCore.nativeLoadState(NativeCore.handle, bytes)) { "The saved state is invalid or incompatible." }
    }.fold({ notice("Machine state restored") }, { notice("Restore failed: ${it.message ?: "invalid state"}") })
}

@Composable
private fun Command(label: String, action: () -> Unit, enabled: Boolean, modifier: Modifier) {
    val palette = LocalUiPalette.current
    Box(modifier.height(34.dp).shadow(3.dp, RoundedCornerShape(6.dp)).background(if (palette.text == Color.White) Color(0xFF16191C) else Color(0xFFE9ECEE), RoundedCornerShape(6.dp)).border(1.dp, palette.muted.copy(alpha = .3f), RoundedCornerShape(6.dp)).clickable(enabled = enabled, onClick = action), contentAlignment = Alignment.Center) {
        Text(label, color = if (enabled) palette.text else palette.muted.copy(alpha = .65f), fontWeight = FontWeight.SemiBold, fontSize = 8.sp, letterSpacing = .5.sp)
    }
}

@Composable
private fun LibrarySummary(files: List<SoftwareFile>, onLibrary: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalUiPalette.current
    Row(modifier.fillMaxWidth().background(palette.panel, RoundedCornerShape(8.dp)).clickable(onClick = onLibrary).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("LOCAL SOFTWARE", color = palette.muted, fontSize = 9.sp, letterSpacing = 1.sp)
        Spacer(Modifier.weight(1f))
        Text(if (files.isEmpty()) "No files imported" else "${files.size} file${if (files.size == 1) "" else "s"}", color = Color.White, fontSize = 10.sp)
        Spacer(Modifier.width(7.dp))
        Text("›", color = palette.accent)
    }
}

@Composable
private fun LibraryCard(files: List<SoftwareFile>, modifier: Modifier, onSelect: (SoftwareFile) -> Unit, onOpen: () -> Unit) {
    val palette = LocalUiPalette.current
    Column(modifier.fillMaxWidth().background(palette.panel, RoundedCornerShape(10.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SOFTWARE LIBRARY", Modifier.weight(1f), color = palette.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("+ IMPORT", Modifier.clickable(onClick = onOpen).padding(5.dp), color = palette.accent, fontSize = 9.sp, letterSpacing = .8.sp)
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color.White.copy(alpha = .12f))
        LazyColumn {
            items(files) { file ->
                Text("${file.name}  ·  ${file.type}${if (file.assetName != null) "  ·  included" else ""}", Modifier.fillMaxWidth().clickable { onSelect(file) }.padding(vertical = 10.dp), color = palette.text, fontSize = 11.sp)
            }
            item {
                Text("Import a CTG or BIN cartridge, or a DSK disk image you are entitled to use. RPK packages and cassette images are not supported.", Modifier.padding(vertical = 8.dp), color = palette.muted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}
