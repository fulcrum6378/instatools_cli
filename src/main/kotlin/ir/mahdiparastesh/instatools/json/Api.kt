package ir.mahdiparastesh.instatools.json

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File
import java.io.FileInputStream
import kotlin.reflect.KClass

class Api {
    val client = HttpClient(CIO) {
        engine {
            proxy = ProxyBuilder.http("http://127.0.0.1:8580/")
        }
    }
    private var cookies: String? = null

    fun loadCookies(path: String = "cookies.txt"): Boolean {
        val f = File(path)
        if (!f.exists()) return false
        cookies = FileInputStream(f).use { String(it.readBytes()) }
        return true
    }

    @Suppress("SpellCheckingInspection", "UastIncorrectHttpHeaderInspection")
    suspend fun <JSON> call(
        url: String,
        clazz: KClass<*>,
        httpMethod: HttpMethod = HttpMethod.Get,
        body: String? = null,
        typeToken: java.lang.reflect.Type? = null,
        onError: ((status: Int, body: String) -> Unit)? = null,
        onSuccess: (json: JSON) -> Unit
    ) {
        val response: HttpResponse = client.request(url) {
            method = httpMethod
            headers {
                append("x-asbd-id", "129477")
                if (cookies!!.contains("csrftoken=")) append(
                    "x-csrftoken",
                    cookies!!.substringAfter("csrftoken=").substringBefore(";")
                )
                append("x-ig-app-id", "936619743392459")
                append("cookie", cookies!!)
            }
            if (body != null) setBody(body)
        }
        val text = response.bodyAsText()
        if (System.getenv("test") == "1")
            println(text)
        if (response.status == HttpStatusCode.OK)
            onSuccess(Gson().fromJson(text, typeToken ?: clazz.java) as JSON)
        else {
            if (onError != null) onError(response.status.value, text)
        }
    }

    suspend fun page(
        url: String,
        onError: (status: Int, html: String) -> Unit,
        onSuccess: (html: String) -> Unit
    ) {
        val response: HttpResponse = client.get(url) {
            headers { append("cookie", cookies!!) }
        }
        val text = response.bodyAsText()
        if (response.status == HttpStatusCode.OK)
            onSuccess(text)
        else
            onError(response.status.value, text)
    }

    companion object {
        const val POST_HASH = "8c2a529969ee035a5063f2fc8602a0fd"

        @Suppress("SpellCheckingInspection", "UNCHECKED_CAST")
        fun graphQlBody(cnfWrapper: PageConfig, shortcode: String): String {
            val siteData = cnfWrapper.define["SiteData"]!![1] as Map<String, Any>
            return "access_token=" +
                    "&__d=" + siteData["haste_site"] +
                    "&__user=0" +
                    "&__a=1" +
                    "&__dyn=7xeUmwlE7ibwKBWo2vwAxu13w8CewSwMwNw9G2S0lW4o0B-q1ew65xO0F" +
                    "E2awt81sbzoaEd82lwv89k2C1Fwc61uwZx-0z8jwae4UaEW0D888cobEaU2eUlwh" +
                    "E2Lx_w4HwJwSyES1Twoob82ZwiU8UdUbGwbO1pw" /*TODO*/ +
                    "&__csr=glhcrillJsB9N5GL8F6LV9lGm4oSAZUOVoCimE8ideXGXAgynCF5KEy2y" +
                    "00gc905eyRc02JG3C4m4o7y0zyw4Za2ye3ywXm3O6204pjgYwKoEy2u7u1RwjlG0" +
                    "j10PwbZ0ww15Kbm0oK0YU" /*TODO*/ +
                    "&__req=3" /*TODO d or 3?*/ +
                    "&__hs=" + siteData["haste_session"] +
                    "&dpr=1" +
                    "&__ccg=" + (cnfWrapper.define["WebConnectionClassServerGuess"]!![1]
                    as Map<String, String>)["connectionClass"]!! +
                    "&__rev=" + (siteData["client_revision"] as Double)
                .toInt().toString() +
                    "&__s=eiw83y%3Aude3gw%3Ap6j381" /*TODO*/ +
                    "&__hsi=" + siteData["haste_session"] +
                    "&__comet_req=7" +
                    "&fb_dtsg=" + ((cnfWrapper.define["DTSGInitialData"]!![1]
                    as Map<String, String>)["token"] ?: "") + // or DTSGInitData and async_get_token
                    // DTSGInitData[1]["token"] is null in guest mode.
                    "&jazoest=26314" /*TODO 26314 or 26301*/ +
                    "&lsd=" + (cnfWrapper.define["LSD"]!![1] as Map<String, String>)["token"]!! +
                    "&__spin_r=" + (siteData["__spin_r"] as Double).toInt() +
                    "&__spin_b=" + siteData["__spin_b"] +
                    "&__spin_t=" + (siteData["__spin_t"] as Double).toInt() +
                    "&fb_api_caller_class=RelayModern" +
                    "&fb_api_req_friendly_name=PolarisPostRootQuery" +
                    /*TODO usePolarisSaveMediaSaveMutation or PolarisPostRootQuery*/
                    "&variables=%7B%22shortcode%22%3A%22$shortcode%22%7D" /*TODO shortcode or media id?!?*/ +
                    "&server_timestamps=true" +
                    "&doc_id=18086740648321782" /*TODO*/
        }
    }

    @Suppress("unused")
    enum class Endpoint(val url: String) {
        // Profiles
        PROFILE("https://www.instagram.com/api/v1/users/web_profile_info/?username=%s"),
        INFO("https://www.instagram.com/api/v1/users/%s/info/"),
        SEARCH(
            "https://www.instagram.com/api/v1/web/search/topsearch/?context=blended&query=%s" +
                    "&include_reel=false&search_surface=web_top_search"
        ), // &rank_token=0.9366187585704904

        // Posts & Stories
        MEDIA_ITEM("https://www.instagram.com/api/v1/media/%s/info/"),
        POSTS(
            "https://www.instagram.com/graphql/query/?query_hash=$POST_HASH" +
                    "&variables={\"id\":\"%1\$s\",\"first\":12,\"after\":\"%2\$s\"}"
        ),
        TAGGED("https://www.instagram.com/api/v1/usertags/%1\$s/feed/?count=12&max_id=%2\$s"),
        STORY("https://www.instagram.com/api/v1/feed/user/%s/story/"),
        HIGHLIGHTS("https://www.instagram.com/api/v1/highlights/%s/highlights_tray/"),
        REEL_ITEM("https://www.instagram.com/api/v1/feed/reels_media/?reel_ids=%s"),
        // StoryReel = "Full-Screen Video"; Story { reel, reel, ... }, Highlights { reel, reel, ... }
        // Adding "media_id=" parameter is of no use, the results are the same!!
        /*NEW_TAGGED( // Requires edges again
            "https://www.instagram.com/graphql/query/?query_hash=$taggedHash" +
                    "&variables={\"id\":\"%1\$s\",\"first\":12,\"after\":\"%2\$s\"}"
        ),*///const val taggedHash = "be13233562af2d229b008d2976b998b5"

        // Interactions (always use "?count=" for more accurate results)
        FOLLOWERS("https://www.instagram.com/api/v1/friendships/%1\$s/followers/?count=200&max_id=%2\$s"),
        FOLLOWING("https://www.instagram.com/api/v1/friendships/%1\$s/following/?count=200&max_id=%2\$s"),
        FRIENDSHIPS_MANY("https://www.instagram.com/api/v1/friendships/show_many/"), /*
        // method = POST, `user_ids=<ids separated by ",">`, expect Rest$Friendships *//*
        FRIENDSHIP("https://www.instagram.com/api/v1/friendships/show/%s/"), // GET */

        //FOLLOW("https://www.instagram.com/api/v1/friendships/create/%s/"),
        UNFOLLOW("https://www.instagram.com/api/v1/friendships/destroy/%s/"),
        /*MUTE("https://www.instagram.com/api/v1/friendships/mute_posts_or_story_from_follow/"),
        UNMUTE("https://www.instagram.com/api/v1/friendships/unmute_posts_or_story_from_follow/"),
        // method = POST, "target_posts_author_id=<USER_ID>" AND(using &)/OR "target_reel_author_id=<USER_ID>",
        // expect Rest$Friendships*/
        /*RESTRICT("https://www.instagram.com/api/v1/web/restrict_action/restrict/"),
        UNRESTRICT("https://www.instagram.com/api/v1/web/restrict_action/unrestrict/"),
        // method = POST, body = "target_user_id=<USER_ID>", expect "{"status":"ok"}" */
        /*BLOCK("https://www.instagram.com/api/v1/web/friendships/%d/block/"),
        UNBLOCK("https://www.instagram.com/api/v1/web/friendships/%d/unblock/"),
        // method = POST, expect "{"status":"ok"}" */

        // Saving
        SAVED("https://www.instagram.com/api/v1/feed/saved/posts/"),
        UNSAVE("https://www.instagram.com/web/save/%s/unsave/"),
        //SAVE("https://www.instagram.com/web/save/%s/save/"),
        // The fucking web API used /web/save for fulcrum6378 and /graphql/query for instatools.apk !?!

        // Messaging
        INBOX("https://www.instagram.com/api/v1/direct_v2/inbox/?cursor=%s"),
        DIRECT("https://www.instagram.com/api/v1/direct_v2/threads/%1\$s/?cursor=%2\$s&limit=%3\$d"),/*
        // persistentBadging=true&folder=[0(PRIMARY)|1(GENERAL)]
        // Avoiding "limit" argument will default to 20, but can be more than that. */
        SEEN("https://www.instagram.com/api/v1/direct_v2/threads/%1\$s/items/%2\$s/seen/"),

        // Logging in/out
        SIGN_OUT("https://www.instagram.com/accounts/logout/ajax/"),// MEDIA_ITEM

        RAW_QUERY("https://www.instagram.com/graphql/query"),
    }

    annotation class Updated2025
}
