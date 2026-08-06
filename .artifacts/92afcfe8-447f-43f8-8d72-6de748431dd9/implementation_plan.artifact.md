# Implementation Plan - Fix Crash in Allergies Screen

The app crashes when opening the "My allergies" screen due to a `ClassCastException` in `AllergiesAdapter`. The code tries to cast a `MaterialSwitch` (defined in XML) to a `SwitchMaterial` (declared in Java). Additionally, the item layout for allergies is broken.

## Proposed Changes

### [UI Components]

#### [MODIFY] [AllergiesAdapter.java](file:///D:/Android/AndroidStudioProjects/app/src/main/java/com/example/allergy/AllergiesAdapter.java)
- Update the `AllergyViewHolder` to use `MaterialSwitch` instead of `SwitchMaterial`.
- Update the import statement to `com.google.android.material.materialswitch.MaterialSwitch`.

#### [MODIFY] [allergy.xml](file:///D:/Android/AndroidStudioProjects/app/src/main/res/layout/allergy.xml)
- Fix the layout constraints to properly position the allergy name and the switch.
- Change `android:layout_width="1dp"` to `0dp` (match constraint) for the TextView.
- Remove `android:layout_weight` (not used in ConstraintLayout).
- Ensure elements do not overlap.

## Verification Plan

### Automated Tests
- Build the project to verify that the code compiles with the updated types.

### Manual Verification
- Deploy the application to a device or emulator.
- Navigate to "My allergies".
- Confirm the screen opens without crashing.
- Verify that the layout displays the allergy name and switch correctly.
- Verify that toggling the switches updates the state.
