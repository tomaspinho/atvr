package com.tomaspinho.atvr.domain

/**
 * Result of a Python `atv_helper` call. All Python functions return a JSON
 * string of the form `{"success": bool, ...}`; DeviceRepository parses the
 * string into one of these sealed types.
 */
sealed interface PythonResult {
    data class Ok(val payload: Map<String, String?>) : PythonResult
    data class Err(val message: String) : PythonResult
}