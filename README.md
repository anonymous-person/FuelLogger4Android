# ⛽ FuelLogger — Android App

Scan fuel receipts and odometer readings with your camera. Data is auto-extracted via ML Kit OCR, reviewed, and saved directly to Google Sheets.

---

## Features

- 📷 **Camera OCR** — Scan receipts and odometer displays; ML Kit extracts all data automatically
- ✏️ **Review & Edit** — Confirm or correct scanned values before saving
- ☁️ **Google Sheets sync** — Every fill-up logs to your spreadsheet instantly
- 📊 **History view** — See recent fill-ups in-app pulled from Sheets
- 📋 **Auto Summary sheet** — Lifetime stats, totals, and last fill-up computed in Sheets

---

## Project Structure

```
FuelLogger/
├── app/src/main/
│   ├── java/com/fuellogger/
│   │   ├── MainActivity.kt        # Home screen + history
│   │   ├── CameraActivity.kt      # Camera + ML Kit OCR
│   │   ├── ReviewActivity.kt      # Edit + save to Sheets
│   │   ├── SettingsActivity.kt    # Webhook URL config
│   │   ├── HistoryAdapter.kt      # RecyclerView adapter
│   │   ├── FuelEntry.kt           # Data model
│   │   ├── OcrParser.kt           # Receipt/odometer text parser
│   │   ├── SheetsRepository.kt    # HTTP → Apps Script
│   │   └── Prefs.kt               # SharedPreferences helper
│   └── res/
│       ├── layout/                # All XML layouts
│       ├── values/                # Colors, themes, strings
│       └── drawable/              # Icons and backgrounds
├── FuelLogger_AppsScript.gs       # Google Sheets backend
└── README.md
```

---

## Setup: Google Sheets Backend

### Step 1 — Create the spreadsheet
1. Go to [sheets.google.com](https://sheets.google.com) and create a new spreadsheet
2. Name it **FuelLogger** (or anything you like)

### Step 2 — Add the Apps Script
1. In the spreadsheet, click **Extensions → Apps Script**
2. Delete any default code in the editor
3. Open `FuelLogger_AppsScript.gs` from this project and paste the entire contents
4. Click **Save** (💾 icon or Ctrl+S)

### Step 3 — Deploy as Web App
1. Click **Deploy → New deployment**
2. Click the gear icon ⚙️ next to "Select type" → choose **Web app**
3. Set:
   - **Description**: FuelLogger API
   - **Execute as**: Me
   - **Who has access**: Anyone
4. Click **Deploy**
5. Authorize the permissions when prompted
6. **Copy the Web App URL** — it looks like:
   `https://script.google.com/macros/s/AKfycb.../exec`

---

## Setup: Android App

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Physical device or emulator with API 24+
- Java 8 compatible JDK

### Build & Install
```bash
# Clone / open the FuelLogger folder in Android Studio
# Let Gradle sync complete

# Connect your Android device (USB debugging enabled)
# Or start an emulator

# Run the app:
./gradlew installDebug
```

Or in Android Studio: **Run → Run 'app'**

### Configure the Webhook
1. Open FuelLogger on your phone
2. Tap the **⋮ menu → Settings**
3. Paste your Google Apps Script Web App URL
4. Tap **Save**

---

## How to Use

### Scan a Receipt + Odometer
1. Tap **📷 Scan Receipt + Odometer**
2. Point camera at your receipt — tap the white circle to capture
3. Review the auto-filled fields; fix anything the OCR missed
4. Tap **📷 Scan Odometer Now** to capture odometer (if not on receipt)
5. Tap **✓ Save to Google Sheet**

### Receipt Only
- Tap **Receipt Only** to scan just the receipt
- Odometer fields will be blank — fill manually or scan separately

### Odometer Only
- Tap **Odometer Only** to update just mileage on an existing entry

---

## OCR Tips for Best Results
- Hold the phone **steady** and ensure good lighting
- Keep the receipt **flat** and fully in frame
- For odometers, capture when **digits are clearly lit**
- If OCR misses fields, you can always edit them manually on the Review screen

---

## Google Sheet Layout

The script auto-creates two sheets:

### "Fuel Log" sheet
| Column | Data |
|--------|------|
| Timestamp | Auto-set when record saved |
| Date, Time | From receipt |
| Station, Address, City, State | Station info |
| Pump #, Fuel Grade | Pump details |
| Gallons, Price/Gal, Total | Transaction amounts |
| Odometer, Trip Miles | Vehicle readings |
| Temp (°F) | Dashboard temperature |
| Payment, Card Last 4 | Payment info |
| Invoice #, Auth Code | Receipt reference |

### "Summary" sheet
Auto-calculated stats: total fill-ups, total gallons, total spent, average price/gal, last fill-up details.

---

## Re-deploying the Apps Script

If you update the `.gs` file, you must **create a new deployment** (not edit the existing one) to pick up changes:
1. Extensions → Apps Script → Deploy → **New deployment**
2. Copy the new URL → update in app Settings

---

## Dependencies

| Library | Purpose |
|---------|---------|
| CameraX | Camera preview & capture |
| ML Kit Text Recognition | On-device OCR |
| OkHttp | HTTP requests to Apps Script |
| Kotlin Coroutines | Async networking |
| Material Components | UI components |
| Gson | JSON serialization |
