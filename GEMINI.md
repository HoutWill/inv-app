# Project Architecture & Design Rules

## UI Design Rules

### 1. Modal & Popup Standard: Always Use Swipe-Up Modal Bottom Sheets
- **MANDATORY**: Never use center-floating popup dialogs (`AlertDialog` or centered `Dialog(...) { Card { ... } }`) for user input, creation forms, editors, restock/movement forms, or operational workflows.
- **STANDARD PATTERN**: All modals must swipe up from the bottom using Material 3 `ModalBottomSheet`:
  - **State**: `val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)`
  - **Drag Handle**: Include a clean top drag handle (`BottomSheetDefaults.DragHandle(...)`).
  - **Shape**: Rounded top corners (`RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)`).
  - **Padding**: Handle `Modifier.navigationBarsPadding().imePadding()` so keyboards never obscure inputs or action buttons.
  - **Dismissal**: Smooth animated swipe-down dismissal and backdrop scrim tap dismissal.
  - **Styling**: Consistent with Nawano theme tokens (`NawanoDark200` / `NawanoDarkCard` in dark mode, `Color.White` / `Color(0xFFF8FAFC)` in light mode, primary accent `NawanoPrimary200` #7871F8).

## Device & Testing Target Rules

### 1. Allowed Test Devices
- **Primary Emulator**: Test exclusively on **Medium Phone API** (`Medium_Phone_API_36.1`, running on `emulator-5556`).
- **Target Brand**: Test on **Xiaomi** devices (physical Xiaomi devices connected via ADB or Xiaomi screen profiles).
- **STRICT PROHIBITION**: Never target or send ADB commands, taps, or builds to `emulator-5554` (reserved third-party/game emulator). Always explicitly pass `-s emulator-5556` (or the connected Xiaomi device serial) for all ADB operations.
