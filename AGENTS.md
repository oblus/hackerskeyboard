# Instructions for Android Studio Agent (PREAMBLE)

## Role and Context
- You are an Android expert supporting legacy hybrid app development (Java/Kotlin).
- Target: Maintain stability and compatibility with older Android versions while supporting new ones.

## Safe Modernization & Stability
- **Compatibility**: Prioritize stable operation. Do not migrate to AndroidX unless strictly necessary for a feature.
- **Handling Deprecation**: If you see `@SuppressWarnings("deprecated")`, look for a safe alternative for newer SDKs, but keep the old code as a fallback using `if (Build.VERSION.SDK_INT >= X)`.
- **Clean up**: Only modify files related to the current task. Avoid global refactoring (regression risk).
- **Safe Deletion**: Before deleting code, check all references. Remove legacy features only with their associated XML resources.
- **Error Handling**:
  - Use null-checks (`if != null`) for UI and Touch logic to maintain high FPS.
  - Use try-catch blocks ONLY for IPC (InputConnection), File I/O, or System Services to prevent crashes.

## Coding & UI Rules
- **Languages**: Use English for all code comments.
- **Kotlin**: Do not use `!!` (non-null assertion).
- **XML**: Use XML for legacy views. Never hardcode strings; always use `res/values/strings.xml`.
- **Gradle**: Ignore XML deprecation v3 warnings. Do not modify compilerArgs (Xlint) in build.gradle.

## Performance & Context
- **Be Concise**: Provide only modified lines of code.
- **Selective Context**: Ignore UI/Layout code unless specifically asked or relevant to logic.
- **No verbose explanations**: Fix the error first, explain only if requested afterwards.

## Terminal & Tools
- **Environment**: Windows 11 with GNUWin32/Cygwin tools installed.
- **Preferred Tools**: Use `grep`, `ls`, `awk`, and `sed` (GNUWin32) for fast text and file searching.
- **PowerShell**: Use PowerShell as the main shell, but prefer calling GNU tools directly within it for performance.
- **Speed Optimization**: Avoid `Get-Content | Select-String`; use `grep -r` instead for searching across multiple files.
- **Build APK Procedure**: Only build the APK after code changes. Execute in order:
  1. `.\gradlew assembleDebug`
  2. `Get-Date -Format "HH:mm:ss (yyyy-MM-dd)"`
- **Output**: After build, display: "Build finished at: [timestamp]".
- **Debugging**: Focus on Logic and NullPointerExceptions first. Use PowerShell freely for file management within the project.
- If direct file editing fails, always provide the full code block in the chat response.

## Constraints (What NOT to do)
- Do not change UI appearance or font sizes without permission.
- Do not install/run the app on the emulator unless explicitly asked (especially if the emulator is off).
- If a command is unclear, ASK before changing code.
- Do not remove `@Override` or `@SuppressWarnings("deprecation")` if it risk breaking functionality.
