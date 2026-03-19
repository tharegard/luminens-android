# Luminens Android

App Android nativa per Luminens — generazione foto con AI, galleria, editor, ordini stampa.

## Setup

1. Clona il repo
2. In Android Studio: **File → Open** → seleziona questa cartella
3. Crea `local.properties` nella root con:
   ```
   sdk.dir=C\:\\Users\\<tuo-nome>\\AppData\\Local\\Android\\Sdk
   SUPABASE_ANON_KEY=<la tua anon key>
   ```
4. Sync Gradle → Run

## Stack

- **Kotlin + Jetpack Compose + Material 3**
- **Supabase-kt** per auth, DB, storage, edge functions
- **GPUImage** per filtri editor real-time
- **uCrop** per crop manuale
- **CameraX** per camera nativa
- **Hilt** per dependency injection
- **Room** per cache locale
- **Coil 3** per image loading
- **Navigation Compose**

## Struttura

```
app/src/main/java/com/luminens/android/
├── di/                    # Hilt modules
├── data/
│   ├── local/             # Room DB + DAO
│   ├── model/             # Data classes
│   ├── remote/            # Supabase calls
│   └── repository/        # Repository pattern
└── presentation/
    ├── auth/              # Login, Register
    ├── gallery/           # Galleria foto
    ├── albums/            # Album
    ├── generate/          # Generazione AI
    ├── camera/            # Camera CameraX
    ├── editor/            # Editor foto semplificato
    ├── print/             # Ordini stampa
    ├── account/           # Account & crediti
    ├── shared/            # Album/foto pubblici
    ├── navigation/        # NavGraph
    ├── theme/             # Colori, tipografia
    └── components/        # UI comuni
```

## Build release

```bash
./gradlew assembleRelease   # APK
./gradlew bundleRelease     # AAB (Play Store)
```
