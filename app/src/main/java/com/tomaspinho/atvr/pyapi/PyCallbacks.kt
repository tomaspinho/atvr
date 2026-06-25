package com.tomaspinho.atvr.pyapi

import com.chaquo.python.PyObject
import com.tomaspinho.atvr.repository.DeviceRepository
import com.tomaspinho.atvr.domain.MediaInfo
import com.tomaspinho.atvr.domain.PowerState

/**
 * Bridges pyatv listener callbacks (called from Python) into Kotlin flows
 * exposed by [DeviceRepository]. Chaquopy lets a Kotlin class implement
 * methods that Python invokes directly; we expose these as a single object
 * whose methods are named `invoke(...)` matching the proxies in atv_helper.py.
 *
 * Because the proxies in atv_helper.py call `callback.invoke(payload)`, we
 * implement one callback per concern and inject it through PyApi.
 */
object PyCallbacks {

    /** Push updater callback: receives a JSON string of playstatus. */
    class PushCallback(private val repo: DeviceRepository) {
        fun invoke(jsonPayload: String) {
            val info = JsonPayloadParser.parseMedia(jsonPayload)
            if (info != null) repo.emitMediaInfo(info)
        }
    }

    /** Power listener callback: receives a power-state string. */
    class PowerCallback(private val repo: DeviceRepository) {
        fun invoke(state: String) {
            val parsed = when (state.lowercase()) {
                "on" -> PowerState.ON
                "off" -> PowerState.OFF
                "standby" -> PowerState.STANDBY
                else -> PowerState.UNKNOWN
            }
            repo.emitPowerState(parsed)
        }
    }

    /** Keyboard listener callback: receives a boolean focus state. */
    class KeyboardCallback(private val repo: DeviceRepository) {
        fun invoke(focused: Boolean) {
            repo.emitKeyboardFocus(focused)
        }
    }

    /** Wrap a Kotlin callback object as a Python-visible proxy. */
    fun proxy(callback: Any): PyObject = PyObject.fromJava(callback)
}