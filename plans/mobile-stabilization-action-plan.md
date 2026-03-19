# Mobile Stabilization Action Plan (March 19, 2026)

## Scope
Stabilize core Android user journeys:
- Delete photos/albums reliably
- Save edited photos reliably
- Show album thumbnails/covers consistently
- Improve editor responsiveness and perceived latency
- Close major parity/flow gaps (print/share/error handling)

## Findings Summary

### Critical
1. Gallery multi-delete swallows per-item failures and always reloads
- File: app/src/main/java/com/luminens/android/presentation/gallery/GalleryViewModel.kt
- Symptom: delete appears inconsistent when one or more items fail.

2. Album cover resolver skips fallback when cover_url exists but is stale/invalid
- File: app/src/main/java/com/luminens/android/data/remote/SupabaseDataSource.kt
- Symptom: album cards can show blank or broken covers.

3. Editor filter performance bottleneck
- File: app/src/main/java/com/luminens/android/presentation/editor/EditorViewModel.kt
- Symptom: slider updates trigger expensive full rerenders, making effects appear delayed.

### High
4. AlbumsViewModel refresh chain is sequential and repeats network round-trips
- File: app/src/main/java/com/luminens/android/presentation/albums/AlbumsViewModel.kt
- Symptom: slow add/remove/reorder album operations.

5. Print flow is still local/mock and does not persist order lifecycle
- File: app/src/main/java/com/luminens/android/presentation/print/CartViewModel.kt
- Symptom: users complete payment step in UI but no robust backend persistence.

6. Error visibility inconsistency across screens
- Some screens show snackbar automatically, others keep stale error state.

### Medium
7. Save-photo operation has no explicit post-save verification/retry
- Files: EditorViewModel + SupabaseDataSource updatePhotoFile
- Symptom: occasional user reports of save uncertainty.

8. Editor memory/throughput risk under repeated edits on large images
- File: app/src/main/java/com/luminens/android/presentation/editor/EditorViewModel.kt

## Execution Plan

## Phase 1 - Reliability Hotfixes (Day 0-1)
1. Harden gallery delete batch
- Aggregate results (success/failure), show count feedback.
- Do not silently ignore failing entries.
- Verify with mixed success scenario.

2. Harden album cover fallback logic
- If cover_url is blank OR invalid/unloadable, fallback to first valid photo.
- Add defensive fallback placeholder only as last resort.
- Verify with albums missing cover_url and with stale URLs.

3. Save-photo trust improvements
- After save, force refresh of updated photo record and check storage_path/url availability.
- Add explicit snackbar for success/failure with actionable message.
- Verify by saving multiple times from viewer and album entry points.

Acceptance criteria:
- Delete and save behave deterministically in 20 repeated attempts.
- Album list always renders a non-broken cover or explicit placeholder.

## Phase 2 - Performance and UX (Day 1-2)
4. Debounce editor rerenders
- Add 100-180ms debounce for slider updates.
- Keep immediate UI value updates while delaying bitmap recompute.

5. Progressive render strategy
- Use preview-size bitmap during interaction, full-size finalize on pause/apply/save.
- Cache last rendered state hash to skip identical recomputation.

6. Optional: move heavy CPU fallback operations to optimized path
- Reduce per-pixel passes where possible.
- Avoid repeated allocations in hot loops.

Acceptance criteria:
- Slider interaction remains responsive on mid-range device/emulator.
- Visible filter updates appear within acceptable latency window.

## Phase 3 - Data and Flow Completion (Day 2-4)
7. Parallelize album refresh operations
- Replace sequential loadAlbumPhotos/loadAvailablePhotos/loadAlbums with controlled parallel refresh and single loading state.

8. Print flow backend integration
- Persist order intent and final status in backend.
- Ensure UI success state is tied to persisted order outcome.

9. Unify error lifecycle pattern
- Standardized error emission, auto-clear policy, and snackbar mapping for feature viewmodels.

Acceptance criteria:
- Album actions no longer feel blocked by repeated sequential loads.
- Print flow produces persistent order history entry.

## Test Matrix
- Gallery: single delete, multi-delete, delete while network unstable.
- Editor: save after multiple adjustments, save after AI preview apply, rapid slider drags.
- Albums: create/rename/add/remove/reorder/share with and without cover_url.
- Generate: reference selection, style/settings transitions, error recovery.
- Print: cart -> checkout -> success/failure -> history.

## Deliverables
- Code fixes by phase
- Regression checklist results
- Short postmortem of root causes and prevention actions

## Priority Queue (Top 8)
1. Batch delete reliability
2. Album cover fallback robustness
3. Save-photo verification + user feedback
4. Editor debounce + progressive rendering
5. Album refresh parallelization
6. Error lifecycle standardization
7. Print persistence integration
8. Regression tests for core journeys
