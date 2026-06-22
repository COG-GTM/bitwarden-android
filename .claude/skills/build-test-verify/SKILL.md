---
name: build-test-verify
version: 0.1.0
description: Build, test, lint, and deploy commands for the Bitwarden Android project. Use when running tests, building APKs/AABs, running lint/detekt, deploying, using fastlane, or discovering codebase structure. Triggered by "run tests", "build", "gradle", "lint", "detekt", "deploy", "fastlane", "assemble", "verify", "coverage".
---

# Build, Test & Verify

## Environment

- `GITHUB_TOKEN` (CI, `read:packages` scope): GitHub Packages auth for the Bitwarden SDK.
- Flavors: `standard` (Play Store), `fdroid` (no Google services). Build types: `debug`, `beta`, `release`.
- SDK resolution failures: verify `GITHUB_TOKEN` in `user.properties`/env and connectivity to `maven.pkg.github.com`.

## Building

```bash
./gradlew app:assembleDebug              # or authenticator:assembleDebug
./gradlew app:assembleStandardRelease    # release (needs signing keys); bundleStandardRelease for AAB
./gradlew app:assembleFdroidRelease      # F-Droid
```

## Testing

The `:app` module uses the `standard` flavor — use `testStandardDebugUnitTest`, NOT `testDebugUnitTest`. Gradle hides failure details, so always pipe through the grep filter to capture them on the first run.

```bash
./gradlew app:testStandardDebugUnitTest 2>&1 | grep -E "FAILED|BUILD|expected:|actual:|AssertionError|failures" | head -30
./gradlew app:testStandardDebugUnitTest --tests "com.x8bit.bitwarden.SomeTest" 2>&1 | grep -E "FAILED|BUILD|expected:|actual:|AssertionError|failures" | head -30
./gradlew :core:test :data:test :network:test :ui:test   # shared modules (no flavor)
./gradlew authenticator:testStandardDebugUnitTest
```

Full failure details: `app/build/reports/tests/testStandardDebugUnitTest/index.html`, or `find app/build/test-results -name "*.xml" -exec grep -l "failure" {} \;`.

Test fixtures live in `<module>/src/testFixtures/` (`core`: `FakeDispatcherManager`, `data`: `FakeSharedPreferences`, `network`: `BaseServiceTest`, `ui`: `BaseViewModelTest`/`BaseComposeTest`). Use MockK (`mockk`/`coEvery`), Turbine via `stateEventFlow()`, and inject `Clock` for deterministic time.

## Lint & Static Analysis

Prefer detekt on staged files only (`-Pprecommit=true` runs `git diff --name-only --cached`, same as the pre-commit hook) — a full scan is slow. Pipe through grep to surface violations on the first run.

```bash
git add -u && ./gradlew -Pprecommit=true detekt 2>&1 | grep -E "FAILED|BUILD|Line |Rule |Signature|detekt" | head -40
./gradlew detekt        # full scan (use sparingly)
./gradlew lint          # Android Lint
./fastlane check        # detekt + lint + tests + coverage
```

## Codebase Discovery

```bash
find ui/src/main/kotlin/com/bitwarden/ui/platform/components/ -name "Bitwarden*.kt" | sort   # UI components
grep -rl "BaseViewModel<" app/src/main/kotlin/ --include="*.kt"                              # ViewModels
find app/src/main/kotlin/ -name "*Navigation.kt" | sort                                      # @Serializable routes
find app/src/main/kotlin/ -name "*Module.kt" -path "*/di/*" | sort                           # Hilt modules
find app/src/main/kotlin/ -name "*Repository.kt" -not -name "*Impl.kt" -path "*/repository/*" | sort
grep -n "search_term" ui/src/main/res/values/strings.xml                                     # existing strings
```

## Deployment & Versioning

Version in `gradle/libs.versions.toml` (`appVersionCode`, `appVersionName`), pattern `YEAR.MONTH.PATCH`. Channels: Play Store (signed AAB via GitHub Actions), F-Droid (dedicated workflow + keys), Firebase App Distribution (beta).
