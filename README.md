# Lojia — Offline-First POS & Shift Management

**Lojia** is an offline-first Point of Sale (POS) and shift report management application designed for single-store retail and food service operations on Android. All data is stored securely and locally on the device using Android Room SQLite database. Optional external services or integrations operate only when explicitly configured by the store owner.

---

## 🎯 Key Capabilities

### 🛒 Point of Sale (POS)
- **Fast Touch Register**: Grid and list catalog views with instant category filtering and barcode scanner search.
- **Cart & Order Management**: Real-time total, item discount, taxes, and order breakdown.
- **Payment Processing**: Support for Cash, Card/Mada, and Digital Wallet payment methods with change calculation.
- **Receipt Generation**: Printable and shareable invoice/receipt with customizable store headers, footers, and tax identifiers.

### 📊 Shift Management & Reconciliation
- **Automated Shift Reports**: Cashier opening, mid-shift entries, and closing revenue summaries.
- **Cash Drawer Audit**: Track gross cash received, electronic payments, operational expenses, staff meals, and net drawer variance.
- **PDF & CSV Export**: Export comprehensive shift audit certificates and ledger summaries directly to device storage.

### 📦 Inventory & Catalog
- **Product & Category Management**: Add, update, and categorize products with SKU/barcode, cost, and selling prices.
- **Stock Tracking**: Low-stock alerts and inventory updates directly linked to sales transactions.

### 🔒 Security & Access Control
- **Role-Based Permissions**: Admin, Cashier, and Staff roles with granular access controls.
- **Biometric & MPIN Authentication**: Hardware-backed biometric authentication (Fingerprint / Face Unlock) via Android `BiometricPrompt` and secure 6-digit MPIN.
- **Credential Protection**: Passwords and PINs are securely hashed before storage in the local database.

### 🌐 Internationalization & Formatting
- **Standardized Currency**: Locale-aware currency formatting conforming to ISO 4217 currency standards.
- **Localized Date & Time**: Standardized date and time formatting based on device locale.
- **Multi-Language Support**: Complete localization with dynamic in-memory and Room persistent translation caching.

---

## 🏗️ Architecture & Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
- **Asynchronous Operations**: Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Local Persistence**: Android Room Database (`RoomDatabase`) with explicit schema migrations
- **Dependency Injection**: Constructor Injection
- **Code Shrinking & Security**: R8 / ProGuard minification enabled for release builds

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 35 (compileSdk 35, minSdk 26)
- JDK 17 / 21

### Build Configurations

#### Debug Build
```bash
./gradlew assembleDebug
```
- Includes sample debug catalog and preloaded demo credentials (`DevCredentials.kt`) for rapid local development and testing.

#### Release Build
```bash
./gradlew assembleRelease
```
- Fully hardened release artifact with R8 code shrinking and resource obfuscation enabled.
- No preloaded seed data; prompts for initial store owner setup on first launch.

---

## 📄 License & Terms

Lojia is proprietary software licensed for store management and point-of-sale operations. All store ledger records, inventory catalogs, and audit logs remain strictly under the local control and ownership of the device operator.
