package com.israadev.nuxlauncher.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val DISCORD_INVITE_URL = "https://discord.gg/UW4wBQg6X5"

/**
 * Robust, high-performance Jetpack Compose Markdown renderer tailored for AI Crash Analysis.
 * Seamlessly handles SSE streaming chunks without parsing crashes.
 */
@Composable
fun NuxMarkdownView(
    markdownText: String,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    showCursor: Boolean = false
) {
    val context = LocalContext.current
    val parsedBlocks = remember(markdownText, isStreaming, showCursor) {
        val textToParse = if (isStreaming && showCursor) "$markdownText ▌" else markdownText
        parseMarkdownBlocks(textToParse)
    }

    val containsDiscord = remember(markdownText) {
        markdownText.contains("discord.gg", ignoreCase = true)
    }
    val isDeveloperBug = remember(markdownText) {
        markdownText.contains("BUG DEVELOPER", ignoreCase = true)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        parsedBlocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val (fontSize, fontColor, topPadding) = when (block.level) {
                        1 -> Triple(12.sp, Color(0xFF38BDF8), 6.dp)
                        2 -> Triple(11.sp, Color(0xFFE2E8F0), 4.dp)
                        else -> Triple(10.sp, Color(0xFFFBBF24), 3.dp)
                    }
                    Spacer(modifier = Modifier.height(topPadding))
                    Text(
                        text = buildAnnotatedContent(block.text),
                        style = TextStyle(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Black,
                            color = fontColor,
                            letterSpacing = 0.3.sp
                        )
                    )
                }

                is MarkdownBlock.StatusBadge -> {
                    val isBug = block.isDeveloperBug
                    val bgColor = if (isBug) Color(0x33F43F5E) else Color(0x2E10B981)
                    val borderColor = if (isBug) Color(0xFFF43F5E) else Color(0xFF10B981)
                    val textColor = if (isBug) Color(0xFFFF6482) else Color(0xFF34D399)
                    val icon = if (isBug) Icons.Default.BugReport else Icons.Default.Build
                    val label = if (isBug) "BUG DEVELOPER / LAUNCHER (PERLU PERBAIKAN TIM)" else "BISA DISELESAIKAN SENDIRI"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(borderColor.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "KLASIFIKASI CRASH",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor.copy(alpha = 0.75f),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = label,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.AlertBox -> {
                    val isDiscordAlert = block.text.contains("discord.gg", ignoreCase = true) || isDeveloperBug
                    val borderColor = if (isDiscordAlert) Color(0xFFF43F5E) else Color(0xFF38BDF8)
                    val bgColor = if (isDiscordAlert) Color(0x1FF43F5E) else Color(0x1F38BDF8)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .fillMaxHeight()
                                    .background(borderColor, RoundedCornerShape(2.dp))
                            )
                            RenderAnnotatedText(
                                annotated = buildAnnotatedContent(block.text),
                                defaultColor = Color(0xFFF1F5F9),
                                fontSize = 9.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                is MarkdownBlock.ListItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 1.dp, bottom = 1.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = block.bullet,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            lineHeight = 13.5.sp
                        )
                        RenderAnnotatedText(
                            annotated = buildAnnotatedContent(block.text),
                            defaultColor = Color(0xFFE2E8F0),
                            fontSize = 9.sp,
                            lineHeight = 13.5.sp
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    val codeScroll = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF070A0E))
                            .border(1.dp, Color(0x2238BDF8), RoundedCornerShape(6.dp))
                            .horizontalScroll(codeScroll)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = block.code,
                            color = Color(0xFF7DD3FC),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            lineHeight = 12.sp
                        )
                    }
                }

                is MarkdownBlock.Divider -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(1.dp)
                            .background(Color(0x22FFFFFF))
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    RenderAnnotatedText(
                        annotated = buildAnnotatedContent(block.text),
                        defaultColor = Color(0xFFE2E8F0),
                        fontSize = 9.sp,
                        lineHeight = 13.5.sp
                    )
                }
            }
        }

        // Quick Direct Discord Action Button if issue is Bug Developer or Discord mentioned
        if (containsDiscord || isDeveloperBug) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF5865F2))
                    .clickable {
                        openUrl(context, DISCORD_INVITE_URL)
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "BUKA TIKET PENGADUAN DI DISCORD RESMI",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        letterSpacing = 0.4.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderAnnotatedText(
    annotated: AnnotatedString,
    defaultColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit
) {
    Text(
        text = annotated,
        style = TextStyle(
            color = defaultColor,
            fontSize = fontSize,
            lineHeight = lineHeight
        )
    )
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Tidak dapat membuka tautan: $url", Toast.LENGTH_SHORT).show()
    }
}

private sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class StatusBadge(val isDeveloperBug: Boolean) : MarkdownBlock()
    data class AlertBox(val text: String) : MarkdownBlock()
    data class ListItem(val bullet: String, val text: String) : MarkdownBlock()
    data class CodeBlock(val code: String) : MarkdownBlock()
    object Divider : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

private fun parseMarkdownBlocks(content: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = content.lines()
    var inCodeBlock = false
    val codeAccumulator = StringBuilder()

    for (rawLine in lines) {
        val line = rawLine.trimEnd()

        if (line.trim().startsWith("```")) {
            if (inCodeBlock) {
                blocks.add(MarkdownBlock.CodeBlock(codeAccumulator.toString().trimEnd()))
                codeAccumulator.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            continue
        }

        if (inCodeBlock) {
            codeAccumulator.append(rawLine).append("\n")
            continue
        }

        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue

        // Check Divider
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            blocks.add(MarkdownBlock.Divider)
            continue
        }

        // Check Status Badges
        if (trimmed.contains("[BUG DEVELOPER", ignoreCase = true) || trimmed.contains("BUG DEVELOPER / LAUNCHER", ignoreCase = true)) {
            blocks.add(MarkdownBlock.StatusBadge(isDeveloperBug = true))
            continue
        } else if (trimmed.contains("[BISA DISELESAIKAN SENDIRI", ignoreCase = true)) {
            blocks.add(MarkdownBlock.StatusBadge(isDeveloperBug = false))
            continue
        }

        // Check Headers
        if (trimmed.startsWith("### ")) {
            blocks.add(MarkdownBlock.Header(3, trimmed.removePrefix("### ").trim()))
            continue
        } else if (trimmed.startsWith("## ")) {
            blocks.add(MarkdownBlock.Header(2, trimmed.removePrefix("## ").trim()))
            continue
        } else if (trimmed.startsWith("# ")) {
            blocks.add(MarkdownBlock.Header(1, trimmed.removePrefix("# ").trim()))
            continue
        }

        // Check Blockquote / Alert
        if (trimmed.startsWith("> ") || trimmed.startsWith(">")) {
            val alertText = trimmed.removePrefix(">").trim()
            blocks.add(MarkdownBlock.AlertBox(alertText))
            continue
        }

        // Check List Items
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
            val bullet = "•"
            val text = trimmed.substring(2).trim()
            blocks.add(MarkdownBlock.ListItem(bullet, text))
            continue
        }
        val numberedRegex = Regex("^(\\d+)\\.\\s+(.*)")
        val numMatch = numberedRegex.find(trimmed)
        if (numMatch != null) {
            val num = numMatch.groupValues[1]
            val text = numMatch.groupValues[2]
            blocks.add(MarkdownBlock.ListItem("$num.", text))
            continue
        }

        // Normal Paragraph
        blocks.add(MarkdownBlock.Paragraph(line))
    }

    if (inCodeBlock && codeAccumulator.isNotEmpty()) {
        blocks.add(MarkdownBlock.CodeBlock(codeAccumulator.toString().trimEnd()))
    }

    return blocks
}

/**
 * Parses bold (**), italic (*), inline code (`), and URLs (http/https) into AnnotatedString
 */
private fun buildAnnotatedContent(rawText: String): AnnotatedString {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = Color(0xFF38BDF8),
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline
        )
    )

    return buildAnnotatedString {
        var i = 0
        val len = rawText.length

        while (i < len) {
            // Check Bold (**text**)
            if (i + 1 < len && rawText[i] == '*' && rawText[i + 1] == '*') {
                val end = rawText.indexOf("**", i + 2)
                if (end != -1) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White))
                    append(rawText.substring(i + 2, end))
                    pop()
                    i = end + 2
                    continue
                }
            }

            // Check Inline Code (`code`)
            if (rawText[i] == '`') {
                val end = rawText.indexOf('`', i + 1)
                if (end != -1) {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8),
                            background = Color(0xFF1E293B)
                        )
                    )
                    append(" ${rawText.substring(i + 1, end)} ")
                    pop()
                    i = end + 1
                    continue
                }
            }

            // Check Markdown Link [text](url)
            if (rawText[i] == '[') {
                val closeBracket = rawText.indexOf(']', i + 1)
                if (closeBracket != -1 && closeBracket + 1 < len && rawText[closeBracket + 1] == '(') {
                    val closeParen = rawText.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val linkText = rawText.substring(i + 1, closeBracket)
                        val url = rawText.substring(closeBracket + 2, closeParen)
                        val startIdx = length
                        append(linkText)
                        val endIdx = length
                        addLink(LinkAnnotation.Url(url = url, styles = linkStyles), startIdx, endIdx)
                        i = closeParen + 1
                        continue
                    }
                }
            }

            // Check Raw URL (https://... or http://...)
            if (rawText.startsWith("http://", i) || rawText.startsWith("https://", i)) {
                var end = i
                while (end < len && !rawText[end].isWhitespace() && rawText[end] != ')' && rawText[end] != ']' && rawText[end] != '*' && rawText[end] != '>') {
                    end++
                }
                val url = rawText.substring(i, end)
                val startIdx = length
                append(url)
                val endIdx = length
                addLink(LinkAnnotation.Url(url = url, styles = linkStyles), startIdx, endIdx)
                i = end
                continue
            }

            // Normal Character
            append(rawText[i])
            i++
        }
    }
}
