# RankIt — Android Ranking List App

A clean Android app built with **Jetpack Compose + Kotlin** for creating and managing ranked lists.

## Features

- ✅ Multiple lists with custom names
- ✅ Add, rename, delete items
- ✅ Drag-and-drop reordering with haptic feedback
- ✅ Live rank numbers (#1, #2, #3…) that update as items move
- ✅ Long-press items/lists → context menu with Rename / Delete
- ✅ Delete confirmation dialogs
- ✅ Light / Dark mode toggle (persisted via DataStore)
- ✅ Room database (local, persistent)
- ✅ Material 3, edge-to-edge

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| DI | Hilt |
| Database | Room |
| Drag & Drop | `sh.calvin.reorderable` |
| Theme Pref | DataStore Preferences |
| Architecture | MVVM + Repository + Kotlin Flow |

## Project Structure

```
app/src/main/java/com/rankit/app/
├── data/
│   ├── db/
│   │   ├── Entities.kt          # RankedListEntity, RankedItemEntity
│   │   ├── Daos.kt              # RankedListDao, RankedItemDao
│   │   └── RankItDatabase.kt   # Room database
│   └── repository/
│       ├── RankItRepository.kt  # Single source of truth
│       └── ThemeRepository.kt  # DataStore dark mode pref
├── di/
│   └── DatabaseModule.kt       # Hilt providers
├── ui/
│   ├── home/
│   │   └── HomeScreen.kt       # List-of-lists screen
│   ├── listdetail/
│   │   └── ListDetailScreen.kt # Items screen with drag/drop
│   ├── components/
│   │   └── Dialogs.kt          # TextInputDialog, ConfirmDeleteDialog
│   ├── theme/
│   │   └── Theme.kt            # Material 3 color scheme
│   └── NavGraph.kt             # Navigation destinations
├── viewmodel/
│   ├── HomeViewModel.kt
│   └── ListDetailViewModel.kt
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
- **Add item**: FAB inside list screen → type name → Add
- **Reorder**: Hold the `⠿` drag handle on the right of any item and drag
- **Rename / Delete item**: Tap `⋮` on an item → choose option → confirm if deleting
- **Rename / Delete list**: Long-press a list card → choose option → confirm if deleting
- **Toggle theme**: Sun/moon icon in the top-right of the home screen

## Dependencies (key)

```toml
reorderable = "sh.calvin.reorderable:reorderable:2.4.3"
hilt = "com.google.dagger:hilt-android:2.54"
room = "androidx.room:room-runtime:2.6.1"
composeBom = "androidx.compose:compose-bom:2025.02.00"
```
