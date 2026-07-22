/**
 * refresh-streak-freeze — Supabase Edge Function
 *
 * Scheduled via Supabase Dashboard → Edge Functions → Schedule
 * Cron: 0 1 * * 1  (Monday 01:00 UTC — one hour after weekly_elo reset)
 *
 * Grants 1 streak freeze to all users whose streak_freeze_count < 1.
 * Uses service_role key (Threat T-03-04 mitigation: key never exposed to client).
 */
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

serve(async (_req: Request): Promise<Response> => {
  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

    if (!supabaseUrl || !serviceRoleKey) {
      return new Response(
        JSON.stringify({ error: "Missing SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY" }),
        { status: 500, headers: { "Content-Type": "application/json" } },
      );
    }

    const supabase = createClient(supabaseUrl, serviceRoleKey, {
      auth: { persistSession: false },
    });

    // Grant 1 freeze to every user who currently has none
    const { error } = await supabase.rpc("grant_streak_freeze");

    if (error) {
      console.error("grant_streak_freeze RPC error:", error.message);
      return new Response(
        JSON.stringify({ error: error.message }),
        { status: 500, headers: { "Content-Type": "application/json" } },
      );
    }

    console.log("refresh-streak-freeze: completed successfully");
    return new Response(
      JSON.stringify({ refreshed: true }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    console.error("refresh-streak-freeze: unexpected error:", message);
    return new Response(
      JSON.stringify({ error: message }),
      { status: 500, headers: { "Content-Type": "application/json" } },
    );
  }
});
