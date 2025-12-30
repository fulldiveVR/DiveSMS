import * as functions from "firebase-functions";
import { defineSecret } from "firebase-functions/params";
import * as admin from "firebase-admin";
import { GoogleAuth } from "google-auth-library";

admin.initializeApp();

// Telegram Bot Token - stored as Firebase Secret
// Set with: firebase functions:secrets:set TELEGRAM_BOT_TOKEN
const telegramBotToken = defineSecret("TELEGRAM_BOT_TOKEN");

// Project configuration
const PACKAGE_NAME = "com.fulldive.extension.divesms";

// Debug mode - set to true to skip Play Integrity verification
const DEBUG_SKIP_INTEGRITY = process.env.DEBUG_SKIP_INTEGRITY === "true";

interface SendMessageRequest {
  integrityToken?: string;
  chatId: string;
  senderName: string;
  senderNumber: string;
  messageBody: string;
  timestamp: number;
  isDebug?: boolean;
}

interface TelegramResponse {
  ok: boolean;
  description?: string;
}

/**
 * Verify Play Integrity token
 */
async function verifyPlayIntegrity(
  integrityToken: string
): Promise<{ valid: boolean; error?: string }> {
  try {
    const auth = new GoogleAuth({
      scopes: ["https://www.googleapis.com/auth/playintegrity"],
    });
    const client = await auth.getClient();
    const accessToken = await client.getAccessToken();

    const response = await fetch(
      `https://playintegrity.googleapis.com/v1/${PACKAGE_NAME}:decodeIntegrityToken`,
      {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${accessToken.token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ integrity_token: integrityToken }),
      }
    );

    if (!response.ok) {
      const errorText = await response.text();
      console.error("Play Integrity API error:", errorText);
      return { valid: false, error: "Integrity verification failed" };
    }

    const result = await response.json();
    const payload = result.tokenPayloadExternal;

    // Check app integrity
    const appRecognitionVerdict = payload?.appIntegrity?.appRecognitionVerdict;
    if (appRecognitionVerdict !== "PLAY_RECOGNIZED") {
      console.warn("App not recognized by Play:", appRecognitionVerdict);
      return { valid: false, error: "App not installed from Play Store" };
    }

    // Check device integrity
    const deviceRecognitionVerdict = payload?.deviceIntegrity?.deviceRecognitionVerdict || [];
    if (!deviceRecognitionVerdict.includes("MEETS_DEVICE_INTEGRITY")) {
      console.warn("Device integrity check failed:", deviceRecognitionVerdict);
      return { valid: false, error: "Device integrity check failed" };
    }

    // Check package name
    if (payload?.appIntegrity?.packageName !== PACKAGE_NAME) {
      console.warn("Package name mismatch:", payload?.appIntegrity?.packageName);
      return { valid: false, error: "Invalid app package" };
    }

    return { valid: true };
  } catch (error) {
    console.error("Play Integrity verification error:", error);
    return { valid: false, error: "Verification service error" };
  }
}

/**
 * Format SMS message for Telegram
 */
function formatTelegramMessage(
  senderName: string,
  senderNumber: string,
  messageBody: string,
  timestamp: number
): string {
  const date = new Date(timestamp);
  const timeStr = date.toLocaleTimeString("en-US", {
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  });
  const dateStr = date.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });

  let message = `*${senderNumber}*\n`;
  if (senderName && senderName !== senderNumber) {
    message += `_${senderName}_\n`;
  }
  message += `\n${messageBody}\n\n`;
  message += `_${timeStr} • ${dateStr}_`;

  return message;
}

/**
 * Send message to Telegram
 */
async function sendTelegramMessage(
  chatId: string,
  text: string,
  botToken: string
): Promise<{ success: boolean; error?: string }> {
  try {
    const telegramApiUrl = `https://api.telegram.org/bot${botToken}`;
    const response = await fetch(`${telegramApiUrl}/sendMessage`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        chat_id: chatId,
        text: text,
        parse_mode: "Markdown",
      }),
    });

    const result: TelegramResponse = await response.json();

    if (!result.ok) {
      console.error("Telegram API error:", result.description);
      return { success: false, error: result.description || "Telegram error" };
    }

    return { success: true };
  } catch (error) {
    console.error("Telegram send error:", error);
    return { success: false, error: "Failed to send message" };
  }
}

/**
 * Main Cloud Function: Forward SMS to Telegram
 */
export const forwardSmsToTelegram = functions
  .runWith({ secrets: [telegramBotToken] })
  .https.onRequest(async (req, res) => {
    const botToken = telegramBotToken.value();
    // CORS headers
    res.set("Access-Control-Allow-Origin", "*");
    res.set("Access-Control-Allow-Methods", "POST");
    res.set("Access-Control-Allow-Headers", "Content-Type");

    if (req.method === "OPTIONS") {
      res.status(204).send("");
      return;
    }

    if (req.method !== "POST") {
      res.status(405).json({ error: "Method not allowed" });
      return;
    }

    const body: SendMessageRequest = req.body;

    // Validate required fields
    if (!body.chatId || !body.messageBody) {
      res.status(400).json({ error: "Missing required fields" });
      return;
    }

    // Check if bot token is configured
    if (!botToken) {
      console.error("Telegram bot token not configured");
      res.status(500).json({ error: "Server configuration error" });
      return;
    }

    // Verify Play Integrity (skip in debug mode)
    const skipIntegrity = DEBUG_SKIP_INTEGRITY || body.isDebug === true;

    if (!skipIntegrity) {
      if (!body.integrityToken) {
        res.status(400).json({ error: "Missing integrity token" });
        return;
      }

      const integrityResult = await verifyPlayIntegrity(body.integrityToken);
      if (!integrityResult.valid) {
        res.status(403).json({
          error: "Integrity verification failed",
          details: integrityResult.error,
        });
        return;
      }
    } else {
      console.warn("Skipping Play Integrity verification (debug mode)");
    }

    // Format and send message
    const formattedMessage = formatTelegramMessage(
      body.senderName || "",
      body.senderNumber || "Unknown",
      body.messageBody,
      body.timestamp || Date.now()
    );

    const telegramResult = await sendTelegramMessage(body.chatId, formattedMessage, botToken);

    if (!telegramResult.success) {
      res.status(500).json({
        error: "Failed to send Telegram message",
        details: telegramResult.error,
      });
      return;
    }

    res.status(200).json({ success: true });
  }
);

/**
 * Telegram Bot Webhook - handles /start command and returns chat ID
 */
export const telegramWebhook = functions
  .runWith({ secrets: [telegramBotToken] })
  .https.onRequest(async (req, res) => {
    const botToken = telegramBotToken.value();

    if (req.method !== "POST") {
      res.status(200).send("OK");
      return;
    }

    const update = req.body;
    const message = update?.message;

    if (!message) {
      res.status(200).send("OK");
      return;
    }

    const chatId = message.chat?.id;
    const text = message.text || "";

    // Handle /start command
    if (text.startsWith("/start")) {
      const responseText =
        `Welcome to WIZE SMS Forwarder!\n\n` +
        `Your Chat ID is:\n\n` +
        `\`${chatId}\`\n\n` +
        `Copy this number and paste it in the WIZE SMS app to receive your SMS messages here.`;

      await fetch(`https://api.telegram.org/bot${botToken}/sendMessage`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          chat_id: chatId,
          text: responseText,
          parse_mode: "Markdown",
        }),
      });
    }

    res.status(200).send("OK");
  });

/**
 * Test function to verify Telegram bot is working
 */
export const testTelegram = functions
  .runWith({ secrets: [telegramBotToken] })
  .https.onRequest(async (req, res) => {
    const botToken = telegramBotToken.value();
    res.set("Access-Control-Allow-Origin", "*");

    const chatId = req.query.chatId as string;
    if (!chatId) {
      res.status(400).json({ error: "Missing chatId parameter" });
      return;
    }

    if (!botToken) {
      res.status(500).json({ error: "Bot token not configured" });
      return;
    }

    const result = await sendTelegramMessage(
      chatId,
      "WIZE SMS test message!\n\nIf you see this, Telegram forwarding is working correctly.",
      botToken
    );

    if (result.success) {
      res.status(200).json({ success: true, message: "Test message sent!" });
    } else {
      res.status(500).json({ error: result.error });
    }
  });
