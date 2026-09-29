package com.israadev.nuxlauncher.core.game.input

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import org.lwjgl.glfw.CallbackBridge

/**
 * Hidden EditText input view that bridges Android IME (Gboard, Samsung Keyboard, etc.)
 * directly to GLFW/Minecraft character callbacks.
 */
class TouchCharInput @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : EditText(context, attrs, defStyleAttr) {

    interface InputListener {
        fun onSend(char: Char)
        fun onBackspace()
        fun onEnter()
    }

    companion object {
        const val TEXT_FILLER = "                              "
        private var sActiveInput: TouchCharInput? = null

        @JvmStatic
        fun disableActiveInput() {
            sActiveInput?.disableKeyboard()
        }

        @JvmStatic
        fun isActive(): Boolean = sActiveInput != null
    }

    private var mIsDoingInternalChanges = false
    private var mListener: InputListener? = null

    init {
        setup()
    }

    private fun setup() {
        setOnEditorActionListener { _, _, _ ->
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(windowToken, 0)
            mListener?.onEnter()
            clear()
            false
        }
        clear()
        disableKeyboard()
    }

    fun enableKeyboard() {
        enable()
        sActiveInput = this
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        clear()
    }

    fun disableKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(windowToken, 0)
        clearFocus()
        visibility = GONE
        isEnabled = false
        if (sActiveInput === this) {
            sActiveInput = null
        }
    }

    fun clear() {
        mIsDoingInternalChanges = true
        setText(TEXT_FILLER)
        setSelection(TEXT_FILLER.length)
        mIsDoingInternalChanges = false
    }

    private fun enable() {
        isEnabled = true
        isFocusable = true
        isFocusableInTouchMode = true
        visibility = VISIBLE
        requestFocus()
    }

    fun setListener(listener: InputListener) {
        mListener = listener
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            disableKeyboard()
        }
        return super.onKeyPreIme(keyCode, event)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            disableKeyboard()
        }
    }

    override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        if (mIsDoingInternalChanges) return

        mListener?.let { listener ->
            for (i in 0 until lengthBefore) {
                listener.onBackspace()
            }
            var count = 0
            var i = start
            while (count < lengthAfter && i < text.length) {
                listener.onSend(text[i])
                count++
                i++
            }
        }

        if (text.isEmpty()) {
            clear()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HidableInputLayout(
    onClose: () -> Unit,
    keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
) {
    var view by remember { mutableStateOf<TouchCharInput?>(null) }
    val localView = LocalView.current

    AndroidView(
        modifier = Modifier
            .alpha(0f)
            .size(1.dp),
        factory = { context ->
            TouchCharInput(context).apply {
                id = View.generateViewId()

                imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or
                        EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                        EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or
                        EditorInfo.IME_ACTION_DONE

                inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

                setEms(10)
                isFocusableInTouchMode = true
            }.also { view0 ->
                view = view0.also {
                    it.setListener(
                        object : TouchCharInput.InputListener {
                            override fun onSend(char: Char) {
                                CallbackBridge.sendChar(char, 0)
                            }

                            override fun onBackspace() {
                                CallbackBridge.sendKeycode(
                                    LwjglGlfwKeycode.GLFW_KEY_BACKSPACE,
                                    '\u0008',
                                    0,
                                    0,
                                    true
                                )
                                CallbackBridge.sendKeycode(
                                    LwjglGlfwKeycode.GLFW_KEY_BACKSPACE,
                                    '\u0008',
                                    0,
                                    0,
                                    false
                                )
                            }

                            override fun onEnter() {
                                CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER)
                                onClose()
                            }
                        }
                    )
                }
            }
        }
    )

    LaunchedEffect(view) {
        view?.enableKeyboard()
    }

    DisposableEffect(Unit) {
        onDispose {
            view?.disableKeyboard()
            keyboardController?.hide()
            view = null
            localView.requestFocus()
        }
    }

    val imeVisible = WindowInsets.isImeVisible
    var lastImeVisible by remember { mutableStateOf(imeVisible) }

    LaunchedEffect(imeVisible) {
        if (lastImeVisible && !imeVisible) {
            onClose()
        }
        lastImeVisible = imeVisible
    }
}
