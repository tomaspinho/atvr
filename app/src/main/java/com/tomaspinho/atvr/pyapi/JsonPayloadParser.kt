package com.tomaspinho.atvr.pyapi

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import com.tomaspinho.atvr.domain.MediaInfo

/**
 * Parses the JSON payload that the Python `_PushProxy` sends on each
 * `playstatus_update`. The payload shape matches `_playstatus_to_dict`.
 */
object JsonPayloadParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseMedia(raw: String): MediaInfo? {
        return runCatching {
            val o = json.parseToJsonElement(raw) as JsonObject
            // pyatv 0.16.x Playing has flat properties — no nested metadata.
            MediaInfo(
                title = o["title"]?.jsonPrimitive?.contentOrNull,
                artist = o["artist"]?.jsonPrimitive?.contentOrNull,
                album = o["album"]?.jsonPrimitive?.contentOrNull,
                app = o["app"]?.jsonPrimitive?.contentOrNull,
                artwork = o["artwork"]?.jsonPrimitive?.contentOrNull,
                position = o["position"]?.jsonPrimitive?.contentOrNull?.toLongOrNull(),
                totalTime = o["total_time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull(),
                deviceState = o["device_state"]?.jsonPrimitive?.contentOrNull,
                mediaType = o["media_type"]?.jsonPrimitive?.contentOrNull
            )
        }.getOrNull()
    }
}