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
