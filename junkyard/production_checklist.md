# F-Droid Packaging Process — Step-by-Step Guide

## Prerequisites

- Fully open-source app with a libre license (GPL-3.0-or-later in Neruppu's case)
- Artifacts built from source (no proprietary SDKs or pre-built `.aar`/`.so` libraries)
- Git tags follow semantic versioning (e.g., `v1.0.0`, `v1.1.0`)

---

## Step 1: Understand the F-Droid Data Repository Structure

**Prompt:**
```
Explain the standard directory layout of the fdroiddata repository for a new Android app submission. Cover: the top-level structure (config.py, metadata/), how metadata/<PackageName>.yml works as a build recipe, where localized description text and screenshots live (metadata/<PackageName>/<locale>/), and what a typical metadata tree looks like for a Kotlin/Gradle multi-module app. Also explain the difference between metafiles inside metadata/ and the legacy root-level fdroid.yml format.
```

F-Droid distributes apps from a single centralized repo: **`fdroiddata`**.

```
fdroiddata/
├── config.py
├── metadata/
│   ├── org.havenapp.neruppu.yml          ← Build recipe (= "metafile")
│   └── org.havenapp.neruppu/
│       └── en-US/
│           ├── title.txt
│           ├── short_description.txt
│           ├── full_description.txt
│           ├── images/
│           │   ├── phoneScreenshots/
│           │   ├── sevenInchScreenshots/
│           │   └── tenInchScreenshots/
│           └── changelogs/
│               └── 1.txt                 ← changelog for versionCode 1
└── ...
```

- `metadata/<PackageName>.yml` — the build recipe
- `metadata/<PackageName>/<locale>/` — localized text & images
- Each build gets its own verified git tag commit in `fdroiddata`

---

## Step 2: Fork fdroiddata and Create the Metadata File

**Prompt:**
```
Walk me through forking and cloning the fdroiddata GitLab repository, setting upstream remotes (origin = my fork, upstream = fdroid/fdroiddata), and creating the initial metadata/ directory structure. Then create a complete, valid metadata/org.havenapp.neruppu.yml build recipe for a modern Gradle multi-module Kotlin Android app. The app has applicationId org.havenapp.neruppu, minSdk 26, targetSdk 35, compileSdk 35, uses AGP 8.2.2, Hilt, Room, and CameraX. It has no native .so libraries. The repo is https://github.com/thamizh-root/neruppu.git. The first release tag is v1.0.0. Include all required YAML keys (Categories, License, SourceCode, IssueTracker, Changelog, AutoName, Name, Summary, Description, RepoType, Repo, Builds, UpdateCheck, AutoUpdateMode). Set subdir to app because it's a multi-module Gradle project. Set gradle: yes and android_update_build_tools: true for AGP 8 + compileSdk 35. Remove ndk.
```

```bash
# Clone your fork
git clone https://gitlab.com/<your-username>/fdroiddata.git
cd fdroiddata
git remote add upstream https://gitlab.com/fdroid/fdroiddata.git
```

Create `metadata/org.havenapp.neruppu.yml`:

```yaml
Categories:
  - Internet
  - Security
License: GPL-3.0-or-later
SourceCode: https://github.com/thamizh-root/neruppu
IssueTracker: https://github.com/thamizh-root/neruppu/issues
Changelog: https://github.com/thamizh-root/neruppu/releases

AutoName: Neruppu
Name: Neruppu
Summary: Physical security monitoring app
Description: |-
  Offline-first security monitoring app that uses device sensors (camera, microphone,
  accelerometer, light) to detect and record security events. Optional remote alerts
  via Matrix.

RepoType: git
Repo: https://github.com/thamizh-root/neruppu.git

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0          # Git tag or SHA
    subdir: app              # Points to :app module in multi-module Gradle
    gradle: yes
    timeout: 600
    android_update_build_tools: true  # Required for AGP 8 + compileSdk 35
    # ndk: 25.1.8937393     # Only if native .so libs are present
    # output: app/build/outputs/apk/release/app-release-unsigned.apk

UpdateCheck: RepoManifest   # Reads versionCode from AndroidManifest.xml on default branch
AutoUpdateMode: RepoManifest
```

### What each block does

| Field | Purpose |
|-------|---------|
| **Categories** | Must match F-Droid's approved category list (e.g., `Internet`, `Security`). |
| **License** | SPDX identifier. F-Droid only accepts libre licenses. |
| **SourceCode / IssueTracker / Changelog** | URLs displayed on the app page. `Changelog` can also be local `changelogs/<versionCode>.txt`. |
| **AutoName** | Override the manifest `applicationLabel` if the APK contains promo text. Omit if the APK name is clean. |
| **RepoType / Repo** | Where F-Droid fetches source. `git` is standard. |
| **Builds** | Array of build recipes. F-Droid checks out the exact `commit`, sets `subdir` to your `:app` module, runs `./gradlew assembleRelease`, and maps `versionName`/`versionCode` to the produced APK. |
| **subdir** | Critical for multi-module Gradle projects like Neruppu. Tells F-Droid to run Gradle from `app/`. |
| **gradle: yes** | Runs `./gradlew`. |
| **ndk** | Pulls a specific NDK version. Remove unless you have `jniLibs/` or native dependencies. |
| **android_update_build_tools: true** | Forces F-Droid to install matching build-tools for your AGP version. |
| **UpdateCheck** | How F-Droid discovers newer versions. `RepoManifest` parses `AndroidManifest.xml` on the default branch. `Tags` requires tags to match `v\d+\.\d+`. |
| **AutoUpdateMode** | Must mirror `UpdateCheck`. |

---

## Step 3: Add Localized Metadata (Optional but Recommended)

**Prompt:**
```
Create the full localized metadata directory tree for Neruppu under metadata/org.havenapp.neruppu/en-US/. I need:
1. title.txt — just "Neruppu"
2. short_description.txt — one-line: "Offline-first security monitoring app"
3. full_description.txt — multi-line description covering camera, microphone, accelerometer, light sensors, Matrix alerts, offline-first architecture, and GPL-3.0 license
4. images/phoneScreenshots/ directory with a README note on screenshot specs (PNG/JPG, 320-1280px, 16:9 or 4:3)
5. changelogs/ directory with a sample 1.txt for versionCode 1

Also list the exact bash mkdir commands to create this tree and explain what F-Droid displays from each file on the app's client page.
```

```bash
mkdir -p metadata/org.havenapp.neruppu/en-US/images/phoneScreenshots
```

Create the text files:

- `metadata/org.havenapp.neruppu/en-US/title.txt` → `Neruppu`
- `metadata/org.havenapp.neruppu/en-US/short_description.txt` → one-line summary
- `metadata/org.havenapp.neruppu/en-US/full_description.txt` → multi-line description

Screenshots: PNG/JPG, 320–1280px wide, 16:9 or 4:3 aspect ratio. Place in images subdirectories.

---

## Step 4: Test the Build Locally Using fdroidserver

**Prompt:**
```
Provide the exact terminal commands to install fdroidserver, initialize a local fdroiddata test environment, and validate the Neruppu build recipe. Include:
1. pip install command for fdroidserver
2. Required environment variables (ANDROID_HOME, JAVA_HOME) with example paths
3. fdroid init command and what it creates
4. fdroid readmeta command to validate YAML syntax
5. fdroid lint command to check policy compliance
6. fdroid update command to fetch source and verify version mapping
7. fdroid build --no-tarball org.havenapp.neruppu command to compile the APK

Also list common Neruppu-specific failure modes and their fixes: AGP 8.2.2 + compileSdk 35 incompatibility with F-Droid's pinned AGP, missing android_update_build_tools flag, Kapt/Hilt annotation processor caching issues, and how to read the resulting APK path from tmp/.
```

```bash
# Install fdroidserver (Debian/Ubuntu: apt install fdroidserver)
pip install --user fdroidserver

# Set Android SDK and JDK 17 paths
export ANDROID_HOME="$HOME/Android/Sdk"
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"   # Adjust for your OS

# Initialize minimal repo structure
fdroid init

# Validate YAML syntax
fdroid readmeta metadata/org.havenapp.neruppu.yml

# Lint metadata (policy checks)
fdroid lint metadata/org.havenapp.neruppu.yml

# Fetch source and verify version mapping
fdroid update

# Build the APK locally (skips source tarball creation)
fdroid build --no-tarball org.havenapp.neruppu
```

### Expected outputs

- Success → APK at `tmp/org.havenapp.neruppu_1.apk` (or `unsigned` variant)
- Failure → Gradle log printed by `fdroidserver` for debugging

### Neruppu-specific considerations

- **AGP 8.2.2 + compileSdk 35**: If F-Droid's pinned AGP is older than 8.3–8.4+, the build may error. If you hit this, temporarily pin `compileSdk` to 34 in the metafile using `ext` override, or wait for F-Droid to update its AGP stack.
- **Kapt / Hilt**: Annotation processors run fine in F-Droid's clean environment once dependencies are cached.
- **Remove `ndk` line** unless you have native `.so` libraries — it slows builds unnecessarily.

---

## Step 5: Commit, Push, and Open the Merge Request

**Prompt:**
```
Provide the exact git commands to commit the F-Droid metadata (and optional localized assets/screenshots) to my fdroiddata fork, push to my GitLab namespace, and open a Merge Request against fdroid:master. Include:
1. git status / git diff review before staging
2. git add commands for metadata/org.havenapp.neruppu.yml and metadata/org.havenapp.neruppu/
3. git commit with a proper F-Droid-style message ("Add <AppName> - <short description>")
4. git push to origin main
5. The GitLab UI steps or glab CLI command to open the MR targeting fdroid:master

Also explain what happens after the MR is opened: CI lint + build pipeline, manual source audit by F-Droid reviewers, what a "verified git tag" means in fdroiddata, and how the app gets merged into the main repository.
```

```bash
git add metadata/org.havenapp.neruppu.yml
git add metadata/org.havenapp.neruppu/  # if you added screenshots/changelogs
git commit -m "Add Neruppu - Offline-first security monitoring app"
git push origin main
```

Then open a **Merge Request** against `fdroid:master` on GitLab.

### What happens after you open the MR

1. F-Droid CI runs `fdroid lint` and a full server build
2. A reviewer performs a manual source audit (license checks, dependency review, etc.)
3. If approved, a verified git tag is added to `fdroiddata` pointing to your metafile snapshot
4. The app is merged into the main F-Droid repository and appears in the client

---

## Step 6: Maintain and Update

**Prompt:**
```
List the exact steps to push an update for an already-published F-Droid app, using Neruppu as the example. Cover:
1. How to release a new version in the upstream repo (git tag v1.1.0, bump versionCode in app/build.gradle.kts)
2. Whether the fdroiddata metafile needs manual editing if AutoUpdateMode is set to RepoManifest vs Tags
3. How to verify the new version is detected (fdroid update, fdroid build)
4. How to submit the update commit to fdroiddata (direct push vs MR via fdroiddata-helper bot)
5. How the F-Droid client receives the update from the signed repository

Also explain the difference between AutoUpdateMode: RepoManifest (reads AndroidManifest.xml on default branch) and AutoUpdateMode: Tags (uses semantic version tags).
```

When you release a new version of Neruppu:

1. Tag the release in your upstream repo: `git tag v1.1.0 && git push --tags`
2. Bump `versionCode` in `app/build.gradle.kts`
3. Update the `Builds` entry in `metadata/org.havenapp.neruppu.yml` (or rely on `AutoUpdateMode`)
4. Push a commit to `fdroiddata` or merge an fdroiddata-helper MR

> **Tip:** Rename your git tags from date-stamps (e.g., `v2026.06.30.044049`) to semantic versions (`v1.0.0`, `v1.1.0`) so `UpdateCheck: Tags` works automatically.
