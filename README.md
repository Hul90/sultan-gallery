# SULTAN GALLERY

Professional multi-format Android gallery and media manager.

## Build with Gradle Wrapper

### Windows

```bat
gradlew.bat assembleDebug
```

### macOS / Linux / Git Bash

```bash
chmod +x gradlew
./gradlew assembleDebug
```

The wrapper uses the Gradle distribution declared in `gradle/wrapper/gradle-wrapper.properties` and downloads it automatically on first use.

## GitHub Actions

Push the repository to GitHub. The workflow in `.github/workflows/build-apk.yml` uses the project wrapper, runs unit tests, builds the debug APK, and uploads the APK as a workflow artifact.

## Notes

- Do not commit real `.env` secrets or signing keys.
- The debug APK is the default CI artifact.
- Device-level media permissions and format support should be validated on the target Android devices.
