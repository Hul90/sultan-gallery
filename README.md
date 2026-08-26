# SULTAN GALLERY

Professional Android gallery and media manager by MD SULTAN MAHAMUD.

## GitHub APK Build

The repository is configured for GitHub Actions. Upload the project contents to a GitHub repository and run the **Build Sultan Gallery Android APK** workflow.

The workflow installs the pinned Gradle version, runs the debug unit tests, builds the debug APK, and uploads the APK as a workflow artifact. No local `.env` or signing key is required for the debug build.

## Runtime permissions

On Android 13+ the app requests photo/video/audio media permissions. On Android 14+ Android may grant **limited selected-photo/video access**; in that mode Android intentionally exposes only the media selected by the user. SULTAN GALLERY detects that state and provides a **Manage** action that opens the app permission settings so the user can change access.

## Developer

**MD SULTAN MAHAMUD**  
Email: sultanmahamud5497@gmail.com  
Mobile: 01740-236384
