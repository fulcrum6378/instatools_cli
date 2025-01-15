package ir.mahdiparastesh.instatools.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import org.apache.commons.text.StringEscapeUtils

/** Resolves an HTML from Instagram and collects required data. */
@Suppress("SpellCheckingInspection", "PropertyName")
class PageConfig(
    val define: HashMap<String, List<Any>>, val require: HashMap<String, List<Any>>
) {
    companion object {
        private const val scheduledServerJS = "{\"require\":[[\"ScheduledServerJS\""

        @Suppress("UNCHECKED_CAST")
        private fun titledListToMap(list: Any): HashMap<String, List<Any>> {
            val map = HashMap<String, List<Any>>()
            (list as? ArrayList<ArrayList<Any>>)?.also { arr ->
                for (i in arr) map[i[0] as String] = i.subList(1, i.size)
            }
            return map
        }

        fun create(map: LinkedTreeMap<String, Any>): PageConfig =
            PageConfig(titledListToMap(map["define"]!!), titledListToMap(map["require"]!!))

        fun findFromHtml(
            rawHtml: String, isEvaluated: Boolean, onFailure: (e: Exception) -> Unit,
            onSuccess: suspend (wrapper: PageConfig) -> Unit,
        ) {
            val html = if (isEvaluated) StringEscapeUtils.unescapeJson(rawHtml) else rawHtml
            /*testHtml?.openFileOutput("login.html", 0)
                ?.use { it.write(html.encodeToByteArray()) }*/

            // Find the JSON blocks containing "scheduledServerJS" and find XIGSharedData
            var read = html
            val jsons = arrayListOf<String>()
            while (read.contains(scheduledServerJS)) {
                read = read.substring(read.indexOf(scheduledServerJS))
                jsons.add(read.substringBefore("</script>"))
                read = read.substringAfter("</script>")
            }
            val json = jsons.find { it.contains("XIGSharedData") }

            if (json != null) try {
                // Find the read PageConfig out of the boilerplate
                @Suppress("UNCHECKED_CAST")
                (GsonBuilder().setLenient().create()
                    .fromJson<Map<String, List<List<Any>>>>(
                        json, object : TypeToken<Map<String, List<List<Any>>>>() {}.type
                    )["require"]!![0][3] as ArrayList<Map<String, Any>>)
                    .find { Gson().toJson(it).contains("XIGSharedData") }!!
                    .values.elementAt(0) as LinkedTreeMap<String, Any>
            } catch (e: JsonSyntaxException) {
                if (System.getenv("test") == "1") throw IllegalStateException(
                    "The structure has changed (${e.message}): $json"
                )
                onFailure(e)
                null
            }?.also {
                /*testJson?.openFileOutput("wrapper.json", 0)
                    ?.use { j -> j.write(Gson().toJson(it).encodeToByteArray()) }*/
                onSuccess(create(it))
            } else onFailure(NeedAuth())
        }

        class NeedAuth : Exception()
    }

    data class PolarisRoot(
        //val actorID: String,
        val rootView: PolarisView,
        //val tracePolicy: String,
        //val meta: PolarisMeta,
        //val prefetchable: Boolean,
        //val entityKeyConfig: Map<String, Any?>,
        //val hostableView: Map<String, Any?>,
        //val url: String "\/p\/CeyIexyDcYd\/"
        val params: PolarisRootParams,
        //val routePath: String,
    )

    data class PolarisView(
        val props: PolarisViewProps,
        val resource: PolarisRootRes,
    )

    data class PolarisViewProps(
        val media_id: String,
        val media_owner_id: String,
        val media_type: Float,
        //val page_logging: Map<String, Any>,
        val user_id: String,
    )

    data class PolarisRootRes(val __dr: String)
    // post => "PolarisPostRoot.react"
    // story => "PolarisStoriesV3Root.react"
    // highlight => "PolarisStoriesV3HighlightsRoot.react"

    // data class PolarisMeta(val title: String/*, val accessory: Any?, val favicon: Any?*/)

    data class PolarisRootParams(
        val highlight_reel_id: String?,
        val initial_media_id: String?,
        val username: String?,
    )
}
