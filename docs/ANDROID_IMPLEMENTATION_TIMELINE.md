# Android implementation timeline

**Project:** `test` (new Kotlin + Jetpack Compose application)
**Cadence:** one milestone per day, starting 29/10/2026.
**Push status:** completed milestones are pushed individually to `origin/main`.

| Date | Milestone | Scope | Acceptance criteria | Commit title |
|---|---|---|---|---|
| 29/10 | FE-07 · API client | `app/src/main/java/com/example/test/data/remote/MoodleApi.kt` | Shared API client, typed HTTP errors, 10-second connect/read timeout, optional bearer token, JSON request/response support. | [`6997985`](https://github.com/namdzzgg-hash/test/commit/6997985806953711f3f245fdcbe4ce0717b62cf5) |
| 30/10 | FE-08 · Models and helpers | `app/src/main/java/com/example/test/data/model/`, `data/repository/` | Typed assignment/course/session models and shared status/date/grade helpers; UI does not depend on hard-coded sample data. | [`828f6ed`](https://github.com/namdzzgg-hash/test/commit/828f6ed) |
| 31/10 | FE-15 · Classwork list | `app/src/main/java/com/example/test/ui/` | Assignment list with To do / Due soon / Done filters, course filter, search, and navigation to details. | [`e2f9272`](https://github.com/namdzzgg-hash/test/commit/e2f9272) |
| 01/11 | FE-16 · Assignment detail | `app/src/main/java/com/example/test/ui/` | Show description and grade contribution; Mark as done calls the API, updates progress optimistically, rolls back on error, and displays a message. | [`86b60f4`](https://github.com/namdzzgg-hash/test/commit/86b60f4) |
| 02/11 | FE-23 · Timetable navigation | `data/remote/`, `data/model/`, `ui/` | Load timetable sessions from API; navigate weeks/months and open the relevant course from a session. | [`6af754e`](https://github.com/namdzzgg-hash/test/commit/6af754e) |
| 03/11 | FE-24 · Timetable states | `app/src/main/java/com/example/test/ui/` | Show In progress / Upcoming / Ended states, empty days, course colors, and a Today action. | [`09fcefa`](https://github.com/namdzzgg-hash/test/commit/09fcefa) |
| 04/11 | FE-31 · Network recovery | `data/remote/`, `ui/` | Consistent offline and timeout messages, retry action, 10-second timeout, and no blank error state. | [`7f4b412`](https://github.com/namdzzgg-hash/test/commit/7f4b412) |
| 05/11 | FE-32 · Regression checks | `app/src/test/`, `.github/workflows/android.yml` | Unit tests for status, filtering, and progress; CI builds the debug APK and runs unit tests. | `test: cover core student flows` |

## Manual checks

1. Launch the app and load the home screen from the backend.
2. Open an assignment and verify course, deadline, description, status, and grade contribution.
3. Mark an assignment done; verify the UI updates immediately and synchronizes after the API succeeds.
4. Force the completion API to fail; verify the UI rolls back and shows an error.
5. Undo completion for an ungraded assignment and verify course progress changes.
6. Exercise all classwork filters, course selection, and text search.
7. Navigate timetable weeks, select a class, and open the matching course.
8. Switch timetable to month view; verify empty days, today marker, and Today action.
9. Stop the backend; verify a useful offline message and that retry works.
10. Relaunch the app and verify server-backed completion state remains consistent.

## Pushing milestones

Keep each day's changes in its own commit and push them in order to `origin/main`.
