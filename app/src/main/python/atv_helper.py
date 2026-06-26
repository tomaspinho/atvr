"""atv_helper.py

Embedded Python module that wraps the `pyatv` library for the Android
ATV Remote app. Invoked from Kotlin via Chaquopy:

    Python.getInstance().getModule("atv_helper").callAttr("python_fn", args...)

All functions return a JSON string. The Kotlin DeviceRepository parses the
result. This module owns the asyncio event loop running on a background
thread, the dict of connected devices, the dict of active pairing sessions,
and the registered listener proxies (push / power / keyboard).

Credentials are NOT stored on the Python side. Kotlin CredentialStorage is
the single source of truth; credentials are passed into
`connect_to_device_sync(identifier, credentials)` as a parameter.
"""

from __future__ import annotations

import asyncio
import base64
import inspect
import json
import re
import threading
import time
from typing import Any, Dict, Optional

import pyatv
from pyatv import interface, const
from pyatv.const import FeatureName, FeatureState, InputAction, Protocol
from pyatv.interface import PushListener, PowerListener, KeyboardListener


# --------------------------------------------------------------------------- #
# Global state
# --------------------------------------------------------------------------- #

_connected_devices: Dict[str, "interface.AppleTV"] = {}
_pairing_sessions: Dict[str, "interface.Pairing"] = {}
_push_callbacks: Dict[str, Any] = {}
_power_callbacks: Dict[str, Any] = {}
_keyboard_listeners: Dict[str, Any] = {}

_global_loop: Optional[asyncio.AbstractEventLoop] = None
_loop_lock = threading.Lock()


# --------------------------------------------------------------------------- #
# Event loop management
# --------------------------------------------------------------------------- #

def _get_loop() -> asyncio.AbstractEventLoop:
    """Return the persistent background asyncio loop, starting it if needed."""
    global _global_loop
    with _loop_lock:
        if _global_loop is None or _global_loop.is_closed():
            _global_loop = asyncio.new_event_loop()
            t = threading.Thread(target=_run_loop, args=(_global_loop,), daemon=True)
            t.start()
        return _global_loop


def _run_loop(loop: asyncio.AbstractEventLoop) -> None:
    asyncio.set_event_loop(loop)
    loop.run_forever()


def _run_sync(coro):
    """Run a coroutine on the background loop, blocking the caller until done."""
    loop = _get_loop()
    future = asyncio.run_coroutine_threadsafe(coro, loop)
    return future.result()


def _to_json(obj: Dict[str, Any]) -> str:
    return json.dumps(obj)


def _error(message: str, **extra: Any) -> str:
    payload = {"success": False, "error": message}
    payload.update(extra)
    return _to_json(payload)


def _ok(**payload: Any) -> str:
    payload["success"] = True
    return _to_json(payload)


# --------------------------------------------------------------------------- #
# Listener proxies (called by pyatv; forward to JNI callbacks)
# --------------------------------------------------------------------------- #

class _PushProxy(PushListener):
    def __init__(self, identifier: str, callback):
        self._id = identifier
        self._cb = callback

    def playstatus_update(self, _updater, playstatus) -> None:
        try:
            self._cb.invoke(json.dumps(_playstatus_to_dict(playstatus)))
        except Exception as exc:  # pragma: no cover - JNI/runtime guard
            print(f"atv_helper push cb error: {exc}")

    def playstatus_error(self, _updater, exception) -> None:
        print(f"atv_helper push error: {exception}")


class _PowerProxy(PowerListener):
    def __init__(self, identifier: str, callback):
        self._id = identifier
        self._cb = callback

    def powerstate_update(self, _old, new) -> None:
        try:
            self._cb.invoke(str(new))
        except Exception as exc:  # pragma: no cover
            print(f"atv_helper power cb error: {exc}")


class _KeyboardProxy(KeyboardListener):
    def __init__(self, identifier: str, callback):
        self._id = identifier
        self._cb = callback

    def focusstate_update(self, _old, new) -> None:
        try:
            self._cb.invoke(bool(new.name == "Focused" if hasattr(new, "name") else new))
        except Exception as exc:  # pragma: no cover
            print(f"atv_helper keyboard cb error: {exc}")


# --------------------------------------------------------------------------- #
# Helpers
# --------------------------------------------------------------------------- #

_PROTOCOL_MAP = {
    "mrp": Protocol.MRP,
    "dmap": Protocol.DMAP,
    "airplay": Protocol.AirPlay,
    "companion": Protocol.Companion,
    "raop": Protocol.RAOP,
}


def _device(identifier: str) -> "interface.AppleTV":
    atv = _connected_devices.get(identifier)
    if atv is None:
        raise KeyError(f"Device {identifier} is not connected")
    return atv


def _playstatus_to_dict(ps) -> Dict[str, Any]:
    # In pyatv 0.16.x, Playing has flat properties (no nested .metadata).
    return {
        "device_state": str(ps.device_state) if ps.device_state else None,
        "media_type": str(ps.media_type) if ps.media_type else None,
        "position": ps.position,
        "total_time": ps.total_time,
        "repeat": str(ps.repeat) if ps.repeat else None,
        "shuffle": str(ps.shuffle) if ps.shuffle else None,
        "title": ps.title,
        "artist": ps.artist,
        "album": ps.album,
        "app": None,
        "artwork": None,
    }


# --------------------------------------------------------------------------- #
# Python functions (called from Kotlin via Chaquopy callAttr)
# --------------------------------------------------------------------------- #

def scan_devices_sync(timeout: float = 5.0, host: Optional[str] = None) -> str:
    """Scan for Apple TVs. When `host` is given, scan a single IP target."""
    async def _scan() -> list:
        kwargs: Dict[str, Any] = {"loop": _get_loop(), "timeout": float(timeout)}
        if host:
            # `host` is an IP address — use the `hosts` parameter (not
            # `identifier`, which matches against device UUIDs/MACs).
            kwargs["hosts"] = [host]
        return await pyatv.scan(**kwargs)

    try:
        configs = _run_sync(_scan())
    except Exception as exc:
        return _error(f"Scan failed: {exc}")

    # Apple TV device models (exclude HomePod, AirPort Express, iTunes, etc.)
    _APPLE_TV_MODELS = {
        const.DeviceModel.Gen2, const.DeviceModel.Gen3,
        const.DeviceModel.Gen4, const.DeviceModel.Gen4K,
        const.DeviceModel.AppleTV4KGen2, const.DeviceModel.AppleTV4KGen3,
        const.DeviceModel.AppleTVGen1,
    }

    devices = []
    for cfg in configs:
        model = cfg.device_info.model if cfg.device_info else const.DeviceModel.Unknown
        name = cfg.name or ""
        is_apple_tv = (
            model in _APPLE_TV_MODELS
            or name.lower().startswith("appletv")
            or name.lower().startswith("apple tv")
        )
        if not is_apple_tv:
            continue
        services = [s.protocol.name.lower() for s in cfg.services]
        devices.append({
            "name": cfg.name or "Apple TV",
            "address": str(cfg.address),
            "identifier": cfg.identifier or str(cfg.address),
            "model": str(model),
            "services": services,
        })
    return _ok(devices=devices)


def connect_to_device_sync(identifier: str, credentials: str = None) -> str:
    """Scan for the device, apply credentials, connect, start listeners."""
    async def _connect():
        loop = _get_loop()
        # Try scanning by identifier first (matches device UUID/MAC).
        configs = await pyatv.scan(identifier=identifier, loop=loop, timeout=5.0)
        if not configs:
            # Fall back to scanning by IP address (identifier may be the
            # device's address if it had no unique identifier).
            try:
                import ipaddress
                ipaddress.ip_address(identifier)
                configs = await pyatv.scan(hosts=[identifier], loop=loop, timeout=5.0)
            except ValueError:
                pass
        if not configs:
            raise RuntimeError(f"Device {identifier} not found on network")
        cfg = configs[0]
        if credentials:
            # credentials is a JSON map of {protocol: creds} or a single string.
            try:
                creds_map = json.loads(credentials)
            except (json.JSONDecodeError, TypeError):
                creds_map = {"mrp": credentials}
            for proto_name, cred in creds_map.items():
                proto = _PROTOCOL_MAP.get(proto_name)
                if proto is not None:
                    cfg.set_credentials(proto, cred)
        atv = await pyatv.connect(cfg, loop=loop)
        return atv

    try:
        atv = _run_sync(_connect())
    except Exception as exc:
        return _error(f"Connect failed: {exc}")

    # Close any existing session for this device before replacing it.
    old = _connected_devices.get(identifier)
    if old is not None:
        try:
            _run_sync(old.close())
        except Exception:
            pass

    _connected_devices[identifier] = atv

    # Start push updater if available (must run on the asyncio loop).
    try:
        features = atv.features
        if features.in_state(FeatureState.Available, FeatureName.PushUpdates):

            async def _start_push():
                atv.push_updater.start()

            _run_sync(_start_push())
    except Exception as exc:  # pragma: no cover
        print(f"atv_helper push start skipped: {exc}")

    return _ok(name=atv._config.name)


def disconnect_device(identifier: str) -> str:
    atv = _connected_devices.pop(identifier, None)
    if atv is None:
        return _error(f"Device {identifier} was not connected")
    try:
        _run_sync(atv.close())
    except Exception as exc:
        return _error(f"Disconnect error: {exc}")
    _push_callbacks.pop(identifier, None)
    _power_callbacks.pop(identifier, None)
    _keyboard_listeners.pop(identifier, None)
    return _ok(message="Disconnected")


def is_connected(identifier: str) -> bool:
    return identifier in _connected_devices


def start_pairing_sync(identifier: str, protocol: str = "auto") -> str:
    print(f"atv_helper: start_pairing_sync(identifier={identifier}, protocol={protocol})")

    # Close any existing pairing sessions for this device before starting anew.
    for key in list(_pairing_sessions.keys()):
        if key.startswith(f"{identifier}_"):
            try:
                _run_sync(_pairing_sessions[key].close())
            except Exception:
                pass
            _pairing_sessions.pop(key, None)

    pairable = ["mrp", "companion", "airplay", "dmap", "raop"]

    async def _begin():
        loop = _get_loop()
        configs = await pyatv.scan(identifier=identifier, loop=loop, timeout=5.0)
        if not configs:
            try:
                import ipaddress
                ipaddress.ip_address(identifier)
                configs = await pyatv.scan(hosts=[identifier], loop=loop, timeout=5.0)
            except ValueError:
                pass
        if not configs:
            raise RuntimeError(f"Device {identifier} not found")
        cfg = configs[0]
        available = {s.protocol.name.lower(): s.protocol for s in cfg.services}
        print(f"atv_helper: found device {cfg.name}, services={list(available.keys())}")

        if protocol == "auto":
            proto_str = next((p for p in pairable if p in available), None)
            if proto_str is None:
                raise RuntimeError(f"No pairable service found (available: {list(available.keys())})")
        else:
            proto_str = protocol

        proto = _PROTOCOL_MAP.get(proto_str, Protocol.MRP)
        if proto_str not in available:
            raise RuntimeError(f"Service {proto_str} not available on device (available: {list(available.keys())})")

        print(f"atv_helper: pairing with protocol={proto_str}")

        # Retry on backoff errors — the Apple TV enforces a cooldown after
        # a failed/cancelled pairing attempt (e.g. "BackOff=4s").
        max_retries = 3
        for attempt in range(max_retries):
            try:
                pairing = await pyatv.pair(cfg, proto, loop=loop)
                await pairing.begin()
                return pairing, proto_str
            except Exception as exc:
                msg = str(exc)
                backoff_match = re.search(r"BackOff=(\d+)s", msg)
                if backoff_match and attempt < max_retries - 1:
                    wait = int(backoff_match.group(1))
                    print(f"atv_helper: backoff {wait}s, retrying (attempt {attempt + 1}/{max_retries})")
                    await asyncio.sleep(wait + 1)
                    continue
                raise

    try:
        pairing, actual_proto = _run_sync(_begin())
    except Exception as exc:
        return _error(f"start_pairing failed: {exc}")

    session_key = f"{identifier}_{actual_proto}"
    _pairing_sessions[session_key] = pairing
    return _ok(session_key=session_key, protocol=actual_proto)


def finish_pairing_sync(session_key: str, pin: str) -> str:
    pairing = _pairing_sessions.get(session_key)
    if pairing is None:
        return _error("Session not found")

    try:
        pairing.pin(int(pin))
        _run_sync(pairing.finish())
        has_paired = bool(pairing.has_paired)
        creds = None
        if has_paired and pairing.service is not None:
            creds = pairing.service.credentials
    except Exception as exc:
        _pairing_sessions.pop(session_key, None)
        return _error(f"finish_pairing failed: {exc}")

    try:
        _run_sync(pairing.close())
    except Exception:
        pass
    _pairing_sessions.pop(session_key, None)

    if not has_paired:
        return _to_json({"success": False, "error": "Pairing failed"})

    proto = session_key.rsplit("_", 1)[-1]
    return _ok(credentials=creds, protocol=proto, device_id=session_key.rsplit("_", 1)[0])


def cancel_pairing_sync(session_key: str) -> str:
    pairing = _pairing_sessions.get(session_key)
    if pairing is None:
        return _error("Session not found")
    try:
        _run_sync(pairing.close())
    except Exception as exc:
        return _error(f"cancel_pairing error: {exc}")
    _pairing_sessions.pop(session_key, None)
    return _ok(message="Pairing cancelled")


def send_remote_command_sync(identifier: str, command: str, action: str = "press") -> str:
    atv = _device(identifier)
    input_action = InputAction.Hold if action == "hold" else InputAction.SingleTap

    remote = atv.remote_control
    audio = atv.audio
    power = atv.power

    method = None
    for obj in (remote, audio, power):
        if hasattr(obj, command):
            method = getattr(obj, command)
            break
    if method is None:
        return _error(f"Command {command} not found")

    async def _run_command():
        sig = inspect.signature(method)
        if "action" in sig.parameters:
            result = method(action=input_action)
        else:
            result = method()
        if inspect.isawaitable(result):
            await result

    try:
        _run_sync(_run_command())
    except Exception as exc:
        return _error(f"Command {command} failed: {exc}")
    return _ok(command=command, action=action)


def get_playing_info_sync(identifier: str) -> str:
    atv = _device(identifier)

    async def _info():
        ps = await atv.metadata.playing()
        artwork_b64 = None
        try:
            art = await atv.metadata.artwork(300, 300)
            if art is not None and art.bytes:
                artwork_b64 = base64.b64encode(art.bytes).decode("ascii")
        except Exception:
            artwork_b64 = None
        return ps, artwork_b64

    try:
        ps, artwork_b64 = _run_sync(_info())
    except Exception as exc:
        return _error(f"get_playing_info failed: {exc}")

    payload = _playstatus_to_dict(ps)
    if artwork_b64:
        payload["metadata"]["artwork"] = artwork_b64
    return _ok(**payload)


def get_power_state_sync(identifier: str) -> str:
    atv = _device(identifier)
    try:
        state = atv.power.power_state
    except Exception as exc:
        return _error(f"get_power_state failed: {exc}")
    return _ok(power_state=str(state))


def get_app_list_sync(identifier: str) -> str:
    atv = _device(identifier)

    async def _list():
        return await atv.apps.app_list()

    try:
        apps = _run_sync(_list())
    except Exception as exc:
        return _error(f"get_app_list failed: {exc}")

    payload = [{"name": a.name, "bundle_id": a.identifier} for a in apps]
    return _ok(apps=payload)


def launch_app_sync(identifier: str, bundle_id: str) -> str:
    atv = _device(identifier)

    target = bundle_id
    if target.lower().startswith("http://") or target.lower().startswith("https://"):
        # Deep-link URL launch instead of bundle id.
        target = target

    async def _launch():
        return await atv.apps.launch_app(target)

    try:
        _run_sync(_launch())
    except Exception as exc:
        return _error(f"launch_app failed: {exc}")
    return _ok(launched=bundle_id)


def set_push_callback_sync(identifier: str, callback) -> str:
    atv = _device(identifier)
    proxy = _PushProxy(identifier, callback)
    try:
        atv.push_updater.listener = proxy
        _push_callbacks[identifier] = proxy
    except Exception as exc:
        return _error(f"set_push_callback failed: {exc}")
    return _ok()


def set_power_callback_sync(identifier: str, callback) -> str:
    atv = _device(identifier)
    proxy = _PowerProxy(identifier, callback)
    try:
        atv.power.listener = proxy
        _power_callbacks[identifier] = proxy
    except Exception as exc:
        return _error(f"set_power_callback failed: {exc}")
    return _ok()


def start_keyboard_listener_sync(identifier: str, callback) -> str:
    atv = _device(identifier)
    proxy = _KeyboardProxy(identifier, callback)
    try:
        atv.keyboard.listener = proxy
        _keyboard_listeners[identifier] = proxy
    except Exception as exc:
        return _error(f"start_keyboard_listener failed: {exc}")
    return _ok()


def keyboard_text_set_sync(identifier: str, text: str) -> str:
    atv = _device(identifier)

    async def _set():
        return await atv.keyboard.text_set(text)

    try:
        _run_sync(_set())
    except Exception as exc:
        return _error(f"keyboard_text_set failed: {exc}")
    return _ok()


def keyboard_text_clear_sync(identifier: str) -> str:
    atv = _device(identifier)

    async def _clear():
        return await atv.keyboard.text_clear()

    try:
        _run_sync(_clear())
    except Exception as exc:
        return _error(f"keyboard_text_clear failed: {exc}")
    return _ok()