# E-INK Android Display Bus Scheduling System (Android 7.1)

Professional implementation guide for building a **bus scheduling display system** on an **Android 7.1 E‑INK device** using the SDKs, APIs, and demo app included in this repository.

---

## 1) Project Goal

Build a terminal app that:

1. Connects to a server (MQTT/CMS).
2. Receives JSON commands (route/schedule/media payloads).
3. Downloads/builds image content from command data.
4. Sends content to the E‑INK controller (T1000/IT8951 path).
5. Lets the controller generate and display the required EPD format output.

This repository already contains the required SDK/API packages and working demo projects to use as the foundation.

---

## 2) Repository Contents

Top-level package:

- `T1000&IT8951 SDK-3.0.0-20260120/`

Important subfolders:

- `T1000-Android API-3.0.0-20260120/`  
  T1000 display API jars + native `.so` + API PDF docs.
- `MCU-Android API-1.6.1-20260120/`  
  MCU power/serial API jars + native `.so` + API PDF docs.
- `xt-epd-deivce-demo/`  
  Main Android demo integrating MQTT + T1000 + MCU + USB resource playback.
- `xt-t1000-mcu-demo-3.0.0/`  
  Additional API demo app.

---

## 3) System Architecture

### Runtime pipeline

1. **Server publishes JSON** to device topic.
2. **Device MQTT service** subscribes and receives message.
3. **JSON is parsed** into image request object.
4. **Image file is downloaded** to local cache.
5. **EpdImage task is created** (position, width, height, mode, interval).
6. **Display queue receives task**.
7. **T1000 helper loads image** (`loadImage(...)`) and triggers display (`display(...)`).
8. **MCU layer manages power/device state** for stable display cycles.

### Main code entry points (demo)

- MQTT service:  
  `xt-epd-deivce-demo/app/src/main/java/com/xingtai/epd/device/demo/service/MyMqttService.kt`
- MQTT message listener:  
  `.../mqtt/listener/MyMqttNotifyListener.kt`
- JSON model:  
  `.../mqtt/entity/ImgRequest.kt`
- Image download + queue insert:  
  `.../mqtt/util/ImgDownloadUtils.kt`
- Display service:  
  `.../service/T1000Service.kt`
- Controller/display implementation:  
  `.../t1000/T1000Helper.kt`, `.../t1000/AbstractT1000Helper.kt`
- USB fallback parser (`slideShowImg.json`):  
  `.../t1000/util/UsbFlashDiskUtils.kt`
- Device setup UI (EPD type, serial, MQTT):  
  `.../ui/main/activity/MainActivity.kt`

---

## 4) Prerequisites

### Hardware

- Android E‑INK terminal device (Android 7.1 class target)
- EPD module + T1000 or IT8951 board
- MCU/power board (if your hardware includes it)
- USB cable and optionally FAT32 USB flash disk for offline resources

### Software

- Android Studio (recent stable)
- JDK 8+ (project uses Java 8 compatibility)
- Gradle wrapper (included in demo folders)
- Network access to your MQTT/CMS server

> Note: In restricted CI/sandbox environments, Gradle dependency download from `dl.google.com` may fail.

---

## 5) Open and Build the Demo

Use `xt-epd-deivce-demo` as the primary implementation base.

1. Open Android Studio.
2. Select project directory:  
   `T1000&IT8951 SDK-3.0.0-20260120/xt-epd-deivce-demo`
3. Let Gradle sync.
4. Build from IDE or run:

```bash
cd "T1000&IT8951 SDK-3.0.0-20260120/xt-epd-deivce-demo"
./gradlew assembleDebug
```

5. Install APK on the E‑INK device.

---

## 6) Initial Device Configuration (App UI)

Open app and configure in this order:

1. **EPD Type**  
   Select exact screen model/resolution.
2. **Battery Type**  
   - Built-in: `BATTERY_3S1P`  
   - Plug-in: `BATTERY_4S1P`  
   - Direct/no battery: `BATTERY_DIRECT`
3. **Serial Port**  
   Select Android↔MCU communication path.
4. **MQTT Settings**  
   Fill:
   - Client ID
   - Server IP
   - Port (default usually 1883)
   - Username / Password
   - Subscribe topic prefix
5. Press each **Confirm** button to persist settings.
6. Start services (**Start Service**): MQTT + T1000 service.

---

## 7) Server Integration (JSON Contract)

Example payload already supported by demo:

```json
{
  "action": "sendImg",
  "devId": "13b0b4eb2e717b9e",
  "url": "https://example.com/bus-stop-A-line-2.jpeg",
  "startX": 0,
  "startY": 0,
  "width": 3200,
  "height": 1800,
  "intervalTime": 30,
  "displayMode": 0
}
```

Field meaning:

- `action`: command type (`sendImg`)
- `devId`: device ID
- `url`: media URL to download
- `startX/startY`: display offset
- `width/height`: target area
- `intervalTime`: playback interval seconds
- `displayMode`: panel refresh mode

Topic convention in demo:

- Subscribe topic: `subTopic + deviceId` (configured in app)
- Publish topic (device -> server): `deviceToServer/{deviceId}`

---

## 8) Step-by-Step Implementation for Bus Scheduling

Use these steps to transform demo into a production bus scheduler:

1. **Define backend schema**  
   Include route number, destination, arrival time, stop name, service alerts, and media URL.
2. **Map schema to app model**  
   Extend `ImgRequest` or add a new request model for timetable/business fields.
3. **Render timetable card**  
   Build final visual image from text/layout template (server-side or app-side), then provide downloadable image URL.
4. **Receive and validate command**  
   In `MyMqttNotifyListener`, validate required fields before processing.
5. **Download and cache image**  
   Use `ImgDownloadUtils.download(...)`; add retry/fallback logic and cleanup policy.
6. **Create EpdImage task**  
   Set geometry (`startX/startY/width/height`), mode (`displayMode`), and schedule interval.
7. **Queue and interrupt sleep for urgent update**  
   Push image into T1000 queue and trigger `requestSleepInterrupted()` for immediate refresh.
8. **Display through controller**  
   `T1000Helper.display()` performs `loadImage()` then `display()`.
9. **Handle power + hardware resilience**  
   Keep MCU handling enabled for power on/off, reconnect, and abnormal USB communication.
10. **Add telemetry and acknowledgement**  
    Publish completion/error status back to server via MQTT publish topic.
11. **Enable boot autostart (optional)**  
    Use auto-boot option so services restart after power cycle.
12. **Pilot-test on real hardware**  
    Validate refresh quality, latency, and long-run stability before production rollout.

---

## 9) USB Offline Fallback Mode

Demo also supports USB package playback:

1. Format USB drive as FAT32.
2. Create directory: `My_Resources`
3. Add:
   - `slideShowImg.json`
   - `image/` folder with referenced files
4. Insert USB into device.
5. App auto-detects mount and parses package via `MediaReceiver` + `UsbFlashDiskUtils`.

Example `slideShowImg.json`:

```json
[
  {
    "name": "1.jpg",
    "startX": 0,
    "startY": 0,
    "width": 3200,
    "height": 1800,
    "intervalTime": 30,
    "displayMode": 0
  }
]
```

---

## 10) Verification Checklist

- Device can connect to MQTT server.
- Device subscribes to correct topic.
- JSON command is parsed without exception.
- Image downloads successfully.
- Display queue receives task.
- Controller `loadImage` and `display` return success.
- E‑INK screen shows expected schedule content.
- Device recovers after cable replug/power cycle.

---

## 11) Troubleshooting

- **Cannot build in CI/sandbox**: check outbound access to Maven/Google repos.
- **No display after message**: verify EPD type, width/height, and displayMode.
- **MQTT connected but no commands**: check topic suffix includes `deviceId`.
- **USB package not detected**: ensure folder name is exactly `My_Resources`.
- **Frequent transfer errors**: check USB cable quality, power stability, and MCU serial settings.

---

## 12) Recommended Next Production Enhancements

- Add strict JSON schema validation + versioning.
- Add signed command verification and MQTT TLS.
- Add rendering templates for multilingual route boards.
- Add watchdog/health metrics and remote diagnostics.
- Add queue prioritization for urgent dispatch notices.

---

## 13) Related Existing Docs

- `T1000&IT8951 SDK-3.0.0-20260120/readme.txt`
- `.../T1000-Android API-3.0.0-20260120/readme.txt`
- `.../MCU-Android API-1.6.1-20260120/readme.txt`
- `.../xt-epd-deivce-demo/instructions for use-en.md`

This README consolidates those materials into an implementation-oriented guide for a bus scheduling E‑INK terminal solution.
