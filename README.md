# Deflate - Personal Wellness & Activity Tracker

**Deflate** is an Android application designed to help users track their daily activities, mood, and wellness journey. The app provides a holistic approach to personal health management with features for activity tracking, mood monitoring, diary entries, and inspirational quotes.

## Overview
<p> <img src="Screenshots/Landing.png" width="200"> 
  <img src="Screenshots/Home.png" width="200"> 
  <img src="Screenshots/Diary.png" width="200">
  <img src="Screenshots/Activities.png" width="200">
</p>

## 🎉 What's New in Version 1.0

### Authentication & Security
- **Multiple Sign-In Options**: Added support for Google Sign-In, Facebook Login, and GitHub OAuth authentication alongside traditional email/password
- **Biometric Authentication**: Implemented fingerprint and face recognition login for enhanced security and convenience
- **Secure Password Management**: Added password change functionality with reauthentication requirements
- **Account Management**: Users can now delete their accounts with double-confirmation security

### 🎭 Mood Tracking System
- **Six Mood Options**: Track your emotional state with Happy, Sad, Anxious, Tired, Excited, and Content moods
- **Visual Mood Indicators**: Each mood has a unique color scheme and icon for easy recognition
- **Mood History**: View your mood patterns over time with detailed analytics
- **Home Screen Quick Logging**: Log your daily mood instantly from the home screen

### 💡 **INNOVATIVE: Intelligent Quote System**
- **Mood-Based Quote Recommendations**: Automatically fetches motivational quotes tailored to your current emotional state using the FavQs API
- **Smart Tag Mapping**: Each mood is mapped to relevant quote categories (e.g., "Sad" → motivation, hope, strength quotes)
- **Offline Quote Cache**: Quotes are cached locally for offline access, ensuring inspiration is always available
- **Multi-Language Quote Translation**: Quotes are automatically translated based on your device language

### 📔 Comprehensive Diary System
- **Rich Text Entries**: Write detailed diary entries with mood association
- **Entry History**: Browse and edit previous diary entries with a clean, organized interface
- **Timestamp Tracking**: All entries are automatically timestamped for easy reference
- **Offline Support**: Write entries even without internet connection - they'll sync automatically when online

### 📊 **INNOVATIVE: Advanced Insights & Analytics**
- **Custom Data Visualizations**:
  - **Mood Bar Chart**: Visualize your mood distribution with custom-built bar chart view
  - **Donut Chart**: See entry categories breakdown (mood, weight, activities)
  - **Steps Line Graph**: Track your daily activity levels over time
  
- **Flexible Time Filters**: Analyze your data by day, week, month, or custom date ranges
- **Streak Statistics**: View completed 7-day streak milestones
- **Entry Categories Breakdown**: See how many mood, weight, and activity entries you've logged
- **Total Steps Tracking**: Monitor your physical activity with weekly, monthly, and yearly views

### 🔥 **INNOVATIVE: Smart Streak Tracking System**
- **Consecutive Day Counting**: Automatic tracking of consecutive days with diary or mood entries
- **Weekly Goal Progress**: Visual progress bar showing days logged in the current week (0-7 days)
- **Streak Persistence**: Maintains streak across app restarts and even if you log later in the day
- **Milestone Notifications**: Celebrates achievements at 1, 3, 7, 14, 30, 60, 90, and 365-day milestones
- **Longest Streak Tracking**: Remembers your best streak performance

### 🚨 **INNOVATIVE: Crisis Detection System**
- **Pattern Analysis**: Monitors mood entries for concerning patterns (80% negative moods over 5 days)
- **Smart Alerts**: Notifies users when prolonged negative mood patterns are detected
- **Cooldown Period**: Implements 3-day notification cooldown to avoid overwhelming users
- **Trend Analysis**: Provides mood trend indicators (Improving, Stable, Declining)

### 📅 Interactive Calendar View
- **Material Calendar Integration**: Beautiful calendar interface showing entry history
- **Date-Based Entry Access**: Tap any date to view entries from that day
- **Visual Entry Indicators**: Dates with entries are highlighted for easy identification

### 🏃 Activity & Health Tracking
- **Step Counter**: Log daily steps with visual graph representations
- **Weight Tracking**: Monitor weight changes over time
- **Activity History**: View all logged activities with timestamps
- **Time-Based Analysis**: Compare activity levels across different time periods

### 🔔 **INNOVATIVE: Smart Notification System**
- **Configurable Reminders**: Choose between daily, weekly, or monthly diary reminders
- **Crisis Notifications**: Automatic alerts when negative mood patterns are detected
- **Streak Reminders**: Notifications to maintain your logging streak
- **WorkManager Integration**: Reliable notification delivery using Android WorkManager
- **Customizable Schedule**: Set your preferred reminder time

### 🌐 Multi-Language Support
- **English & Afrikaans**: Full app localization in two languages
- **Language Toggle**: Easy switching between languages in settings
- **Real-Time Translation**: Quotes and dynamic content are translated on-the-fly using ML Kit
- **Persistent Language Choice**: Your language preference is saved across sessions

### 💾 **INNOVATIVE: Offline-First Architecture**
- **Room Database Integration**: All data is stored locally first for instant access
- **Automatic Sync**: Data syncs to Firebase Firestore when internet connection is available
- **Conflict Resolution**: Smart handling of data conflicts between local and cloud storage
- **Offline Functionality**: Full app functionality without internet connection
- **Sync Indicators**: Visual feedback when data is being synced

### 🎨 Modern User Interface
- **Material Design 3**: Implemented latest Material Design guidelines
- **Custom Fonts**: Beautiful typography with custom font families
- **Smooth Animations**: Polished transitions and micro-interactions
- **Dark Mode Ready**: UI components designed with dark mode considerations
- **Responsive Layouts**: Optimized for different screen sizes and orientations
- **Bottom Navigation**: Quick access to all major app sections

### 🔧 Settings & Customization
- **Profile Management**: Update your display name with offline support
- **Password Updates**: Change password with security reauthentication
- **Biometric Toggle**: Enable/disable biometric authentication
- **Notification Preferences**: Full control over notification frequency and types
- **Language Selection**: Choose your preferred language
- **Account Deletion**: Option to permanently delete your account and all data

## 🛠️ Technical Stack

### Core Technologies
- **Language**: Kotlin
- **Platform**: Android (API 24+)
- **Architecture**: MVVM with Activities
- **Build System**: Gradle with Kotlin DSL

  ### External API
- **FavQs API**: For inspirational quotes based on mood

1. **Installation**
- Within the repository, click on the "<> Code" drop down on the far right next to the "Go to file" and "+" buttons.
- On the Local tab, click on the last option: "Download ZIP".
- Once the zip file has downloaded, open your local file explorer.
- Go to your Downloads.
- Click on the "Deslynn_ST10251981_PROG7314_Part2.zip" folder, should be most recent in Downloads.
- Extract the files and store the project in the location of choice.
- Navigate to Android Studio.
- To open the project, click File > Open > Choose the project.

2. **Build and Run**
   -  To run the application, click on the play button.

   ## 🔐 Security Features

- **Firebase Authentication**: Secure user management
- **Data Encryption**: All data encrypted in transit and at rest
- **Input Validation**: Comprehensive input sanitization
- **OAuth Integration**: Secure third-party authentication

## 🌟 Key Features in Detail

### Mood-Based Quote System
The app selects inspirational quotes based on your current mood:
- **Happy**: Joy, positivity, life quotes
- **Sad**: Motivation, hope, strength quotes
- **Anxious**: Wisdom, peace, calm quotes
- **Tired**: Energy, perseverance, success quotes
- **Excited**: Inspiration, enthusiasm, adventure quotes
- **Content**: Peace, gratitude, harmony quotes

### Real-time Data Sync
- All user data automatically synced with Firebase Firestore
- Offline support with local caching
- Real-time updates across devices

---
## Git Actions

## 📺 Youtube
- link: https://www.youtube.com/watch?v=8cBRrMAoGz0 
