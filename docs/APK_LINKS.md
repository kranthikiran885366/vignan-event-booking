# APK and App Links

This file is the single source for repository, CI, release, and APK download links.

## Repository Links
- Repository: https://github.com/kranthikiran885366/vignan-event-booking
- Main branch: https://github.com/kranthikiran885366/vignan-event-booking/tree/main
- Actions: https://github.com/kranthikiran885366/vignan-event-booking/actions
- Releases: https://github.com/kranthikiran885366/vignan-event-booking/releases
- Pull requests: https://github.com/kranthikiran885366/vignan-event-booking/pulls
- Issues: https://github.com/kranthikiran885366/vignan-event-booking/issues

## Issue Templates
- New bug report: https://github.com/kranthikiran885366/vignan-event-booking/issues/new?template=bug_report.md
- New feature request: https://github.com/kranthikiran885366/vignan-event-booking/issues/new?template=feature_request.md

## Release APK Links
- Latest release APK: https://github.com/kranthikiran885366/vignan-event-booking/releases/latest/download/app-release.apk
- Release page (manual asset selection): https://github.com/kranthikiran885366/vignan-event-booking/releases

## CI Artifact APK (Debug)
1. Open Actions: https://github.com/kranthikiran885366/vignan-event-booking/actions
2. Open latest successful run of Android CI
3. Download artifact named app-debug-apk

## Release Publish Flow (Recommended)
1. Push commits to `main`
2. Run release workflow from Actions
3. Confirm release asset `app-release.apk` is uploaded
4. Share this stable link:
	- https://github.com/kranthikiran885366/vignan-event-booking/releases/latest/download/app-release.apk

## Local Build APK Paths
- Debug APK: app/build/outputs/apk/debug/app-debug.apk
- Release APK: app/build/outputs/apk/release/app-release.apk

## Quick Build Commands
```powershell
$env:GRADLE_USER_HOME="$PWD/.gradle-local"; ./gradlew.bat :app:assembleDebug --no-daemon
$env:GRADLE_USER_HOME="$PWD/.gradle-local"; ./gradlew.bat :app:assembleRelease --no-daemon
```

## Verification Tips
- If release APK link returns 404, confirm at least one GitHub Release exists.
- If CI artifact is missing, ensure Android CI workflow run completed successfully.
