package com.stanleycx.fruitos.data

/**
 * Classe automatiquement les apps en catégories façon App Library Fruit OS.
 *
 * Stratégie en 3 niveaux (du plus fiable au plus approximatif) :
 *   1. Mapping explicite de packages très connus (WhatsApp, Spotify…)
 *   2. Catégorie système Android (ApplicationInfo.category) — fiable mais souvent absente
 *   3. Fallback par mots-clés sur le packageName puis le label
 *
 * Le niveau 1 passe avant le système car certains éditeurs déclarent des catégories
 * système farfelues (ex: des réseaux sociaux taggés "productivity").
 */
object AppCategorizer {

    /**
     * Mapping explicite de packages connus → catégorie.
     * Priorité maximale. À enrichir au fil du temps.
     */
    private val knownPackages: Map<String, AppCategory> = buildMap {
        // Réseaux sociaux
        put("com.whatsapp", AppCategory.SOCIAL)
        put("com.facebook.katana", AppCategory.SOCIAL)
        put("com.facebook.orca", AppCategory.SOCIAL)
        put("com.instagram.android", AppCategory.SOCIAL)
        put("com.zhiliaoapp.musically", AppCategory.SOCIAL) // TikTok
        put("com.snapchat.android", AppCategory.SOCIAL)
        put("com.twitter.android", AppCategory.SOCIAL)
        put("com.x.android", AppCategory.SOCIAL)
        put("org.telegram.messenger", AppCategory.SOCIAL)
        put("com.discord", AppCategory.SOCIAL)
        put("com.linkedin.android", AppCategory.SOCIAL)
        put("com.reddit.frontpage", AppCategory.SOCIAL)
        put("com.pinterest", AppCategory.SOCIAL)
        put("com.bsky.app", AppCategory.SOCIAL) // Bluesky
        put("com.google.android.apps.messaging", AppCategory.SOCIAL)
        put("com.viber.voip", AppCategory.SOCIAL)
        put("jp.naver.line.android", AppCategory.SOCIAL)
        put("com.skype.raider", AppCategory.SOCIAL)
        put("com.signal", AppCategory.SOCIAL)
        put("org.thoughtcrime.securesms", AppCategory.SOCIAL) // Signal

        // Divertissement (vidéo / musique / streaming)
        put("com.spotify.music", AppCategory.ENTERTAINMENT)
        put("com.google.android.youtube", AppCategory.ENTERTAINMENT)
        put("com.google.android.apps.youtube.music", AppCategory.ENTERTAINMENT)
        put("com.netflix.mediaclient", AppCategory.ENTERTAINMENT)
        put("com.disney.disneyplus", AppCategory.ENTERTAINMENT)
        put("com.amazon.avod.thirdpartyclient", AppCategory.ENTERTAINMENT) // Prime Video
        put("tv.twitch.android.app", AppCategory.ENTERTAINMENT)
        put("com.deezer.android.app", AppCategory.ENTERTAINMENT)
        put("deezer.android.app", AppCategory.ENTERTAINMENT)
        put("com.soundcloud.android", AppCategory.ENTERTAINMENT)
        put("com.plexapp.android", AppCategory.ENTERTAINMENT)
        put("com.canal.android.canal", AppCategory.ENTERTAINMENT) // Canal+
        put("fr.m6.m6replay", AppCategory.ENTERTAINMENT)
        put("air.fr.francetv.pluzz", AppCategory.ENTERTAINMENT) // france.tv

        // Créativité (photo / vidéo / dessin / musique)
        put("com.adobe.lrmobile", AppCategory.CREATIVITY) // Lightroom
        put("com.adobe.psmobile", AppCategory.CREATIVITY) // Photoshop Express
        put("com.google.android.apps.photos", AppCategory.CREATIVITY)
        put("com.canva.editor", AppCategory.CREATIVITY)
        put("com.gopro.smarty", AppCategory.CREATIVITY)
        put("video.editor.videomaker.effects.fx", AppCategory.CREATIVITY)
        put("com.instagram.boomerang", AppCategory.CREATIVITY)
        put("com.niksoftware.snapseed", AppCategory.CREATIVITY)

        // Productivité & finance
        put("com.google.android.gm", AppCategory.PRODUCTIVITY) // Gmail
        put("com.microsoft.office.outlook", AppCategory.PRODUCTIVITY)
        put("com.google.android.apps.docs", AppCategory.PRODUCTIVITY)
        put("com.google.android.apps.docs.editors.docs", AppCategory.PRODUCTIVITY)
        put("com.google.android.apps.docs.editors.sheets", AppCategory.PRODUCTIVITY)
        put("com.google.android.calendar", AppCategory.PRODUCTIVITY)
        put("com.microsoft.office.word", AppCategory.PRODUCTIVITY)
        put("com.microsoft.office.excel", AppCategory.PRODUCTIVITY)
        put("com.microsoft.teams", AppCategory.PRODUCTIVITY)
        put("us.zoom.videomeetings", AppCategory.PRODUCTIVITY)
        put("com.Slack", AppCategory.PRODUCTIVITY)
        put("com.notion.id", AppCategory.PRODUCTIVITY)
        put("com.todoist", AppCategory.PRODUCTIVITY)
        put("com.dropbox.android", AppCategory.PRODUCTIVITY)
        put("com.paypal.android.p2pmobile", AppCategory.PRODUCTIVITY)
        put("com.revolut.revolut", AppCategory.PRODUCTIVITY)
        put("com.bankin", AppCategory.PRODUCTIVITY)

        // Infos & lecture
        put("com.google.android.apps.magazines", AppCategory.INFO_READING)
        put("flipboard.app", AppCategory.INFO_READING)
        put("com.google.android.googlequicksearchbox", AppCategory.INFO_READING)
        put("com.medium.reader", AppCategory.INFO_READING)
        put("com.amazon.kindle", AppCategory.INFO_READING)
        put("fr.lemonde", AppCategory.INFO_READING)
        put("com.lefigaro", AppCategory.INFO_READING)

        // Voyages / cartes
        put("com.google.android.apps.maps", AppCategory.TRAVEL)
        put("com.waze", AppCategory.TRAVEL)
        put("com.ubercab", AppCategory.TRAVEL)
        put("com.airbnb.android", AppCategory.TRAVEL)
        put("com.booking", AppCategory.TRAVEL)
        put("com.tripadvisor.tripadvisor", AppCategory.TRAVEL)
        put("com.blablacar.android", AppCategory.TRAVEL)
        put("com.sncf.fusion", AppCategory.TRAVEL) // SNCF Connect

        // Shopping & alimentation
        put("com.amazon.mShop.android.shopping", AppCategory.SHOPPING)
        put("com.ebay.mobile", AppCategory.SHOPPING)
        put("com.einnovation.temu", AppCategory.SHOPPING)
        put("com.contextlogic.wish", AppCategory.SHOPPING)
        put("com.ubercab.eats", AppCategory.SHOPPING)
        put("fr.leboncoin", AppCategory.SHOPPING)
        put("com.deliveroo.orderapp", AppCategory.SHOPPING)
        put("fr.justeat.android", AppCategory.SHOPPING)
        put("com.vinted", AppCategory.SHOPPING)

        // Santé & forme
        put("com.google.android.apps.fitness", AppCategory.HEALTH)
        put("com.fitbit.FitbitMobile", AppCategory.HEALTH)
        put("com.strava", AppCategory.HEALTH)
        put("com.myfitnesspal.android", AppCategory.HEALTH)
        put("com.nike.ntc", AppCategory.HEALTH)
        put("cc.calm.android", AppCategory.HEALTH)
        put("com.headspace.android", AppCategory.HEALTH)

        // Éducation
        put("com.duolingo", AppCategory.EDUCATION)
        put("org.khanacademy.android", AppCategory.EDUCATION)
        put("com.babbel.mobile.android.en", AppCategory.EDUCATION)
        put("com.coursera.android", AppCategory.EDUCATION)
        put("com.quizlet.quizletandroid", AppCategory.EDUCATION)

        // Utilitaires
        put("com.google.android.deskclock", AppCategory.UTILITIES)
        put("com.android.chrome", AppCategory.UTILITIES)
        put("org.mozilla.firefox", AppCategory.UTILITIES)
        put("com.google.android.calculator", AppCategory.UTILITIES)
        put("com.android.settings", AppCategory.UTILITIES)
        put("com.google.android.GoogleCamera", AppCategory.UTILITIES)
        put("com.google.android.contacts", AppCategory.UTILITIES)
        put("com.google.android.dialer", AppCategory.UTILITIES)
        put("com.google.android.apps.authenticator2", AppCategory.UTILITIES)
        put("com.lastpass.lpandroid", AppCategory.UTILITIES)
        put("com.google.android.keep", AppCategory.UTILITIES)
    }

    /**
     * Mots-clés cherchés dans le packageName ET le label (insensible à la casse).
     * L'ordre compte : la première catégorie qui matche gagne. On met donc les
     * catégories les plus "spécifiques" en premier pour éviter les faux positifs.
     */
    private val keywordRules: List<Pair<AppCategory, List<String>>> = listOf(
        AppCategory.GAMES to listOf(
            "game", "games", "gaming", ".games.", "playgames", "gameloft", "supercell",
            "miniclip", "rovio", "king.candy", "puzzle", "arcade"
        ),
        AppCategory.SOCIAL to listOf(
            "social", "messenger", "chat", "whatsapp", "telegram", "instagram",
            "facebook", "snapchat", "tiktok", "discord", "reddit", "twitter",
            "linkedin", "dating", "tinder", "bumble"
        ),
        AppCategory.ENTERTAINMENT to listOf(
            "music", "musique", "video", "stream", "netflix", "spotify", "youtube",
            "podcast", "radio", "movie", "film", "tv", "player", "media", "audio"
        ),
        AppCategory.CREATIVITY to listOf(
            "photo", "camera", "edit", "draw", "paint", "design", "canva", "adobe",
            "lightroom", "photoshop", "creative", "studio", "art"
        ),
        AppCategory.PRODUCTIVITY to listOf(
            "office", "docs", "sheet", "word", "excel", "mail", "email", "outlook",
            "gmail", "calendar", "agenda", "note", "task", "todo", "work", "meet",
            "zoom", "teams", "slack", "drive", "cloud", "bank", "banque", "finance",
            "pay", "wallet", "money", "invest", "bourse"
        ),
        AppCategory.INFO_READING to listOf(
            "news", "presse", "journal", "magazine", "read", "book", "kindle",
            "actualite", "info", "lecture", "rss", "feed"
        ),
        AppCategory.TRAVEL to listOf(
            "map", "maps", "navigation", "gps", "travel", "voyage", "flight", "vol",
            "hotel", "trip", "uber", "taxi", "transit", "metro", "train", "sncf",
            "booking", "airbnb", "ratp"
        ),
        AppCategory.SHOPPING to listOf(
            "shop", "shopping", "store", "amazon", "ebay", "buy", "market",
            "food", "eat", "delivery", "livraison", "restaurant", "grocery",
            "courses", "fashion", "mode"
        ),
        AppCategory.HEALTH to listOf(
            "health", "sante", "fitness", "workout", "gym", "run", "running",
            "sport", "yoga", "meditation", "calm", "sleep", "diet", "nutrition",
            "medic", "pharma", "doctor", "doctolib"
        ),
        AppCategory.EDUCATION to listOf(
            "learn", "education", "school", "course", "study", "language", "langue",
            "duolingo", "quiz", "dictionary", "dictionnaire", "university", "ecole"
        ),
        AppCategory.UTILITIES to listOf(
            "clock", "alarm", "calculator", "calcul", "weather", "meteo", "browser",
            "chrome", "firefox", "file", "fichier", "scan", "qr", "flashlight",
            "torch", "setting", "parametre", "tool", "utility", "vpn", "battery",
            "clean", "manager", "contact", "phone", "dialer", "sms", "keyboard",
            "clavier", "authenticator", "password"
        )
    )

    /**
     * Détermine la catégorie d'une app.
     *
     * @param packageName identifiant du package (ex: "com.whatsapp")
     * @param label nom affiché (ex: "WhatsApp")
     * @param systemCategory valeur de ApplicationInfo.category (ou CATEGORY_UNDEFINED = -1)
     */
    fun categorize(
        packageName: String,
        label: String,
        systemCategory: Int
    ): AppCategory {
        // Niveau 1 : package connu
        knownPackages[packageName]?.let { return it }

        // Niveau 2 : catégorie système Android
        AppCategory.fromSystemCategory(systemCategory)?.let { return it }

        // Niveau 3 : mots-clés sur packageName + label
        val haystack = "$packageName ${label}".lowercase()
        for ((category, keywords) in keywordRules) {
            if (keywords.any { keyword -> haystack.contains(keyword) }) {
                return category
            }
        }

        // Rien trouvé
        return AppCategory.OTHER
    }
}