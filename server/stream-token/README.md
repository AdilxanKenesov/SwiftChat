# Stream Video token serveri

SwiftChat'dagi qo'ng'iroqlar (Stream Video) uchun foydalanuvchi token'ini beradigan kichik Cloudflare Worker.

## Nega kerak
Stream har bir foydalanuvchi uchun **API Secret** bilan imzolangan JWT talab qiladi. Secret ilovada bo'lsa, uni
APK'dan olib, istalgan foydalanuvchi nomidan qo'ng'iroqqa kirish mumkin bo'lardi. Shuning uchun secret faqat shu
serverda (Cloudflare secret sifatida) turadi.

## Qanday ishlaydi
1. Ilova `POST /token` yuboradi, sarlavhada o'zining Relay access token'i bilan.
2. Server shu token bilan Relay'dan `GET /v1/users/me` so'raydi. Relay qaytargan `id` — haqiqiy foydalanuvchi.
3. Server shu `id` uchun 1 soatlik HS256 JWT imzolaydi va qaytaradi. Muddati tugasa SDK o'zi yangisini so'raydi.

| Javob | Ma'nosi |
|---|---|
| `200 {token, userId, expiresAt}` | token tayyor |
| `401` | Relay token'i eskirgan yoki yaroqsiz — ilova token'ni yangilab avtomatik qayta so'raydi |
| `502` | Relay javob bermayapti — vaqtinchalik (foydalanuvchi logout qilinmaydi) |
| `500` | serverda `STREAM_API_SECRET` sozlanmagan |

Tashqi kutubxona yo'q: JWT Web Crypto bilan imzolanadi.

## Deploy (bir marta)
Cloudflare'da bepul akkaunt kerak (karta talab qilinmaydi).

```bash
cd server/stream-token
npx wrangler login                          # brauzerda Cloudflare'ga kirish
npx wrangler secret put STREAM_API_SECRET   # Stream dashboard → App → API Secret'ni shu yerga yapishtiring
npx wrangler deploy                         # manzilni chiqaradi: https://swiftchat-stream-token.<akkaunt>.workers.dev
```

Keyin ilova uchun `local.properties`ga (git'ga kirmaydi):

```properties
STREAM_TOKEN_URL=https://swiftchat-stream-token.<akkaunt>.workers.dev/token
```

Ilova yangi token bilan ishlayotganini tekshirgach, Stream dashboard'da **"Disable Auth Checks"ni o'chiring**.
Shundan keyin dev-token bilan hech kim kira olmaydi.

## Tekshirish
```bash
curl https://swiftchat-stream-token.<akkaunt>.workers.dev/health      # {"ok":true}
node --test server/stream-token/test/index.test.mjs                   # 7 ta test (Node 18+)
```

Secret hech qachon kodga, `wrangler.toml`ga yoki chatga yozilmaydi.
