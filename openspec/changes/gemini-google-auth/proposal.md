# Proposal: Gemini Google OAuth Authentication

## Intent

Replace the developer-provided `GEMINI_API_KEY` (from `.env` via BuildConfig) with per-user Google OAuth tokens so each user consumes their own Gemini quota. Add Firebase Auth + Google Sign-In as the authentication mechanism, and gate all Gemini-powered features behind successful Google sign-in.

## Scope

### In Scope
- Add Firebase Auth (`firebase-auth`) + Google Sign-In (`play-services-auth`) dependencies
- Create Google Sign-In flow: button in AuthenticationScreen, token handling in AuthViewModel
- Switch GeminiApiService from `x-goog-api-key` header to `Authorization: Bearer <OAuth token>`
- Add OkHttp interceptor that injects the current user's OAuth access token into Gemini requests
- Gate Gemini features behind auth state (hide when not signed in)
- Add `google-services.json` configuration

### Out of Scope
- Removing local PIN-based auth (both auth methods coexist)
- Backend/proxy for Gemini calls
- Gemini quota monitoring or usage tracking UI

## Capabilities

### New Capabilities
- `google-sign-in`: Firebase Auth + Google Sign-In with token lifecycle management
- `gemini-user-auth`: Gemini API calls authenticated via user OAuth tokens instead of developer API key

### Modified Capabilities
- None at spec level — existing local auth is preserved; Gemini API key flow is replaced

## Approach

1. **Dependencies**: Add `firebase-auth` and `play-services-auth` to `libs.versions.toml` and `build.gradle.kts`. Firebase BOM 34.12.0 already present.
2. **Google Sign-In**: Wire `GoogleSignInClient` in a new `GoogleAuthModule` Hilt module. Expose `FirebaseAuth` and `GoogleSignInClient` as singletons.
3. **Token injection**: Add `GoogleAuthInterceptor` (OkHttp Interceptor) that reads the Firebase user's ID token and attaches it as `Authorization: Bearer <token>` header. Apply only to Gemini requests via a separate OkHttpClient or conditional interceptor.
4. **GeminiApiService**: Remove `@Header("x-goog-api-key")` parameter. Token arrives via interceptor.
5. **GeminiRepository**: Remove `apiKey` parameter from `generateLessonFeedback()`. Token is implicit.
6. **AuthViewModel**: Add `signInWithGoogle()` method that launches the Google Sign-In intent, exchanges credential for Firebase Auth, observes `FirebaseAuth.authStateListener`.
7. **SessionManager**: Add `isGoogleSignedIn: StateFlow<Boolean>` derived from Firebase auth state.
8. **Feature gating**: In UI composables, observe `sessionManager.isGoogleSignedIn`; hide Gemini buttons/cards when false.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `gradle/libs.versions.toml` | Modified | Add `firebase-auth`, `play-services-auth` |
| `app/build.gradle.kts` | Modified | Add new dependency declarations |
| `app/` | New file | `google-services.json` |
| `di/NetworkModule.kt` | Modified | Add `GoogleAuthInterceptor`, remove `provideGeminiApiKey()` |
| `di/` | New file | `GoogleAuthModule.kt` — Hilt module for Firebase/Google auth |
| `data/remote/GeminiApiService.kt` | Modified | Remove `x-goog-api-key` header param |
| `data/remote/GeminiRepository.kt` | Modified | Remove `apiKey` param from `generateLessonFeedback()` |
| `ui/viewmodel/AuthViewModel.kt` | Modified | Add `signInWithGoogle()`, Firebase auth state observation |
| `di/SessionManager.kt` | Modified | Add `isGoogleSignedIn` StateFlow |
| `ui/screens/AuthenticationScreen.kt` | Modified | Add "Sign in with Google" button |
| `.env.example` | Modified | Remove `GEMINI_API_KEY` or mark as deprecated |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `google-services.json` missing or misconfigured | Medium | Document required Firebase project setup; validate at build time |
| Gemini API rejects OAuth tokens (scope mismatch) | Low | Request `cloud-platform` scope during sign-in; test with real token before merge |
| Token expiry during long practice sessions | Medium | OkHttp interceptor auto-refreshes via `FirebaseUser.getIdToken(true)` |
| Google Sign-In UI flow breaks existing PIN auth UX | Low | Keep both flows independent; Google Sign-In is additive |
| Firebase Auth requires Play Services on device | Low | App already targets API 24+; all modern devices have Play Services |

## Rollback Plan

1. Revert `GeminiApiService.kt` to use `@Header("x-goog-api-key")`
2. Remove `GoogleAuthInterceptor` and `GoogleAuthModule`
3. Restore `provideGeminiApiKey()` in NetworkModule
4. Remove Google Sign-In UI from AuthenticationScreen
5. Feature gates become no-ops (Gemini always visible)

## Dependencies

- Firebase project with Authentication (Google provider) enabled
- `google-services.json` in `app/` directory
- SHA-1 fingerprint registered in Firebase Console for Google Sign-In

## Success Criteria

- [ ] User can sign in with Google account from AuthenticationScreen
- [ ] Gemini API calls succeed using signed-in user's OAuth token (not developer API key)
- [ ] When not signed in, Gemini-powered UI elements are not rendered
- [ ] Developer incurs zero Gemini API charges when users are signed in
- [ ] Existing PIN-based auth (register/login) continues to work unchanged
- [ ] `GEMINI_API_KEY` is no longer required at build time
