package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CopyPadPreferences
import com.example.model.ClippingItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CopyPadViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = CopyPadPreferences(application)

    private val _text = MutableStateFlow(prefs.getDraftText())
    val text: StateFlow<String> = _text.asStateFlow()

    private val _isAutoCopyEnabled = MutableStateFlow(prefs.isAutoCopyEnabled())
    val isAutoCopyEnabled: StateFlow<Boolean> = _isAutoCopyEnabled.asStateFlow()

    private val _lastCopiedInfo = MutableStateFlow(prefs.getLastCopiedInfo())
    val lastCopiedInfo: StateFlow<Triple<String, Long, String>?> = _lastCopiedInfo.asStateFlow()

    private val _history = MutableStateFlow(prefs.getHistory())
    val history: StateFlow<List<ClippingItem>> = _history.asStateFlow()

    private val _justCopied = MutableStateFlow(false)
    val justCopied: StateFlow<Boolean> = _justCopied.asStateFlow()

    fun updateText(newText: String) {
        _text.value = newText
        prefs.saveDraftText(newText)
    }

    fun setAutoCopyEnabled(enabled: Boolean) {
        _isAutoCopyEnabled.value = enabled
        prefs.setAutoCopyEnabled(enabled)
    }

    /**
     * Copies text to clipboard.
     * Can be invoked from the explicit UI button or automatically when exiting.
     */
    fun copyToClipboard(
        context: Context,
        source: String = "Manual Copy",
        showToast: Boolean = true
    ): Boolean {
        val currentText = _text.value
        if (currentText.isEmpty()) {
            if (showToast) {
                Toast.makeText(context, "Nothing to copy", Toast.LENGTH_SHORT).show()
            }
            return false
        }

        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                val clip = ClipData.newPlainText("CopyPad", currentText)
                clipboard.setPrimaryClip(clip)

                // Record copy in preferences and state
                prefs.recordCopy(currentText, source)
                _lastCopiedInfo.value = prefs.getLastCopiedInfo()
                _history.value = prefs.getHistory()

                // Trigger UI button animation
                _justCopied.value = true
                viewModelScope.launch {
                    delay(1800)
                    _justCopied.value = false
                }

                // Show toast for pre-Android 13 (Android 13+ has native clipboard preview)
                if (showToast && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Called when the app is exiting in any way (onPause, onStop, or BackHandler).
     * Automatically copies the non-empty text to clipboard if enabled.
     */
    fun handleAppExit(context: Context) {
        if (!_isAutoCopyEnabled.value) return
        val currentText = _text.value
        if (currentText.isNotEmpty()) {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                if (clipboard != null) {
                    val clip = ClipData.newPlainText("CopyPad Auto-Copy", currentText)
                    clipboard.setPrimaryClip(clip)
                    prefs.recordCopy(currentText, "Auto-Copy on Exit")
                    prefs.saveDraftText(currentText)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Pastes text from clipboard into the text box.
     */
    fun pasteFromClipboard(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val item = clipboard?.primaryClip?.getItemAt(0)
            val clipText = item?.text?.toString()
            if (!clipText.isNullOrEmpty()) {
                val updated = if (_text.value.isEmpty()) clipText else "${_text.value}\n$clipText"
                updateText(updated)
                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to paste", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearText() {
        updateText("")
    }

    fun restoreItem(item: ClippingItem) {
        updateText(item.text)
    }

    fun clearAllHistory() {
        prefs.clearHistory()
        _history.value = emptyList()
    }

    // Text manipulation utilities
    fun transformUpperCase() {
        updateText(_text.value.uppercase())
    }

    fun transformLowerCase() {
        updateText(_text.value.lowercase())
    }

    fun transformTitleCase() {
        val transformed = _text.value.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
        updateText(transformed)
    }

    fun transformTrimWhitespace() {
        val lines = _text.value.lines().map { it.trim() }
        updateText(lines.joinToString("\n").trim())
    }

    fun transformAddBullets() {
        val lines = _text.value.lines()
        val bulleted = lines.joinToString("\n") { line ->
            if (line.isBlank() || line.startsWith("• ")) line else "• $line"
        }
        updateText(bulleted)
    }

    fun shareText(context: Context) {
        val currentText = _text.value
        if (currentText.isEmpty()) {
            Toast.makeText(context, "Nothing to share", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, currentText)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
}
