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
        ),
        BadgeDefinition(
            id = "wet_feather",
            title = "Wet Feather",
            largeResId = R.drawable.badge_wet_feather_large
        ),
        BadgeDefinition(
            id = "100th",
            title = "100th",
            largeResId = R.drawable.badge_100th_large
        ),
        BadgeDefinition(
            id = "200th",
            title = "200th",
            largeResId = R.drawable.badge_200th_large
        )
    )

    fun badgeForId(id: String?): BadgeDefinition? =
        badges.firstOrNull { it.id == id?.trim()?.lowercase() }

    fun normalizeBadgeId(id: String?): String? =
        badgeForId(id)?.id
}
