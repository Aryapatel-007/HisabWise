# HisabWise

HisabWise is a native Android application designed for expense tracking and management. It provides capabilities for both offline personal expense management and synchronized group expense sharing.

## Download App

**[Download HisabWise APK](release/HisabWise.apk)**  
*(Click to download and install directly on your Android device)*

## Features

- **Private Hisab (Offline Tracking):** Manage personal expenses locally on the device using Room Database for secure and fast data storage.
- **Group Hisab (Shared Tracking):** Collaborate with multiple users to track shared expenses. The application automatically synchronizes data via Firebase and calculates optimal settlement amounts.
- **Automated Split Calculations:** The system automatically computes expense distributions and provides a clear summary of outstanding balances among group members.
- **Modern User Interface:** Built entirely with Jetpack Compose, utilizing Material Design 3 guidelines for a responsive and consistent user experience.

## Technical Stack

- **UI Framework:** Jetpack Compose, Material Design 3
- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel)
- **Dependency Injection:** Dagger-Hilt
- **Local Storage:** Room Database, DataStore (User Preferences)
- **Backend & Authentication:** Firebase Realtime Database, Firebase Authentication
- **Asynchronous Operations:** Kotlin Coroutines & Flow

## Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17 or higher

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/Aryapatel-007/HisabWise.git
   ```
2. Open the project in Android Studio.
3. Add a valid `google-services.json` file inside the `app/` directory to enable Firebase functionality.
4. Build and run the project.

## Contributing

Contributions are welcome. Please open a Pull Request for any proposed changes or improvements.
