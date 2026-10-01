// Node'ning o'z test runner'i bilan (qo'shimcha kutubxonasiz): `node --test server/stream-token/test`.
import { test } from "node:test";
import assert from "node:assert/strict";
import { createHmac } from "node:crypto";
import worker, { signJwt } from "../src/index.js";

const SECRET = "test-secret";
const env = { STREAM_API_SECRET: SECRET, RELAY_BASE_URL: "https://relay.example" };

function decode(part) {
  return JSON.parse(Buffer.from(part.replace(/-/g, "+").replace(/_/g, "/"), "base64").toString());
}

function withRelay(handler, fn) {
  const original = globalThis.fetch;
  globalThis.fetch = async (url, init) => handler(String(url), init);
  return fn().finally(() => { globalThis.fetch = original; });
}

const post = (headers = {}) => new Request("https://worker.example/token", { method: "POST", headers });

test("signature matches HS256 computed by node:crypto", async () => {
  const jwt = await signJwt({ user_id: "u1", exp: 1 }, SECRET);
  const [h, b, s] = jwt.split(".");
  const expected = createHmac("sha256", SECRET).update(`${h}.${b}`).digest("base64url");
  assert.equal(s, expected);
  assert.deepEqual(decode(h), { alg: "HS256", typ: "JWT" });
});

test("issues token for the user Relay says owns the access token", () =>
  withRelay(async (url, init) => {
    assert.equal(url, "https://relay.example/v1/users/me");
    assert.equal(init.headers.Authorization, "Bearer relay-token");
    return new Response(JSON.stringify({ id: "user-42", displayName: "Ali" }), { status: 200 });
  }, async () => {
    const res = await worker.fetch(post({ Authorization: "Bearer relay-token" }), env);
    assert.equal(res.status, 200);
    const body = await res.json();
    assert.equal(body.userId, "user-42");
    const payload = decode(body.token.split(".")[1]);
    assert.equal(payload.user_id, "user-42");
    assert.ok(payload.exp > payload.iat);
  }));

test("expired relay token gives 401 so the app refreshes and retries", () =>
  withRelay(async () => new Response("{}", { status: 401 }), async () => {
    const res = await worker.fetch(post({ Authorization: "Bearer old" }), env);
    assert.equal(res.status, 401);
  }));

test("missing authorization is rejected without calling relay", () =>
  withRelay(async () => { throw new Error("relay must not be called"); }, async () => {
    assert.equal((await worker.fetch(post(), env)).status, 401);
  }));

test("relay outage is 502 not 401 (app must not log the user out)", () =>
  withRelay(async () => new Response("down", { status: 503 }), async () => {
    assert.equal((await worker.fetch(post({ Authorization: "Bearer t" }), env)).status, 502);
  }));

test("missing secret is a server error", async () => {
  const res = await worker.fetch(post({ Authorization: "Bearer t" }), { RELAY_BASE_URL: env.RELAY_BASE_URL });
  assert.equal(res.status, 500);
});

test("only POST /token is served", async () => {
  assert.equal((await worker.fetch(new Request("https://worker.example/token"), env)).status, 405);
  assert.equal((await worker.fetch(new Request("https://worker.example/other", { method: "POST" }), env)).status, 404);
});
