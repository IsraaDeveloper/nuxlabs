package com.movtery.zalithlauncher.bridge

import android.util.Log
import androidx.compose.ui.input.pointer.PointerIcon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.view.PointerIcon as NativePointerIcon

object ZLBridgeStates {
    private val _cursorMode = MutableStateFlow(CURSOR_ENABLED)
    val cursorMode = _cursorMode.asStateFlow()

    @JvmStatic
    fun changeCursorMode(mode: Int) {
        require(mode in 0..1)
        Log.d("NUX_INPUT", "changeCursorMode: mode=$mode (0=DISABLED/inGame, 1=ENABLED/menu)")
        this._cursorMode.update { mode }
    }

    private val _cursorShape = MutableStateFlow(CursorShape.Arrow)
    val cursorShape = _cursorShape.asStateFlow()

    @JvmStatic
    fun changeCursorShape(shape: CursorShape) {
        _cursorShape.update { shape }
    }

    @JvmStatic
    private val _windowChangeKey = MutableStateFlow(false)
    val windowChangeKey = _windowChangeKey.asStateFlow()

    fun onWindowChange() {
        this._windowChangeKey.update { old -> old.not() }
    }
}

const val CURSOR_ENABLED = 1
const val CURSOR_DISABLED = 0

enum class CursorShape(
    val composeIcon: PointerIcon
) {
    Arrow(PointerIcon.Default),
    IBeam(PointerIcon.Text),
    Hand(PointerIcon.Hand),
    CrossHair(PointerIcon.Crosshair),
    ResizeNS(PointerIcon(NativePointerIcon.TYPE_VERTICAL_DOUBLE_ARROW)),
    ResizeEW(PointerIcon(NativePointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW)),
    ResizeAll(PointerIcon(NativePointerIcon.TYPE_ALL_SCROLL)),
    NotAllowed(PointerIcon(NativePointerIcon.TYPE_NO_DROP))
}
