const OPENAI_URL = "https://api.openai.com/v1/chat/completions"

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "authorization, content-type, x-client-info, apikey",
}

/**
 * Supabase Edge Function — openai-proxy
 *
 * Routes all chat completion requests through this server-side proxy so that
 * the OPENAI_API_KEY never leaves the Supabase secrets store. The Android
 * client authenticates with the Supabase anon key; this function attaches the
 * real OpenAI key before forwarding to api.openai.com.
 */
Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: corsHeaders })
  }

  try {
    const openAiKey = Deno.env.get("OPENAI_API_KEY")
    if (!openAiKey) {
      return new Response(
        JSON.stringify({ error: "OPENAI_API_KEY not configured" }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // Forward the request body verbatim to OpenAI
    const body = await req.text()

    const openAiResponse = await fetch(OPENAI_URL, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${openAiKey}`,
        "Content-Type": "application/json",
      },
      body,
    })

    const responseBody = await openAiResponse.text()

    return new Response(responseBody, {
      status: openAiResponse.status,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    })
  } catch (error) {
    return new Response(
      JSON.stringify({ error: (error as Error).message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }
})
