# PROJECT KNOWLEDGE BASE
**Project:** android_frameworks_base
**Seeded-at-ref:** crdroid/16.0
**Seeded-at-oid:** 15593548c424fbcc4d44b2d07160dbc530ee428d
**Seeded-at-date:** 2026-08-14
**Generated-policy-sha256:** 831fceec497e89cad3d18f57f71d7d9fbc2bf2498062efbc079821f326bd17f0

## UPSTREAM DISTILLATION

### Role and ownership map

This is the platform framework repository: public and hidden Java APIs, resources, JNI bridges, system-server implementations, framework applications, SystemUI, native support libraries, command-line services, and their tests. In the 1vivy personal ROM it is the framework consumer/integration layer above device and HAL implementations; do not place a device-specific HAL, proprietary backend, or device sepolicy domain here.

- [CURRENT_PATH] `core/` owns much of the framework Java surface, internal utilities, resources, JNI, and core tests. Read its child guide before changing it.
- [CURRENT_PATH] `services/` owns system-server startup and service implementations, including USB and location/GNSS service code. Read its child guide before changing it.
- [CURRENT_PATH] `packages/SystemUI/` is the privileged system UI and has a separate build/test graph and child guide.
- [CURRENT_PATH] `graphics/`, [CURRENT_PATH] `libs/hwui/`, and [CURRENT_PATH] `native/` carry graphics APIs, rendering implementation, and framework-native support.
- [CURRENT_PATH] `location/` contains client-facing location framework code; server implementations remain under [CURRENT_PATH] `services/`.
- [CURRENT_PATH] `api/` and [CURRENT_PATH] `core/api/` are API assembly and signature surfaces. API changes are not complete until annotations, generated/current signature files, and compatibility checks agree.
- [CURRENT_PATH] `cmds/` contains framework command and daemon entry points; [CURRENT_PATH] `packages/` contains framework-owned apps and providers beyond SystemUI.
- [CURRENT_PATH] `OWNERS`, area-specific [CURRENT_PATH] `*_OWNERS` files, nested [CURRENT_PATH] `OWNERS` files, and [CURRENT_PATH] `TEST_MAPPING` express review and test ownership. Use the narrowest applicable file.

### Framework, API, and service surfaces

- [CURRENT_PATH] `Android.bp` defines central modules including `framework-minus-apex`, its header jar, install dependencies, and cross-repository visibility.
- [CURRENT_PATH] `core/api/current.txt`, [CURRENT_PATH] `core/api/system-current.txt`, and sibling signature files are checked-in API contracts, not ordinary hand-maintained documentation.
- [CURRENT_PATH] `core/res/Android.bp` builds `framework-res`; [CURRENT_PATH] `core/res/res/` contains platform resources consumed across framework and system-server code.
- [CURRENT_PATH] `services/java/com/android/server/SystemServer.java` is the system-server composition root. A service addition needs deliberate lifecycle placement and a matching implementation/build dependency.
- [CURRENT_PATH] `services/usb/java/com/android/server/usb/UsbHostManager.java` and [CURRENT_PATH] `services/usb/java/com/android/server/usb/UsbDeviceManager.java` establish USB host and device service ownership here.
- [CURRENT_PATH] `services/core/java/com/android/server/location/LocationManagerService.java` and [CURRENT_PATH] `services/core/java/com/android/server/location/gnss/GnssManagerService.java` establish location and GNSS service ownership here; hardware implementations remain behind platform HAL interfaces.
- Binder/API work commonly spans manager or API declarations, service implementation, permissions/resources, build source sets, tests, and API text. Keep those halves in one reviewed change.

### Build graph and verification

Run commands from a configured Android build root, not from this repository in isolation.

- [CURRENT_COMMAND] `m framework-minus-apex services SystemUI` compiles the principal framework, system-server, and UI integration modules.
- [CURRENT_COMMAND] `atest FrameworksCoreTests` exercises the module declared by [CURRENT_PATH] `core/tests/coretests/Android.bp`.
- [CURRENT_COMMAND] `atest FrameworksServicesTests` exercises the service test module declared by [CURRENT_PATH] `services/tests/servicestests/Android.bp`.
- [CURRENT_COMMAND] `atest SystemUITests` exercises the SystemUI test module declared by [CURRENT_PATH] `packages/SystemUI/tests/Android.bp`.
- [CURRENT_PATH] `PREUPLOAD.cfg` is the formatting/check hook authority. Its scope is selective: do not assume one formatter applies uniformly to Java, Kotlin, C++, Blueprint, and hidden-API files.
- [CURRENT_PATH] `TEST_MAPPING` supplies repository-level presubmit groupings; nested mappings may narrow verification for a touched subsystem.

### Policy and extension boundaries

Framework permission declarations, manifest permissions, AppOps checks, Binder caller checks, service publication, and resource overlays are policy-sensitive framework surfaces. SELinux domains, service contexts, labels, and allow rules live in platform/device sepolicy repositories, not here. When adding a service or hardware-facing path, derive the external policy half from the real service/domain and keep the framework permission check; never weaken either layer to hide an integration failure.

crDroid and Lineage extensions are established precedents rather than reasons to bypass framework boundaries:

- [CURRENT_PATH] `core/java/com/android/internal/util/crdroid/` contains crDroid internal framework utilities.
- [CURRENT_PATH] `services/core/java/com/android/server/crdroid/` contains a crDroid service extension and is composed from SystemServer.
- [CURRENT_PATH] `services/core/Android.bp` and [CURRENT_PATH] `services/usb/Android.bp` show dependencies on Lineage platform internals.
- [CURRENT_PATH] `packages/SystemUI/src/com/android/systemui/crdroid/` contains crDroid UI extensions; Lineage settings/resources are consumed in adjacent SystemUI code.

Follow the existing pattern: framework API or internal contract first, service implementation at the owning server layer, presentation in SystemUI/Settings, and external hardware/policy in their owning repositories. Do not introduce a private device shortcut where an AOSP/Lineage extension seam already exists.

### Commit conventions

Recent seed history favors concise imperative subjects, usually area-prefixed (`core:`, `services:`, `SystemUI:`), with fixups kept visibly tied to their original subject. Keep API, implementation, tests, and generated signature consequences reviewable. Preserve upstream authorship when porting and do not add AI/agent trailers.

## OUR DELTAS

None at seed. Later entries must name topic commit OIDs and must not rewrite upstream truth.

Delivery-parent context (current tree, not seed truth): `12180491f4cc5af35f0a0a347d4d6bc5887ac4e1` is a linear thirteen-commit replay above the seed. Seven camera-layer commits end at `727730fc1c4bfa2c223f487acd7894ffa242be18`; six 1vivy commits then add Plus Key dispatch and battery/SystemUI integration. Treat those commits as inherited inputs to later personal-ROM work, not as upstream architecture.
