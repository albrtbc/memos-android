package me.mudkip.moememos.util

sealed class EmbedInfo {
    data class YouTube(val videoId: String, val url: String) : EmbedInfo()
    data class Twitter(val tweetId: String, val url: String) : EmbedInfo()
    data class Reddit(val url: String) : EmbedInfo()
}

private val YOUTUBE_RE =
    Regex("""^(?:https?://)?(?:www\.|m\.)?(?:youtube\.com/(?:watch\?v=|embed/|shorts/)|youtu\.be/)([\w-]{11})""")

private val TWITTER_RE =
    Regex("""^https?://(?:www\.)?(?:twitter\.com|x\.com)/\w+/status/(\d+)""")

private val REDDIT_RE =
    Regex("""^https?://(?:www\.)?reddit\.com/r/\w+/comments/\w+""")

fun detectEmbed(url: String): EmbedInfo? {
    YOUTUBE_RE.find(url)?.let { match ->
        return EmbedInfo.YouTube(videoId = match.groupValues[1], url = url)
    }
    TWITTER_RE.find(url)?.let { match ->
        return EmbedInfo.Twitter(tweetId = match.groupValues[1], url = url)
    }
    if (REDDIT_RE.containsMatchIn(url)) {
        return EmbedInfo.Reddit(url = url)
    }
    return null
}
