# E-INK Android Display Bus Scheduling System (Android 7.1)

This repository contains an Android 7.1 E‑INK display implementation built around the Xingtai T1000 + MCU SDK stack.

The **current application workflow** in `xt-epd-deivce-demo` is now:

1. Foreground HTTP polling service requests bus data from SmartTrans API.
2. JSON response is parsed into a `BusTextRequest` model.
3. Text content is rendered into PNG images (full image or changed-row images).
4. Rendered images are enqueued as `EpdImage` tasks.
5. T1000 consumer thread loads and displays them on the E‑INK panel.
6. Full or partial refresh mode is selected based on change amount + panel type.

---

## 1) Repository layout

Top-level package:

- `T1000&IT8951 SDK-3.0.0-20260120/`

Main folders:

- `T1000-Android API-3.0.0-20260120/` – T1000 API jars/native libs/docs.
- `MCU-Android API-1.6.1-20260120/` – MCU API jars/native libs/docs.
- `xt-epd-deivce-demo/` – primary integrated Android app (current SmartTrans deployment base).
- `xt-t1000-mcu-demo-3.0.0/` – additional SDK demo.

Primary module map (`xt-epd-deivce-demo/settings.gradle`):

- `:app` (main app)
- `:mqtt` (MQTT support module)
- `:sys-api`
- `:smt40a-sys-api`

---

## 2) Current runtime architecture (what is active now)

### 2.1 App startup flow

Code references:

- `.../app/src/main/java/com/xingtai/epd/device/demo/ui/main/activity/MainActivity.kt`
- `.../app/src/main/java/com/xingtai/epd/device/demo/service/HttpPollingService.kt`
- `.../app/src/main/java/com/xingtai/epd/device/demo/service/T1000Service.kt`

Sequence:

1. `MainActivity` requests storage permissions.
2. App loads saved config:
   - Screen type + battery/autoboot (`AppConfigUtils`)
   - Serial config (`SpUtils.getSerialConfig()`)
   - MQTT config (`SpUtils.getMqttConfig()`) — retained for legacy compatibility.
3. If minimum valid config exists (screen type + serial), app starts:
   - `HttpPollingService`
   - `T1000Service`

### 2.2 Service responsibilities

- **`HttpPollingService`**: fetches bus data every 30 seconds, parses response, triggers render.
- **`T1000Service`**: manages USB device lifecycle, MCU handling, image display consumer thread, USB media receiver.

---

## 3) HTTP polling pipeline (server → display)

Code references:

- Polling/parsing: `.../service/HttpPollingService.kt`
- Rendering: `.../mqtt/util/BusTextRenderUtils.kt`
- Queue/device display: `.../t1000/AbstractT1000Helper.kt`, `.../t1000/T1000Helper.kt`

### 3.1 Polling

`HttpPollingService` runs as a foreground service and schedules `poll()` with fixed delay:

- Interval: **30s**
- Endpoint:
  - `https://www.smarttrans.ro/focsani/api/api-get-baza-dev--e-paper.php?stid=4`
- Timeout:
  - Connect: 10s
  - Read: 10s
- Request header: `Accept: application/json`

### 3.2 JSON parsing contract

The parser expects a **JSON array**, finds the first object where `id == 1`, then reads:

- `name` → station name
- `data.rawElements` (or fallback `data.rawElement`) → bus items

From each item (up to 5):

- `toStopName` → destination label
- `displayedTime` → arrival text

Mapped model:

- `BusTextRequest.action = "sendBusInfo"`
- `BusTextRequest.stationName = name`
- `BusTextRequest.lines = [BusLine(destinationStopName, arrivalTime)]`

If any required structure is missing, request is dropped and logged.

### 3.3 Render to image

`BusTextRenderUtils.render(request)`:

- Determines screen dimensions from `AppConfigUtils.screenType`.
- Builds portrait canvas (then rotates -90° for panel orientation).
- Renders **8 rows total**:
  - Row 0: header (station + NTP time `HH:mm`)
  - Rows 1–5: bus lines (destination left, arrival right)
  - Rows 6–7: reserved blank footer
- Alternating row style:
  - Even rows: black background / white text
  - Odd rows: white background / black text
- Long text is truncated with ellipsis.

Generated PNG files are stored under app cache image dir:

- `AppConstant.getImgDir()` (`.../cache/Img`)

### 3.4 Full vs partial refresh logic

`BusTextRenderUtils` compares new request content with cached state.

- **First render**: always full refresh.
- **If >4 rows changed**: full refresh.
- **If 1–4 rows changed**: partial row refresh only for changed rows.
- Header clock is refreshed every minute independently via scheduled task.

Display modes used:

- Color screen:
  - full = mode `0`
  - partial = mode `0`
- Mono screen:
  - full = mode `2` (GC16-like full refresh)
  - partial = mode `1` (DU-style partial refresh)

### 3.5 Queue + immediate wake-up

For each generated image/row:

1. Create `EpdImage` with geometry (`startX`, `startY`, `width`, `height`), display mode, format=file.
2. Enqueue using `T1000HelperFactory.instance?.addImage(epdImage)`.
3. If queue offer fails (queue full), clear queue and enqueue latest image.
4. Call `requestSleepInterrupted()` to wake consumer immediately and reduce latency.

Queue implementation details:

- Blocking queue in `AbstractT1000Helper`
- Max queue size: **2**
- Consumer thread loops in `T1000Helper.runConsumer()`

### 3.6 Device transfer + physical refresh

`T1000Helper.display(...)` executes:

1. Ensure USB permission + open connection.
2. Enable internal image cache on T1000.
3. Resolve image dimensions.
4. `loadImage(...)`
5. `display(...)`
6. Sleep for image interval unless interrupted by newer urgent task.

Error handling:

- Load/display/device exceptions set communication exception flag.
- Consumer can power-cycle via MCU path and retry.

---

## 4) Expected output on device

For each polling cycle with valid server data:

1. Screen shows station name in top header row.
2. Right side of header shows NTP-synced time (`HH:mm`).
3. Up to 5 arrivals are shown as destination + arrival text.
4. Bottom 2 rows remain blank (reserved).
5. If only few values change, only affected rows refresh (faster / less flashing).
6. If many values change, full refresh is used for visual consistency.

If server response is empty/invalid:

- No new image is pushed.
- Last displayed content remains.
- Error is logged.

---

## 5) Step-by-step Android device implementation/deployment

### Step 1 — Open and build

Project:

- `T1000&IT8951 SDK-3.0.0-20260120/xt-epd-deivce-demo`

Build:

```bash
cd "T1000&IT8951 SDK-3.0.0-20260120/xt-epd-deivce-demo"
./gradlew assembleDebug
```

> In restricted environments, build can fail if `dl.google.com` is blocked.

### Step 2 — Install APK on target E‑INK Android device

Install debug APK from Android Studio or adb.

### Step 3 — First-run configuration in UI

In `MainActivity`:

1. Select **EPD Type** (must match hardware panel).
2. Select **Battery Type** (for MCU power behavior).
3. Select **Serial Port**.
4. (Optional/legacy) fill MQTT fields.
5. Save each section with corresponding button.
6. Press **Start Service**.

### Step 4 — Runtime service start

Start actions trigger:

- `HttpPollingService.startService(...)`
- `T1000Service.startService(...)`

### Step 5 — Verify data path

1. Poll log appears every ~30s.
2. Parse log shows station and line count.
3. Render log indicates full/partial refresh strategy.
4. T1000 logs show `loadImage` + `display` success.

### Step 6 — Auto boot behavior (optional)

If `autoBootApp` enabled in app config:

- `DeviceBootReceiver` launches `MainActivity` on `BOOT_COMPLETED`.

---

## 6) Current data contract details

### 6.1 HTTP response shape expected by parser

```json
[
  {
    "id": 1,
    "name": "Station Name",
    "data": {
      "rawElements": [
        {
          "toStopName": "Downtown",
          "displayedTime": "3 min"
        }
      ]
    }
  }
]
```

Notes:

- Only object with `id=1` is used.
- `rawElement` (singular) is also accepted as fallback.
- Maximum displayed lines = 5.

### 6.2 Legacy MQTT path (still present in code)

Legacy classes still exist (`MyMqttService`, `MyMqttNotifyListener`) but the current UI startup uses HTTP polling service, not MQTT service.

---

## 7) Files to maintain (with purpose)

### Core runtime

- `.../service/HttpPollingService.kt`
  - Poll interval, endpoint URL, HTTP behavior, JSON parse mapping.
- `.../mqtt/util/BusTextRenderUtils.kt`
  - Layout, row rendering, full/partial logic, per-minute clock updates, file pruning.
- `.../t1000/AbstractT1000Helper.kt`
  - Queue, consumer thread lifecycle, sleep interrupt, USB detach behavior.
- `.../t1000/T1000Helper.kt`
  - Device open/load/display logic, error handling, MCU-assisted recovery.
- `.../service/T1000Service.kt`
  - Foreground service lifecycle, USB plug events, MCU init, resource playback control.

### App configuration and persistence

- `.../util/AppConfigUtils.kt`
  - Read/write `app_config.json`, screen type, autoboot, battery flags.
- `.../app/AppConstant.kt`
  - Storage paths, image cache directory, config constants.
- `.../util/SpUtils.kt`
  - MMKV persistence for serial + MQTT config.

### Startup and integration

- `.../ui/main/activity/MainActivity.kt`
  - User config flow and start/stop actions.
- `.../receiver/DeviceBootReceiver.kt`
  - Boot-completed launch behavior.
- `.../app/src/main/AndroidManifest.xml`
  - Permissions, service declarations, boot receiver registration.

---

## 8) Maintenance runbook

### 8.1 Change API endpoint or polling behavior

Edit:

- `HttpPollingService.API_URL`
- `POLL_INTERVAL_S`, connect/read timeouts
- Parse logic in `parseArrayPayload(...)`

### 8.2 Adjust display layout

Edit in `BusTextRenderUtils`:

- `MAX_LINES`, `TOTAL_ROWS`
- Row drawing styles (`drawRow`, `bgColor`, `fgColor`)
- Truncation behavior (`truncate`)

### 8.3 Tune refresh strategy

Edit in `BusTextRenderUtils.render(...)`:

- threshold `changes > 4` for full refresh
- display modes (0/1/2)
- clock tick schedule and row refresh mode

### 8.4 Diagnose queue latency / dropped updates

Inspect:

- `AbstractT1000Helper.MAX_IMAGES` (current queue size=2)
- queue overflow behavior in `BusTextRenderUtils.enqueueImage(...)`
- sleep interruption path `requestSleepInterrupted()`

### 8.5 Debug hardware communication issues

Inspect:

- `T1000Helper.display(...)` load/display return codes
- `T1000Helper.runConsumer()` exception-stop path
- `T1000Service` USB attach/detach and MCU init path

### 8.6 Manage generated image cache

- Cached PNGs are created in `cache/Img`.
- `BusTextRenderUtils.pruneOldFiles(...)` keeps latest 20 files.

### 8.7 Update startup defaults and persistent config

- App config file location and keys: `AppConstant`, `AppConfigUtils`.
- MMKV keys for serial/MQTT: `AppConstant.SpParams`, `SpUtils`.

---

## 9) Troubleshooting

- **No display**: verify selected screen type and serial config in `MainActivity`.
- **Foreground service not stable**: check manifest permissions (`FOREGROUND_SERVICE`, boot permission).
- **Data arrives but wrong text mapping**: confirm server fields match `toStopName` and `displayedTime` parse logic.
- **Clock inaccurate**: check network/NTP availability (`NtpClient` logs).
- **Slow or excessive flashing**: tune full/partial threshold and mode mapping in `BusTextRenderUtils`.
- **Repeated USB errors**: inspect T1000 cable/power + recovery logs in `T1000Helper` and `T1000Service`.

---

## 10) Notes for future contributors

- Current production-oriented flow is HTTP polling + on-device text rendering.
- MQTT code remains in repository as legacy/alternative path.
- Keep README and code references synchronized whenever endpoint, parser, row layout, or refresh policy changes.
