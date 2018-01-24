package com.afflyas.afflyasnavigation

/**
 * Possible states of android navigation bar
 * NAV_DEFAULT - navigation bar is not displayed (device has physical buttons or something)
 * NAV_TRANSLUCENT_BOTTOM - navigation bar on bottom side
 * NAV_TRANSLUCENT_LEFT - navigation bar on left side (when landscape)
 * NAV_TRANSLUCENT_RIGHT - navigation bar on right side (when landscape)
 */
enum class AndroidNavigationState {
    NAV_DEFAULT,
    NAV_TRANSLUCENT_BOTTOM,
    NAV_TRANSLUCENT_LEFT,
    NAV_TRANSLUCENT_RIGHT,
}