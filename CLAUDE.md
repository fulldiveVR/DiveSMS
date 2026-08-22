# DiveSMS — agent guide

A fork of QKSMS: an Android SMS/MMS client that can also forward messages to Telegram and email.
Read this whole file before touching anything. Section 1 is where agents lose the most time.

## 1. Package root is NOT the applicationId

Every Kotlin/Java source file in every module lives under `com.moez.QKSMS.*`, including brand-new code.
The `:presentation` module's `namespace`/`applicationId` is `com.fulldive.extension.divesms`.

Consequence: in a file whose package is `com.moez.QKSMS.feature.foo`, generated classes are imported as
`com.fulldive.extension.divesms.R` and `com.fulldive.extension.divesms.databinding.*`. Do not "fix" the
package to match the applicationId, and do not create sources under `com/fulldive/...`.
`:android-smsmms` is the exception: its namespace and package are `com.klinker.android.send_message`.

User-visible app name is currently "Wize SMS".

## 2. The internal maven repo is gone (resolved)

`office.fulldive.com:8083` accepts TCP but never answers HTTP from outside the office network, so every
build stalled on it. It has been removed from the root `build.gradle` and from `presentation/build.gradle`
(no other module declared it), together with the two artifacts it was the only source of:

    com.snore.guard:divesms:1.4.0     # zero source references, dropped outright
    com.fulldive:popups:1.0.20        # com.fulldive.startapppopups.PopupManager

Consequence: **local builds have no startup popups and no snore.guard SDK.** `MainActivity` still reaches
PopupManager through `Class.forName(...)` inside a nested try/catch, so the missing class is swallowed at
runtime exactly as before. That reflection block is deliberate — leave it, so the SDK works again if the
dependency is ever restored. The unused `PopupManager` import in `injection/AppComponent.kt` was removed.

One transitive casualty: `io.realm:android-adapters:3.1.0` was jcenter-only and the internal repo had been
mirroring it. It is now pulled from jitpack as `com.github.realm:realm-android-adapters:3.1.0` — identical
upstream source, same `io.realm` package, still `transitive = false`.

Nothing in the build needs office-network access any more. `./gradlew :presentation:assembleNoAnalyticsDebug`
succeeds offline-of-the-office in about 50s.

## 3. Modules and dependency direction

    :presentation  ->  :data  ->  :domain  ->  :common  ->  :android-smsmms

Strictly one-way. Never introduce a back-edge.

- `:common` — compat shims, util/extension functions, a vendored Glide gif encoder.
- `:domain` — Realm models (`model/`), repository **interfaces** (`repository/`), interactors
  (`interactor/`, ~36), manager and mapper interfaces, `util/Preferences.kt`,
  `telegram/TelegramService.kt`, `email/`.
- `:data` — repository `*Impl`s, mappers, managers, receivers, services, filters, Realm migrations
  (`migration/`), Telegram/email senders, WorkManager forwarding.
- `:presentation` — the application module. UI under `feature/*`, base classes under `common/base/*`,
  the Dagger graph under `injection/`, `common/Navigator.kt`, `res/`.
- `:android-smsmms` — vendored Klinker MMS stack on okhttp 2.5 + `org.apache.http.legacy`.
  **Frozen third-party code. Do not refactor it.**

`functions/` is a separate Node 20 TypeScript Firebase Cloud Functions project, not a Gradle module.

## 4. Architecture: MVI with Rx state reducers

Base classes in `presentation/src/main/java/com/moez/QKSMS/common/base/`:

- `QkViewModel<View : QkView<State>, State : Any>(initialState)` — an AndroidX `ViewModel` holding a
  `BehaviorSubject<State>` plus a `PublishSubject<State.() -> State>` reducer stream, scanned on
  `AndroidSchedulers.mainThread()`. `newState { copy(...) }` pushes a reducer; `bindView(view)`
  subscribes and calls `view.render(state)`.
  **Gotcha: in this fork, `bindView` and `newState` wrap their bodies in empty `catch` blocks. Exceptions
  during a render or a reducer are swallowed silently.** If a screen mysteriously does not update, add
  logging inside the reducer before assuming the state is wrong.
- `QkViewContract<State> : LifecycleOwner { fun render(state: State) }`; `QkView` is the alias features use.
- `QkActivity` / `QkThemedActivity` — activity bases with view binding.
- `QkController` / `QkPresenter` — Conductor `LifecycleController` + presenter pair for sub-screens hosted
  inside an activity (settings, blocking, backup).
- `QkAdapter`, `QkRealmAdapter` (`RealmRecyclerViewAdapter`), `QkViewHolder`, `FlowableAdapter`.

So: activities use MVI-with-ViewModel, Conductor controllers use MVI-with-Presenter. Both share the
render/state contract.

`domain/src/main/java/com/moez/QKSMS/interactor/Interactor.kt` — `abstract class Interactor<Params> : Disposable`
with `buildObservable(params): Flowable<*>`, executed `subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())`.

Canonical feature layout, one package per screen:

    feature/<name>/
      XActivity.kt
      XActivityModule.kt
      XView.kt
      XState.kt
      XViewModel.kt
      XAdapter.kt

Conductor screens use `XController.kt` + `XPresenter.kt`, injected via a `fun inject(controller: X)`
added to `AppComponent`.

## 5. New screen checklist

1. Create the five files above in `presentation/src/main/java/com/moez/QKSMS/feature/<name>/`.
2. Add `@Provides @IntoMap @ViewModelKey(XViewModel::class)` to the feature's `@Module`.
3. Add `@ActivityScope @ContributesAndroidInjector(modules = [XActivityModule::class])` to
   `presentation/src/main/java/com/moez/QKSMS/injection/android/ActivityBuilderModule.kt`.
4. Add an `<activity>` entry to `presentation/src/main/AndroidManifest.xml`.
5. Add a `show*()` method to `presentation/src/main/java/com/moez/QKSMS/common/Navigator.kt`
   (it already has 21 of them).

New receivers and services get entries in `BroadcastReceiverBuilderModule.kt` / `ServiceBuilderModule.kt`
in the same `injection/android/` package.

## 6. Conventions

**Layering.** Interface in `:domain`, `Impl` suffix in `:data`, bound in
`presentation/src/main/java/com/moez/QKSMS/injection/AppModule.kt` as
`fun provideFoo(impl: FooImpl): Foo = impl`. Repositories return Rx types over Realm.

**Threading.** Interactors force `Schedulers.io()` then main. `QkViewModel` reducers and `render` are
main-thread only — pushing a Realm object from a background thread crashes by design. Activity
subscriptions use `autoDispose(scope())`; ViewModels use a `CompositeDisposable`.

**Resources.** Layouts are snake_case matching the class: `compose_activity.xml`, `about_controller.xml`,
`*_list_item.xml`, `*_view.xml`, `*_dialog.xml` (76 layouts). 21 locale dirs plus `values-night` and
`color-night`.

**Kotlin style.** 4-space indent, no semicolons, expression-bodied `@Provides`. Every new source file gets
the GPLv3 header block copied from a neighbouring file (596 files carry "This file is part of QKSMS").
`jvmTarget` is 1.8 — do not use stdlib APIs newer than Java 8.

**Navigation.** There is no Jetpack Navigation. `Navigator.kt` issues explicit `Intent`s; Conductor 3.2.0
handles in-activity stacks.

**UI.** Views + XML only. There is no Jetpack Compose anywhere in this repo. ViewBinding is enabled.

## 7. Toolchain and libraries

Gradle 8.13 (wrapper, `-all` dist), AGP 8.13.0, Kotlin 2.2.0, JDK 17 works. compileSdk/targetSdk 36,
minSdk 23, ndkVersion 24.0.8215888, sourceCompatibility/jvmTarget 1.8. No version catalog — versions are
`ext.*` in the root `build.gradle`. `gradle.properties` sets `org.gradle.caching=true` and `-Xmx8g`.

Dagger 2.16 (deliberately pinned old) + `dagger-android-support`, via kapt.
RxJava 2.1.4 + RxAndroid + RxKotlin + RxBinding 2.0.0 + AutoDispose 1.4.0 + RxDogTag. Coroutines 1.8.1 is
also present (workers, email, referral) — the codebase is mixed, but Rx dominates; prefer Rx in existing
Rx code paths.
Realm 10.19.0 — **the `realm-android` plugin must be applied before `kotlin-android` or the build fails.**
`android-adapters` 3.1.0, `allowWritesOnUiThread(true)`, `compactOnLaunch()`.
Glide 4.16.0 (kapt). OkHttp 4.12.0 in `:presentation`, okhttp 2.5.0 inside `:android-smsmms`.
Moshi 1.15.2, libphonenumber-android, ez-vcard, Material 1.13.0, ConstraintLayout 2.2.1, ExoPlayer 2.19.1,
Flexbox 3.0.0, Timber 5.0.1, WorkManager 2.9.0, play-services-auth 21.0.0, `com.sun.mail:android-mail` 1.6.7.
Amplitude 2.16.0 exists only in the `withAnalytics` flavor (`data/src/withAnalytics/`).

## 8. Flavors — build commands MUST name the variant

Dimension `analytics` -> `withAnalytics` / `noAnalytics`, declared in BOTH `:presentation` and `:data`, and
cross-wired via explicit `noAnalyticsDebug` / `withAnalyticsRelease` configurations. Four variants.

- `:domain` and `:common` have NO flavors: `testDebugUnitTest`, `lintDebug`.
- `:presentation` and `:data` DO: `testNoAnalyticsDebugUnitTest`, `lintNoAnalyticsDebug`,
  `assembleNoAnalyticsDebug`.

`noAnalytics` is the standard local variant. `withAnalytics` additionally needs `presentation/google-services.json`
(gitignored, present on disk) and the `AMPLITUDE_API_KEY` env var, and is NOT the local default.

An unqualified `test` or `assemble` will either fail or build all four variants. Always qualify.

## 9. Build and verify

    scripts/verify.sh
    scripts/verify.sh --skip-functions

One lane, no probing: `:domain:testDebugUnitTest`, `:presentation:testNoAnalyticsDebugUnitTest`,
`:presentation:assembleNoAnalyticsDebug`, then the `functions/` TypeScript build.
Silent on success, `ERROR:`-prefixed lines on failure, exit 0/non-zero.

Direct commands:

    ./gradlew :presentation:assembleNoAnalyticsDebug              # the standard local build
    ./gradlew :domain:testDebugUnitTest
    ./gradlew :presentation:testNoAnalyticsDebugUnitTest
    npm --prefix functions ci && npm --prefix functions run build # tsc strict is the real gate

Rough timings on an M-series Mac: `assembleNoAnalyticsDebug` ~50s cold, `:domain:testDebugUnitTest` ~1m.
The APK lands at `presentation/build/outputs/apk/noAnalytics/debug/DiveSMS-v<version>-debug.apk` (~51 MB).

## 10. What verification actually exists (very little)

Six test files against ~36 interactors, 8 repositories, 16 feature packages.

Unit tests:
- `domain/src/test/java/com/moez/QKSMS/interactor/TemplateEngineTest.kt`
- `domain/src/test/java/com/moez/QKSMS/interactor/FilterEngineTest.kt`  (together 20 tests, green)
- `presentation/src/test/java/com/moez/QKSMS/feature/main/MainViewModelLifecycleTest.kt`
  **Currently RED (pre-existing).** Both tests stub `realmResults.asObservable()`, but `asObservable` is a
  Kotlin extension function, which Mockito cannot stub; the real extension runs and NPEs on the mock's null
  `asFlowable()`. This is the one thing keeping `scripts/verify.sh` non-zero.

Instrumentation tests (need a connected device or emulator):
- `presentation/src/androidTest/java/com/moez/QKSMS/feature/main/MainViewModelTest.kt`
- `data/src/androidTest/java/com/moez/QKSMS/repository/MessageRepositoryTest.kt`
- `data/src/androidTest/java/com/moez/QKSMS/util/PhoneNumberUtilsTest.kt`

Frameworks: JUnit 4.13.2, Mockito 5.14.2 (+ mockito-kotlin 4.1.0 in `:domain`, mockito-android),
Espresso 3.6.1, AndroidX test runner 1.6.2, kotlinx-coroutines-test.
No Robolectric, no Turbine, no MockWebServer.

Not gates, do not rely on them:
- Lint: `lint { abortOnError false }` in `:presentation`, `:data`, `:android-smsmms`. No `lint.xml`, no baseline.
- No ktlint, detekt, spotless, or `.editorconfig` anywhere. No coverage config.
- There is no CI. The dead upstream configs (`.circleci/`, `.travis.yml`, `secrets.tar.enc`) have been
  removed. `.github/` contains only `ISSUE_TEMPLATE.md` and no workflows.
- `_scripts/` is NOT CI. It is actively maintained i18n tooling (Python + shell, `_scripts/i18n/`).

Implication: when you change behaviour, write a failing test first where a test is possible
(`:domain` interactors are the easy target), because nothing else will catch a regression.

## 11. Danger zones

**Default SMS app.** The manifest declares `SMS_DELIVER` and `WAP_PUSH_DELIVER` receivers, a
`RESPOND_VIA_MESSAGE` service, and `sms`/`smsto`/`mms`/`mmsto` intent filters on `ComposeActivity`.
Changing any receiver or intent filter can silently disqualify the app from being the default SMS app,
with no build or lint error. Message delivery cannot be tested at all without granting the default-SMS role.

**Realm migrations.** `data/src/main/java/com/moez/QKSMS/migration/QkRealmMigration.kt`, current
`SchemaVersion = 12`, is a hand-written `if (version == N)` chain ending in
`check(version >= newVersion) { "Migration missing..." }`. ANY change to a model in `domain/.../model/`
requires bumping `SchemaVersion` and appending a migration block, or the app crashes on launch for every
existing user. A second, non-Realm migration path also exists: `QkMigration.performMigration()`.

**`:android-smsmms`** is vendored third-party code. Frozen.

**`functions/`** deploys to Firebase project `ai-asia-382012`. The Telegram bot token is a Firebase secret.
`DEBUG_SKIP_INTEGRITY=true` disables Play Integrity checks server-side — never ship that on.

**`io.fabric`** was applied inside the `withAnalytics` block and is a retired plugin id that no longer
resolves; it made every `*WithAnalytics*` task fail during configuration. The `apply plugin: 'io.fabric'`
line has been deleted. `apply plugin: 'com.google.gms.google-services'` and the surrounding
`if (... contains("WithAnalytics"))` block are intact. Crashlytics is NOT wired up: the root buildscript
still declares `com.google.firebase:firebase-crashlytics-gradle:2.9.9` but nothing applies it.

## 12. Release and signing (an agent normally cannot do this)

Build types `debug` / `release`. Release: `minifyEnabled true`, `shrinkResources true`,
`proguard-android.txt` + `presentation/proguard-rules.pro`, ndk `debugSymbolLevel FULL`, `multiDexEnabled true`.
`signingConfigs.release` reads `file('../keys/keys.jks')` — the `keys/` directory exists but is EMPTY and
gitignored — plus env vars `FULLDIVE_KEYSTORE_PASSWORD`, `FULLDIVE_ALIAS`, `FULLDIVE_ALIAS_PASSWORD`.
`:data` injects `buildConfigField AMPLITUDE_API_KEY` from the environment; when unset, `BuildConfig` holds
the literal string `"null"` in every build, debug included.
`presentation/google-services.json` exists on disk but is gitignored (project `ai-asia-382012`, single
package `com.fulldive.extension.divesms`). The Google Services and Crashlytics plugins apply only when the
task request contains `WithAnalytics`.

A release build needs all four env vars, a real `keys/keys.jks`, and — for `withAnalytics` — `google-services.json`.
Do not attempt one unless the user explicitly sets that up.

## 13. functions/

`divesms-functions`, `engines.node` 20. Deps: `firebase-admin ^12`, `firebase-functions ^5`,
`google-auth-library ^9`. Dev: `typescript ^5`, `@types/node ^20`.
Scripts: `build` (tsc), `build:watch`, `serve`, `shell`, `start`, `deploy`, `logs`. There is no test or lint
script — `npm run build` is the only gate, and `tsconfig.json` sets `strict: true` with `noImplicitReturns`
and `noUnusedLocals`, so `tsc` is a genuine verification step.
Source is a single file: `functions/src/index.ts`. `firebase.json` predeploys the build.
`functions/.env` is present but untracked and holds `TELEGRAM_BOT_TOKEN` and `DEBUG_SKIP_INTEGRITY`.
`node_modules/` is not installed by default; `npm --prefix functions ci` uses the committed lockfile.
