

### F11. Post-Upload Media Cleanup Policy
- **Issue**: No automatic or manual deletion of captured photos/audio after upload to Telegram or Matrix.
- **Requirement**: Before launch, implement a media lifecycle policy:
  - **Auto-delete after successful upload** — when a media file is successfully pushed to Telegram or Matrix, the local copy in `filesDir/captures/` and `filesDir/audio_captures/` must be deleted.
  - **Mutual exclusion of upload targets** — users must be restricted to selecting at most one remote target (Telegram OR Matrix, not both). Simultaneous dual-target upload adds unnecessary complexity and increases the risk of partial-failure states leaving orphaned files.
  - Delete photos/media after uploading, but keep event logs and show upload status such as `Uploaded to Telegram` or `Uploaded to Matrix`.
  - Both logs and media can be deleted from the Logs screen delete button with password confirmation.
  - Delete password must be set from the Settings screen. Users must be able to add, reset, and remove it.
  - Security concern: if someone has app access, they may open Settings and reset/delete the password. A normal app-level password is not enough for high-risk protection.
  - Network failure handling: pending media must remain stored locally and retry automatically until upload succeeds.
- **Implementation steps**:
  1. Add `AlertTarget` enum with `NONE`, `TELEGRAM`, and `MATRIX`.
  2. Store the active target in encrypted settings.
  3. Enforce mutual exclusion when saving Telegram or Matrix config:
     - Saving Telegram sets target to `TELEGRAM` and clears Matrix config.
     - Saving Matrix sets target to `MATRIX` and clears Telegram config.
     - Upload worker reads only the active target.
  4. Extend the `Event` domain/data model and Room entity with upload metadata:
     - `uploadStatus`: `PENDING`, `UPLOADED`, or `FAILED`.
     - `uploadTarget`: Telegram or Matrix.
     - `uploadedAt`: upload success timestamp.
     - `failureReason`: optional retry/debug text.
  5. Save media paths in Room, but do not delete them until upload success is confirmed.
  6. Create a `MediaUploadRepository` or extend `HandleSensorEventUseCase` so sensor events enqueue media uploads after capture.
  7. Create a `MediaUploadWorker` using WorkManager with `NetworkType.CONNECTED` constraints.
  8. Worker flow:
     - fetch pending events with media paths.
     - read active upload target.
     - upload the media through Telegram or Matrix.
     - on success, update event status to `UPLOADED`, set target and timestamp, then delete the local media file.
     - on failure, update event status to `FAILED` or leave it pending, keep the media file, and retry later.
  9. Use WorkManager exponential backoff for network failures.
  10. Add upload status badges to `LogsScreen`:
      - `Pending upload`
      - `Uploaded to Telegram`
      - `Uploaded to Matrix`
      - `Upload failed`
  11. If media was uploaded and deleted locally, show a placeholder such as `Uploaded and deleted locally` instead of previewing the file.
  12. Add delete-password storage using salted PBKDF2 hash values, never plain text.
  13. Add Settings UI for:
      - set delete password
      - change delete password
      - remove delete password
  14. Require old password or biometric confirmation before changing/removing the delete password.
  15. Require password confirmation before deleting logs/media from the Logs screen.
  16. Consider requiring the same password/biometric before opening sensitive Settings pages, because someone with unlocked app access could otherwise reset the delete password.
  17. Optional stronger protection: add biometric confirmation for destructive actions and a decoy/empty mode when the wrong delete password is entered.
  18. Optional cleanup policy: add age/size-based eviction for failed uploads, but never delete pending media before successful upload unless the user explicitly deletes the event log.


Need maybe maybe use "bird by bird" order.
Let's craft with 12 chunks:
0. Baseline/test command.
1. Domain model upload metadata.
2. Data/Room migration/DAO queries.
3. AlertTarget encrypted settings and mutual exclusion.
4. Media upload repository abstraction and existing transports integration.
5. WorkManager worker and retry policy.
6. Enqueue from sensor events.
7. Local media cleanup after success.
8. Logs screen statuses/placeholders.
9. Delete password crypto.
10. Settings UI password add/change/remove.
11. Logs delete flow with password.
12. Security hardening: biometric or protected settings.
13. F-Droid publishing checklist/release.
Need include "acceptance criteria and test commands". Maybe "Recommended chunk size 30-90 min".
Need maybe "Chunk dependencies" and "do not start next until tests pass".
Need mention "network problem" in chunk 5/6.
Need maybe "Data migration test with old DB".
Need maybe "F-Droid risk: WorkManager persists; no Google Play services".
Need maybe "Potential problem: deleting local media before marking uploaded can lose evidence; mark uploaded then delete."
Need maybe "Manual deletion with password must delete media even if pending upload; update upload status CANCELLED/DELETED."
Need maybe "Event status enum include DELETED? Actually event log deletion removes; maybe not. For media after manual deletion show missing/deleted."
Need maybe "password reset security: require old password or biometric; if lost, cannot reset without clearing app data".
Need maybe "if user removes password, require old password." Good.
Need maybe "No direct reset button".
Need maybe "chunks and tests".
Let's final.
/agents                    
Switch agent
/compact                   
Compact session
/connect                   
Connect provider
Code 
·
Auto Free
Kilo Gateway
14.7K (6%)
ctrl+p commands
IDX Disabled
Split F11 into these independent chunks. Each chunk should build/test before moving to the next.
Chunk 1 — Upload metadata model
Goal: Add upload state to Event.
Files likely affected:
- domain/.../Event.kt
- data/.../EventEntity.kt
- data/.../EventDao.kt
- data/.../EventMapper.kt
- ui log/state code
Add:
enum class UploadStatus {
    PENDING,
    UPLOADED,
    FAILED
}
Event fields:
uploadStatus: UploadStatus = UploadStatus.PENDING
uploadTarget: String? = null
uploadedAt: Long? = null
failureReason: String? = null
Tests:
- mapper test: domain event ↔ Room entity.
- Room migration test if DB version changes.
- existing tests updated for new fields.
---
Chunk 2 — Room migration and DAO queries
Goal: Make pending uploads queryable.
Add DAO methods:
@Query("SELECT * FROM events WHERE uploadStatus = 'PENDING' AND mediaUri IS NOT NULL ORDER BY createdAt ASC LIMIT :limit")
suspend fun getPendingUploadEvents(limit: Int): List<EventEntity>
or use integer status column instead of string enum.
Important F-Droid-safe choice: use integer enum columns, not serialized JSON.
Tests:
- insert old-style event.
- migrate DB.
- query pending upload events.
---
Chunk 3 — Active upload target settings
Goal: Enforce Telegram OR Matrix.
Add:
enum class AlertTarget {
    NONE,
    TELEGRAM,
    MATRIX
}
Store in EncryptedSharedPreferences.
Rules:
- Saving Telegram config:
  - set AlertTarget.TELEGRAM
  - clear Matrix config
- Saving Matrix config:
  - set AlertTarget.MATRIX
  - clear Telegram config
- Worker reads only active target.
Files likely affected:
- Telegram config repository
- Matrix config repository
- Settings ViewModel
- Settings UI
Tests:
- saving Telegram clears Matrix.
- saving Matrix clears Telegram.
- no config means AlertTarget.NONE.
---
Chunk 4 — Media upload repository abstraction
Goal: Create one upload coordinator.
Create:
interface MediaUploadRepository {
    suspend fun enqueueUpload(eventId: Long)
    suspend fun uploadPendingEvents()
}
Responsibilities:
- fetch pending events
- read active target
- upload media
- update event status
- delete media only after success
Do not delete media inside Telegram/Matrix transport.
Tests:
- fake transport success updates event and deletes file.
- fake transport failure keeps file and keeps status pending/failed.
- no active target does not upload.
---
Chunk 5 — WorkManager upload worker
Goal: Retry uploads when network is unavailable.
Create:
class MediaUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params)
Constraints:
NetworkType.CONNECTED
Retry:
setBackoffCriteria(
    BackoffPolicy.EXPONENTIAL,
    15.minutes,
    TimeUnit.MILLISECONDS
)
Behavior:
- success: update DB, delete media.
- failure: keep media, retry later.
- no target: return success or unique success so worker does not spam.
Tests:
- WorkManager test with fake repository.
- simulate network unavailable.
- verify retry policy.
---
Chunk 6 — Enqueue upload after sensor event
Goal: Every captured media event enters upload queue.
Flow:
sensor trigger
→ capture photo/audio
→ save event
→ enqueue MediaUploadWorker
Files likely affected:
- HandleSensorEventUseCase
- MonitoringService
- camera/audio capture flow
Rule: enqueue should not block event creation.
Use:
serviceScope.launch {
    mediaUploadRepository.enqueueUpload(eventId)
}
Tests:
- event creation enqueues worker.
- event without media does not enqueue.
- upload failure does not crash monitoring service.
---
Chunk 7 — Upload success media deletion
Goal: Delete local media only after successful upload.
Paths:
- filesDir/captures/
- filesDir/audio_captures/
Correct order:
upload succeeds
→ update Room event as uploaded
→ delete local media
Not:
delete media
→ update Room
because failure between those two loses evidence.
Tests:
- success deletes file.
- failed upload keeps file.
- missing file does not crash.
- deleted file event still shows uploaded status.
---
Chunk 8 — Logs upload status badges
Goal: Show upload state in LogsScreen.
Badges:
- Pending upload
- Uploaded to Telegram
- Uploaded to Matrix
- Upload failed
- Uploaded and deleted locally
If media exists, preview normally.
If media was uploaded and deleted, show placeholder.
Tests/UI checks:
- pending badge visible.
- uploaded Telegram badge visible.
- uploaded Matrix badge visible.
- failed badge visible.
- missing uploaded media shows placeholder, not broken preview.
---
Chunk 9 — Delete password crypto
Goal: Add secure delete-password storage.
Use:
- random salt
- PBKDF2
- stored hash, never raw password
Store:
deletePasswordSalt: String?
deletePasswordHash: String?
Functions:
setDeletePassword(password: String)
verifyDeletePassword(password: String): Boolean
hasDeletePassword(): Boolean
removeDeletePassword(oldPassword: String): Boolean
Security rule: never provide “forgot password” reset. If password is lost, user must clear app data.
Tests:
- wrong password fails.
- correct password succeeds.
- removing password requires old password.
- stored value is not equal to raw password.
---
Chunk 10 — Settings UI for delete password
Goal: Add password management.
Settings actions:
- Set delete password.
- Change delete password.
- Remove delete password.
Recommended flow:
1. User taps delete password setting.
2. If password exists:
   - require old password or biometric.
3. If password does not exist:
   - require new password twice.
4. Remove password:
   - require old password or biometric.
Do not allow silent reset.
Tests/UI checks:
- add password.
- change password with old password.
- remove password with old password.
- wrong old password is rejected.
---
Chunk 11 — Logs delete flow with password
Goal: Protect destructive deletion.
Flow:
user taps delete
→ if password set, show password dialog
→ verify password
→ delete selected event logs
→ delete associated media files
→ update upload worker state
Manual deletion should delete media even if upload is pending.
Optional status:
DELETED
or simply remove event from Room.
Tests:
- delete without password when no password is set.
- delete with correct password.
- wrong password does not delete.
- media file deleted after log deletion.
- upload worker does not try to upload deleted event.
---
Chunk 12 — Optional stronger protection
Goal: Reduce risk of attacker opening app and resetting password.
Recommended:
- require delete password/biometric before opening sensitive Settings pages.
- require biometric before:
  - removing delete password
  - changing delete password
  - clearing integration config
  - deleting all logs
- optional decoy mode for wrong password.
Minimum production recommendation:
Delete password protects log deletion.
Biometric or old password protects password removal/change.
---
Chunk 13 — F-Droid publishing readiness
Goal: Make sure this feature is F-Droid-safe.
Checklist:
- no proprietary SDKs.
- no Google Play services dependency.
- WorkManager is allowed.
- encrypted settings are allowed.
- network config remains HTTPS-only.
- no hardcoded Telegram/Matrix tokens.
- no cleartext HTTP.
- no logging of credentials.
- delete password is salted/hashed.
- user can build reproducibly.
- release build disables HTTP logging.
Validation:
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
If available:
fdroid build -v --server org.havenapp.neruppu
---
Recommended build order
Implement in this exact order:
1. Chunk 1 — upload metadata model.
2. Chunk 2 — Room migration and DAO.
3. Chunk 3 — active upload target settings.
4. Chunk 4 — media upload repository abstraction.
5. Chunk 5 — WorkManager worker.
6. Chunk 6 — enqueue after sensor event.
7. Chunk 7 — delete media after upload success.
8. Chunk 8 — logs status badges.
9. Chunk 9 — delete password crypto.
10. Chunk 10 — Settings UI.
11. Chunk 11 — Logs delete flow.
12. Chunk 12 — optional biometric/security hardening.
13. Chunk 13 — F-Droid validation.
This order keeps every chunk independently testable and avoids mixing upload lifecycle, UI, and password security into one risky change.
