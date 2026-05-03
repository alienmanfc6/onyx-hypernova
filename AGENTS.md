# AGENTS.md — RankIt Codebase Guide

This file is for AI coding assistants. Read it before making changes to this repository. RankIt is an Android Jetpack Compose app for creating and managing ranked lists. Package: `com.alienmantech.onyx_hypernova`.

---

## Architecture Overview

**Pattern:** MVVM + Repository + Kotlin Flow

```
UI (Compose)
    ↓ collectAsState()
ViewModel (StateFlow<UiState>)
    ↓ viewModelScope.launch / collect
Repository (RankItRepository, ThemeRepository, BackupRepository)
    ↓ DAOs / DataStore / ContentResolver
Room (SQLite)  |  DataStore (prefs)  |  Android SAF (backup files)
```

**Key architectural decisions:**

- Each screen has one `uiState: StateFlow<XUiState>` built with `combine(flow1, flow2, ...) { ... }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), XUiState())`. Never split into multiple StateFlows per screen.
- `ListDetailViewModel` uses `_localItems: MutableStateFlow<List<RankedItemEntity>?>` for optimistic drag-and-drop UI. The DB is only written on drag-end (`onDragEnd`). During a drag, `uiState` uses `_localItems ?: dbItems`.
- Theme is app-controlled, not system-driven. `HomeViewModel.uiState.isDarkMode` (from DataStore) is collected in `MainActivity` and passed to `RankItTheme`. There is no dynamic color or system dark-mode delegation.
- Screens never interact with DAOs directly — all data access goes through a Repository.

---

## Project Structure

```
app/src/main/java/com/alienmantech/onyx_hypernova/
├── data/
│   ├── backup/
│   │   ├── BackupModels.kt      # BackupFile, BackupList, BackupItem (JSON DTOs)
│   │   └── BackupRepository.kt  # Gson serialize/deserialize + Android SAF URI I/O
│   ├── db/
│   │   ├── Daos.kt              # RankedListDao, RankedItemDao, TagDao
│   │   ├── Entities.kt          # All @Entity classes — the schema lives here
│   │   ├── Migrations.kt        # MIGRATION_1_2, MIGRATION_2_3
│   │   └── RankItDatabase.kt    # @Database annotation, version 3
│   └── repository/
│       ├── RankItRepository.kt  # Single source of truth (lists, items, tags, backup)
│       └── ThemeRepository.kt   # DataStore dark mode preference
├── di/
│   └── DatabaseModule.kt        # Hilt @Module — DB, DAOs, ContentResolver providers
├── ui/
│   ├── components/
│   │   └── Dialogs.kt           # Reusable dialog composables shared across screens
│   ├── home/
│   │   └── HomeScreen.kt        # List-of-lists screen with backup overflow menu
│   ├── listdetail/
│   │   └── ListDetailScreen.kt  # Items screen: drag/drop, color picker, tag editor
│   ├── theme/
│   │   ├── NotepadColors.kt     # NotepadPalette, light/dark instances, colorPalette list
│   │   └── Theme.kt             # RankItTheme composable, LocalNotepadPalette provider
│   └── NavGraph.kt              # RankItNavGraph: "home" → "list/{listId}"
├── viewmodel/
│   ├── HomeViewModel.kt         # HomeUiState, BackupStatus sealed class
│   └── ListDetailViewModel.kt   # ListDetailUiState, optimistic _localItems drag state
├── MainActivity.kt              # @AndroidEntryPoint, Compose host, theme wiring
└── RankItApplication.kt         # @HiltAndroidApp
```

---

## Key Files and Their Roles

| File | Owns |
|---|---|
| `Entities.kt` | All Room `@Entity` data classes. The schema definition lives here. |
| `Daos.kt` | All DAO interfaces. Add new queries here. |
| `Migrations.kt` | Explicit Room migrations. Must be updated on every DB version bump. |
| `RankItDatabase.kt` | `@Database` annotation listing entities and current version. Must stay in sync with `Migrations.kt`. |
| `RankItRepository.kt` | All business logic for lists, items, tags, and backup coordination. |
| `DatabaseModule.kt` | Hilt providers: Room DB instance, each DAO, and `ContentResolver`. |
| `HomeViewModel.kt` | List-of-lists state, dark mode toggle, backup status (`BackupStatus` sealed class). |
| `ListDetailViewModel.kt` | Per-list items, per-item tags, drag state, color/tag update actions. |
| `Dialogs.kt` | Shared reusable dialogs: `TextInputDialog`, `AddItemWithTagsDialog`, `TagPickerContent`, `TagPickerDialog`, `ConfirmDeleteDialog`, `ConfirmImportDialog`. |
| `NotepadColors.kt` | `NotepadPalette` data class, light/dark instances, `LocalNotepadPalette`, helper `@Composable` color functions, `colorPalette` (null + 8 neon hex strings). |
| `Theme.kt` | `RankItTheme` composable — wires Material 3 color scheme and provides `LocalNotepadPalette`. |
| `NavGraph.kt` | Route constants and `RankItNavGraph` composable. Navigation arg `listId` is `Long`. |
| `BackupRepository.kt` | Gson read/write + Android SAF Uri stream handling. |
| `BackupModels.kt` | Plain data classes that map to/from JSON (`BackupFile`, `BackupList`, `BackupItem`). |

---

## Database Schema and Migrations

### Tables

**`ranked_lists`**
| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER` PK autoincrement | |
| `name` | `TEXT` | |
| `createdAt` | `INTEGER` | epoch ms |
| `updatedAt` | `INTEGER` | epoch ms, updated on any write |

**`ranked_items`**
| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER` PK autoincrement | |
| `listId` | `INTEGER` FK | CASCADE DELETE from `ranked_lists` |
| `name` | `TEXT` | |
| `position` | `INTEGER` | 0-based; UI displays `position + 1` |
| `color` | `TEXT` nullable | hex string e.g. `"#FF0040"`, null = no color |

**`tags`**
| Column | Type | Notes |
|---|---|---|
| `id` | `INTEGER` PK autoincrement | |
| `name` | `TEXT` | always stored lowercase, trimmed |

**`item_tag_cross_ref`** (junction table)
| Column | Type | Notes |
|---|---|---|
| `itemId` | `INTEGER` | CASCADE DELETE from `ranked_items` |
| `tagId` | `INTEGER` | CASCADE DELETE from `tags` |
| Composite PK | `(itemId, tagId)` | |

### Adding a new DB column — step-by-step

1. Add the column to the relevant `@Entity` in `Entities.kt`
2. Write `val MIGRATION_N_(N+1)` in `Migrations.kt` with the `ALTER TABLE` SQL
3. Bump `version` in the `@Database` annotation in `RankItDatabase.kt`
4. Register the migration in `DatabaseModule.provideDatabase` → `.addMigrations(...)`
5. Add any new DAO query in `Daos.kt`
6. Expose via `RankItRepository` if screens need it

**Never** use `fallbackToDestructiveMigration()` — it is intentionally absent from this project.

---

## Hilt Dependency Injection

```
@HiltAndroidApp        → RankItApplication
@AndroidEntryPoint     → MainActivity
@HiltViewModel         → HomeViewModel, ListDetailViewModel
@Inject constructor    → RankItRepository, ThemeRepository, BackupRepository (all @Singleton)
@Module @InstallIn(SingletonComponent::class) → DatabaseModule
```

`DatabaseModule` provides: `RankItDatabase`, `RankedListDao`, `RankedItemDao`, `TagDao`, `ContentResolver`.

**Adding a new injectable service:**
- If it's a class you own: annotate with `@Singleton` and `@Inject constructor` — Hilt discovers it automatically as long as its own constructor params are already injectable.
- If it's a third-party or interface: add a `@Provides` function to `DatabaseModule` (or a new `@Module` file).

---

## Flow and StateFlow Patterns

**Collecting in a Composable:**
```kotlin
val state by viewModel.uiState.collectAsState()
```

**Building `uiState` in a ViewModel:**
```kotlin
val uiState: StateFlow<XUiState> = combine(flow1, flow2) { a, b ->
    XUiState(fieldA = a, fieldB = b)
}.catch { /* handle error */ }
 .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), XUiState())
```

**DAO Flows vs suspend functions:**
- `Flow<T>` DAOs — for live-observing data; used in Repository and combined into `uiState`
- `suspend fun` DAOs — for one-time reads/writes; called from `viewModelScope.launch { }` or inside `withTransaction`

**Optimistic drag state (`_localItems`):**
```kotlin
private val _localItems = MutableStateFlow<List<RankedItemEntity>?>(null)

// In uiState combine:
val items = _localItems.value ?: dbItems

fun onDragMove(reordered: List<RankedItemEntity>) { _localItems.value = reordered }
fun onDragEnd(reordered: List<RankedItemEntity>) {
    viewModelScope.launch {
        repository.reorderItems(reordered)
        _localItems.value = null  // let DB flow take over again
    }
}
```

**Per-item sub-flows (tags):**
`ListDetailViewModel` uses `flatMapLatest` to watch a list of items and collect a tag `Flow` for each, merging them into `Map<Long, List<TagEntity>>`. Preserve this pattern when adding other per-item sub-data.

---

## Adding a New Screen

1. Create `ui/newfeature/NewFeatureScreen.kt` — `@Composable fun NewFeatureScreen(onBack: () -> Unit, ...)`
2. Create `viewmodel/NewFeatureViewModel.kt` — `@HiltViewModel class NewFeatureViewModel @Inject constructor(...)`; expose `val uiState: StateFlow<NewFeatureUiState>`
3. Add a route constant to `NavGraph.kt`'s `private object Routes`
4. Add a `composable(Routes.NEW_FEATURE) { NewFeatureScreen(...) }` block in `RankItNavGraph`
5. Pass navigation callbacks as lambdas — never pass `NavController` into a screen composable

Obtain the ViewModel in the screen via `hiltViewModel()`.

---

## Compose Conventions

**Colors:**
- Standard Material colors: `MaterialTheme.colorScheme.*`
- Notepad-theme colors (preferred for backgrounds, toolbars, text): `@Composable` helpers from `NotepadColors.kt`:
  - `notePadPageColor()` — main background
  - `notePadToolbarColor()` — top bars
  - `notePadLineColor()` — dividers / list lines
  - `notePadHighlightColor()` — drag highlight
  - `notePadInkColor()` — primary text
  - `notePadSurfaceColor()` — card/surface backgrounds
- All screens set `Scaffold(containerColor = notePadPageColor())` for consistency

**Item color palette:**
`colorPalette` in `NotepadColors.kt` is `List<String?>` — `null` means "no color / clear", followed by 8 neon hex strings. Render null as a clear/transparent swatch.

**Dialogs:**
- Reusable dialogs that are used by multiple screens → `Dialogs.kt`
- Screen-specific dialogs (e.g., color picker) → inline in the screen file

**Tag chips:**
- `SuggestionChip` — read-only display of a tag
- `InputChip` with trailing close icon — selected/editable tag that can be removed

**Experimental APIs:**
Use `@OptIn(ExperimentalMaterial3Api::class)` on the specific composable that needs it. Do not apply it file-wide.

---

## Key Invariants — Do Not Violate

1. **Room migration versioning**: Every schema change requires a new `MIGRATION_N_(N+1)` in `Migrations.kt` and a version bump in `RankItDatabase.kt`. Never bump the version without a migration. Never add `fallbackToDestructiveMigration`.

2. **Cascade deletes**: `ranked_items` cascades on `ranked_lists` delete. `item_tag_cross_ref` cascades on both `ranked_items` and `tags` delete. New foreign keys should follow the same pattern unless there is an explicit reason not to.

3. **Position is 0-based**: `RankedItemEntity.position` is always 0-based. Displayed rank = `position + 1`. `reorderItems` in `RankItRepository` rebuilds positions via `mapIndexed { index, item -> item.copy(position = index) }`. Never store 1-based positions.

4. **Tag name normalization**: Tags are always `trim().lowercase()` before storage and lookup. This is enforced in `RankItRepository.setTagsForItem`. Input that bypasses normalization will create duplicates.

5. **Backup import is atomic and destructive**: `importBackup` runs inside `db.withTransaction` and calls `deleteAllLists()` + `deleteAllTags()` first. This is by design. Do not make it additive without explicit intent.

6. **Screens never call DAOs directly**: All data access goes through `RankItRepository` (or `ThemeRepository` / `BackupRepository`). ViewModels inject repositories, not DAOs.

7. **`BackupStatus` lifecycle**: Always call `viewModel.clearBackupStatus()` after consuming a `Success` or `Error` state (via `LaunchedEffect` in `HomeScreen`). Leaving it unconsumed causes the snackbar to re-trigger on recomposition.

8. **`listId` nav arg is `Long`**: Route is `list/{listId}` with `NavType.LongType`. `ListDetailViewModel` reads it via `savedStateHandle.get<Long>("listId")` with `requireNotNull`. Always pass a `Long` in navigation calls — never an `Int`.

9. **Tags are never explicitly deleted**: Tags persist in the `tags` table even when no items reference them (they remain as autocomplete suggestions). Only a full backup import wipes the tags table. Do not add logic that auto-purges unreferenced tags without explicit product intent.

10. **`ColorPickerDialog` lives in `ListDetailScreen.kt`**, not `Dialogs.kt`, because it is list-detail-specific and directly references `colorPalette` from `NotepadColors.kt`. Do not move it unless you fully understand its usage context.

---

## Build and Run

**Prerequisites:** Android Studio Ladybug (2024.2) or newer, JDK 17

```bash
# Open in Android Studio and let Gradle sync, then run normally.
# Or from the command line:
./gradlew assembleDebug       # debug build
./gradlew assembleRelease     # release build (R8 minification via proguard-rules.pro)
./gradlew installDebug        # build + install on connected device/emulator
```

- Minimum SDK: API 26 (Android 8.0)
- Target SDK: 35
- `local.properties` is gitignored — Android Studio generates it automatically

---

## Testing Notes

No tests currently exist in this project. If adding tests:

- **DAO tests**: `@RunWith(AndroidJUnit4::class)` with `Room.inMemoryDatabaseBuilder(...)` — use a test-only Hilt module to provide the in-memory DB
- **ViewModel tests**: `kotlinx-coroutines-test` (`UnconfinedTestDispatcher`) + `turbine` for Flow/StateFlow assertions
- **Do not** add `fallbackToDestructiveMigration()` to the production `DatabaseModule` to make tests easier — create a separate `@TestInstallIn` module instead
- **Migration tests**: Use `MigrationTestHelper` from `androidx.room:room-testing` to verify each migration in isolation
