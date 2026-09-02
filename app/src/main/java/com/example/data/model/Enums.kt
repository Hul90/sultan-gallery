package com.example.data.model

enum class MediaTab(val title: String) {
    ALBUMS("All Albums"),
    ALL("All Media"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    FAVORITES("Favorites"),
    AUDIO("Audio"),
    RECENT("Recent"),
    SCREENSHOTS("Screenshots"),
    DOWNLOADS("Downloads"),
    CAMERA("Camera"),
    WHATSAPP("WhatsApp"),
    TELEGRAM("Telegram"),
    TRASH("Trash"),
    VAULT("Secret Vault")
}

enum class FormatFilter(val label: String, val extensions: List<String>) {
    ALL("All Formats", emptyList()),
    JPEG("JPEG", listOf("jpg", "jpeg", "jpe", "jfif", "jif", "jfi")),
    PNG("PNG / APNG", listOf("png", "apng")),
    WEBP("WebP", listOf("webp")),
    AVIF("AVIF", listOf("avif")),
    GIF("GIF", listOf("gif")),
    HEIC("HEIC / HEIF", listOf("heic", "heif", "hif")),
    TIFF("TIFF", listOf("tif", "tiff")),
    BMP("BMP", listOf("bmp", "dib")),
    RAW("Camera RAW", listOf("dng", "cr2", "cr3", "crw", "nef", "nrw", "arw", "srf", "sr2", "raf", "rw2", "rwl", "orf", "pef", "ptx", "mrw", "kdc", "dcr", "erf", "x3f", "iiq", "3fr", "mef", "mos")),
    SVG("SVG Vector", listOf("svg")),
    PSD("PSD / PSB", listOf("psd", "psb", "ai", "eps")),
    PDF("PDF Documents", listOf("pdf")),
    OTHERS("Other Formats", listOf("tga", "ico", "cur", "jp2", "j2k", "jxl", "qoi", "dds", "hdr", "exr", "icns", "pcx", "ppm", "pgm", "pbm", "pam"))
}

enum class SortOrder(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    SIZE_DESC("Largest First"),
    SIZE_ASC("Smallest First")
}

enum class GridMode(val columns: Int, val label: String) {
    COMPACT(4, "Compact Grid"),
    NORMAL(3, "Standard Grid"),
    LARGE(2, "Large View"),
    LIST(1, "List View")
}

enum class FilterType(val displayName: String) {
    ORIGINAL("Original"),
    VIVID("Vivid"),
    WARM("Warm"),
    COOL("Cool"),
    CINEMATIC("Cinematic"),
    VINTAGE("Vintage"),
    BW("B & W"),
    PORTRAIT("Portrait"),
    DRAMATIC("Dramatic"),
    SOFT("Soft"),
    HDR("HDR Effect")
}

enum class CropPreset(val label: String, val ratioX: Float, val ratioY: Float) {
    FREE("Free", 0f, 0f),
    SQUARE("1:1 (Square)", 1f, 1f),
    FOUR_THREE("4:3", 4f, 3f),
    THREE_FOUR("3:4", 3f, 4f),
    SIXTEEN_NINE("16:9", 16f, 9f),
    NINE_SIXTEEN("9:16 (Story)", 9f, 16f),
    PASSPORT("Passport (35:45)", 3.5f, 4.5f),
    WHATSAPP_DP("WhatsApp DP (1:1)", 1f, 1f),
    FACEBOOK_COVER("FB Cover (16:9)", 16f, 9f),
    INSTA_POST("Instagram (4:5)", 4f, 5f),
    YOUTUBE_THUMB("YouTube (16:9)", 16f, 9f)
}
