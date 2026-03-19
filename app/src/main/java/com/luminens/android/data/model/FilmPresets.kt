package com.luminens.android.data.model

/**
 * Film preset — maps CSS filter parameters to GPUImage filter chain.
 * Parameters match film-presets.ts from the web app exactly.
 */
data class FilmPreset(
    val id: String,
    val name: String,
    val description: String,
    val category: FilmCategory,
    // CSS-equivalent parameters (0-100 scale unless noted)
    val contrast: Int,        // CSS contrast% (100 = neutral)
    val saturation: Int,      // CSS saturate% (100 = neutral, 0 = B&W)
    val brightness: Int,      // CSS brightness% (100 = neutral)
    val hueRotate: Int,       // degrees (-180 to 180)
    val sepia: Double,        // 0.0 to 1.0
    val grain: Int,           // 0 to 100
    val grainSize: GrainSize,
    val shadows: Int,         // -25 to +25 (additive on brightness in shadows)
    val highlights: Int,      // -25 to +25
    val fade: Int,            // 0 to 100
    val warmth: Int,          // -50 to +50 (negative = cool, positive = warm)
)

enum class FilmCategory { BASE, KODAK, FUJI, ILFORD, CINESTILL, AGFA, CINEMA }
enum class GrainSize { FINE, MEDIUM, COARSE }

object FilmPresetsData {

    val all: List<FilmPreset> = listOf(
        // ── Base ──────────────────────────────────────────────────────────────
        FilmPreset("none", "Nessuno", "Immagine originale",
            FilmCategory.BASE, 100, 100, 100, 0, 0.0, 0, GrainSize.FINE, 0, 0, 0, 0),
        FilmPreset("custom", "Personalizzato", "Crea tua pellicola",
            FilmCategory.BASE, 100, 100, 100, 0, 0.0, 0, GrainSize.FINE, 0, 0, 0, 0),

        // ── Kodak ─────────────────────────────────────────────────────────────
        FilmPreset("portra-400", "Kodak Portra 400", "Skin tones caldi, ombre morbide",
            FilmCategory.KODAK, 95, 90, 102, 5, 0.08, 8, GrainSize.FINE, 10, -5, 8, 15),
        FilmPreset("kodak-ektar", "Kodak Ektar 100", "Colori vividi, alta saturazione",
            FilmCategory.KODAK, 110, 130, 100, -5, 0.0, 3, GrainSize.FINE, -10, 0, 0, 5),
        FilmPreset("kodak-gold", "Kodak Gold 200", "Toni caldi nostalgici anni 90",
            FilmCategory.KODAK, 105, 110, 102, 10, 0.12, 10, GrainSize.FINE, 5, -5, 10, 25),
        FilmPreset("kodak-tri-x", "Kodak Tri-X 400", "B&W iconico, contrasto marcato",
            FilmCategory.KODAK, 125, 0, 98, 0, 0.0, 20, GrainSize.COARSE, -15, 10, 0, 0),
        FilmPreset("kodak-ultramax", "Kodak Ultramax 400", "Colori pop anni 2000",
            FilmCategory.KODAK, 108, 125, 103, 8, 0.05, 12, GrainSize.MEDIUM, 0, 5, 5, 12),
        FilmPreset("kodak-ektachrome", "Kodak Ektachrome E100", "Slide film, colori saturi",
            FilmCategory.KODAK, 118, 140, 100, 3, 0.0, 3, GrainSize.FINE, -12, 8, 0, 0),

        // ── Fuji ──────────────────────────────────────────────────────────────
        FilmPreset("fuji-pro-400h", "Fuji Pro 400H", "Toni pastello, verde-azzurro",
            FilmCategory.FUJI, 92, 85, 105, -10, 0.03, 6, GrainSize.FINE, 15, 0, 12, -10),
        FilmPreset("fuji-superia", "Fuji Superia 400", "Verdi freddi, contrasto morbido",
            FilmCategory.FUJI, 95, 95, 101, -15, 0.02, 12, GrainSize.MEDIUM, 8, -3, 8, -8),
        FilmPreset("fuji-velvia", "Fuji Velvia 50", "Saturazione estrema, grana finissima",
            FilmCategory.FUJI, 125, 160, 98, 5, 0.0, 2, GrainSize.FINE, -15, 10, 0, 5),

        // ── Ilford B&W ────────────────────────────────────────────────────────
        FilmPreset("ilford-hp5", "Ilford HP5", "B&W classico con grana media",
            FilmCategory.ILFORD, 115, 0, 100, 0, 0.0, 18, GrainSize.MEDIUM, -5, 5, 5, 0),
        FilmPreset("fomapan-400", "Fomapan 400", "B&W europeo, tonalità grigie fredde",
            FilmCategory.ILFORD, 110, 0, 98, 0, 0.0, 20, GrainSize.MEDIUM, 0, 3, 8, -5),

        // ── CineStill / Polaroid ──────────────────────────────────────────────
        FilmPreset("cinestill-800t", "CineStill 800T", "Look cinematografico tungsteno",
            FilmCategory.CINESTILL, 105, 95, 98, 15, 0.05, 22, GrainSize.MEDIUM, 5, -10, 15, -20),
        FilmPreset("polaroid-sx70", "Polaroid SX-70", "Fade caldo, instant",
            FilmCategory.CINESTILL, 85, 70, 106, 8, 0.18, 5, GrainSize.FINE, 20, -10, 25, 18),

        // ── Agfa / Lomo ───────────────────────────────────────────────────────
        FilmPreset("agfa-vista", "Agfa Vista 200", "Toni caldi vintage, nostalgico",
            FilmCategory.AGFA, 88, 90, 104, 12, 0.15, 8, GrainSize.FINE, 12, -8, 15, 20),
        FilmPreset("lomo-800", "Lomography 800", "Cross-process, grana forte",
            FilmCategory.AGFA, 115, 135, 102, 25, 0.08, 30, GrainSize.COARSE, 5, -5, 12, -15),

        // ── Cinema ────────────────────────────────────────────────────────────
        FilmPreset("cine-teal-orange", "Teal & Orange", "Look blockbuster Hollywood",
            FilmCategory.CINEMA, 115, 120, 100, -12, 0.06, 5, GrainSize.FINE, -10, 5, 3, 10),
        FilmPreset("cine-bleach-bypass", "Bleach Bypass", "Desaturato, contrasto estremo",
            FilmCategory.CINEMA, 135, 40, 96, 0, 0.04, 15, GrainSize.MEDIUM, -20, 15, 0, -5),
        FilmPreset("cine-noir", "Noir", "Film noir B&N alto contrasto",
            FilmCategory.CINEMA, 130, 0, 95, 0, 0.0, 18, GrainSize.MEDIUM, -25, 10, 0, 0),
        FilmPreset("cine-technicolor", "Technicolor", "Cinema anni 50-60, saturazione intensa",
            FilmCategory.CINEMA, 112, 155, 102, 8, 0.05, 4, GrainSize.FINE, -5, 5, 5, 15),
        FilmPreset("cine-blockbuster", "Blockbuster", "Action moderno, contrasto pompato",
            FilmCategory.CINEMA, 120, 115, 98, -8, 0.0, 6, GrainSize.FINE, -15, 10, 0, -12),
        FilmPreset("cine-indie", "Indie Film", "Look A24, fade pronunciato",
            FilmCategory.CINEMA, 85, 75, 105, 5, 0.10, 12, GrainSize.MEDIUM, 20, -10, 22, 8),
        FilmPreset("cine-vintage-70s", "Vintage Cinema", "Pellicola anni 70, grana grossa",
            FilmCategory.CINEMA, 95, 80, 103, 12, 0.14, 28, GrainSize.COARSE, 10, -8, 18, 22),
        FilmPreset("cine-scifi-cold", "Sci-Fi Cold", "Blade Runner, toni freddi",
            FilmCategory.CINEMA, 108, 85, 96, -20, 0.0, 8, GrainSize.FINE, -8, -5, 5, -30),
    )

    fun findById(id: String) = all.find { it.id == id }
}

data class PhotoStyle(
    val id: String,
    val name: String,
    val category: StyleCategory,
    val isPremium: Boolean,
    val previewResId: Int? = null, // drawable resource for preview thumbnail
    val previewUrl: String? = null, // remote URL for preview thumbnail
)

enum class StyleCategory { ADULTS, KIDS }

object PhotoStylesData {
    val all = listOf(
        PhotoStyle("natural", "Natural", StyleCategory.ADULTS, false),
        PhotoStyle("editorial", "Editorial", StyleCategory.ADULTS, false),
        PhotoStyle("modern-lifestyle", "Modern Lifestyle", StyleCategory.ADULTS, false),
        PhotoStyle("minimalist", "Minimalist", StyleCategory.ADULTS, false),
        PhotoStyle("glamour", "Glamour", StyleCategory.ADULTS, false),
        PhotoStyle("vintage", "Vintage", StyleCategory.ADULTS, false),
        PhotoStyle("linkedin-pro", "LinkedIn Pro", StyleCategory.ADULTS, true),
        PhotoStyle("linkedin-banner", "LinkedIn Banner", StyleCategory.ADULTS, true),
        PhotoStyle("executive-portrait", "Executive Portrait", StyleCategory.ADULTS, true),
        PhotoStyle("minimalist-tech", "Minimalist Tech", StyleCategory.ADULTS, true),
        PhotoStyle("high-end-beauty", "High-End Beauty", StyleCategory.ADULTS, true),
        PhotoStyle("cinematic", "Cinematic", StyleCategory.ADULTS, true),
        PhotoStyle("artistic", "Artistic", StyleCategory.ADULTS, true),
        PhotoStyle("ansel-adams", "Ansel Adams", StyleCategory.ADULTS, true),
        PhotoStyle("annie-leibovitz", "Annie Leibovitz", StyleCategory.ADULTS, true),
        PhotoStyle("helmut-newton", "Helmut Newton", StyleCategory.ADULTS, true),
        PhotoStyle("peter-lindbergh", "Peter Lindbergh", StyleCategory.ADULTS, true),
        PhotoStyle("martin-schoeller", "Martin Schoeller", StyleCategory.ADULTS, true),
        PhotoStyle("steve-mccurry", "Steve McCurry", StyleCategory.ADULTS, true),
        PhotoStyle("wes-anderson", "Wes Anderson", StyleCategory.ADULTS, true),
        PhotoStyle("kids-studio", "Kids Studio", StyleCategory.KIDS, false),
        PhotoStyle("school-day", "School Day", StyleCategory.KIDS, false),
    )
}

data class GenerationParams(
    val referencePhotoPaths: List<String> = emptyList(),
    val style: String = "",
    val setting: String = "",
    val subSetting: String? = null,
    val category: String = "adults", // "adults" | "kids"
    val numShots: Int = 1,
    val aspectRatio: String = "1:1",
    val resolution: String = "2K",
    val customPrompt: String? = null,
)
