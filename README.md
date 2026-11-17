# CampusLife

An Android app that helps university students organise tasks, notes and campus events. It also has an AI study assistant that answers questions from your own notes, and a community feed for sharing material with other students.

A prebuilt APK is available at [`apk/CampusLife.apk`](apk/CampusLife.apk).

## Features

- **Authentication**: email/password sign-up and login with Firebase Auth.
- **Tasks**: create, edit and complete assignments with categories, due dates, times and an importance flag. Tasks finished after their due date are marked as late.
- **Notes**: rich notes with file/image attachments and web links. Text in attached images is extracted with on-device OCR (ML Kit).
- **Events**: schedule campus events and track attendance.
- **AI Chat Assistant (RAG)**: ask questions about your notes. Notes are split into chunks and embedded, the most relevant chunks are found with cosine similarity, and Gemini answers using them as context.
- **Community Feed**: post, edit and delete shared notes. Includes search, category filters, image uploads and saving a post to your own notes, with OCR and vectorisation so the assistant can use it too.
- **Daily Summary**: an AI-generated report on your productivity, built from your task and event history.
- **Reminders and notifications**: a daily reminder plus reminders for specific tasks and events, scheduled with AlarmManager. The reminder time is set in Settings.
- **Trash**: restore deleted items or delete them permanently.
- **Onboarding**: interactive feature tour using TapTargetView.
- **Offline-first**: tasks, notes, events and note embeddings are stored locally with Room.

## Tech Stack

**Android app** (`app/`)
- Kotlin, XML layouts with Material Components (Jetpack Compose theme included)
- Room (SQLite) with KSP, Kotlin Coroutines and Flow
- Firebase Auth, Firestore and Storage, plus FirebaseUI
- Google ML Kit Text Recognition (OCR)
- Google Generative AI SDK (Gemini 2.5 Flash) and MediaPipe Text
- Retrofit with Gson, Volley, kotlinx-serialization
- Min SDK 24, Target/Compile SDK 35

**Backend** (`CampusLife_backend/`)
- Python with FastAPI and Uvicorn
- `sentence-transformers` (`all-MiniLM-L6-v2`) for text embeddings
- `google-genai` (Gemini 2.5 Flash) for answering questions
- Endpoints: `POST /get_embedding/` and `POST /ask/`

## Getting Started

### Backend
```bash
cd CampusLife_backend
pip install -r requirements.txt
echo "GEMINI_API_KEY=your_key" > .env
uvicorn main:app --host 0.0.0.0 --port 8000
```

### Android
1. Open the project in Android Studio (Ladybug or newer, JDK 17+).
2. Add your `google-services.json` to `app/`.
3. In `ApiService.kt`, set `BASE_URL` to your machine's local IP, e.g. `http://192.168.x.x:8000/`.
4. Sync Gradle and run the app on an emulator or device on the same network as the backend.
