# Implementation Plan - Retail360 Stabilization and Branding

This plan outlines the steps to fix existing errors, unify branding under "Retail360", add UI previews, and complete the underlying infrastructure (Database, Firebase, Repositories) for deployment readiness.

## User Review Required

> [!IMPORTANT]
> - **Package Change**: All code will be moved/updated to the `com.example.retail360` package.
> - **Mock Services**: Initial deployment-ready UI will use mock implementations for Firebase and Cloudinary where real credentials aren't provided, but the architecture will support full integration.
> - **Extensions**: I will add a `Extensions.kt` file to handle `collectAsStateSafe` and currency formatting (`ksh`).

## Proposed Changes

### 1. Infrastructure & Data Layer
- **[NEW] [AppDatabase.kt](file:///C:/Users/Tom/AndroidStudioProjects/Retail360/app/src/main/java/com/example/retail360/data/AppDatabase.kt)**: Room database definition.
- **[NEW] [Daos.kt](file:///C:/Users/Tom/AndroidStudioProjects/Retail360/app/src/main/java/com/example/retail360/data/Daos.kt)**: All required Room DAOs (Customer, Product, Visit, Availability, Sale).
- **[MODIFY] [Graph.kt](file:///C:/Users/Tom/AndroidStudioProjects/Retail360/app/src/main/java/com/example/retail360/util/Graph.kt)**: Complete dependency injection for repositories, database, and Firebase.
- **[NEW] [FirebaseHelper.kt](file:///C:/Users/Tom/AndroidStudioProjects/Retail360/app/src/main/java/com/example/retail360/data/FirebaseHelper.kt)**: Mock/Real Firebase wrapper.
- **[MODIFY] [CustomerRepository.kt](file:///C:/Users/Tom/AndroidStudioProjects/Retail360/app/src/main/java/com/example/retail360/data/CustomerRepository.kt)** & **[VisitRepository.kt](file:///C:/Users/Tom/AndroidStudioProjects/Retail360/app/src/main/java/com/example/retail360/data/VisitRepository.kt)**: Implement basic logic.

### 2. Utilities
- **[NEW] [Extensions.kt](file:///C:/Users/Tom/AndroidStudioProjects/Retail360/app/src/main/java/com/example/retail360/util/Extensions.kt)**: `collectAsStateSafe` and `ksh` string formatting.
- **[MODIFY] [SyncWorker.kt](file:///C:/Users/Tom/AndroidStudioProjects/Retail360/app/src/main/java/com/example/retail360/data/SyncWorker.kt)**: Fix imports and logic.

### 3. Navigation & Main Entry
- **[MODIFY] [MainActivity.kt](file:///C:/Users/Tom/AndroidStudioProjects/Retail360/app/src/main/java/com/example/retail360/MainActivity.kt)**: Initialize `Graph` and setup navigation.
- **[MODIFY] [NavGraph.kt](file:///C:/Users/Tom/AndroidStudioProjects/Retail360/app/src/main/java/com/example/retail360/navigation/NavGraph.kt)**: Fix all screen imports.

### 4. UI Screens (Global Fixes & Previews)
- Go through all screens in `ui/theme/screens/`:
    - Rename "SalesAutomation" -> "Retail360".
    - Fix all `com.example.salesautomation` imports.
    - Add `@Preview` for each screen.
    - Screens include: `CustomerCreation`, `ActiveVisit`, `Availability`, `CheckIn`, `CheckOut`, `CustomerDetails`, `CustomerList`, `Dashboard`, `ProductScan`, `Sales`, `SyncStatus`, `VisitSummary`.

## Verification Plan

### Automated Tests
- Run `gradle build` to ensure no unresolved references.

### Manual Verification
- Review generated `@Preview` images for all screens to ensure they look "deployment-ready".
- Verify the splash screen branding change to "Retail360".
