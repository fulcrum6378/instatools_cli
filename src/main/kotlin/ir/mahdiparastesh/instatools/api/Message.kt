package ir.mahdiparastesh.instatools.api

import ir.mahdiparastesh.instatools.util.Utils

@Suppress("PropertyName")
data class Message(
    val item_id: String,
    val message_id: String,
    val user_id: Double,
    val timestamp: Double, // microseconds
    val item_type: String,
    val is_sent_by_viewer: Boolean,
    val reactions: Reactions?,

    // Data Types
    val action_log: ActionLog?,
    val animated_media: AnimatedMedia?,
    val clip: ClipShare?, // shared some kinda video post
    val direct_media_share: DirectMediaShare?, // tagged you in a post (item_type == "media_share")
    val felix_share: FelixShare?, // shared some kinda video post
    val like: String?,
    val link: Link?,
    val live_viewer_invite: LiveViewerInvite?,
    val media: Media?, // uploaded a picture or video
    val media_share: Media?, // shared a picture or some kinda video post
    val placeholder: PlaceHolder?,
    val profile: User?,
    val raven_media: Media?, // captured and uploaded by the blue button or direct story
    val reel_share: ReelShare?, // the user's own reel which was story once and now is in the archive
    val story_share: StoryShare?, // shared a normal or highlighted story
    val text: String?, // no different if is a saved reply
    val video_call_event: VideoCallEvent?, // plus audio call
    val voice_media: Voice?,

    // 1. Clip: is a short video which appears in DM as a rectangle and has a video icon in top right
    // plus user profile picture and name at the bottom,
    // clip will not appear in a profile's "Videos" section
    // 2. Felix: is a long videos which appears in DM as a rectangle and has no icons except user
    // profile picture and name at the bottom, this video will appear in their profile's "Videos".
    // 3. Found in {media_share}: There is also some other kind of short video post which appears
    // in DM this time as a SQUARE and also appears in "Videos" section.
) {

    data class Inbox(
        var threads: List<DmThread>,
        var has_older: Boolean,
        var oldest_cursor: String?,
    )

    data class DmThread(
        var has_older: Boolean,
        val items: List<Message>,
        val thread_id: String,
        val thread_v2_id: String,
        val users: List<User>,
        val thread_title: String,
        val is_group: Boolean,
        val folder: Float,
    ) {
        fun title() = if (!is_group) users.getOrNull(0)?.visName() else thread_title

        fun exportFileName() =
            "Exported ${title()}_${Utils.fileDateTime(Utils.now())}"
    }

    data class Reactions(val emojis: List<Emoji>)

    data class Emoji(
        val timestamp: Double,
        val sender_id: Double,
        val emoji: String,
    )


    class ActionLog

    data class AnimatedMedia(val images: AnimatedMediaImages)

    data class AnimatedMediaImages(val fixed_height: AnimatedMediaImage)

    data class AnimatedMediaImage(
        val height: String,
        val size: String,
        val url: String,
        val width: String,
    )

    data class ClipShare(val clip: Media)

    data class DirectMediaShare(val text: String, val media: Media)

    data class FelixShare(
        val video: Media,
        val text: String?,
        override val title: String?,
        override val message: String?,
    ) : PlaceHolder

    data class Link(val link_context: LinkContext, val text: String)

    data class LinkContext(val link_url: String)

    data class LiveViewerInvite(
        val broadcast: LiveBroadcast?,
        val cta_button_name: String,
        val text: String,
        override val title: String?,
        override val message: String?,
    ) : PlaceHolder

    data class LiveBroadcast(val broadcast_owner: User)

    data class ReelShare(
        val media: Media?,
        val reel_type: String,
        val text: String,
        override val title: String?,
        override val message: String?,
    ) : PlaceHolder

    data class StoryShare(
        val media: Media?,
        val reel_id: String?,
        val reel_type: String?,
        val text: String,
        override val title: String?,
        override val message: String?,
    ) : PlaceHolder

    data class Voice(val media: VoiceMedia)

    data class VoiceMedia(val audio: Audio)

    data class Audio(val audio_src: String)

    data class VideoCallEvent(val action: String, val description: String)

    interface PlaceHolder {
        val title: String?
        val message: String?
    }
}
