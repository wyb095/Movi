package com.group2.movi.domain.model

import com.google.firebase.firestore.GeoPoint
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class TaskMatchInsight(
    val score: Double,
    val matchPercent: Int,
    val routeOffsetKm: Double,
    val scheduleSummary: String,
    val reason: String,
    val corridorMatch: Boolean = false
)

data class EnhancedMatchFactors(
    val routeScore: Double,
    val categoryScore: Double,
    val priceScore: Double
)

data class EnhancedMatchInsight(
    val baseMatch: TaskMatchInsight,
    val factors: EnhancedMatchFactors,
    val compositeScore: Double
)

const val HIGH_ROUTE_MATCH_PERCENT = 75
private val GEO_REGEX = Regex("""(-?\d{1,2}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)""")
private val MAPS_REGEX = Regex("""@(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)""")

fun extractGeoPoint(raw: String): GeoPoint? {
    val input = raw.trim()
    if (input.isBlank()) return null

    val direct = GEO_REGEX.find(input)
    if (direct != null) {
        val lat = direct.groupValues[1].toDoubleOrNull()
        val lng = direct.groupValues[2].toDoubleOrNull()
        if (lat != null && lng != null) return GeoPoint(lat, lng)
    }

    val maps = MAPS_REGEX.find(input)
    if (maps != null) {
        val lat = maps.groupValues[1].toDoubleOrNull()
        val lng = maps.groupValues[2].toDoubleOrNull()
        if (lat != null && lng != null) return GeoPoint(lat, lng)
    }

    val queryIdx = input.indexOf("q=")
    if (queryIdx >= 0) {
        val tail = input.substring(queryIdx + 2)
        val query = tail.substringBefore("&")
        val queryMatch = GEO_REGEX.find(query)
        if (queryMatch != null) {
            val lat = queryMatch.groupValues[1].toDoubleOrNull()
            val lng = queryMatch.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null) return GeoPoint(lat, lng)
        }
    }
    return null
}

/** Carrier's commute path: a straight segment from origin to destination. Empty if either end is missing. */
internal fun commuteCorridor(entry: CommuteEntry): List<GeoPoint> {
    val origin = entry.originLocation ?: return emptyList()
    val destination = entry.destinationLocation ?: return emptyList()
    return listOf(origin, destination)
}

/** Sum of pickup + dropoff nearest-point distances to the carrier's corridor. Null if corridor incomplete or task has no coords. */
internal fun corridorDeviationKm(task: Task, entry: CommuteEntry): Double? {
    val corridor = commuteCorridor(entry)
    if (corridor.size < 2) return null
    val pickup = task.pickupLocation ?: return null
    val dropoff = task.dropoffLocation ?: return null
    val segments = corridor.zipWithNext()
    val pickupDev = segments.minOf { (a, b) -> pointToSegmentKm(pickup, a, b) }
    val dropoffDev = segments.minOf { (a, b) -> pointToSegmentKm(dropoff, a, b) }
    return pickupDev + dropoffDev
}

internal fun taskRoutePoints(task: Task): List<GeoPoint> {
    val pickup = task.pickupLocation ?: return emptyList()
    val dropoff = task.dropoffLocation ?: return emptyList()
    return listOf(pickup, dropoff)
}

internal fun routeOverlapPercent(task: Task, entry: CommuteEntry): Int? {
    val averageDeviationKm = averageRouteDeviationKm(task, entry) ?: return null
    return matchPercentForDeviationKm(averageDeviationKm)
}

internal fun averageRouteDeviationKm(task: Task, entry: CommuteEntry): Double? {
    val corridor = commuteCorridor(entry)
    if (corridor.size < 2) return null
    val routePoints = taskRoutePoints(task)
    if (routePoints.isEmpty()) return null
    val segments = corridor.zipWithNext()
    return routePoints
        .map { point -> segments.minOf { (a, b) -> pointToSegmentKm(point, a, b) } }
        .average()
}

internal fun matchPercentForDeviationKm(deviationKm: Double): Int {
    if (deviationKm <= 3.0) return 100
    if (deviationKm >= 20.0) return 0
    val breakpoints = listOf(
        3.0 to 100.0,
        5.0 to 90.0,
        8.0 to 75.0,
        12.0 to 60.0,
        20.0 to 35.0
    )
    val segment = breakpoints.zipWithNext().firstOrNull { (start, end) ->
        deviationKm in start.first..end.first
    } ?: return 0
    val (start, end) = segment
    val progress = (deviationKm - start.first) / (end.first - start.first)
    return (start.second + (end.second - start.second) * progress)
        .roundToInt()
        .coerceIn(0, 100)
}

fun findBestMatch(
    task: Task,
    schedule: List<CommuteEntry>,
    nowMillis: Long = System.currentTimeMillis()
): TaskMatchInsight? {
    if (schedule.isEmpty()) return null
    return schedule.mapNotNull { computeMatch(task, it) }.maxByOrNull { it.score }
}

fun calculateEnhancedMatch(
    task: Task,
    schedule: List<CommuteEntry>,
    preferences: PreferenceProfile,
    nowMillis: Long = System.currentTimeMillis()
): EnhancedMatchInsight? {
    val baseMatch = findBestMatch(task, schedule, nowMillis) ?: return null
    val factors = EnhancedMatchFactors(
        routeScore = baseMatch.matchPercent / 100.0,
        categoryScore = categoryPreferenceScore(task, preferences),
        priceScore = pricePreferenceScore(task, preferences)
    )
    val compositeScore = (
        factors.routeScore * 0.65 +
            factors.categoryScore * 0.20 +
            factors.priceScore * 0.15
        ).coerceIn(0.0, 1.0)
    return EnhancedMatchInsight(
        baseMatch = baseMatch,
        factors = factors,
        compositeScore = compositeScore
    )
}

fun enhancedMatchReason(insight: EnhancedMatchInsight?): String? {
    if (insight == null) return null
    val reasons = mutableListOf<String>()
    if (insight.factors.categoryScore >= 0.95) reasons += "preferred category"
    if (insight.factors.priceScore >= 0.95) reasons += "good price fit"
    return if (reasons.isEmpty()) {
        insight.baseMatch.reason
    } else {
        "${insight.baseMatch.reason} · ${reasons.joinToString(", ")}"
    }
}

fun countMatchingCarriers(
    task: Task,
    carriers: List<User>,
    nowMillis: Long = System.currentTimeMillis()
): Int = carriers.count { carrier ->
    carrier.userId != task.requesterId &&
        (findBestMatch(task, carrier.commuteSchedule, nowMillis)?.matchPercent ?: 0) >= HIGH_ROUTE_MATCH_PERCENT
}

private fun computeMatch(task: Task, entry: CommuteEntry): TaskMatchInsight? {
    if (entry.direction != task.direction) return null
    val routeOffsetKm = averageRouteDeviationKm(task, entry) ?: return null
    val matchPercent = matchPercentForDeviationKm(routeOffsetKm)
    val reason = if (routeOffsetKm < 0.2) {
        "Right on your route"
    } else {
        String.format(Locale.US, "About %.1f km off your route", routeOffsetKm)
    }
    return TaskMatchInsight(
        score = matchPercent / 100.0,
        matchPercent = matchPercent,
        routeOffsetKm = routeOffsetKm,
        scheduleSummary = entry.scheduleDaySummary(),
        reason = reason,
        corridorMatch = matchPercent >= HIGH_ROUTE_MATCH_PERCENT
    )
}

private fun categoryPreferenceScore(task: Task, preferences: PreferenceProfile): Double {
    if (preferences.sampleSize < 3) return 0.5
    return if (task.category in preferences.topCategories) 1.0 else 0.5
}

private fun pricePreferenceScore(task: Task, preferences: PreferenceProfile): Double {
    val median = preferences.medianAcceptedPriceHkd ?: return 0.5
    if (preferences.sampleSize < 3 || median <= 0.0) return 0.5
    return when {
        task.offeredPrice >= median * 0.9 -> 1.0
        task.offeredPrice >= median * 0.7 -> 0.75
        else -> 0.45
    }
}

private fun haversineKm(a: GeoPoint, b: GeoPoint): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * earthRadiusKm * asin(sqrt(h))
}

/**
 * Shortest distance (km) from p to segment a→b.
 * Uses equirectangular local-plane projection; error < 0.5% across the HK/SZ bbox.
 */
internal fun pointToSegmentKm(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
    val earthRadiusKm = 6371.0
    val refLat = Math.toRadians((a.latitude + b.latitude) / 2.0)
    fun xy(g: GeoPoint): Pair<Double, Double> = Pair(
        Math.toRadians(g.longitude) * cos(refLat) * earthRadiusKm,
        Math.toRadians(g.latitude) * earthRadiusKm
    )
    val (px, py) = xy(p)
    val (ax, ay) = xy(a)
    val (bx, by) = xy(b)
    val dx = bx - ax
    val dy = by - ay
    val lenSq = dx * dx + dy * dy
    if (lenSq == 0.0) return haversineKm(p, a)
    val t = (((px - ax) * dx) + ((py - ay) * dy)) / lenSq
    val tClamped = t.coerceIn(0.0, 1.0)
    val cx = ax + tClamped * dx
    val cy = ay + tClamped * dy
    val diffX = px - cx
    val diffY = py - cy
    return sqrt(diffX * diffX + diffY * diffY)
}
