package com.israadev.nuxlauncher.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import com.israadev.nuxlauncher.ui.theme.LocalNuxScale
import com.israadev.nuxlauncher.ui.theme.resp

/**
 * Dark Obsidian Cyber-Glass Card with subtle hairline border and double-bezel depth
 */
@Composable
fun NuxCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = NuxColors.SurfaceWhite,
    borderColor: Color = NuxColors.CardBorder,
    shadowColor: Color = Color.Transparent,
    shadowOffset: Dp = 0.dp,
    cornerRadius: Dp = (16.dp).resp(),
    borderWidth: Dp = NuxSizes.BorderWidth,
    fillMaxHeight: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val cardModifier = if (fillMaxHeight) modifier.fillMaxSize() else modifier.fillMaxWidth()
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = cardModifier
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .clip(shape)
    ) {
        content()
    }
}

/**
 * Dark Obsidian Cyber-Glass Interactive Button with spring micro-scale tactile feedback
 */
@Composable
fun NuxButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NuxColors.ForestGreen,
    contentColor: Color = Color.White,
    borderColor: Color = NuxColors.CardBorder,
    enabled: Boolean = true,
    shadowOffset: Dp = 0.dp,
    cornerRadius: Dp = (14.dp).resp(),
    contentPadding: PaddingValues = PaddingValues(horizontal = (12.dp).resp(), vertical = (4.dp).resp()),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "btnScale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                if (enabled) backgroundColor else NuxColors.SurfaceElevated.copy(alpha = 0.6f),
                shape
            )
            .border(
                width = NuxSizes.BorderWidth,
                color = if (enabled) borderColor else Color(0x1AFFFFFF),
                shape = shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .defaultMinSize(minHeight = (32.dp).resp())
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

/**
 * Cyber Emerald Badge / Pill Indicator
 */
@Composable
fun NuxBadge(
    text: String,
    backgroundColor: Color = NuxColors.SoftLime,
    textColor: Color = NuxColors.ForestGreen,
    borderColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape((7.dp).resp())
    val effectiveBorderColor = borderColor ?: textColor.copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .background(backgroundColor, shape)
            .border(1.dp, effectiveBorderColor, shape)
            .padding(horizontal = (8.dp).resp(), vertical = (3.dp).resp())
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = (10.sp).resp(),
            fontWeight = FontWeight.Bold,
            letterSpacing = (0.5.sp).resp(),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Dark Obsidian Text Input Field
 */
@Composable
fun NuxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape((10.dp).resp())
    val resolvedTextStyle = textStyle ?: TextStyle(
        color = NuxColors.DarkGray,
        fontSize = (13.sp).resp(),
        fontWeight = FontWeight.SemiBold
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NuxColors.SurfaceInput, shape)
            .border(1.dp, NuxColors.CardBorder, shape)
            .padding(horizontal = (12.dp).resp(), vertical = (8.dp).resp()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = NuxColors.GrayNeutral.copy(alpha = 0.6f),
                    fontSize = (13.sp).resp(),
                    fontWeight = FontWeight.Normal
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = resolvedTextStyle,
                cursorBrush = SolidColor(NuxColors.ForestGreen),
                singleLine = true,
                visualTransformation = visualTransformation,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width((8.dp).resp()))
            trailingContent()
        }
    }
}

/**
 * Dark Obsidian Glass Styled Dialog Container
 */
@Composable
fun NuxDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    fillMaxHeight: Boolean = false,
    content: @Composable () -> Unit
) {
    val isTablet = LocalNuxScale.current.isTablet
    val defaultWidthFraction = if (isTablet) 0.82f else 0.96f

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = (12.dp).resp(), vertical = (8.dp).resp()),
            contentAlignment = Alignment.Center
        ) {
            val dialogModifier = if (fillMaxHeight) {
                modifier.fillMaxWidth(defaultWidthFraction).fillMaxHeight(0.96f)
            } else {
                modifier.fillMaxWidth(defaultWidthFraction).wrapContentHeight()
            }
            NuxCard(
                modifier = dialogModifier,
                backgroundColor = NuxColors.SurfaceElevated,
                borderColor = Color(0x33FFFFFF),
                borderWidth = 1.dp,
                cornerRadius = (18.dp).resp(),
                fillMaxHeight = fillMaxHeight
            ) {
                content()
            }
        }
    }
}
