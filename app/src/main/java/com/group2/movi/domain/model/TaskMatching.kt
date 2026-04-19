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
    val reason: String
)

private const val DEFAULT_SPEED_KMH = 24.0
private const val SERVICE_BUFFER_MINUTES = 10
private const val CORRIDOR_BUFFER_KM = 3.0
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

internal fun pointToSegmentKm(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
    fun toPlanar(point: GeoPoint, refLatRadians: Double): Pair<Double, Double> {
        val x = point.longitude * 111.320 * cos(refLatRadians)
        val y = point.latitude * 110.574
        return x to y
    }

    val refLatRadians = Math.toRadians((p.latitude + a.latitude + b.latitude) / 3.0)
    val (px, py) = toPlanar(p, refLatRadians)
    val (ax, ay) = toPlanar(a, refLatRadians)
    val (bx, by) = toPlanar(b, refLatRadians)
    val abx = bx - ax
    val aby = by - ay
    val abLengthSquared = (abx * abx) + (aby * aby)
    if (abLengthSquared == 0.0) {
        return sqrt(((px - ax) * (px - ax)) + ((py - ay) * (py - ay)))
    }
    val t = (((px - ax) * abx) + ((py - ay) * aby)) / abLengthSquared
    val clamped = t.coerceIn(0.0, 1.0)
    val closestX = ax + (abx * clamped)
    val closestY = ay + (aby * clamped)
    return sqrt(((px - closestX) * (px - closestX)) + ((py - closestY) * (py - closestY)))
}

internal fun commuteCorridor(entry: CommuteEntry): List<GeoPoint> {
    return buildList {
        entry.originLocation?.let(::add)
        add(portCenter(entry.port))
        entry.destinationLocation?.let(::add)
    }
}

internal fun corridorDeviationKm(task: Task, entry: CommuteEntry): Double? {
    val pickup = task.pickupLocation ?: return null
    val dropoff = task.dropoffLocation ?: return null
    val corridor = commuteCorridor(entry)
    val segments = corridor.zipWithNext()
    if (segments.isEmpty()) return null

    fun nearestDistance(point: GeoPoint): Double = segments.minOf { (start, end) ->
        pointToSegmentKm(point, start, end)
    }

    return nearestDistance(pickup) + nearestDistance(dropoff)
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

    val hasCompleteCorridor = entry.originLocation != null && entry.destinationLocation != null
    val corridorDeviation = corridorDeviationKm(task, entry)
    if (hasCompleteCorridor && corridorDeviation != null && corridorDeviation > CORRIDOR_BUFFER_KM) {
        return null
    }

    val corridorDetourMinutes = estimateCorridorDetourMinutes(task, entry)
    val detourMinutes = corridorDetourMinutes ?: estimateDetourMinutes(task)
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
    val score = (timeScore * 0.6) + (detourScore * 0.32) + urgencyBoost
    val deadlineLabel = when {
        minutesBeforeDeadline >= 60 -> "${minutesBeforeDeadline / 60}h before deadline"
        minutesBeforeDeadline >= 0 -> "$minutesBeforeDeadline min before deadline"
        else -> "${-minutesBeforeDeadline} min late risk"
    }
    val reasonPrefix = if (corridorDetourMinutes != null) {
        "Within your commute corridor · ~$detourMinutes min detour"
    } else {
        "Near ${CrossingPort.label(entry.port)} · ~$detourMinutes min fallback"
    }

    return TaskMatchInsight(
        score = score,
        detourMinutes = detourMinutes,
        minutesBeforeDeadline = minutesBeforeDeadline,
        scheduleSummary = "${entry.dayOfWeek} ${entry.departureTime}",
        reason = "$reasonPrefix · $deadlineLabel"
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

private fun estimateCorridorDetourMinutes(task: Task, entry: CommuteEntry): Int? {
    if (entry.originLocation == null || entry.destinationLocation == null) return null
    val deviationKm = corridorDeviationKm(task, entry) ?: return null
    return (((deviationKm * 2) / DEFAULT_SPEED_KMH) * 60.0 + SERVICE_BUFFER_MINUTES)
        .roundToInt()
        .coerceAtLeast(SERVICE_BUFFER_MINUTES)
}
