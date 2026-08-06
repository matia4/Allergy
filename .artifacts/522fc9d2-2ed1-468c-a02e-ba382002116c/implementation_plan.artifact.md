# Implementation Plan - Fix Render Issues in `allergy.xml`

The goal is to fix the `Resources$NotFoundException` in the Layout Preview for `allergy.xml`. The error is caused by `SwitchMaterial` failing to find its default thumb drawable resource. Additionally, there are several XML syntax and layout errors in the file that need to be addressed.

## User Review Required

> [!IMPORTANT]
> I am replacing `com.google.android.material.switchmaterial.SwitchMaterial` (Material 2) with `com.google.android.material.materialswitch.MaterialSwitch` (Material 3). This is recommended as the project is using a Material 3 theme (`Theme.Material3.DayNight.NoActionBar`). `MaterialSwitch` provides a better experience in Material 3 environments and avoids the legacy resource resolution issues seen with `SwitchMaterial` in some preview configurations.

## Proposed Changes

### [Layout Component]

#### [MODIFY] [allergy.xml](file:///D:/Android/AndroidStudioProjects/app/src/main/res/layout/allergy.xml)

- Remove duplicate `xmlns:android` declaration.
- Remove invalid `android:orientation="horizontal"` from `ConstraintLayout`.
- Fix `TextView` (`tvAllergyName`):
    - Change `android:layout_width` from `1dp` to `0dp`.
    - Remove `android:layout_weight` (not supported in `ConstraintLayout`).
    - Add missing `app:layout_constraintEnd_toStartOf` and `app:layout_constraintBottom_toBottomOf` to properly center it vertically and prevent overlapping with the switch.
- Fix `SwitchMaterial`:
    - Replace with `com.google.android.material.materialswitch.MaterialSwitch`.
    - Properly constrain it to the end of the parent and center it vertically.
- Add `tools:context=".MainActivity"` to help the layout editor resolve the application theme.

## Verification Plan

### Manual Verification
- I will use `analyze_file` to ensure the XML is syntactically correct.
- Since I cannot see the Layout Preview directly, I will rely on the fact that these changes address the specific error reported (missing AppCompat resource in `SwitchMaterial`) and fix the malformed XML.
