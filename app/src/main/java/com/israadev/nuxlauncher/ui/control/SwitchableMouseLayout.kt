package com.israadev.nuxlauncher.ui.control

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import com.movtery.zalithlauncher.bridge.CURSOR_DISABLED
import com.movtery.zalithlauncher.bridge.CURSOR_ENABLED
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.lwjgl.glfw.CallbackBridge

enum class MouseControlMode {
    SLIDE, // Trackpad mode: Drag to move cursor, tap anywhere to click at cursor
    CLICK  // Direct touch: Tap or drag directly sets cursor to finger and clicks
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SwitchableMouseLayout(
    modifier: Modifier = Modifier,
    screenWidth: Float,
    screenHeight: Float,
    cursorMode: Int,
    controlMode: MouseControlMode,
    cursorPosition: Offset,
    cursorSensitivity: Float = 1.25f,
    onCursorPositionChange: (Offset) -> Unit,
    onMouse: () -> Unit = {},
    onTouch: () -> Unit = {},
    onTap: (Offset) -> Unit = { pos ->
        val winW = CallbackBridge.windowWidth
        val winH = CallbackBridge.windowHeight
        val targetX = if (screenWidth > 0 && winW > 0) pos.x * (winW.toFloat() / screenWidth) else pos.x
        val targetY = if (screenHeight > 0 && winH > 0) pos.y * (winH.toFloat() / screenHeight) else pos.y
        CallbackBridge.putMouseEventWithCoords(
            LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT,
            targetX,
            targetY
        )
    },
    onLongPress: (Offset) -> Unit = {
        CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, true)
    },
    onLongPressEnd: () -> Unit = {
        CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, false)
    },
    onCapturedMove: (Offset) -> Unit = { delta ->
        CallbackBridge.sendCursorDelta(delta.x * cursorSensitivity, delta.y * cursorSensitivity)
    }
) {
    val coroutineScope = rememberCoroutineScope()
    val viewConfiguration = LocalViewConfiguration.current
    val currentControlMode by rememberUpdatedState(controlMode)
    val currentCursorMode by rememberUpdatedState(cursorMode)
    val currentCursorPos by rememberUpdatedState(cursorPosition)
    val currentSensitivity by rememberUpdatedState(cursorSensitivity)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnLongPressEnd by rememberUpdatedState(onLongPressEnd)
    val currentOnCapturedMove by rememberUpdatedState(onCapturedMove)
    val currentOnCursorPositionChange by rememberUpdatedState(onCursorPositionChange)
    val currentOnMouse by rememberUpdatedState(onMouse)
    val currentOnTouch by rememberUpdatedState(onTouch)

    val isCaptured = currentCursorMode == CURSOR_DISABLED

    fun sendScaledCursor(x: Float, y: Float) {
        val winW = CallbackBridge.windowWidth
        val winH = CallbackBridge.windowHeight
        val targetX = if (screenWidth > 0 && winW > 0) x * (winW.toFloat() / screenWidth) else x
        val targetY = if (screenHeight > 0 && winH > 0) y * (winH.toFloat() / screenHeight) else y
        CallbackBridge.sendCursorPos(targetX, targetY)
    }

    // 1. Hardware Pointer Capture (Standard Android 8.0+ API for physical mouse in game mode)
    SimpleMouseCapture(
        enabled = isCaptured,
        cursorSensitivity = currentSensitivity,
        onMouse = currentOnMouse,
        onCapturedMove = currentOnCapturedMove
    )

    var lastMouseButtons by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 2. Intercept hardware mouse events (movement, clicks, scroll wheel in menu mode)
            .pointerInteropFilter { event ->
                val isMouse = event.isFromSource(InputDevice.SOURCE_MOUSE) ||
                        event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE)
                val isTouch = event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN)

                if (isTouch) {
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        currentOnTouch()
                    }
                    return@pointerInteropFilter false
                }

                if (!isMouse) {
                    return@pointerInteropFilter false
                }

                // Physical mouse action detected -> notify auto-hide UI
                currentOnMouse()

                // Dispatch mouse buttons (left, right, middle)
                val buttons = event.buttonState
                val changed = lastMouseButtons xor buttons

                fun dispatchBtn(btn: Int, glfwBtn: Int) {
                    if (changed and btn != 0) {
                        val pressed = buttons and btn != 0
                        CallbackBridge.sendMouseButton(glfwBtn, pressed)
                    }
                }

                dispatchBtn(MotionEvent.BUTTON_PRIMARY, LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT)
                dispatchBtn(MotionEvent.BUTTON_SECONDARY, LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT)
                dispatchBtn(MotionEvent.BUTTON_TERTIARY, LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE)
                dispatchBtn(MotionEvent.BUTTON_BACK, LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT)
                lastMouseButtons = buttons

                when (event.actionMasked) {
                    MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_MOVE -> {
                        if (!isCaptured) {
                            val newX = event.x.coerceIn(0f, screenWidth)
                            val newY = event.y.coerceIn(0f, screenHeight)
                            val newPos = Offset(newX, newY)
                            currentOnCursorPositionChange(newPos)
                            sendScaledCursor(newX, newY)
                        }
                    }
                    MotionEvent.ACTION_SCROLL -> {
                        val scrollX = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
                        val scrollY = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                        CallbackBridge.sendScroll(scrollX.toDouble(), scrollY.toDouble())
                    }
                }
                true
            }
            // 3. Touch gesture layer for mobile touchscreen trackpad & taps
            .pointerInput(cursorMode, controlMode) {
                awaitEachGesture {
                    var activePointerId: PointerId? = null
                    var startPosition = Offset.Zero
                    var isDragging = false
                    var longPressTriggered = false
                    var longPressJob: Job? = null

                    // Wait for first touch down event (ignore hardware mouse/stylus to avoid double click)
                    val downEvent = awaitFirstDown(requireUnconsumed = false)
                    if (downEvent.type != PointerType.Touch) {
                        return@awaitEachGesture
                    }
                    currentOnTouch()
                    activePointerId = downEvent.id
                    startPosition = downEvent.position
                    isDragging = false
                    longPressTriggered = false

                    val capturedNow = currentCursorMode == CURSOR_DISABLED

                    if (!capturedNow && currentControlMode == MouseControlMode.CLICK) {
                        currentOnCursorPositionChange(downEvent.position)
                        sendScaledCursor(downEvent.position.x, downEvent.position.y)
                    }

                    // Start long press timer (mining in-game or dragging item in menu)
                    longPressJob = coroutineScope.launch {
                        delay(viewConfiguration.longPressTimeoutMillis)
                        if (!isDragging) {
                            longPressTriggered = true
                            val targetPos = if (capturedNow) {
                                Offset.Zero
                            } else if (currentControlMode == MouseControlMode.CLICK) {
                                startPosition
                            } else {
                                currentCursorPos
                            }
                            currentOnLongPress(targetPos)
                        }
                    }

                    // Track motion events
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == activePointerId } ?: break

                        if (change.changedToUpIgnoreConsumed()) {
                            longPressJob.cancel()
                            if (longPressTriggered) {
                                currentOnLongPressEnd()
                            } else if (!isDragging) {
                                // Clean tap registered
                                if (currentCursorMode == CURSOR_DISABLED) {
                                    CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT)
                                } else {
                                    val clickPos = if (currentControlMode == MouseControlMode.CLICK) {
                                        change.position
                                    } else {
                                        currentCursorPos
                                    }
                                    currentOnTap(clickPos)
                                }
                            }
                            change.consume()
                            break
                        }

                        if (change.positionChanged()) {
                            val dragDistance = (change.position - startPosition).getDistance()
                            if (!isDragging && dragDistance > viewConfiguration.touchSlop) {
                                isDragging = true
                                longPressJob.cancel()
                            }

                            if (currentCursorMode == CURSOR_DISABLED) {
                                // In gameplay: send relative delta for infinite 360-degree camera rotation
                                val delta = change.positionChange()
                                currentOnCapturedMove(delta)
                            } else {
                                // In menu: move cursor on screen
                                if (currentControlMode == MouseControlMode.SLIDE) {
                                    if (isDragging || longPressTriggered) {
                                        val delta = change.positionChange()
                                        val newX = (currentCursorPos.x + delta.x * currentSensitivity).coerceIn(0f, screenWidth)
                                        val newY = (currentCursorPos.y + delta.y * currentSensitivity).coerceIn(0f, screenHeight)
                                        val newPos = Offset(newX, newY)
                                        currentOnCursorPositionChange(newPos)
                                        sendScaledCursor(newX, newY)
                                    }
                                } else {
                                    currentOnCursorPositionChange(change.position)
                                    sendScaledCursor(change.position.x, change.position.y)
                                }
                            }
                            change.consume()
                        }
                    }
                }
            }
    )
}

@Composable
private fun SimpleMouseCapture(
    enabled: Boolean,
    cursorSensitivity: Float,
    onMouse: () -> Unit,
    onCapturedMove: (Offset) -> Unit
) {
    val view = LocalView.current
    val currentOnMouse by rememberUpdatedState(onMouse)
    val currentOnCapturedMove by rememberUpdatedState(onCapturedMove)
    val currentSensitivity by rememberUpdatedState(cursorSensitivity)

    fun syncCaptureState() {
        if (enabled) {
            if (view.hasWindowFocus()) {
                view.requestPointerCapture()
            }
        } else {
            view.releasePointerCapture()
        }
    }

    DisposableEffect(view, enabled) {
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (enabled && hasFocus) {
                view.requestPointerCapture()
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        syncCaptureState()

        if (enabled) {
            val pointerListener = View.OnCapturedPointerListener { _, event ->
                currentOnMouse()
                when (event.actionMasked) {
                    MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_MOVE -> {
                        var deltaX = 0f
                        var deltaY = 0f

                        val relX = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)
                        val relY = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)
                        deltaX += if (relX != 0f) relX else event.x
                        deltaY += if (relY != 0f) relY else event.y

                        val historySize = event.historySize
                        for (i in 0 until historySize) {
                            deltaX += event.getHistoricalAxisValue(MotionEvent.AXIS_RELATIVE_X, i)
                            deltaY += event.getHistoricalAxisValue(MotionEvent.AXIS_RELATIVE_Y, i)
                        }

                        currentOnCapturedMove(Offset(deltaX * currentSensitivity, deltaY * currentSensitivity))
                        true
                    }
                    MotionEvent.ACTION_SCROLL -> {
                        val scrollX = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
                        val scrollY = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                        CallbackBridge.sendScroll(scrollX.toDouble(), scrollY.toDouble())
                        true
                    }
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_BUTTON_PRESS -> {
                        val glfwBtn = when (event.actionButton) {
                            MotionEvent.BUTTON_PRIMARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT
                            MotionEvent.BUTTON_SECONDARY, MotionEvent.BUTTON_STYLUS_SECONDARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT
                            MotionEvent.BUTTON_TERTIARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE
                            else -> null
                        }
                        if (glfwBtn != null) {
                            CallbackBridge.sendMouseButton(glfwBtn, true)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_BUTTON_RELEASE -> {
                        val glfwBtn = when (event.actionButton) {
                            MotionEvent.BUTTON_PRIMARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT
                            MotionEvent.BUTTON_SECONDARY, MotionEvent.BUTTON_STYLUS_SECONDARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT
                            MotionEvent.BUTTON_TERTIARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE
                            else -> null
                        }
                        if (glfwBtn != null) {
                            CallbackBridge.sendMouseButton(glfwBtn, false)
                        }
                        true
                    }
                    else -> false
                }
            }
            view.setOnCapturedPointerListener(pointerListener)
        } else {
            view.setOnCapturedPointerListener(null)
        }

        onDispose {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            view.setOnCapturedPointerListener(null)
        }
    }
}
