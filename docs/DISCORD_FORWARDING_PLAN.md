# Plan: Add Discord SMS Forwarding

## Summary

Add Discord as a third SMS forwarding channel using a bot with DM delivery. Users join your Discord server, run `/start`, get their User ID, and receive SMS notifications via direct message. Mirrors the Telegram UX exactly.

## Architecture

```
Telegram (current):
Android App → Cloud Function → Telegram Bot API → User Chat

Discord (proposed):
Android App → Cloud Function → Discord Bot API → User DM
```

**User Flow** (identical to Telegram):
1. Tap "Setup Discord" in app → Opens Discord server invite
2. Join "Wize SMS" server
3. Type `/start` in any channel
4. Bot DMs them their User ID
5. Paste User ID in app → Done

## Cost: $0/month

| Item | Cost |
|------|------|
| Discord Bot | Free |
| Firebase Functions | Free (well within Spark limits) |
| Discord API | Free |

---

## Part 1: Discord Bot Setup (Manual, One-time)

### Step 1: Create Discord Application
1. Go to https://discord.com/developers/applications
2. Click **"New Application"** → Name: `Wize SMS`
3. Go to **Bot** tab → Click **"Add Bot"**
4. Enable **"Message Content Intent"** (required for slash commands)
5. Click **"Reset Token"** → Copy and save securely

### Step 2: Configure Bot Permissions
1. Go to **OAuth2 → URL Generator**
2. Select scopes: `bot`, `applications.commands`
3. Select permissions: `Send Messages`, `Use Slash Commands`
4. Copy the generated URL

### Step 3: Add Bot to Your Server
1. Open the URL in browser
2. Select your server → Authorize

### Step 4: Store Token in Firebase
```bash
firebase functions:secrets:set DISCORD_BOT_TOKEN
# Paste the bot token when prompted
```

### Step 5: Create Server Invite Link
1. Server Settings → Invites → Create Invite
2. Set to never expire
3. Save link for app (e.g., `https://discord.gg/yourcode`)

---

## Part 2: Firebase Cloud Functions

### File: `functions/src/index.ts`

Add 3 new functions (same pattern as Telegram):

#### 1. `forwardSmsToDiscord` - Main forwarding function
```typescript
const discordBotToken = defineSecret("DISCORD_BOT_TOKEN");

export const forwardSmsToDiscord = functions
  .runWith({ secrets: [discordBotToken] })
  .https.onRequest(async (req, res) => {
    // 1. Validate request (userId, messageBody required)
    // 2. Verify Play Integrity (reuse existing verifyPlayIntegrity())
    // 3. Create DM channel: POST /users/@me/channels {recipient_id}
    // 4. Send embed message: POST /channels/{id}/messages
  });
```

#### 2. `discordInteraction` - Handle /start slash command
```typescript
export const discordInteraction = functions
  .runWith({ secrets: [discordBotToken] })
  .https.onRequest(async (req, res) => {
    // Handle Discord Interaction webhook
    // When user runs /start:
    //   1. Get user ID from interaction
    //   2. DM user their ID
    //   3. Respond with ephemeral message
  });
```

#### 3. `testDiscord` - Test message function
```typescript
export const testDiscord = functions
  .runWith({ secrets: [discordBotToken] })
  .https.onRequest(async (req, res) => {
    // Send test DM to verify setup
  });
```

### Discord API Helpers (add to index.ts)
```typescript
async function createDMChannel(userId: string, botToken: string): Promise<string> {
  const res = await fetch("https://discord.com/api/v10/users/@me/channels", {
    method: "POST",
    headers: {
      "Authorization": `Bot ${botToken}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ recipient_id: userId })
  });
  const data = await res.json();
  return data.id; // DM channel ID
}

async function sendDiscordDM(channelId: string, embed: object, botToken: string) {
  await fetch(`https://discord.com/api/v10/channels/${channelId}/messages`, {
    method: "POST",
    headers: {
      "Authorization": `Bot ${botToken}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ embeds: [embed] })
  });
}
```

### Discord Embed Format
```typescript
const embed = {
  title: `SMS from ${senderNumber}`,
  description: messageBody,
  color: 0x58ACFF, // Blue
  fields: [
    { name: "Contact", value: senderName, inline: true },
    { name: "SIM", value: "SIM 1", inline: true }
  ],
  timestamp: new Date(timestamp).toISOString(),
  footer: { text: "Wize SMS" }
};
```

---

## Part 3: Android App Changes

### New Files to Create

| File | Purpose |
|------|---------|
| `domain/.../discord/DiscordService.kt` | Interface |
| `data/.../discord/DiscordServiceImpl.kt` | Implementation |

### Files to Modify

| File | Changes |
|------|---------|
| `domain/.../util/Preferences.kt` | Add `discordForwardingEnabled`, `discordUserId` |
| `domain/.../model/ForwardingStatus.kt` | Add `DISCORD` to `ForwardingType` enum |
| `domain/.../interactor/ForwardSms.kt` | Add `forwardToDiscord()` method |
| `data/.../manager/ForwardingStatusManager.kt` | Handle Discord status |
| `presentation/.../forwarding/ForwardingActivity.kt` | Add Discord UI section |
| `presentation/.../forwarding/ForwardingViewModel.kt` | Handle Discord logic |
| `presentation/.../forwarding/ForwardingState.kt` | Add `discordStatus` |
| `presentation/.../forwarding/ForwardingView.kt` | Add Discord intents |
| `presentation/.../injection/AppModule.kt` | Provide DiscordService |
| `presentation/.../res/layout/forwarding_activity.xml` | Add Discord section |
| `presentation/.../res/values/strings.xml` | Add Discord strings |
| `presentation/.../res/values-*/strings.xml` | Translations (20 files) |

---

## Part 4: UI Layout

Add Discord section to `forwarding_activity.xml` (copy Telegram section structure):

```
┌─────────────────────────────────────────┐
│ Discord                      [Connected]│
├─────────────────────────────────────────┤
│ ⚠️ Warning banner (if errors)           │
├─────────────────────────────────────────┤
│ [Toggle] Enable Discord forwarding      │
│ [Input]  User ID: 123456789012345678    │
│ [Button] Send test message              │
│ [Button] Setup Discord (opens invite)   │
└─────────────────────────────────────────┘
```

---

## Part 5: New Strings (English)

```xml
<!-- Discord section -->
<string name="forwarding_discord_title">Discord</string>
<string name="forwarding_discord_enabled">Enable Discord forwarding</string>
<string name="forwarding_discord_user_id">Discord User ID</string>
<string name="forwarding_discord_user_id_hint">Your Discord User ID</string>
<string name="forwarding_discord_test">Send test message</string>
<string name="forwarding_discord_help">Setup Discord</string>
<string name="forwarding_discord_help_message">1. Join our Discord server\n2. Type /start in any channel\n3. Copy your User ID from the bot\'s reply\n4. Paste it here</string>
```

Plus translations for all 20 languages.

---

## Implementation Order

1. **Discord Bot Setup** (manual, 10 min)
   - Create application, bot, add to server
   - Store token in Firebase secrets

2. **Cloud Functions** (code)
   - Add `forwardSmsToDiscord`
   - Add `discordInteraction`
   - Add `testDiscord`
   - Deploy: `firebase deploy --only functions`

3. **Register Slash Command** (one-time API call)
   ```bash
   curl -X POST "https://discord.com/api/v10/applications/YOUR_APP_ID/commands" \
     -H "Authorization: Bot YOUR_BOT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"name":"start","description":"Get your User ID for SMS forwarding"}'
   ```

4. **Android: Domain Layer**
   - Add DiscordService interface
   - Add preferences
   - Update ForwardSms interactor

5. **Android: Data Layer**
   - Add DiscordServiceImpl

6. **Android: Presentation Layer**
   - Update ForwardingActivity UI
   - Update ViewModel
   - Add translations

7. **Test & Deploy**
   - Build APK
   - Test end-to-end flow

---

## Security

- Play Integrity verification (same as Telegram)
- Bot token stored as Firebase Secret (never in code)
- User ID validated (must be numeric, 17-19 digits)
- User must share server with bot to receive DMs
