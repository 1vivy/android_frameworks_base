# CORE FRAMEWORK GUIDE

## UPSTREAM DISTILLATION

[CURRENT_PATH] `core/` owns foundational Java APIs and internals, resources, JNI bridges, generated/config inputs, and broad framework tests. Changes here have platform-wide blast radius; distinguish public API, hidden/internal API, resource contract, and native implementation.

### Where to look

- [CURRENT_PATH] `core/java/android/` is the principal framework API implementation tree; [CURRENT_PATH] `core/java/com/android/internal/` is hidden platform implementation shared by trusted components.
- [CURRENT_PATH] `core/res/` owns platform resources and manifest declarations; resource IDs, config overlays, permissions, and styleables are compatibility surfaces.
- [CURRENT_PATH] `core/jni/` registers native bridges used by core Java classes. Keep Java declarations, JNI registration/signatures, ownership, and lifetime behavior synchronized.
- [CURRENT_PATH] `core/api/` carries API signature text and API build rules. Public/system/test/module-lib exposure must be intentional.
- [CURRENT_PATH] `core/tests/coretests/` declares `FrameworksCoreTests`; nearby specialized test directories cover narrower contracts.
- [CURRENT_PATH] `core/java/com/android/internal/util/crdroid/` is the established crDroid internal-utility extension seam. Do not expose it as public API accidentally.

### Conventions and verification

Use platform annotations and nullability consistently with the surrounding API. A Java/native bridge change requires matching types and ownership on both sides; a resource or permission change requires its consumer and overlay/security impact to be reviewed.

- [CURRENT_COMMAND] `m framework-minus-apex` compiles the central non-mainline framework module declared from the repository build graph.
- [CURRENT_COMMAND] `atest FrameworksCoreTests` runs the broad core test module from [CURRENT_PATH] `core/tests/coretests/Android.bp`.

API text is generated contract output. Do not edit signature files merely to silence compatibility checks, and do not add hidden reflection/type-casting shims when a maintained internal interface exists.

## DELIVERY-PARENT DELTAS

The current delivery parent modifies framework/JNI and graphics-adjacent files for Oplus compatibility, including [CURRENT_PATH] `core/java/android/hardware/HardwareBuffer.java`, [CURRENT_PATH] `core/java/android/media/ImageReader.java`, [CURRENT_PATH] `core/jni/android_hardware_HardwareBuffer.cpp`, and [CURRENT_PATH] `core/jni/android_media_ImageReader.cpp`. Review those as inherited replay deltas, not seed precedent.
