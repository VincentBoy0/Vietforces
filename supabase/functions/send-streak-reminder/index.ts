/**
 * Supabase Edge Function — send-streak-reminder
 *
 * Scheduled via pg_cron at 19:00 UTC every day.
 * Queries users who:
 *   1. Have an FCM token registered in public.fcm_tokens
 *   2. Have NOT practiced today (last_practice_date < today or NULL)
 *   3. Are not banned
 * Then sends an FCM HTTP v1 push notification to each eligible user
 * encouraging them to maintain their streak.
 *
 * Required Supabase secrets (set via: supabase secrets set KEY=value):
 *   SUPABASE_URL                  — auto-injected by Supabase
 *   SUPABASE_SERVICE_ROLE_KEY     — auto-injected by Supabase
 *   CRON_SECRET                   — shared secret for pg_cron invocation auth
 *   FIREBASE_PROJECT_ID           — Firebase project ID (from Firebase Console)
 *   FIREBASE_SERVICE_ACCOUNT_JSON — Full JSON string of Firebase service account
 *                                   key with firebase-messaging permission
 *                                   (supabase secrets set FIREBASE_SERVICE_ACCOUNT_JSON="$(cat serviceAccount.json)")
 *
 * pg_cron schedule (run once in Supabase SQL Editor):
 *   SELECT cron.schedule(
 *     'send-streak-reminder',
 *     '0 19 * * *',
 *     $$ SELECT net.http_post(
 *          url     := 'https://<project-ref>.supabase.co/functions/v1/send-streak-reminder',
 *          headers := '{"Authorization":"Bearer <CRON_SECRET>"}',
 *          body    := '{}'
 *        ) $$
 *   );
 *
 * Threat mitigations:
 *   T-04-02 — CRON_SECRET verified on every request; 401 if absent or wrong.
 *   T-04-05 — FIREBASE_SERVICE_ACCOUNT_JSON stored as Supabase secret only;
 *             never committed to source control.
 *   T-04-SC — Only Deno std + @supabase/supabase-js from esm.sh; no npm installs.
 */

import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "authorization, content-type, x-client-info, apikey",
}

// ---------------------------------------------------------------------------
// PEM → DER conversion helper for Web Crypto import
// ---------------------------------------------------------------------------
function pemToDer(pem: string): ArrayBuffer {
  const base64 = pem
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/-----BEGIN RSA PRIVATE KEY-----/, "")
    .replace(/-----END RSA PRIVATE KEY-----/, "")
    .replace(/\n/g, "")
    .trim()
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes.buffer
}

// ---------------------------------------------------------------------------
// Build a signed JWT and exchange it for a Firebase OAuth2 access token
// ---------------------------------------------------------------------------
async function getFirebaseAccessToken(serviceAccountJson: string): Promise<string> {
  const serviceAccount = JSON.parse(serviceAccountJson)
  const { client_email: clientEmail, private_key: privateKeyPem } = serviceAccount

  const now = Math.floor(Date.now() / 1000)
  const claims = {
    iss: clientEmail,
    sub: clientEmail,
    aud: "https://oauth2.googleapis.com/token",
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    iat: now,
    exp: now + 3600,
  }

  // Encode JWT header + payload
  const header = btoa(JSON.stringify({ alg: "RS256", typ: "JWT" }))
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
  const payload = btoa(JSON.stringify(claims))
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")

  const signingInput = `${header}.${payload}`

  // Import the RSA private key using Web Crypto API
  const privateKeyDer = pemToDer(privateKeyPem)
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    privateKeyDer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  )

  // Sign the JWT
  const encoder = new TextEncoder()
  const signatureBuffer = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    encoder.encode(signingInput)
  )
  const signature = btoa(String.fromCharCode(...new Uint8Array(signatureBuffer)))
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")

  const jwt = `${signingInput}.${signature}`

  // Exchange JWT for access token
  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  })

  if (!tokenResponse.ok) {
    const errorBody = await tokenResponse.text()
    throw new Error(`Firebase token exchange failed (${tokenResponse.status}): ${errorBody}`)
  }

  const tokenData = await tokenResponse.json()
  return tokenData.access_token as string
}

// ---------------------------------------------------------------------------
// Send a single FCM HTTP v1 notification
// ---------------------------------------------------------------------------
async function sendFcmNotification(
  fcmToken: string,
  projectId: string,
  accessToken: string
): Promise<{ success: boolean; error?: string }> {
  const url = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`

  const message = {
    message: {
      token: fcmToken,
      notification: {
        title: "🔥 Streak sắp mất!",
        body: "Bạn chưa luyện tập hôm nay! Đừng để mất streak nhé.",
      },
      data: {
        screen: "daily_challenge",
      },
    },
  }

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(message),
  })

  if (!response.ok) {
    const errorBody = await response.text()
    return { success: false, error: `FCM error ${response.status}: ${errorBody}` }
  }

  return { success: true }
}

// ---------------------------------------------------------------------------
// Main handler
// ---------------------------------------------------------------------------
Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: corsHeaders })
  }

  // -------------------------------------------------------------------------
  // T-04-02: Verify CRON_SECRET — reject unsolicited requests
  // -------------------------------------------------------------------------
  const cronSecret = Deno.env.get("CRON_SECRET")
  if (!cronSecret) {
    console.error("send-streak-reminder: CRON_SECRET not configured")
    return new Response(
      JSON.stringify({ error: "Server misconfiguration: CRON_SECRET not set" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }

  const authHeader = req.headers.get("Authorization") ?? ""
  if (authHeader !== `Bearer ${cronSecret}`) {
    return new Response(
      JSON.stringify({ error: "Unauthorized" }),
      { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL")!
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    const firebaseProjectId = Deno.env.get("FIREBASE_PROJECT_ID")
    const firebaseServiceAccountJson = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON")

    if (!firebaseProjectId || !firebaseServiceAccountJson) {
      console.error("send-streak-reminder: FIREBASE_PROJECT_ID or FIREBASE_SERVICE_ACCOUNT_JSON not configured")
      return new Response(
        JSON.stringify({ error: "Firebase credentials not configured" }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    const supabase = createClient(supabaseUrl, serviceRoleKey)

    // Today's UTC date (YYYY-MM-DD)
    const today = new Date().toISOString().split("T")[0]

    // -----------------------------------------------------------------------
    // Query users who haven't practiced today and have an FCM token
    // Joins: users → fcm_tokens (for token) → user_progress (for last_practice_date)
    // -----------------------------------------------------------------------
    const { data: usersToNotify, error: queryError } = await supabase
      .from("users")
      .select(`
        id,
        fcm_tokens!inner ( token ),
        user_progress ( last_practice_date )
      `)
      .eq("is_banned", false)
      // WA-05: Respect per-user streak notification opt-out preference.
      // Column added by migration 010_notif_preferences.sql.
      .eq("notif_streak_enabled", true)

    if (queryError) {
      console.error("send-streak-reminder: users query error", queryError)
      return new Response(
        JSON.stringify({ error: queryError.message }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // Filter: only users who have not practiced today
    type UserRow = {
      id: string
      fcm_tokens: { token: string } | { token: string }[]
      user_progress: { last_practice_date: string | null } | { last_practice_date: string | null }[] | null
    }

    const eligibleUsers = (usersToNotify as UserRow[]).filter((user) => {
      const progress = Array.isArray(user.user_progress)
        ? user.user_progress[0]
        : user.user_progress
      const lastPractice = progress?.last_practice_date ?? null
      return lastPractice === null || lastPractice < today
    })

    if (eligibleUsers.length === 0) {
      console.log("send-streak-reminder: no eligible users to notify")
      return new Response(
        JSON.stringify({ success: true, notified: 0, failed: 0 }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // -----------------------------------------------------------------------
    // Obtain Firebase access token (T-04-05: service account key stays server-side)
    // -----------------------------------------------------------------------
    let accessToken: string
    try {
      accessToken = await getFirebaseAccessToken(firebaseServiceAccountJson)
    } catch (tokenError) {
      console.error("send-streak-reminder: failed to obtain Firebase access token", tokenError)
      return new Response(
        JSON.stringify({ error: `Firebase auth failed: ${(tokenError as Error).message}` }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // -----------------------------------------------------------------------
    // Send FCM notification to each eligible user
    // -----------------------------------------------------------------------
    let notified = 0
    let failed = 0

    for (const user of eligibleUsers) {
      const fcmData = Array.isArray(user.fcm_tokens)
        ? user.fcm_tokens[0]
        : user.fcm_tokens
      const token = fcmData?.token

      if (!token) {
        failed++
        continue
      }

      const result = await sendFcmNotification(token, firebaseProjectId, accessToken)
      if (result.success) {
        notified++
        console.log(`send-streak-reminder: notified user ${user.id}`)
      } else {
        failed++
        console.warn(`send-streak-reminder: failed for user ${user.id}: ${result.error}`)
      }
    }

    console.log(`send-streak-reminder: done — notified=${notified}, failed=${failed}`)

    return new Response(
      JSON.stringify({ success: true, notified, failed }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  } catch (error) {
    console.error("send-streak-reminder: unhandled error", error)
    return new Response(
      JSON.stringify({ error: (error as Error).message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }
})
