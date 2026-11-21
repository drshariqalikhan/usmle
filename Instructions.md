# Step-by-Step Hobby Coder Guide: Testing on Samsung Galaxy A06 5G

Welcome! This guide is written specifically for you as a hobby coder. You do **not** need a paid developer account, you do **not** need expensive software, and you can test this app directly on your physical **Samsung Galaxy A06 5G**.

---

## 🎯 Quick Reality Check: What You Need & What You Don't

| Item | Do You Need It? | Explanation |
| :--- | :---: | :--- |
| **Google Play Developer Account** | ❌ **NO** | You only need an account (\$25 fee) if you publish to the public store. Android allows **free sideloading** directly to your own phone. |
| **Android Studio on PC** | ⚙️ **OPTIONAL** | **Method 1** builds the APK in the cloud for free using GitHub Actions (zero software on your PC!). **Method 2** uses Android Studio if you prefer a local setup. |
| **Anki Desktop** | ❌ **NO** | Just download **AnkiDroid** (100% free and open-source) from the Google Play Store on your Samsung phone. |
| **OpenRouter API Key** | ✅ **YES** | You already have this! You can paste it directly into the app or tap 1-click browser sign-in. |
| **Samsung Galaxy A06 5G** | ✅ **READY** | It runs Android with Google Play Services pre-installed, which fully supports the ML Kit Document Scanner and on-device OCR. |

---

## Part 1: Prepare Your Samsung Galaxy A06 5G (Takes 2 Minutes)

Before installing any debug app, turn on Android Developer Options on your phone:

1. **Open Settings** on your Samsung Galaxy A06.
2. Scroll to the bottom and tap **About phone**.
3. Tap **Software information**.
4. Find **Build number** and tap it **7 times** quickly.
   * *A message will pop up saying: "Developer mode has been enabled!"*
5. Go back to the main **Settings** menu and scroll to the bottom. You will now see **Developer options**.
6. Tap **Developer options** and toggle **USB debugging** to **ON** (press *OK* when prompted).

---

## Part 2: Install AnkiDroid on Your Phone (Free)

To enable the flashcard export feature for missed quiz questions:

1. Open the **Google Play Store** on your Samsung phone.
2. Search for **AnkiDroid Flashcards** (by *David Allison / Nicolas Raoul*).
3. Tap **Install** (it is completely free, open-source, and has no ads).
4. Open AnkiDroid once to grant standard storage permissions.

---

## Part 3: Getting the App onto Your Phone (Choose Method 1 or 2)

### 🚀 Method 1: Cloud Build via GitHub Actions (Recommended — Zero PC Setup)

Since this project is hosted on GitHub, GitHub can compile the Android APK in the cloud for you for free:

1. **Commit and push** your latest code to your GitHub repository:
   ```bash
   git add .
   git commit -m "Add GitHub Actions APK builder and manual key entry"
   git push origin main
   ```
2. Open your repository in your web browser on your computer or phone:
   * Go to the **Actions** tab at the top.
   * Click on the workflow named **Build Android Debug APK**.
   * It will automatically run on your push (or click **Run workflow**).
   * It takes ~2–3 minutes to compile.
3. Once the build shows a green checkmark `✓`:
   * Click on the completed workflow run.
   * Scroll down to the **Artifacts** section at the bottom.
   * Click **step2ck-scanquiz-debug-apk** to download the zip file.
4. **Transfer & Install on Samsung Galaxy A06**:
   * If downloaded on your computer: send the `.apk` file to your phone via Google Drive, WhatsApp, Quick Share, or a USB cable.
   * If downloaded on your phone's browser: open your phone's **My Files** app > **Downloads**, extract the zip, and tap `app-debug.apk`.
   * When Android asks *"Do you want to install this app from an unknown source?"*, tap **Settings** > **Allow from this source**, then tap **Install**.

---

### 💻 Method 2: Local Build via Android Studio (If You Prefer Local PC Software)

If you would like a local development setup on your PC:

1. **Download Android Studio**:
   * Visit [developer.android.com/studio](https://developer.android.com/studio) and download Android Studio (Ladybug or later).
   * Run the installer with default settings (it installs Java, Android SDK, and device drivers automatically).
2. **Open the Project**:
   * Launch Android Studio and click **Open**.
   * Select your project folder (`usmle`).
   * Allow Gradle to complete its initial sync (this downloads the required libraries).
3. **Connect Your Samsung Phone**:
   * Plug your Samsung Galaxy A06 into your computer using a USB-C data cable.
   * Unlock your phone screen. A popup will ask: *"Allow USB debugging?"* Check **Always allow from this computer** and tap **Allow**.
   * In the top toolbar of Android Studio, select your **Samsung SM-A065** in the device dropdown.
4. **Click Run**:
   * Click the green **Play (Run 'app')** button (or press `Shift + F10`).
   * Android Studio will build the app and install it directly onto your phone.

---

## Part 4: Testing the App End-to-End

Once **Step 2 CK Scan-to-Quiz** opens on your phone:

### 1. Add Your OpenRouter API Key
* On the Home Screen, under **Daily Free Quota**, tap **Connect Key**.
* In the popup dialog, you have two choices:
  * **Option A (Instant Paste):** Paste your existing `sk-or-v1-...` API key into the text box and tap **Save Key**.
  * **Option B (Browser Sign-In):** Tap **Sign In with OpenRouter (Browser)** to authorize via your OpenRouter account.
* The quota bar will update to show your free daily request count (e.g., `50 / 50 left`).

### 2. Scan Textbook Pages
* Tap **Open Page Scanner**.
* The Google ML Kit Document Scanner will open using your Samsung camera:
  * Line up 1 to 8 pages of your USMLE review book (e.g., First Aid, Pathoma).
  * Take photos of the pages. The scanner will automatically detect page borders, correct skew, and remove shadows.
  * Tap **Done**.

### 3. Review Detected Elements & Configure Quiz
* The **Confirmation Card** will display:
  * Pages scanned, total text characters.
  * Detected tables (e.g., pharmacology comparison tables).
  * Detected figures (flowcharts, diagnostic algorithms).
* Toggle **Include Tables** and/or **Include Figures** ON.
* Select question count: **5 questions** (default).
* Tap **Generate Clinical Quiz**.
  * *The app makes exactly ONE API call to OpenRouter's free medical models (e.g., Llama 3.3 70B / Gemma 3 27B) with zero prompt bloat.*

### 4. Take the Exam & Review Scorecard
* Answer the board-style multiple choice questions (`A` through `E`).
* Tap **Finish & Grade Quiz**.
* The grading runs **100% locally on your phone (0 API calls)**:
  * Shows your score percentage and full question-by-question rationales.
  * Shows your **Targeted Weak-Area Analysis** mapped back to specific textbook tables/sections.

### 5. Export Missed Questions to AnkiDroid
* On the Scorecard, tap **Export Missed to Anki**.
* Tap **Unlock Pro** (simulated one-time unlock in test mode).
* When the Android Share Sheet appears, select **AnkiDroid**.
* AnkiDroid will open and show: *"Imported X cards successfully!"*
* You can now review your flashcards on your phone anytime, completely offline.

---

## 🛠️ Troubleshooting & Frequently Asked Questions

* **"App Not Installed" error when tapping the APK:**
  Make sure you uninstalled any conflicting test version first. In Samsung Settings > Apps > Special access > Install unknown apps, verify that the app you used to download the file (Chrome or My Files) has permission enabled.
* **Camera scanner doesn't open:**
  Make sure Google Play Services is updated on your phone (open Play Store, search for "Google Play services", and tap update if available).
* **"Rate limit reached" error:**
  OpenRouter free tier accounts allow 50 free requests per day (resetting at 00:00 UTC) and up to 20 requests per minute. The app has a built-in 5-second cooldown to prevent accidental rapid clicks.

