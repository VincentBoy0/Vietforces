/**
 * Supabase Edge Function — generate-daily-challenge
 *
 * Scheduled via pg_cron at 00:00 UTC every day.
 * Inserts one row into public.daily_challenges with 10 randomly-selected
 * vocabulary IDs and a random game_mode.  Idempotent: returns 200 "already
 * exists" if a challenge row for today is already present.
 *
 * Required Supabase secrets (set via: supabase secrets set KEY=value):
 *   SUPABASE_URL              — auto-injected by Supabase
 *   SUPABASE_SERVICE_ROLE_KEY — auto-injected by Supabase
 *   CRON_SECRET               — shared secret for pg_cron invocation auth
 *                               (supabase secrets set CRON_SECRET=<random>)
 *
 * pg_cron schedule (run once in Supabase SQL Editor):
 *   SELECT cron.schedule(
 *     'generate-daily-challenge',
 *     '0 0 * * *',
 *     $$ SELECT net.http_post(
 *          url     := 'https://<project-ref>.supabase.co/functions/v1/generate-daily-challenge',
 *          headers := '{"Authorization":"Bearer <CRON_SECRET>"}',
 *          body    := '{}'
 *        ) $$
 *   );
 *
 * Threat mitigations:
 *   T-04-01 — CRON_SECRET verified on every request; 401 if absent or wrong.
 *   T-04-06 — SELECT-before-INSERT idempotency prevents duplicate rows.
 *   T-04-SC — Only Deno std + @supabase/supabase-js from esm.sh; no npm installs.
 */

import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "authorization, content-type, x-client-info, apikey",
}

// ---------------------------------------------------------------------------
// Static vocabulary pool — mirrors VocabularyRepository.allVocabulary (154 IDs)
// ---------------------------------------------------------------------------
const VOCABULARY_POOL: string[] = [
  // Animals (20)
  "animal_001", "animal_002", "animal_003", "animal_004", "animal_005",
  "animal_006", "animal_007", "animal_008", "animal_009", "animal_010",
  "animal_011", "animal_012", "animal_013", "animal_014", "animal_015",
  "animal_016", "animal_017", "animal_018", "animal_019", "animal_020",
  // Body (2)
  "body_001", "body_002",
  // Clothing (9)
  "clothing_001", "clothing_002", "clothing_003", "clothing_004", "clothing_005",
  "clothing_006", "clothing_007", "clothing_008", "clothing_009",
  // Food (20)
  "food_001", "food_002", "food_003", "food_004", "food_005",
  "food_006", "food_007", "food_008", "food_009", "food_010",
  "food_011", "food_012", "food_013", "food_014", "food_015",
  "food_016", "food_017", "food_018", "food_019", "food_020",
  // Household (13)
  "household_001", "household_002", "household_003", "household_004", "household_005",
  "household_006", "household_007", "household_008", "household_009", "household_010",
  "household_011", "household_012", "household_013",
  // Kitchen (12)
  "kitchen_001", "kitchen_002", "kitchen_003", "kitchen_004", "kitchen_005",
  "kitchen_006", "kitchen_007", "kitchen_008", "kitchen_009", "kitchen_010",
  "kitchen_011", "kitchen_012",
  // Places (15)
  "place_001", "place_002", "place_003", "place_004", "place_005",
  "place_006", "place_007", "place_008", "place_009", "place_010",
  "place_011", "place_012", "place_013", "place_014", "place_015",
  // School (12)
  "school_001", "school_002", "school_003", "school_004", "school_005",
  "school_006", "school_007", "school_008", "school_009", "school_010",
  "school_011", "school_012",
  // Sentences (45)
  "sentence_001", "sentence_002", "sentence_003", "sentence_004", "sentence_005",
  "sentence_006", "sentence_007", "sentence_008", "sentence_009", "sentence_010",
  "sentence_011", "sentence_012", "sentence_013", "sentence_014", "sentence_015",
  "sentence_016", "sentence_017", "sentence_018", "sentence_019", "sentence_020",
  "sentence_021", "sentence_022", "sentence_023", "sentence_024", "sentence_025",
  "sentence_026", "sentence_027", "sentence_028", "sentence_029", "sentence_030",
  "sentence_031", "sentence_032", "sentence_033", "sentence_034", "sentence_035",
  "sentence_036", "sentence_037", "sentence_038", "sentence_039", "sentence_040",
  "sentence_041", "sentence_042", "sentence_043", "sentence_044", "sentence_045",
  // Vehicles (6)
  "vehicle_001", "vehicle_002", "vehicle_003", "vehicle_004", "vehicle_005",
  "vehicle_006",
]

const GAME_MODES = [
  "image_to_word",
  "word_to_image",
  "fill_blank",
  "sentence_order",
  "syllable_match",
]

/** Fisher-Yates in-place shuffle, returns the same array */
function shuffleArray<T>(arr: T[]): T[] {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr
}

Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: corsHeaders })
  }

  // -------------------------------------------------------------------------
  // T-04-01: Verify CRON_SECRET — reject any request that lacks the correct
  // Authorization header to prevent unsolicited daily_challenges inserts.
  // -------------------------------------------------------------------------
  const cronSecret = Deno.env.get("CRON_SECRET")
  if (!cronSecret) {
    console.error("generate-daily-challenge: CRON_SECRET not configured")
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
    const supabase = createClient(supabaseUrl, serviceRoleKey)

    // Today's UTC date (YYYY-MM-DD)
    const today = new Date().toISOString().split("T")[0]

    // -----------------------------------------------------------------------
    // T-04-06 idempotency check — return early if challenge already exists
    // -----------------------------------------------------------------------
    const { data: existing, error: selectError } = await supabase
      .from("daily_challenges")
      .select("id")
      .eq("challenge_date", today)
      .maybeSingle()

    if (selectError) {
      console.error("generate-daily-challenge: SELECT error", selectError)
      return new Response(
        JSON.stringify({ error: selectError.message }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    if (existing) {
      console.log(`generate-daily-challenge: challenge for ${today} already exists`)
      return new Response(
        JSON.stringify({ success: true, date: today, already_exists: true }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // -----------------------------------------------------------------------
    // Pick 10 random vocabulary IDs via Fisher-Yates shuffle
    // -----------------------------------------------------------------------
    const pool = [...VOCABULARY_POOL]
    shuffleArray(pool)
    const vocabularyIds = pool.slice(0, 10)

    // Pick a random game_mode
    const gameMode = GAME_MODES[Math.floor(Math.random() * GAME_MODES.length)]

    // -----------------------------------------------------------------------
    // Insert the new daily challenge
    // -----------------------------------------------------------------------
    const { error: insertError } = await supabase
      .from("daily_challenges")
      .insert({
        challenge_date: today,
        game_mode: gameMode,
        vocabulary_ids: vocabularyIds,
        bonus_elo: 50,
      })

    if (insertError) {
      console.error("generate-daily-challenge: INSERT error", insertError)
      return new Response(
        JSON.stringify({ error: insertError.message }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    console.log(`generate-daily-challenge: created challenge for ${today} (mode=${gameMode}, words=${vocabularyIds.length})`)

    return new Response(
      JSON.stringify({
        success: true,
        date: today,
        game_mode: gameMode,
        word_count: vocabularyIds.length,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  } catch (error) {
    console.error("generate-daily-challenge: unhandled error", error)
    return new Response(
      JSON.stringify({ error: (error as Error).message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }
})
