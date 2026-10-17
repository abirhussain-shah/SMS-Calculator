# SpendSMS — Installation, Build & Deployment Guide

SpendSMS is an offline-first, privacy-focused expense tracker for Bangladesh (bKash, Nagad, Rocket, card transactions, and bank alerts). It has zero network libraries, no internet permission (`android.permission.INTERNET` is omitted), and stores all data securely on-device.

---

## 1. How to Build the APKs

### Prerequisites
- **JDK**: Java Development Kit 21
- **Android SDK**: `minSdk 35`, `targetSdk 36`, `compileSdk 36`
- **PowerShell / Terminal**: Run commands from the project root (`e:\Antigravity Practice\Mobile app-SMS calculator`).

### A. Debug APK (Instant Build for Testing)
Run the following command:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\gradlew.bat assembleDebug
```
- **Output Location**: `app\build\outputs\apk\debug\app-debug.apk`
- **Direct Copy**: `SpendSMS-debug.apk` in project root

---

### B. Release APK (Optimized & Minified)

#### Step 1: Create your Release Keystore (One-Time Setup)
Run this command from your terminal:
```bash
keytool -genkeypair -v -keystore spendsms-release.jks -alias spendsms -keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12
```
- You will be prompted to enter a password for the keystore and provide certificate details (name, organization, etc.).
- This produces `spendsms-release.jks` in the project root.

#### Step 2: Configure `keystore.properties`
Copy `keystore.properties.example` to `keystore.properties`:
```properties
storeFile=spendsms-release.jks
storePassword=YOUR_ACTUAL_KEYSTORE_PASSWORD
keyAlias=spendsms
keyPassword=YOUR_ACTUAL_KEY_PASSWORD
```
> [!CAUTION]
> `keystore.properties` and `*.jks` are automatically ignored in `.gitignore`. **NEVER** commit your real passwords or keystore file to Git.

#### Step 3: Build the Release APK
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\gradlew.bat assembleRelease
```
- **Output Location**: `app\build\outputs\apk\release\app-release.apk`
- **Optimizations**: Fully minified with R8, unused resources shrunk, baseline profiles compiled (~3.8 MB).

---

## 2. Version Updates (How to Bump Version)

When releasing an update, open [`app/build.gradle.kts`](app/build.gradle.kts) and increment `versionCode` and `versionName` in `defaultConfig`:

```kotlin
defaultConfig {
    applicationId = "com.spendsms"
    minSdk = 35
    targetSdk = 36
    versionCode = 2          // Increment by +1 for every new release build
    versionName = "1.0.1"    // Follow semantic versioning: "1.0.1", "1.1.0", etc.
    ...
}
```

Android uses `versionCode` (an integer) to determine if an installed APK can be upgraded. It must strictly increase with every release.

---

## 3. How to Install on Your Phone

### Option A: Via USB with ADB (Fastest)
1. Enable **Developer Options** on your phone:
   - Go to **Settings** $\rightarrow$ **About Phone** $\rightarrow$ tap **Build Number** 7 times.
   - Go to **Settings** $\rightarrow$ **System / Developer Options** $\rightarrow$ turn **ON** **USB Debugging**.
2. Connect your phone to your PC via USB cable.
3. Run:
   ```powershell
   adb install -r "app\build\outputs\apk\debug\app-debug.apk"
   ```

### Option B: Sideload via WhatsApp / Telegram / File Transfer
1. **Send the APK**:
   - Open WhatsApp Web or Desktop on your PC.
   - Attach [`SpendSMS-debug.apk`](file:///e:/Antigravity%20Practice/Mobile%20app-SMS%20calculator/SpendSMS-debug.apk) as a **Document** to your own chat (*"Message yourself"*).
   - Alternatively, connect via USB in "File Transfer" mode and copy to your phone's **Downloads** folder.
2. **Install**:
   - On your phone, tap the file in WhatsApp or your File Manager app.
   - When prompted with *"Install unknown apps"*, tap **Settings** $\rightarrow$ toggle **Allow from this source** $\rightarrow$ tap **Install**.

---

## 4. Granting SMS Permissions (Android 13/14/15 Restricted Settings)

On modern Android versions, sideloaded apps outside Google Play have sensitive permissions (like SMS) restricted by default:

1. Open your phone's **Settings** $\rightarrow$ **Apps** $\rightarrow$ **SpendSMS** (or **See all apps** $\rightarrow$ **SpendSMS**).
2. Tap the **three dots (⋮)** in the top-right corner of the App Info screen.
3. Tap **"Allow restricted settings"** and authenticate with your fingerprint or screen PIN.
4. Now tap **Permissions**:
   - **SMS**: Tap and select **Allow**.
   - **Notifications**: Tap and select **Allow** (needed for budget alerts and recurring reminders).

---

## 5. 10-Step Post-Install Manual Test Checklist

Run through these 10 steps on your physical phone to confirm end-to-end functionality:

| # | Test Step | Action & Expected Outcome |
|---|---|---|
| **1** | **App Launch & Welcome** | Open SpendSMS. Splash screen loads smoothly into Onboarding/Home screen with Material 3 styling. |
| **2** | **Permission Validation** | Complete permission prompts (SMS and Notifications). Confirm status switches to granted without crashes. |
| **3** | **Historical SMS Import** | Run historical SMS scan. Confirm past bKash, Nagad, Rocket, card, and bank SMS are read and listed. |
| **4** | **Live Incoming SMS** | Trigger or receive a new banking/MFS transaction SMS. Confirm the incoming SMS receiver parses the amount, sender, and merchant automatically. |
| **5** | **Needs Review & Auto-Suggest** | Navigate to the "Needs Review" screen. Open an item; verify smart suggestions pre-fill category and merchant with the "Auto-suggested" badge. Save with "Remember for future" checked. |
| **6** | **Merchant Category Rules** | Go to **Settings $\rightarrow$ Merchant Category Rules**. Confirm the newly remembered rule is listed. Add a custom rule and search for it. |
| **7** | **Category Budgets & Alerts** | Open **Budgets**. Set a monthly limit for a category (e.g. Food). Add/convert transactions exceeding 80% and 100%. Verify local notification banner fires. |
| **8** | **Recurring Bills & Reminders** | Open **Recurring Payments**. Verify detected recurring subscriptions/bills appear. Tap "Track" and confirm scheduled due date reminder. |
| **9** | **Cash Entry & Search** | Tap **+ Cash Entry**. Add a manual Tk 250 expense. Go to **Search & Filters**, type the merchant or filter by date, and verify instant match. |
| **10** | **App Lock, Theme & CSV Export** | In **Settings**: toggle Dark/Light theme, toggle Bengali/English language, export transactions to CSV, and enable Biometric/PIN lock. Background the app, reopen, and confirm biometric unlock prompt. |

---

## 6. ⚠️ Critical Keystore Security Notice

> [!CAUTION]
> **BACK UP YOUR KEYSTORE AND PASSWORDS SECURELY**
> 
> - **Never lose `spendsms-release.jks` or its passwords**: Android strictly enforces that all updates to an installed application must be digitally signed with the exact same cryptographic key certificate.
> - **Consequence of loss**: If you lose the keystore file or forget the passwords, you will **never** be able to install an updated version over the old app. Users would be forced to uninstall the app completely, which permanently deletes their local SQLite database, custom merchant rules, and budget records.
> - **Safe Storage**: Store `spendsms-release.jks` and your passwords in an encrypted password vault (Bitwarden, 1Password, or a secure offline drive). **Never commit them to public Git repositories.**
