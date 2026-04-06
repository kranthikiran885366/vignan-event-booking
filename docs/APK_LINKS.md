# APK Links

## Local APK Paths
- Debug APK:
  - app/build/outputs/apk/debug/app-debug.apk
- Release APK:
  - app/build/outputs/apk/release/app-release.apk

## GitHub Actions APK Artifacts
- Workflow: Android CI
- Artifact name: app-debug-apk
- Access path:
  - GitHub -> Actions -> Android CI -> latest successful run -> Artifacts

## GitHub Release APK Direct Link
After running release workflow, APK is published as release asset.

Use this URL pattern:
- https://github.com/<owner>/<repo>/releases/latest/download/app-release.apk

Replace <owner>/<repo> with your actual GitHub repository path.
