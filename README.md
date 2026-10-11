# MusicTinder

Reproductor de música estilo Tinder para Android. Desliza para clasificar canciones: izquierda (Rechazar), derecha (Me gusta), arriba (Superlike). Los Superlikes dejan de evaluarse y pueden quitarse para volver al mazo. Rechazados y Likes siguen apareciendo en el ciclo.

## Características
- Kotlin + Jetpack Compose
- Reproducción con Media3/ExoPlayer (play/pause, ±30 segundos, barra de progreso)
- Selección de carpeta con Storage Access Framework (SAF), sin permisos de almacenamiento
- Persistencia con Room (canciones y decisiones) + DataStore (carpeta seleccionada)
- Swipe estilo Tinder con animaciones
- Listas editables: Rechazados y Superlikes
- En Rechazados: quitar de lista o borrar archivo del dispositivo
- Botón "Borrar todo del dispositivo" para Rechazados
- Scanner recursivo con filtro de audio

## Requisitos
- Android 7.0+ (minSdk 24)
- compileSdk 34

## Build
```bash
cd ~/MusicTinder
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Estructura
- `app/src/main/java/com/example/musictinder/` - UI (Compose), ViewModel, DI, reproducción, scanner
- `app/src/main/java/com/example/musictinder/data/` - Room + DataStore
