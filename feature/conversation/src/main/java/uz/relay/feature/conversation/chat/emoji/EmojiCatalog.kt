package uz.relay.feature.conversation.chat.emoji

/**
 * Emoji panelidagi to'plam (tashqi kutubxonasiz). Eng ko'p ishlatiladiganlar, Telegram'dagi kabi kategoriyalarga
 * bo'lingan; kategoriya tugmasida [icon] chiziladi. Ko'pchiligi Unicode 10 gacha; bir nechtasi (🥰 🥱 🤍 🟢 kabi)
 * Unicode 11–12 dan — tizim shrifti yangilanmagan Android 8–9 telefonlarda ular "kvadrat" bo'lib ko'rinishi mumkin.
 */
internal data class EmojiCategory(val icon: String, val emojis: List<String>)

/** Bo'shliq bilan ajratilgan qator → ro'yxat; takrorlar olib tashlanadi (panel to'rida kalit sifatida ishlatiladi). */
private fun list(value: String): List<String> = value.trim().split(Regex("\\s+")).distinct()

internal val EmojiCatalog: List<EmojiCategory> = listOf(
    EmojiCategory(
        "😀",
        list(
            """
            😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🤩 🥳 😏
            😒 😞 😔 😟 😕 🙁 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🤭 🤫 🤥 😶
            😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕 🤑 🤠 😈 👿 👹 👺 🤡 💩 👻 💀
            👽 🤖 🎃 😺 😸 😹 😻 😼 😽 🙀 😿 😾
            """
        )
    ),
    EmojiCategory(
        "👍",
        list(
            """
            👋 🤚 🖐️ ✋ 🖖 👌 🤏 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 🖕 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🤝 🙏 ✍️ 💅
            🤳 💪 🦾 🦵 🦶 👂 👃 🧠 👀 👁️ 👅 👄 💋 👶 🧒 👦 👧 🧑 👨 👩 🧓 👴 👵 🙍 🙎 🙅 🙆 💁 🙋 🙇 🤦 🤷
            """
        )
    ),
    EmojiCategory(
        "❤️",
        list(
            """
            ❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 ♥️ 💯 💢 💥 💫 💦 💨 🔥 ✨ ⭐ 🌟 💤 🎉
            """
        )
    ),
    EmojiCategory(
        "🐶",
        list(
            """
            🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🙈 🙉 🙊 🐔 🐧 🐦 🐤 🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🐛
            🦋 🐌 🐞 🐜 🐢 🐍 🦎 🐙 🦑 🦀 🐡 🐠 🐟 🐬 🐳 🐋 🦈 🐊 🐅 🐆 🦓 🐘 🦏 🐪 🦒 🐃 🐄 🐎 🐑 🐐 🐕 🐈
            🌵 🎄 🌲 🌳 🌴 🌱 🌿 🍀 🍁 🍂 🌷 🌹 🥀 🌺 🌸 🌼 🌻 🌞 🌝 🌚 🌙 ⭐ ☀️ ⛅ ☁️ 🌧️ ⛈️ ❄️ ☃️ 🌈 🌊
            """
        )
    ),
    EmojiCategory(
        "🍏",
        list(
            """
            🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🍆 🥑 🥦 🥒 🌶️ 🌽 🥕 🧄 🧅 🥔 🍠 🥐 🥖 🍞 🧀
            🥚 🍳 🥞 🥓 🍗 🍖 🌭 🍔 🍟 🍕 🥪 🌮 🌯 🥗 🍝 🍜 🍲 🍛 🍣 🍱 🥟 🍤 🍚 🍙 🍘 🍥 🥠 🍢 🍡 🍧 🍨 🍦
            🥧 🧁 🍰 🎂 🍮 🍭 🍬 🍫 🍿 🍩 🍪 🥛 ☕ 🍵 🧃 🥤 🍶 🍺 🍻 🥂 🍷 🍸 🍹 🧊
            """
        )
    ),
    EmojiCategory(
        "⚽",
        list(
            """
            ⚽ 🏀 🏈 ⚾ 🥎 🎾 🏐 🏉 🥏 🎱 🏓 🏸 🏒 🏑 🥍 🏏 🥅 ⛳ 🏹 🎣 🥊 🥋 🎽 ⛸️ 🎿 🏂 🏋️ 🤸 🤾 🏌️ 🏇 🧘
            🏄 🏊 🚴 🏆 🥇 🥈 🥉 🏅 🎖️ 🎗️ 🎫 🎟️ 🎪 🎭 🎨 🎬 🎤 🎧 🎼 🎹 🥁 🎷 🎺 🎸 🎻 🎲 ♟️ 🎯 🎳 🎮 🧩 🎁
            """
        )
    ),
    EmojiCategory(
        "🚗",
        list(
            """
            🚗 🚕 🚙 🚌 🚎 🏎️ 🚓 🚑 🚒 🚐 🚚 🚛 🚜 🛴 🚲 🛵 🏍️ 🚨 🚔 🚍 🚘 🚖 🚡 🚠 🚟 🚃 🚋 🚝 🚄 🚅 🚈 🚂
            ✈️ 🛫 🛬 🚀 🛸 🚁 ⛵ 🚤 🛳️ ⛴️ 🚢 ⚓ ⛽ 🚧 🚦 🗺️ 🗿 🗽 🗼 🏰 🏯 🏟️ 🎡 🎢 🎠 ⛲ 🏖️ 🏝️ 🏜️ 🌋 ⛰️ 🏔️
            🏕️ 🏠 🏡 🏢 🏥 🏦 🏨 🏪 🏫 🏛️ 🕌 ⛪ 🕋 🌃 🌆 🌇 🌉
            """
        )
    ),
    EmojiCategory(
        "💡",
        list(
            """
            ⌚ 📱 💻 ⌨️ 🖥️ 🖨️ 🖱️ 💾 💿 📷 📸 📹 🎥 📞 ☎️ 📺 📻 ⏰ ⏳ 🔋 🔌 💡 🔦 🕯️ 💸 💵 💰 💳 💎 ⚖️ 🔧 🔨
            🛠️ ⚙️ 🔫 💣 🔪 🛡️ 🔮 💊 💉 🧬 🌡️ 🧹 🧺 🧻 🚿 🛁 🔑 🗝️ 🚪 🛋️ 🛏️ 🎈 🎀 🎊 ✉️ 📦 📝 📁 📅 📌 📎 ✂️
            🔒 🔓 📚 📖 🔗 🧲 🧪 🔬 🔭 📡
            """
        )
    ),
    EmojiCategory(
        "✅",
        list(
            """
            ✅ ☑️ ✔️ ❌ ❎ ➕ ➖ ➗ ✖️ ❓ ❔ ❗ ❕ ‼️ ⁉️ ⚠️ 🚫 ⛔ 📛 🔞 ♻️ 💲 💱 ©️ ®️ ™️ 🔝 🔜 🆗 🆕 🆓 🆒
            🆘 ℹ️ 🔟 1️⃣ 2️⃣ 3️⃣ 4️⃣ 5️⃣ 6️⃣ 7️⃣ 8️⃣ 9️⃣ 0️⃣ #️⃣ *️⃣ ▶️ ⏸️ ⏹️ ⏺️ ⏭️ ⏮️ 🔀 🔁 🔂 ⬆️ ⬇️ ⬅️ ➡️ ↩️ ↪️
            🔴 🟠 🟡 🟢 🔵 🟣 ⚫ ⚪ 🟤 🔺 🔻 🔶 🔷 🔸 🔹 🇺🇿 🇷🇺 🇺🇸 🇬🇧 🇹🇷 🇰🇿 🇰🇬 🇹🇯 🇩🇪 🇫🇷 🇯🇵 🇰🇷 🇨🇳
            """
        )
    )
)
