# xt-epd-deivce-demo

Primary Android demo app for E‑INK display using T1000 + MCU SDK.

## Current active flow in this app

The deployed flow in this module is:

1. `HttpPollingService` polls the SmartTrans HTTP API every 30 seconds.
2. JSON is parsed into `BusTextRequest` / `BusLine`.
3. `BusTextRenderUtils` renders bus text into PNG images.
4. Images are queued through `T1000HelperFactory`.
5. `T1000Helper` loads/displays images with full or partial refresh.

## Important files

- Polling: `app/src/main/java/com/xingtai/epd/device/demo/service/HttpPollingService.kt`
- Rendering: `app/src/main/java/com/xingtai/epd/device/demo/mqtt/util/BusTextRenderUtils.kt`
- Display queue/device: `app/src/main/java/com/xingtai/epd/device/demo/t1000/AbstractT1000Helper.kt`
- T1000 transfer/display: `app/src/main/java/com/xingtai/epd/device/demo/t1000/T1000Helper.kt`
- Service lifecycle: `app/src/main/java/com/xingtai/epd/device/demo/service/T1000Service.kt`
- UI/service start: `app/src/main/java/com/xingtai/epd/device/demo/ui/main/activity/MainActivity.kt`

## Build

```bash
cd "T1000&IT8951 SDK-3.0.0-20260120/xt-epd-deivce-demo"
./gradlew assembleDebug
```

## Update Log

### V1.0.0 2024-09-14

First edition

### V1.1.0 2024-09-27

Fix startup crash

### V1.2.0 2024-12-16

1. Add automatic front light on startup
2. Improve example projects

### V1.3.0 2025-06-06

Improve example projects

### V2.0.0 2025-07-10

1. Adjustment of dependency composition
2. Improve example projects

### V2.1.0 2025-09-16

1. Optimize image processing speed
2. Display image feedback more promptly
3. Add to Startup Preferences

### V2.1.1 2026-01-20

1. Support 16 KB page sizes
2. Optimise E6 display
