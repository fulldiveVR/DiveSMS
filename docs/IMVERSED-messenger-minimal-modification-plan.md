# IMVERSED Messenger — Minimal Modification Plan

## Objective

Modify the existing `fulldiveVR/DiveSMS` Android application rather than building a new app. Keep its existing SMS/MMS functionality and make only the changes necessary to create a genuine IMVERSED-branded downloadable communications application for Class 09 trademark evidence.

## Source project

- Repository: https://github.com/fulldiveVR/DiveSMS
- Upstream reference: https://github.com/moezbhatti/qksms
- License: GNU GPLv3 — https://raw.githubusercontent.com/fulldiveVR/DiveSMS/master/LICENSE

## Required changes

### 1. Rebrand the existing app

- Rename the application to **IMVERSED Messenger**.
- Replace the launcher icon.
- Add IMVERSED to the launch/splash screen.
- Update the app title, About screen, settings, notifications, and visible metadata.
- Preserve required copyright, GPLv3, and third-party notices.

### 2. Keep the existing functionality

Do not build new product functionality. Retain the current application capabilities, including where already supported:

- SMS messaging
- MMS messaging
- Conversation list
- Message composition
- Sending and receiving messages
- Notifications
- Existing Android UI and architecture

Do not add blockchain, gaming, AI, social-networking, or other unrelated functionality for this project.

### 3. Add minimal product information

The About screen should show:

- IMVERSED Messenger
- Version/build number
- Short product description
- Copyright information
- GPLv3/open-source notice
- Support and privacy links, if available

Suggested description:

> IMVERSED Messenger is downloadable Android communications software for composing, sending, receiving, and organizing SMS and MMS messages.

Use this wording only after product/legal approval.

### 4. Build and distribute the existing app

- Fork the repository into the approved account or organization.
- Build a signed Android release.
- Verify that the application installs and launches.
- Publish a real consumer-accessible download page or approved app-store listing.
- Ensure the download page shows the IMVERSED mark, product name, description, platform, version, and download/install action.

A mockup, renamed repository, or screenshot of an unreleased build is not sufficient evidence.

## Evidence screenshots

Capture real screenshots from the released or release-candidate app:

1. **Launch screen** — IMVERSED Messenger visible.
2. **Messaging screen** — existing conversation UI with test data.
3. **Compose screen** — existing message composition flow with test data.
4. **About screen** — IMVERSED name, version, product description, and legal links.
5. **Download page** — IMVERSED mark next to the software name, description, and download action.
6. **Release/install page** — Google Play listing or public APK download page.

For each screenshot, record the URL, date, app version/build, device, and Android version. Never include real private messages, phone numbers, or contacts.

## PM execution checklist

- [ ] Fork `fulldiveVR/DiveSMS`.
- [ ] Build the unmodified baseline first.
- [ ] Apply IMVERSED branding only.
- [ ] Preserve GPLv3 and third-party notices.
- [ ] Verify SMS/MMS, permissions, conversations, and notifications.
- [ ] Add/update the About and legal screens.
- [ ] Create a signed release build.
- [ ] Publish a real download/install path.
- [ ] Capture the six evidence screenshots.
- [ ] Record URLs, dates, versions, and device details.
- [ ] Have trademark counsel review the final product description and specimens.

## Legal requirements

Before distribution or trademark submission, confirm with counsel:

- GPLv3 obligations for modified and distributed software.
- Third-party dependency and asset licenses.
- The exact Class 09 goods wording supported by the app.
- Whether an APK download page or Google Play listing is preferred.
- Which screenshots will be submitted as specimens.
- The appropriate first-use-in-commerce date.

This document is a product implementation brief, not legal advice.

## Design and technical references

Use these as references, not as material to copy:

- Material 3: https://m3.material.io/
- Android app architecture: https://developer.android.com/topic/architecture
- Android permissions: https://developer.android.com/training/permissions/requesting
- Android SMS documentation: https://developer.android.com/guide/topics/telephony/sms
- Android adaptive icons: https://developer.android.com/develop/ui/views/launch/icon_design_adaptive
- Google Play listing guidance: https://developer.android.com/distribute/marketing-tools/store-listing
- USPTO specimen examples: https://www.uspto.gov/trademarks/laws/specimen-examples
- USPTO TMEP: https://tmep.uspto.gov/

## Bottom line

Reuse the existing DiveSMS application. Keep its working messaging features. Change the brand to IMVERSED Messenger, publish a real installable version, and document the actual software use with in-app and download-page screenshots. Do not expand the scope beyond the minimum required changes.
