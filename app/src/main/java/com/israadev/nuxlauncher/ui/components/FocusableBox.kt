package com.israadev.nuxlauncher.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@Composable
fun FocusableBox(
    modifier: Modifier = Modifier,
    requestKey: Any? = null,
    canRequestFocus: () -> Boolean = { true }
) {
    val focusRequester = remember { FocusRequester() }
    val currentCanRequestFocus by rememberUpdatedState(canRequestFocus)

    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable(enabled = true)
    )

    LaunchedEffect(requestKey) {
        if (currentCanRequestFocus()) {
            focusRequester.requestFocus()
        }
    }
}
