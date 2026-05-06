# CampusLife Android Application

## Project Overview
CampusLife is an Android application designed to help university students manage their daily academic tasks, personal notes, and campus events. The application utilizes a local Room database to ensure data persistence and provides a structured way to organize academic responsibilities.

---

## Core Features

*   **Task Management**: Create, edit, and track academic assignments with priority levels and due dates.
*   **Note Taking**: Store detailed notes with support for file attachments and external links.
*   **Event Scheduling**: Organize and view upcoming campus events.
*   **Data Persistence**: Integrated with SQLite via the Room Persistence Library for offline data access.
*   **Trash System**: A safety mechanism to recover or permanently delete items.

---

## Technical Architecture

The project follows the Model-View-Controller (MVC) and Data Access Object (DAO) patterns to maintain a clean separation of concerns.

*   **Database**: Room Database for local structured storage.
*   **Asynchronous Operations**: Kotlin Coroutines and Flow for non-blocking database queries.
*   **UI Components**: RecyclerView for efficient list rendering and Material Design components for the interface.

---

## Data Models

The application manages three primary entities located in the `data` package:

1.  **Task**: Contains title, category, date, time, importance status, and completion state.
2.  **Note**: Includes title, content, date, and mutable lists for attachment URIs and web links.
3.  **Event**: Stores event details, timing, and attendance status.

---

## Implementation Details

### Room Database Configuration
The `AppDatabase` class serves as the main access point for the underlying SQLite database. It includes a singleton pattern to prevent multiple instances of the database from opening at the same time.

---

## Requirements

*   Android Studio Ladybug or newer.
*   JDK 17 or higher.
*   Android API Level 24 (Nougat) or higher.

---

## Dependencies

The following key libraries are used in this project:

*   **androidx.room**: Database management.
*   **kotlinx-coroutines**: Asynchronous programming.
*   **kotlinx-serialization**: JSON parsing for complex data types.
*   **com.getkeepsafe.taptargetview**: Interactive user tutorials.

---

## Installation

1.  Clone the repository to your local machine.
2.  Open the project in Android Studio.
3.  Ensure the KSP plugin is properly configured in your `build.gradle` file.
4.  Sync the project with Gradle files.
5.  Build and run the application on an emulator or physical device.