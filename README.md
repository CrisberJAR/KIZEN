# Kizen

Organizador de vida para Android: pastel, local-first y listo para sincronizar con Alexa.

## Qué incluye

- Tareas, listas, hábitos y rachas
- Recordatorios locales + mantenimiento nocturno
- Nube opcional: sync con un API Node (`/api/v3/...`)
- Insights para Alexa; Gemini solo en el servidor (variable de entorno)

## Generar el APK

```bat
set JAVA_HOME=C:\Program Files\Java\jdk-21
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
gradlew.bat assembleDebug
```

Instalable: `app\build\outputs\apk\debug\app-debug.apk`

## API local (Fase nube)

En una terminal, con Node 18+:

```bat
node server\index.mjs
```

Queda en `http://127.0.0.1:8787`.

En la app: icono de nube → activa sync.

- Emulador: `http://10.0.2.2:8787`
- Teléfono en la misma Wi‑Fi: `http://IP-DE-TU-PC:8787`

Opcional, IA de verdad (nunca va en el APK):

```bat
set GEMINI_API_KEY=tu_clave
node server\index.mjs
```

### Endpoints

| Método | Ruta | Uso |
|---|---|---|
| GET/PUT | `/api/v3/sync` | Snapshot listas, tareas, hábitos |
| GET | `/api/v3/tasks/insights` | Texto para Alexa |
| POST | `/api/v3/ai/summary` | Resumen; usa Gemini si hay env |
| POST | `/api/v3/alexa/events` | Intents de la Skill |

Cabecera: `X-Kizen-User-Id`.
