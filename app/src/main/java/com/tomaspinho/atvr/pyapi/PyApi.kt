package com.tomaspinho.atvr.pyapi

import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import com.tomaspinho.atvr.domain.PythonResult

/**
 * Thin wrapper around `Python.getInstance().getModule("atv_helper")`.
 * Each call runs on Dispatchers.IO and returns a parsed [PythonResult].
 *
 * Functions that take a Java callback object pass the PyObject through
 * directly (the caller provides a PyCallback wrapper).
 */
object PyApi {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun module(): PyObject =
        Python.getInstance().getModule("atv_helper")

    private fun parse(raw: String): PythonResult {
        val obj = json.parseToJsonElement(raw) as JsonObject
        val success = obj["success"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
        return if (success) {
            // Keep primitive values as their string content; serialize arrays /
            // objects back to JSON strings so callers can re-parse them.
            PythonResult.Ok(obj.toMap().mapValues { (_, el) ->
                when {
                    el is JsonPrimitive -> el.contentOrNull
                    else -> el.toString()
                }
            })
        } else {
            val msg = obj["error"]?.jsonPrimitive?.contentOrNull ?: "Unknown error"
            PythonResult.Err(msg)
        }
    }

    private fun call(fn: () -> String): PythonResult =
        try {
            parse(fn())
        } catch (e: PyException) {
            PythonResult.Err(e.message ?: "Python error")
        }

    suspend fun scanDevices(timeout: Double = 5.0, host: String? = null): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("scan_devices_sync", timeout, host).toString() }
        }

    suspend fun connectToDevice(identifier: String, credentials: String? = null): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("connect_to_device_sync", identifier, credentials).toString() }
        }

    suspend fun disconnectDevice(identifier: String): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("disconnect_device", identifier).toString() }
        }

    suspend fun startPairing(identifier: String, protocol: String = "auto"): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("start_pairing_sync", identifier, protocol).toString() }
        }

    suspend fun finishPairing(sessionKey: String, pin: String): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("finish_pairing_sync", sessionKey, pin).toString() }
        }

    suspend fun cancelPairing(sessionKey: String): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("cancel_pairing_sync", sessionKey).toString() }
        }

    suspend fun sendRemoteCommand(identifier: String, command: String, action: String = "press"): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("send_remote_command_sync", identifier, command, action).toString() }
        }

    suspend fun getPlayingInfo(identifier: String): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("get_playing_info_sync", identifier).toString() }
        }

    suspend fun getPowerState(identifier: String): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("get_power_state_sync", identifier).toString() }
        }

    suspend fun getAppList(identifier: String): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("get_app_list_sync", identifier).toString() }
        }

    suspend fun launchApp(identifier: String, bundleId: String): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("launch_app_sync", identifier, bundleId).toString() }
        }

    suspend fun setPushCallback(identifier: String, callback: PyObject): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("set_push_callback_sync", identifier, callback).toString() }
        }

    suspend fun setPowerCallback(identifier: String, callback: PyObject): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("set_power_callback_sync", identifier, callback).toString() }
        }

    suspend fun startKeyboardListener(identifier: String, callback: PyObject): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("start_keyboard_listener_sync", identifier, callback).toString() }
        }

    suspend fun sendKeyboardText(identifier: String, text: String): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("keyboard_text_set_sync", identifier, text).toString() }
        }

    suspend fun clearKeyboardText(identifier: String): PythonResult =
        withContext(Dispatchers.IO) {
            call { module().callAttr("keyboard_text_clear_sync", identifier).toString() }
        }
}