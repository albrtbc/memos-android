package me.mudkip.moememos.ui.component

import android.net.Uri
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.withLink

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownCheckBox
import com.mikepenz.markdown.compose.elements.MarkdownParagraph
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.mikepenz.markdown.compose.extendedspans.ExtendedSpans
import com.mikepenz.markdown.compose.extendedspans.RoundedCornerSpanPainter
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import com.mikepenz.markdown.model.markdownExtendedSpans
import com.mikepenz.markdown.model.rememberMarkdownState
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import me.mudkip.moememos.util.EmbedInfo
import me.mudkip.moememos.util.detectEmbed
import me.mudkip.moememos.util.findCustomTagMatches
import me.mudkip.moememos.util.getCustomTagName
import me.mudkip.moememos.util.isCustomTagSupportedNode
import com.mikepenz.markdown.m3.Markdown as M3Markdown

@Composable
fun Markdown(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    imageBaseUrl: String? = null,
    checkboxChange: ((checked: Boolean, startOffset: Int, endOffset: Int) -> Unit)? = null,
    selectable: Boolean = false,
    onTagClick: ((tag: String) -> Unit)? = null,
) {
    fun withOptionalTextAlign(style: TextStyle): TextStyle {
        return if (textAlign == null) style else style.copy(textAlign = textAlign)
    }

    val bodyTextStyle = withOptionalTextAlign(MaterialTheme.typography.bodyLarge)
    val h1TextStyle = withOptionalTextAlign(MaterialTheme.typography.headlineLarge)
    val h2TextStyle = withOptionalTextAlign(MaterialTheme.typography.headlineMedium)
    val h3TextStyle = withOptionalTextAlign(MaterialTheme.typography.headlineSmall)
    val h4TextStyle = withOptionalTextAlign(MaterialTheme.typography.titleLarge)
    val h5TextStyle = withOptionalTextAlign(MaterialTheme.typography.titleMedium)
    val h6TextStyle = withOptionalTextAlign(MaterialTheme.typography.titleSmall)
    val uriHandler = LocalUriHandler.current
    val tagLinkStyle = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            background = MaterialTheme.colorScheme.surfaceVariant,
        )
    )
    val tagLinkListener = remember(uriHandler, onTagClick) {
        LinkInteractionListener { link ->
            val url = (link as? LinkAnnotation.Url)?.url ?: return@LinkInteractionListener
            if (url.startsWith(TAG_LINK_PREFIX)) {
                onTagClick?.invoke(Uri.decode(url.removePrefix(TAG_LINK_PREFIX)))
                return@LinkInteractionListener
            }
            uriHandler.openUri(url)
        }
    }
    val imageTransformer = remember(imageBaseUrl) {
        object : ImageTransformer {
            @Composable
            override fun transform(link: String): ImageData {
                return Coil3ImageTransformerImpl.transform(resolveMarkdownImageLink(link, imageBaseUrl))
            }

            @Composable
            override fun intrinsicSize(painter: Painter): Size {
                return Coil3ImageTransformerImpl.intrinsicSize(painter)
            }
        }
    }
    val gfmFlavour = remember { GFMFlavourDescriptor() }
    val markdownState = rememberMarkdownState(
        content = text,
        retainState = true,
        flavour = gfmFlavour
    )

    val markdownContent: @Composable () -> Unit = {
        M3Markdown(
            markdownState = markdownState,
            modifier = modifier,
            imageTransformer = imageTransformer,
            typography = markdownTypography(
                h1 = h1TextStyle,
                h2 = h2TextStyle,
                h3 = h3TextStyle,
                h4 = h4TextStyle,
                h5 = h5TextStyle,
                h6 = h6TextStyle,
                text = bodyTextStyle,
                paragraph = bodyTextStyle,
                ordered = bodyTextStyle,
                bullet = bodyTextStyle,
                list = bodyTextStyle
            ),
            extendedSpans = markdownExtendedSpans {
                ExtendedSpans(
                    RoundedCornerSpanPainter(
                        cornerRadius = 6.sp,
                        padding = RoundedCornerSpanPainter.TextPaddingValues(
                            horizontal = 4.sp,
                            vertical = 2.sp
                        ),
                    )
                )
            },
            annotator = markdownAnnotator(
                config = markdownAnnotatorConfig(eolAsNewLine = true),
                annotate = { content, child ->
                    if (child.type != MarkdownTokenTypes.TEXT) {
                        return@markdownAnnotator false
                    }
                    if (!isCustomTagSupportedNode(child)) {
                        return@markdownAnnotator false
                    }
                    val source = child.getUnescapedTextInNode(content)
                    val tags = findCustomTagMatches(source).toList()
                    if (tags.isEmpty()) {
                        return@markdownAnnotator false
                    }

                    var cursor = 0
                    tags.forEach { match ->
                        val start = match.range.first
                        val endInclusive = match.range.last
                        if (start > cursor) {
                            append(source.substring(cursor, start))
                        }
                        val tagRaw = getCustomTagName(match)
                        withLink(
                            LinkAnnotation.Url(
                                url = TAG_LINK_PREFIX + Uri.encode(tagRaw),
                                styles = tagLinkStyle,
                                linkInteractionListener = tagLinkListener
                            )
                        ) {
                            append(match.value)
                        }
                        cursor = endInclusive + 1
                    }
                    if (cursor < source.length) {
                        append(source.substring(cursor))
                    }
                    true
                }
            ),
            components = markdownComponents(
                codeFence = highlightedCodeFence,
                codeBlock = highlightedCodeBlock,
                paragraph = { model ->
                    val embed = detectParagraphEmbed(model.content, model.node)
                    if (embed != null) {
                        when (embed) {
                            is EmbedInfo.YouTube -> YouTubeEmbedCard(embed.videoId, embed.url)
                            is EmbedInfo.Twitter -> TwitterEmbedCard(embed.tweetId, embed.url)
                            is EmbedInfo.Reddit -> RedditEmbedCard(embed.url)
                        }
                    } else {
                        MarkdownParagraph(
                            content = model.content,
                            node = model.node,
                            style = model.typography.paragraph,
                        )
                    }
                },
                checkbox = {
                    val node = it.node
                    MarkdownCheckBox(
                        content = it.content,
                        node = it.node,
                        style = it.typography.text,
                        checkedIndicator = { checked, modifier ->
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = if (checkboxChange != null) {
                                        { checkboxChange(!checked, node.startOffset, node.endOffset) }
                                    } else {
                                        null
                                    },
                                    modifier = modifier.semantics {
                                        role = Role.Checkbox
                                        stateDescription = if (checked) "Checked" else "Unchecked"
                                    },
                                )
                            }
                        }
                    )
                }
            )
        )
    }

    if (selectable) {
        SelectionContainer {
            markdownContent()
        }
    } else {
        markdownContent()
    }
}

/**
 * Checks if a paragraph node contains only a single bare link (link text == URL)
 * that matches a supported embed platform (YouTube, Twitter/X, Reddit).
 */
private fun detectParagraphEmbed(content: String, paragraphNode: org.intellij.markdown.ast.ASTNode): EmbedInfo? {
    val significantChildren = paragraphNode.children.filter {
        it.type != MarkdownTokenTypes.EOL && it.type != MarkdownTokenTypes.WHITE_SPACE
    }
    if (significantChildren.size != 1) return null
    val child = significantChildren[0]

    val url: String
    if (child.type == GFMTokenTypes.GFM_AUTOLINK || child.type == MarkdownElementTypes.AUTOLINK) {
        url = content.substring(child.startOffset, child.endOffset).trim('<', '>')
    } else if (child.type == MarkdownElementTypes.INLINE_LINK) {
        val linkDest = child.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }
            ?: return null
        url = content.substring(linkDest.startOffset, linkDest.endOffset)
        val linkText = child.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
            ?: return null
        val text = content.substring(linkText.startOffset, linkText.endOffset)
            .removePrefix("[").removeSuffix("]")
        if (text != url) return null
    } else {
        return null
    }

    return detectEmbed(url)
}

private fun resolveMarkdownImageLink(link: String, imageBaseUrl: String?): String {
    val uri = link.toUri()
    if (uri.scheme != null || imageBaseUrl.isNullOrBlank()) {
        return link
    }
    return imageBaseUrl.toUri().buildUpon().path(link).build().toString()
}

private const val TAG_LINK_PREFIX = "moememos://tag/"
