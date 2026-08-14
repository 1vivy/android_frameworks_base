# SYSTEM SERVICES GUIDE

## UPSTREAM DISTILLATION

[CURRENT_PATH] `services/` builds the system-server process and its service implementations. Startup ordering, Binder publication, boot phases, permissions, watchdog behavior, lock ordering, and cross-service calls are architectural contracts.

### Where to look

- [CURRENT_PATH] `services/java/com/android/server/SystemServer.java` is the composition and lifecycle root; place startup in the correct bootstrap/core/other service phase.
- [CURRENT_PATH] `services/core/Android.bp` assembles `services.core`; [CURRENT_PATH] `services/Android.bp` composes the broader `services` module and its dependencies.
- [CURRENT_PATH] `services/core/java/com/android/server/` owns most service implementations. Area-specific ownership files override broad repository ownership.
- [CURRENT_PATH] `services/usb/` owns USB host/device service code and depends on Lineage platform internals.
- [CURRENT_PATH] `services/core/java/com/android/server/location/` owns location and GNSS server integration; HAL/device implementation remains external.
- [CURRENT_PATH] `services/core/java/com/android/server/policy/PhoneWindowManager.java` is the central policy/input dispatch surface and already uses Lineage device-key extension contracts.
- [CURRENT_PATH] `services/tests/servicestests/` declares `FrameworksServicesTests`; [CURRENT_PATH] `services/tests/mockingservicestests/` carries focused mocked service tests.

### Service and policy rules

A new Binder service needs a stable interface owner, permission/caller enforcement, intentional publication name, SystemServer lifecycle wiring, build sources/dependencies, tests, and matching external SELinux service-context/domain policy. This repository owns the framework checks and lifecycle; platform/device sepolicy owns labels and allow rules. Never catch-and-ignore startup failures for a mandatory service or grant broad policy to compensate for a wrong service boundary.

Lineage/crDroid precedents include [CURRENT_PATH] `services/core/java/com/android/server/crdroid/CustomDeviceConfigService.java`, Lineage hardware/settings integrations in input and power services, and Lineage key-handler configuration in PhoneWindowManager. Reuse those seams rather than adding device-specific global hooks.

- [CURRENT_COMMAND] `m services` compiles the system-server service graph.
- [CURRENT_COMMAND] `atest FrameworksServicesTests` runs the broad service test module from [CURRENT_PATH] `services/tests/servicestests/Android.bp`.

## DELIVERY-PARENT DELTAS

The current delivery parent wires Oplus compatibility services and 1vivy behavior through [CURRENT_PATH] `services/java/com/android/server/SystemServer.java`, [CURRENT_PATH] `services/core/java/com/android/server/BatteryService.java`, and [CURRENT_PATH] `services/core/java/com/android/server/policy/InfinitiPlusKey.java`. Its matching focused tests include [CURRENT_PATH] `services/tests/mockingservicestests/src/com/android/server/policy/InfinitiPlusKeyTest.java`. These are inherited replay inputs, not seed architecture.
