package com.example

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.CopyPadViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CopyPad", appName)
  }

  @Test
  fun `auto copy on exit copies non-empty text to clipboard`() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = CopyPadViewModel(context)
    val testText = "Important meeting notes to copy on exit"

    viewModel.updateText(testText)
    assertEquals(testText, viewModel.text.value)

    viewModel.handleAppExit(context)

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    assertNotNull(clipboard.primaryClip)
    assertEquals(testText, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
  }
}
