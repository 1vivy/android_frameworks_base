# SYSTEMUI GUIDE

## UPSTREAM DISTILLATION

SystemUI is a privileged, long-lived application spanning lockscreen, status/navigation bars, notifications, quick settings, biometrics, power indications, screenshots, and window-management UI. Preserve dependency-injection and feature boundaries; avoid turning presentation code into hardware or policy ownership.

### Where to look

- [CURRENT_PATH] `packages/SystemUI/Android.bp` defines `SystemUI-core`, `SystemUI-application`, resources, test utilities, and source groups.
- [CURRENT_PATH] `packages/SystemUI/src/com/android/systemui/` is the production Java/Kotlin tree; feature packages generally own controllers, repositories/interactors, views/Compose, and injection wiring together.
- [CURRENT_PATH] `packages/SystemUI/res/` owns application resources; Lineage platform resources and settings are established external extension inputs.
- [CURRENT_PATH] `packages/SystemUI/src/com/android/systemui/crdroid/` is the explicit crDroid UI-extension area, including OnTheGo, headers, logos, and battery-bar features.
- [CURRENT_PATH] `packages/SystemUI/tests/` declares `SystemUITests`; [CURRENT_PATH] `packages/SystemUI/tests/Android.bp` defines focused presubmit variants.

### Conventions and verification

Keep expensive or blocking work off UI callbacks, retain lifecycle-aware collection/registration, and update tests at the owning feature boundary. Hardware data must arrive through a maintained framework/service interface; SystemUI should not become a private HAL client. Settings keys and Lineage resources are cross-repository contracts, so verify their declaration and default/overlay owner.

- [CURRENT_COMMAND] `m SystemUI` compiles the application graph.
- [CURRENT_COMMAND] `atest SystemUITests` runs the broad test module from [CURRENT_PATH] `packages/SystemUI/tests/Android.bp`.

Kotlin formatting scope is explicitly listed in the repository [CURRENT_PATH] `PREUPLOAD.cfg`; follow nearby Java/Kotlin style and existing injection patterns rather than applying a repository-wide generic formatter assumption.

## DELIVERY-PARENT DELTAS

The current delivery parent changes [CURRENT_PATH] `packages/SystemUI/src/com/android/systemui/statusbar/KeyguardIndicationController.java` to consume charging data supplied by lower framework/service layers. Keep that presentation/service split intact in later personal-ROM work.
