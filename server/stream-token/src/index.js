/**
 * SwiftChat — Stream Video token serveri (Cloudflare Worker).
 *
 * Nega kerak: Stream har bir foydalanuvchi uchun API Secret bilan imzolangan JWT talab qiladi. Secret ilovada
 * bo'lsa, istalgan odam uni APK'dan olib, istalgan foydalanuvchi nomidan qo'ng'iroqqa kira olardi. Shuning uchun
 * secret faqat shu serverda (Cloudflare secret) turadi.
 *
 * Kim ekanini qanday biladi: ilova o'zining Relay access token'ini yuboradi, server shu token bilan Relay'dan
 * `GET /v1/users/me` so'raydi. Relay javob bergan `id` — haqiqiy foydalanuvchi; token faqat shu id uchun beriladi.
 * Ilova o'zi aytgan id'ga ishonilmaydi.
 *
 * So'rov:  POST /token   Authorization: Bearer <Relay access token>
 * Javob:   200 {"token": "<jwt>", "userId": "...", "expiresAt": <epoch ms>}
 *          401 — Relay token'i eskirgan/yaroqsiz (ilova token'ni yangilab qayta so'raydi)
 */

/** Stream token muddati. SDK muddati tugaganda o'zi yangisini so'raydi. */
const TOKEN_TTL_SECONDS = 60 * 60;
/** Serverlar soati biroz farq qilsa ham token "hali boshlanmagan" deb rad etilmasin. */
const CLOCK_SKEW_SECONDS = 60;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === "/health") return json({ ok: true });
    if (url.pathname !== "/token") return json({ code: "NOT_FOUND" }, 404);
    if (request.method !== "POST") return json({ code: "METHOD_NOT_ALLOWED" }, 405);

    if (!env.STREAM_API_SECRET) return json({ code: "SERVER_MISCONFIGURED" }, 500);

    const auth = request.headers.get("Authorization") || "";
    if (!auth.startsWith("Bearer ") || auth.length <= 7) return json({ code: "UNAUTHORIZED" }, 401);

    const userId = await relayUserId(env.RELAY_BASE_URL, auth);
    if (userId === null) return json({ code: "UNAUTHORIZED" }, 401);
    if (userId === undefined) return json({ code: "RELAY_UNAVAILABLE" }, 502);

    const now = Math.floor(Date.now() / 1000);
    const exp = now + TOKEN_TTL_SECONDS;
    const token = await signJwt({ user_id: userId, iat: now - CLOCK_SKEW_SECONDS, exp }, env.STREAM_API_SECRET);
    return json({ token, userId, expiresAt: exp * 1000 });
  },
};

/**
 * Relay'dan token egasini so'raydi.
 * @returns id; `null` — token yaroqsiz (401/403); `undefined` — Relay ishlamayapti yoki javob kutilmagan.
 */
export async function relayUserId(relayBaseUrl, authorization) {
  let response;
  try {
    response = await fetch(new URL("/v1/users/me", relayBaseUrl), {
      headers: { Authorization: authorization, Accept: "application/json" },
    });
  } catch {
    return undefined;
  }
  if (response.status === 401 || response.status === 403) return null;
  if (!response.ok) return undefined;
  const body = await response.json().catch(() => null);
  return typeof body?.id === "string" && body.id.length > 0 ? body.id : undefined;
}

/** HS256 JWT — Stream server SDK'lari yasaydigan token bilan bir xil format (tashqi kutubxonasiz). */
export async function signJwt(payload, secret) {
  const encoder = new TextEncoder();
  const header = base64url(encoder.encode(JSON.stringify({ alg: "HS256", typ: "JWT" })));
  const body = base64url(encoder.encode(JSON.stringify(payload)));
  const key = await crypto.subtle.importKey("raw", encoder.encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const signature = await crypto.subtle.sign("HMAC", key, encoder.encode(`${header}.${body}`));
  return `${header}.${body}.${base64url(new Uint8Array(signature))}`;
}

function base64url(bytes) {
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json", "Cache-Control": "no-store" },
  });
}
