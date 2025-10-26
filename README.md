# BMR-Ai

<div align="center">

<img src="docs/assets/app-icon.png" alt="BMR Studio Logo" width="150" height="150" />

### *Your Personal AI Nutritionist & Calorie Tracking Companion*

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen?style=for-the-badge)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5.4-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

[📱 Download on Play Store](https://play.google.com/store/apps/details?id=com.kreggscode.bmr) • [🌐 Visit Website](https://www.kreggscode.com) • [📧 Contact](mailto:kreg9da@gmail.com)

</div>

---

## 🌟 Features

### 📊 BMR & TDEE Calculator
- Calculate your Basal Metabolic Rate using Mifflin-St Jeor or Harris-Benedict formulas
- Get personalized TDEE based on activity levels
- Macro recommendations for your goals
- AI analysis of your metabolic needs

### 📸 AI Food Scanner
- **Camera Recognition**: Point and shoot to identify foods instantly
- **Gallery Import**: Analyze existing food photos
- **Manual Entry**: Add foods with detailed nutritional info
- **Barcode Scanner**: Quick product lookup
- **Powered by Pollinations AI** for accurate food recognition

### 🍽️ Personalized Diet Plans
- AI-generated 7-day meal plans
- Customizable for dietary preferences (vegetarian, vegan, keto, etc.)
- Automatic shopping list generation
- Macro-balanced meals aligned with your goals

### 🤖 AI Nutritionist Chat
- 24/7 access to personalized nutrition advice
- Context-aware responses based on your profile
- Evidence-based recommendations
- Quick prompts for common questions

### 📈 Progress Tracking
- Visual weight trend charts
- Calorie intake monitoring
- Macro distribution analysis
- Achievement system for motivation
- Weekly and monthly insights

### 🎨 Premium UI/UX
- **Glassmorphic Design**: Stunning frosted glass effects
- **Animated Gradients**: Dynamic color transitions
- **Edge-to-Edge Display**: Full screen immersive experience
- **Dark & Light Themes**: Automatic theme switching
- **Smooth Animations**: Spring physics and fade transitions
- **Floating Navigation**: Beautiful bottom navigation bar

## 🛠️ Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository Pattern
- **Dependency Injection**: Hilt
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil
- **AI Integration**: Pollinations AI API
- **Navigation**: Navigation Compose
- **Animations**: Lottie, Compose Animations
- **Camera**: CameraX
- **Permissions**: Accompanist

## 📱 Screenshots

| Splash Screen | Home Dashboard | BMR Calculator |
|---------------|----------------|----------------|
| ![Splash](screenshots/splash.png) | ![Home](screenshots/home.png) | ![Calculator](screenshots/calculator.png) |

| Food Scanner | Diet Plans | AI Chat |
|--------------|------------|---------|
| ![Scanner](screenshots/scanner.png) | ![Diet](screenshots/diet.png) | ![Chat](screenshots/chat.png) |

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog | 2023.1.1 or higher
- JDK 17
- Android SDK 34
- Minimum Android 8.0 (API 26)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/kreggscode/BMR-Ai.git
```

2. Open the project in Android Studio

3. Sync the project with Gradle files

4. Add Pollinations AI configuration (optional):
   - The app works with fallback data if API is unavailable
   - For full functionality, configure API endpoints in `NetworkModule.kt`

5. Run the app on an emulator or physical device

## 🏗️ Project Structure

```
app/
├── src/main/java/com/kreggscode/bmr/
│   ├── data/
│   │   ├── local/          # Room database & DAOs
│   │   └── remote/         # API services & DTOs
│   ├── di/                 # Dependency injection modules
│   ├── presentation/
│   │   ├── screens/        # UI screens
│   │   └── viewmodels/     # ViewModels
│   └── ui/
│       ├── components/     # Reusable UI components
│       └── theme/          # Colors, typography, theme
└── res/                    # Resources (layouts, drawables, values)
```

## ✨ Key Components

### Glassmorphic Cards
Beautiful frosted glass effect cards with gradient borders:
```kotlin
GlassmorphicCard(
    modifier = Modifier.fillMaxWidth(),
    cornerRadius = 24.dp,
    borderWidth = 1.5.dp,
    blurRadius = 25.dp
) {
    // Your content here
}
```

### Animated Gradient Buttons
Eye-catching buttons with animated gradients:
```kotlin
AnimatedGradientButton(
    text = "Calculate BMR",
    onClick = { /* action */ },
    isLoading = false
)
```

### Circular Progress Display
Beautiful circular progress indicators with gradients:
```kotlin
CircularProgress(
    progress = 0.75f,
    strokeWidth = 12.dp,
    colors = listOf(PrimaryTeal, PrimaryIndigo, PrimaryPurple)
)
```

## 🎨 Design System

### Color Palette
- **Primary**: Teal (#14B8A6) → Indigo (#6366F1) → Purple (#8B5CF6)
- **Accent**: Coral (#F87171) → Pink (#EC4899)
- **Success**: Emerald (#10B981)
- **Warning**: Amber (#F59E0B)
- **Error**: Red (#EF4444)

### Typography
- **Font**: System default with Material3 type scale
- **Headings**: Bold, scaled for hierarchy
- **Body**: Regular weight, optimized line height

### Spacing
- **Padding**: 20dp standard, 12dp compact
- **Corner Radius**: 20-24dp for cards, 16dp for buttons
- **Elevation**: Subtle shadows for depth

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 👨‍💻 Author

**Kreggs Code**
- 🌐 Website: [www.kreggscode.com](https://www.kreggscode.com)
- 📧 Email: [kreg9da@gmail.com](mailto:kreg9da@gmail.com)
- 💼 GitHub: [@kreggscode](https://github.com/kreggscode)

## 🙏 Acknowledgments

- [Pollinations AI](https://pollinations.ai) for AI-powered features
- [Material Design 3](https://m3.material.io) for design guidelines
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI toolkit
- [CameraX](https://developer.android.com/training/camerax) for camera functionality
- [Hilt](https://dagger.dev/hilt/) for dependency injection

## 📞 Support

For support, email [kreg9da@gmail.com](mailto:kreg9da@gmail.com) or open an issue in the GitHub repository.

## ⭐ Rate the App

If you enjoy using BMR Studio, please consider:
- ⭐ [Rating on Play Store](https://play.google.com/store/apps/details?id=com.kreggscode.bmr)
- 🌟 Starring this repository
- 📢 Sharing with friends and family

---

<div align="center">

Made with ❤️ by **Kreggs Code**

[Website](https://www.kreggscode.com) • [Email](mailto:kreg9da@gmail.com) • [Play Store](https://play.google.com/store/apps/details?id=com.kreggscode.bmr)

</div>
