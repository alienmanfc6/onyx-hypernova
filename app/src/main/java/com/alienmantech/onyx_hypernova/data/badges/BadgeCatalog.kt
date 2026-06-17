package com.alienmantech.onyx_hypernova.data.badges

import androidx.annotation.DrawableRes
import com.alienmantech.onyx_hypernova.R

data class BadgeDefinition(
    val id: String,
    val title: String,
    @DrawableRes val largeResId: Int
)

object BadgeCatalog {
    val badges: List<BadgeDefinition> = listOf(
        BadgeDefinition(
            id = "night",
            title = "Night",
            largeResId = R.drawable.badge_night_large
        ),
        BadgeDefinition(
            id = "trimless",
            title = "Trimless",
            largeResId = R.drawable.badge_trimless_large
        )
    )

    fun badgeForId(id: String?): BadgeDefinition? =
        badges.firstOrNull { it.id == id?.trim()?.lowercase() }

    fun normalizeBadgeId(id: String?): String? =
        badgeForId(id)?.id
}
