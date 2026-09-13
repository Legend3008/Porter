# Porter Logistics Platform — Shipper Android Application

A production-grade Android mobile application for the **Porter Logistics Platform (Shipper Client)**, engineered for intercity and inter-terminal container haulage.

Built strictly according to Clean Architecture, Jetpack Compose Material 3 design tokens, and multi-module Gradle patterns as specified in the platform architectural documents.

---

## Architecture Overview

The codebase is split into **20 Gradle modules** organized into layers:

```
android/shipper/
├── app/                        # Application entry, Compose Navigation graph, FCM service
├── core/
│   ├── common/                 # Sealed 7-state UiState, AppError model & recovery actions
│   ├── designsystem/           # Porter design tokens: Colors, Typography (Inter), Shapes
│   ├── ui/                     # Shared components: PorterButton, PorterCard, StatusBadge, TopBar
│   ├── network/                # OkHttp, Retrofit, AuthInterceptor, IdempotencyInterceptor
│   ├── auth/                   # TokenStorage with EncryptedSharedPreferences (AES256-GCM)
│   ├── database/               # Room PorterDatabase, DAOs, Entity models
│   ├── notifications/          # Notification channels & PorterNotificationManager
│   └── permissions/            # Runtime permission helpers (Location, Notifications)
├── domain/
│   ├── model/                  # Domain entities: Booking, Quote, Payment, Trip, Document, etc.
│   ├── repository/             # Domain repository contracts (Auth, Booking, Tracking, Payment)
│   └── usecase/                # Business logic use cases (GetBookingsUseCase, CreateShipmentDraftUseCase)
├── data/
│   ├── api/                    # Retrofit service interfaces & endpoint contracts
│   ├── database/               # Local data sources wrapping Room caching
│   └── repository/             # Repository implementations with Hilt DI bindings
└── feature/
    ├── auth/                   # Splash, Onboarding, Login, Registration, OTP
    ├── home/                   # Shipment overview, Active dispatch hero, Live bookings
    ├── booking/                # 10-screen booking wizard (Cargo → Container → Route → Quote → Pay)
    ├── tracking/               # Real-time GPS telemetry, Freshness indicator, Milestone timeline
    ├── documents/              # Bill of Lading, Gate Pass, Delivery Order upload & management
    ├── payments/               # GST Tax Invoices, Invoice Detail, Payment History logs
    ├── notifications/          # Push alerts for driver assignment, in-transit milestones
    └── profile/                # Shipper company profile, Settings, Support, Account deletion
```

---

## Tech Stack

- **Language**: Kotlin 1.9.23
- **UI Framework**: Jetpack Compose with Material 3
- **Dependency Injection**: Hilt / Dagger
- **Local Persistence**: Room 2.6.1 + EncryptedSharedPreferences (AndroidX Security Crypto)
- **Networking**: Retrofit 2.9.0 + OkHttp 4.12.0
- **Asynchrony & State**: Kotlin Coroutines & Flow (`StateFlow` / `SharedFlow`)
- **Datetime**: `kotlinx-datetime`
- **Build System**: Gradle 8.7 + Android Gradle Plugin 8.3.2 (Target SDK 34, Min SDK 24)

---

## Core Engineering Principles

1. **Deterministic State Machine**: Every screen models UI with a sealed 7-state `UiState`: `Initial`, `Loading`, `Success`, `Empty`, `Error`, `Offline`, and `Unauthorized`.
2. **Financial Precision**: All monetary values are handled exclusively in **Paise (`Long`)** to prevent IEEE-754 floating-point inaccuracies.
3. **Hardware Keystore Encryption**: Shipper auth tokens are secured using AES256-GCM via Android KeyStore and excluded from automatic backups.
4. **Idempotent APIs**: Mutating network calls transmit unique UUID `Idempotency-Key` headers to guarantee single-execution safety.
5. **Real-time Telemetry & Freshness**: Live tracking incorporates data freshness observables (`LIVE`, `STALE`, `RECONNECTING`, `OFFLINE`).

---

## Building & Running

Ensure JDK 17 or JDK 21 is configured:

```bash
cd android/shipper

# Build debug APK
./gradlew assembleDebug

# Output APK:
# android/shipper/app/build/outputs/apk/debug/app-debug.apk
```
