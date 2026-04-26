package com.group2.movi.domain.model

enum class ComplianceAlertType {
    PROHIBITED_ITEM,
    DECLARATION_REQUIRED,
    DOCUMENTATION_REQUIRED,
    VALUE_WARNING,
    INSURANCE_RECOMMENDATION
}

enum class ComplianceSeverity {
    BLOCKING,
    WARNING,
    INFO
}

data class ComplianceAlert(
    val type: ComplianceAlertType,
    val severity: ComplianceSeverity,
    val title: String,
    val message: String,
    val suggestedAction: String
)

private data class ComplianceRule(
    val declarationThresholdHkd: Double? = null,
    val requiresDocumentation: Boolean = false,
    val warningMessage: String? = null
)

private val PROHIBITED_KEYWORDS = listOf(
    "weapon", "gun", "knife", "ammo", "ammunition", "explosive", "bomb", "firework",
    "drug", "cocaine", "heroin", "marijuana", "cannabis",
    "counterfeit", "fake", "replica gun", "ivory", "rhino horn", "tiger",
    "firearm", "live animal", "fresh meat", "cash over", "cash",
    "武器", "枪", "刀具", "炸药", "毒品", "大麻", "可卡因", "海洛因",
    "仿冒", "假货", "象牙", "犀牛角", "虎骨", "活体动物", "鲜肉", "现金"
)

private val CATEGORY_RULES = mapOf(
    TaskCategory.FOOD to ComplianceRule(
        declarationThresholdHkd = 1000.0,
        warningMessage = "Food items can trigger customs declaration requirements, especially fresh or temperature-sensitive goods."
    ),
    TaskCategory.MEDICINE to ComplianceRule(
        declarationThresholdHkd = 1500.0,
        requiresDocumentation = true,
        warningMessage = "Medicine often requires supporting documentation such as a prescription or purchase proof."
    ),
    TaskCategory.PARCEL to ComplianceRule(
        declarationThresholdHkd = 4000.0,
        warningMessage = "High-value parcels may attract customs duties or additional inspection."
    ),
    TaskCategory.DOCUMENT to ComplianceRule(
        warningMessage = "Important documents are usually allowed, but confidential materials still carry handling risk."
    ),
    TaskCategory.OTHER to ComplianceRule(
        declarationThresholdHkd = 2000.0,
        warningMessage = "Unclassified items should be declared when value or contents are unclear."
    )
)

object ComplianceChecker {

    fun checkTaskCompliance(task: Task): List<ComplianceAlert> {
        val prohibitedAlert = checkProhibited(task)
        if (prohibitedAlert != null) return listOf(prohibitedAlert)

        val alerts = mutableListOf<ComplianceAlert>()
        val rule = CATEGORY_RULES[task.category]
        val declaredValue = task.declaredItemValueHkd ?: task.offeredPrice.takeIf { it > 0.0 }

        rule?.warningMessage?.let { warning ->
            alerts += ComplianceAlert(
                type = ComplianceAlertType.DECLARATION_REQUIRED,
                severity = ComplianceSeverity.INFO,
                title = "Customs notice",
                message = warning,
                suggestedAction = "Double-check the item details before handoff."
            )
        }

        if (rule?.requiresDocumentation == true) {
            alerts += ComplianceAlert(
                type = ComplianceAlertType.DOCUMENTATION_REQUIRED,
                severity = ComplianceSeverity.WARNING,
                title = "Supporting documents recommended",
                message = "This category commonly needs documentation at the border.",
                suggestedAction = "Ask the requester to share documentation proof in chat before pickup."
            )
        }

        if (rule?.declarationThresholdHkd != null && declaredValue != null && declaredValue > rule.declarationThresholdHkd) {
            alerts += ComplianceAlert(
                type = ComplianceAlertType.VALUE_WARNING,
                severity = ComplianceSeverity.WARNING,
                title = "Declaration or tax risk",
                message = "Declared value HK$${declaredValue.toInt()} is above the usual threshold for ${task.category.lowercase()}.",
                suggestedAction = "Confirm who handles duties and declare the item if required."
            )
        }

        if (declaredValue != null && declaredValue >= 3000.0) {
            alerts += ComplianceAlert(
                type = ComplianceAlertType.INSURANCE_RECOMMENDATION,
                severity = ComplianceSeverity.INFO,
                title = "Insurance recommended",
                message = "This item is high value and may benefit from extra protection.",
                suggestedAction = "Clarify insurance or compensation expectations before accepting."
            )
        }

        return alerts
    }

    private fun checkProhibited(task: Task): ComplianceAlert? {
        val haystack = listOf(task.title, task.description).joinToString(" ").lowercase()
        val hit = PROHIBITED_KEYWORDS.firstOrNull { keyword ->
            haystack.contains(keyword.lowercase())
        } ?: return null
        return ComplianceAlert(
            type = ComplianceAlertType.PROHIBITED_ITEM,
            severity = ComplianceSeverity.BLOCKING,
            title = "Potential prohibited item",
            message = "The item description contains \"$hit\", which may indicate prohibited cross-border goods.",
            suggestedAction = "Edit the listing or remove the prohibited item before posting."
        )
    }
}
