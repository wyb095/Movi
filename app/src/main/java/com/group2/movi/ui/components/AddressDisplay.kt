package com.group2.movi.ui.components

private val PLUS_CODE_REGEX = Regex("""^[A-Z0-9]{2,8}\+[A-Z0-9]{2,3}$""", RegexOption.IGNORE_CASE)
private val POSTAL_CODE_REGEX = Regex("""^[A-Z]?\d{5,6}(?:-\d{4})?$""", RegexOption.IGNORE_CASE)
private val ROAD_LIKE_REGEX = Regex(
    """(^\d)|\b(road|rd|street|st|avenue|ave|boulevard|blvd|drive|dr|lane|ln|highway|hwy|dao|lu|jie)\b""",
    RegexOption.IGNORE_CASE
)
private val GENERIC_REGION_NAMES = setOf(
    "china",
    "city",
    "hong kong",
    "hong kong sar",
    "shenzhen",
    "guangdong",
    "guangdong province",
    "new territories",
    "kowloon"
)

fun displayablePlace(address: String, fallback: String): String {
    if (address.isBlank()) return fallback

    val cleanedSegments = address
        .split(",")
        .mapNotNull(::normalizeAddressSegment)

    if (cleanedSegments.isEmpty()) return fallback

    val preferred = cleanedSegments.firstOrNull { segment ->
        !looksLikeRoadAddress(segment) && !isGenericRegionName(segment)
    } ?: cleanedSegments.firstOrNull { !isGenericRegionName(it) }
        ?: cleanedSegments.first()

    return preferred.take(30)
}

private fun normalizeAddressSegment(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    if (PLUS_CODE_REGEX.matches(trimmed) || POSTAL_CODE_REGEX.matches(trimmed)) return null

    val withoutPostal = trimmed.replace(Regex("""\b\d{5,6}\b"""), " ")
    val cleaned = withoutPostal
        .replace(Regex("""\bHong Kong SAR\b""", RegexOption.IGNORE_CASE), "Hong Kong")
        .replace(Regex("""\bNanshan Qu\b""", RegexOption.IGNORE_CASE), "Nanshan")
        .replace(Regex("""\bNan Shan Qu\b""", RegexOption.IGNORE_CASE), "Nanshan")
        .replace(Regex("""\bLuohu Qu\b""", RegexOption.IGNORE_CASE), "Luohu")
        .replace(Regex("""\bFutian Qu\b""", RegexOption.IGNORE_CASE), "Futian")
        .replace(Regex("""\bBaoan Qu\b""", RegexOption.IGNORE_CASE), "Baoan")
        .replace(Regex("""\bLonggang Qu\b""", RegexOption.IGNORE_CASE), "Longgang")
        .replace(Regex("""\bLonghua Qu\b""", RegexOption.IGNORE_CASE), "Longhua")
        .replace(Regex("""\bShen Zhen Shi\b""", RegexOption.IGNORE_CASE), "Shenzhen")
        .replace(Regex("""\bGuang Dong Sheng\b""", RegexOption.IGNORE_CASE), "Guangdong")
        .replace(Regex("""\b([A-Za-z]+)\s+District\b""", RegexOption.IGNORE_CASE), "$1")
        .replace(Regex("""\b([A-Za-z]+)\s+(Qu|Shi|Sheng)\b""", RegexOption.IGNORE_CASE)) {
            it.groupValues[1]
        }
        .replace(
            Regex(
                """\b(road|rd|street|st|avenue|ave|boulevard|blvd|drive|dr|lane|ln|highway|hwy|dao|lu|jie)\.?$""",
                RegexOption.IGNORE_CASE
            ),
            ""
        )
        .replace(Regex("""\s+"""), " ")
        .trim()

    if (cleaned.isEmpty()) return null
    return cleaned
}

private fun looksLikeRoadAddress(segment: String): Boolean =
    ROAD_LIKE_REGEX.containsMatchIn(segment)

private fun isGenericRegionName(segment: String): Boolean =
    segment.lowercase() in GENERIC_REGION_NAMES
