# AI Implementation Guidelines

## Project Overview
This repository contains the **Pyllar** project, which is being migrated to **Kotlin Multiplatform (KMP)**.

## Core Directories
- `android/`: This is the **existing Android project**. It serves as the primary reference for all business logic, UI flows, and feature implementations.
- `pyllarmulti/`: This is the **KMP project** (Compose Multiplatform). This is where the cross-platform logic and the iOS-specific integrations are being built.

## Instructions for AI Assistants
When assisting with this project, please follow these rules:

1. **Reference Android for Truth**: Before implementing any feature or fixing any bug in the KMP project (`pyllarmulti/`), always refer to the corresponding implementation in the `android/` directory.
2. **Feature Parity**: The goal is to achieve 1:1 feature parity with the Android version. Ensure that the KMP version mirrors the behavior, state management, and API handling found in the Android code.
3. **KMP Best Practices**: While the Android project is the reference for logic, the implementation in `pyllarmulti/` should follow modern Kotlin Multiplatform and Compose Multiplatform best practices (e.g., using `commonMain` for shared logic, `iosMain` for iOS-specific APIs).
4. **Consistency**: Use the Android implementation as a blueprint for naming conventions, API request structures, and ViewModel state transitions.

## Contextual Notes
- If you are asked to build a feature for iOS, your first step should be to find the equivalent feature in the `android/` folder and analyze how it works.
- Maintain alignment between the two projects unless explicitly instructed otherwise.
