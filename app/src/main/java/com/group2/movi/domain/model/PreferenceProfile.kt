package com.group2.movi.domain.model

import com.google.firebase.Timestamp

data class PreferenceProfile(
    val userId: String = "",
    val topCategories: List<String> = emptyList(),
    val medianAcceptedPriceHkd: Double? = null,
    val sampleSize: Int = 0,
    val lastUpdated: Timestamp? = null
) {
    companion object {
        fun empty(userId: String) = PreferenceProfile(userId = userId)
    }
}

enum class InteractionAction {
    VIEW,
    ACCEPT,
    COMPLETE
}

data class InteractionContext(
    val category: String = TaskCategory.PARCEL,
    val price: Double = 0.0,
    val matchPercent: Int = 0
)

data class UserInteraction(
    val userId: String = "",
    val taskId: String = "",
    val action: String = InteractionAction.VIEW.name,
    val taskCategory: String = TaskCategory.PARCEL,
    val taskPrice: Double = 0.0,
    val matchPercent: Int = 0,
    val timestamp: Timestamp? = null
) {
    val parsedAction: InteractionAction
        get() = runCatching { InteractionAction.valueOf(action) }
            .getOrDefault(InteractionAction.VIEW)
}
