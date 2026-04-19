package com.group2.movi.domain.model

import com.google.firebase.firestore.GeoPoint
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class TaskMatchInsight(
    val score: Double,
    val detourMinutes: Int,
    val minutesBeforeDeadline: Int,
    val scheduleSummary: String,
    val reason: String,
    val corridorMatch: Boolean = false
)

private const val DEFAULT_SPEED_KMH = 24.0
private const val SERVICE_BUFFER_MINUTES = 10
private const val CORRIDOR_BUFFER_KM = 5.0 // combined pickup + dropoff deviation budget
private val GEO_REGEX = Regex("""(-?\d{1,2}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)""")
private val MAPS_REGEX = Regex("""@(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)""")

private val portCenters = mapOf(
    CrossingPort.FUTIAN to GeoPoint(22.5154, 114.0638),
    CrossingPort.LO_WU to GeoPoint(22.5289, 114.1133),
    CrossingPort.HUANGGANG to GeoPoint(22.5201, 114.0450),
    CrossingPort.LOK_MA_CHAU to GeoPoint(22.5089, 114.0731),
    CrossingPort.HEUNG_YUEN_WAI to GeoPoint(22.5486, 114.1688)
)

fun portCenter(port: String): GeoPoint = portCenters[port] ?: portCenters.getValue(CrossingPort.FUTIAN)

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

fun estimateDetourMinutes(task: Task): Int {
    val origin = task.pickupLocation ?: portCenter(task.crossingPort)
    val destination = task.dropoffLocation ?: portCenter(task.crossingPort)
    val port = portCenter(task.crossingPort)
    val totalKm = haversineKm(origin, port) + haversineKm(destination, port)
    return ((totalKm / DEFAULT_SPEED_KMH) * 60.0 + SERVICE_BUFFER_MINUTES).roundToInt().coerceAtLeast(8)
}

/** Carrier's commute path: origin → port → destination. Empty if either end is missing. */
internal fun commuteCorridor(entry: CommuteEntry): List<GeoPoint> {
    val origin = entry.originLocation ?: return emptyList()
    val destination = entry.destinationLocation ?: return emptyList()
    return listOf(origin, portCenter(entry.port), destination)
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

/** Detour estimate based on the real corridor. Null when carrier has no origin/destination yet. */
fun estimateCorridorDetourMinutes(task: Task, entry: CommuteEntry): Int? {
    val dev = corridorDeviationKm(task, entry) ?: return null
    // ×2 because the carrier drives off-corridor then back; + buffer for handoff time.
    return ((dev * 2 / DEFAULT_SPEED_KMH) * 60.0 + SERVICE_BUFFER_MINUTES)
        .roundToInt()
        .coerceAtLeast(SERVICE_BUFFER_MINUTES)
}

fun findBestMatch(
    task: Task,
    schedule: List<CommuteEntry>,
    nowMillis: Long = System.currentTimeMillis()
): TaskMatchInsight? {
    if (schedule.isEmpty()) return null
    return schedule.mapNotNull { computeMatch(task, it, nowMillis) }.maxByOrNull { it.score }
}

fun countMatchingCarriers(
    task: Task,
    carriers: List<User>,
    nowMillis: Long = System.currentTimeMillis()
): Int = carriers.count { carrier ->
    carrier.userId != task.requesterId &&
        findBestMatch(task, carrier.commuteSchedule, nowMillis) != null
}

fun taskMarkerPoint(task: Task): GeoPoint = task.pickupLocation ?: portCenter(task.crossingPort)

private fun computeMatch(
    task: Task,
    entry: CommuteEntry,
    nowMillis: Long
): TaskMatchInsight? {
    if (entry.port != task.crossingPort || entry.direction != task.direction) return null
    val deadline = task.requiredBefore ?: return null
    val departure = nextDeparture(entry, nowMillis) ?: return null
    val minutesBeforeDeadline = Duration.between(
        departure.toInstant(),
        deadline.toDate().toInstant()
    ).toMinutes().toInt()
    if (minutesBeforeDeadline < -15) return null

    val corridorDev = corridorDeviationKm(task, entry)
    val corridorDetour = estimateCorridorDetourMinutes(task, entry)
    val corridorActive = corridorDev != null && corridorDetour != null
    // Hard filter: when carrier has a full corridor, tasks that sit too far off it are excluded.
    if (corridorActive && corridorDev!! > CORRIDOR_BUFFER_KM) return null

    val detourMinutes = corridorDetour ?: estimateDetourMinutes(task)
    if (detourMinutes > 120) return null

    val timeScore = when {
        minutesBeforeDeadline in 45..180 -> 1.0
        minutesBeforeDeadline in 15..44 -> 0.8
        minutesBeforeDeadline in 0..14 -> 0.65
        minutesBeforeDeadline in -15..-1 -> 0.35
        else -> 0.2
    }
    val detourScore = (1.0 - detourMinutes / 120.0).coerceIn(0.0, 1.0)
    val urgencyBoost = if (task.isUrgent) 0.08 else 0.0
    val corridorBoost = if (corridorActive) 0.05 else 0.0
    val score = (timeScore * 0.6) + (detourScore * 0.32) + urgencyBoost + corridorBoost
    val deadlineLabel = when {
        minutesBeforeDeadline >= 60 -> "${minutesBeforeDeadline / 60}h before deadline"
        minutesBeforeDeadline >= 0 -> "$minutesBeforeDeadline min before deadline"
        else -> "${-minutesBeforeDeadline} min late risk"
    }
    val reason = if (corridorActive) {
        "Within your commute corridor · ~$detourMinutes min detour · $deadlineLabel"
    } else {
        "Via ${CrossingPort.label(entry.port)} · ~$detourMinutes min detour · $deadlineLabel"
    }

    return TaskMatchInsight(
        score = score,
        detourMinutes = detourMinutes,
        minutesBeforeDeadline = minutesBeforeDeadline,
        scheduleSummary = "${entry.dayOfWeek} ${entry.departureTime}",
        reason = reason,
        corridorMatch = corridorActive
    )
}

private fun nextDeparture(entry: CommuteEntry, nowMillis: Long): ZonedDateTime? {
    val day = runCatching { DayOfWeek.valueOf(entry.dayOfWeek) }.getOrNull() ?: return null
    val time = runCatching { LocalTime.parse(entry.departureTime) }.getOrNull() ?: return null
    val zone = ZoneId.systemDefault()
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    var candidate = now.with(TemporalAdjusters.nextOrSame(day))
        .withHour(time.hour)
        .withMinute(time.minute)
        .withSecond(0)
        .withNano(0)
    if (candidate.isBefore(now.minusMinutes(30))) {
        candidate = candidate.plusWeeks(1)
    }
    return candidate
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
