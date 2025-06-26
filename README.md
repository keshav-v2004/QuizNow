# 🧠 Computer Science Quiz App

A modern and interactive **Quiz App** built using **Jetpack Compose** to help users practice and master various topics in **Computer Science**. The app integrates with **Gemini AI** to generate fresh, non-repetitive questions. Users can **bookmark** questions they are unsure about, which are securely stored using **Firebase**. Authentication is handled through **Firebase Auth**, supporting both **email/password** and **Google Sign-In** options.

---

## ✨ Features

- 🎯 **AI-Generated Questions**
  - Powered by **Gemini AI**, the app generates unique questions every time to keep practice sessions fresh and challenging.
  - Eliminates repetition, improving learning efficiency.

- 📚 **Wide Topic Coverage**
  - Practice questions across multiple domains of Computer Science:
    - Data Structures and Algorithms
    - DevOPS
    - Machine Learning
    - AWS
    - OOPS
    - Data Science, and more.

- 🔖 **Bookmark System**
  - Users can bookmark questions they find difficult or important.
  - Bookmarked questions are saved to the user's Firebase account for later review.

- 🔐 **User Authentication**
  - Supports **Email and Password** authentication.
  - Seamless **Google Sign-In** authentication.

- 🖌️ **Modern Declarative UI**
  - Built entirely with **Jetpack Compose**, offering a sleek and responsive interface.
  - Optimized for performance and scalability.

- ☁️ **Firebase Integration**
  - Realtime syncing of bookmarks and user sessions.
  - Secure cloud data storage and retrieval.

---

## 🛠️ Tech Stack

| Layer             | Technology                        |
|------------------|------------------------------------|
| UI                | Jetpack Compose                    |
| Language          | Kotlin                             |
| AI Integration    | Gemini API (REST/SDK)              |
| Backend/Database  | Firebase Firestore or Realtime DB  |
| Authentication    | Firebase Auth (Email & Google)     |
| Networking        | Retrofit / Ktor                    |
| Dependency Injection | Hilt / Koin (Optional)         |

---

## 🚀 Getting Started

### ✅ Prerequisites

- Android Studio Giraffe (or later)
- A Firebase project with:
  - Firebase Authentication
  - Firestore or Realtime Database
- Google Cloud Console project with:
  - Gemini API key
  - SHA-1 FingerPrint for Google Sign-In

---

## 🚀 App Demo Video
https://youtube.com/shorts/qyAaOIUfGgg

---

### 🔧 Setup Instructions

1. **Clone the repository**

```bash
git clone https://github.com/yourusername/quiz-app.git
cd quiz-app
