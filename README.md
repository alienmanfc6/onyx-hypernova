# RankIt — Android Ranking List App

A clean Android app built with **Jetpack Compose + Kotlin** for creating and managing ranked lists.

## Features

- Multiple named ranked lists
- Add, rename, delete items within lists
- Drag-and-drop reordering with haptic feedback
- Live rank numbers (#1, #2, #3…) that update as items move
- Item coloring — choose from a neon color palette (or clear to no color)
- Tag system — attach multiple tags per item; tags are shared app-wide with autocomplete suggestions
- Long-press lists or tap the item options icon → context menus (rename, delete)
- Delete confirmation dialogs
- Light / Dark mode toggle (persisted via DataStore)
- Backup / Restore — export all data to a JSON file; import from JSON (atomic, replaces all existing data)
- Room database v3 (2 migrations: v1→v2 added colors, v2→v3 added tags)
- Material 3, edge-to-edge

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| DI | Hilt |
| Database | Room |
| Drag & Drop | `sh.calvin.reorderable` |
| Theme Pref | DataStore Preferences |
| Backup Serialization | Gson 2.10.1 |
| Architecture | MVVM + Repository + Kotlin Flow |

## Project Structure

```
app/src/main/java/com/alienmantech/onyx_hypernova/
├── data/
│   ├── backup/
│   │   ├── BackupModels.kt      # BackupFile, BackupList, BackupItem data classes
│   │   └── BackupRepository.kt  # Export/import JSON via ContentResolver + Gson
│   ├── db/
│   │   ├── Daos.kt              # RankedListDao, RankedItemDao, TagDao
│   │   ├── Entities.kt          # RankedListEntity, RankedItemEntity, TagEntity, ItemTagCrossRef
│   │   ├── Migrations.kt        # MIGRATION_1_2 (add color), MIGRATION_2_3 (add tags)
│   │   └── RankItDatabase.kt    # Room database, version 3
│   └── repository/
│       ├── RankItRepository.kt  # Single source of truth for lists, items, tags, backup
│       └── ThemeRepository.kt   # DataStore dark mode preference
├── di/
│   └── DatabaseModule.kt        # Hilt providers (DB, DAOs, ContentResolver)
├── ui/
│   ├── components/
│   │   └── Dialogs.kt           # TextInputDialog, AddItemWithTagsDialog, TagPickerContent,
│   │                            #   TagPickerDialog, ConfirmDeleteDialog, ConfirmImportDialog
│   ├── home/
│   │   └── HomeScreen.kt        # List-of-lists screen with backup overflow menu
│   ├── listdetail/
│   │   └── ListDetailScreen.kt  # Items screen with drag/drop, color picker, tag editor
│   ├── theme/
│   │   ├── NotepadColors.kt     # NotepadPalette, light/dark values, colorPalette (neon hex list)
│   │   └── Theme.kt             # RankItTheme — Material 3 color schemes + NotepadPalette provider
│   └── NavGraph.kt              # RankItNavGraph: home → list/{listId}
├── viewmodel/
│   ├── HomeViewModel.kt         # HomeUiState, BackupStatus sealed class, backup actions
│   └── ListDetailViewModel.kt   # ListDetailUiState, optimistic drag state
├── MainActivity.kt
└── RankItApplication.kt
```

## Getting Started

1. Clone / open in **Android Studio Ladybug (2024.2)** or newer
2. Sync Gradle — all dependencies resolve from Maven Central / Google
3. Run on a device or emulator with **API 26+**

## Key UX Details

- **Add list**: FAB on home screen → type name → Create
- **Open list**: Tap any list card
- **Add item**: FAB inside list screen → dialog with item name + optional tags → Add
- **Reorder**: Hold the `⠿` drag handle on the right of any item and drag
- **Rename / Delete item**: Tap the item options icon → bottom sheet → choose Rename or Delete → confirm if deleting
- **Rename / Delete list**: Long-press a list card → dropdown menu → confirm if deleting
- **Change item color**: Item options → bottom sheet → Change Color → pick from neon palette (or clear)
- **Edit item tags**: Item options → bottom sheet → Edit Tags → add/remove tag chips
- **Toggle tag display**: Label icon in the list detail top bar shows/hides tag chips
- **Toggle theme**: Sun/moon icon in the top-right of the home screen
- **Export backup**: Overflow menu `⋮` on home screen → Export backup → choose save location
- **Import backup**: Overflow menu `⋮` on home screen → Import backup → pick JSON file → confirm (replaces all data)

## Dependencies (key)

```toml
reorderable = "sh.calvin.reorderable:reorderable:2.4.3"
hilt = "com.google.dagger:hilt-android:2.54"
room = "androidx.room:room-runtime:2.6.1"
composeBom = "androidx.compose:compose-bom:2025.02.00"
gson = "com.google.code.gson:gson:2.10.1"
```
