package com.afflyas.afflyasnavigation.notification

import androidx.annotation.ColorInt

object ANNotificationHelper {

    /**
     * Get text color for given notification. If color is not set (0), returns default value.
     *
     * @param notification     AHNotification, non null
     * @param defaultTextColor int default text color for all notifications
     * @return - text color for given notification. If 0 then default
     */
    fun getTextColor(notification: ANNotification, @ColorInt defaultTextColor: Int): Int {
        val textColor = notification.textColor
        return if (textColor == 0) defaultTextColor else textColor
    }

    /**
     * Get background color for given notification. If color is not set (0), returns default value.
     *
     * @param notification           AHNotification, non null
     * @param defaultBackgroundColor int default background color for all notifications
     * @return - background color for given notification. If 0 then default
     */
    fun getBackgroundColor(notification: ANNotification, @ColorInt defaultBackgroundColor: Int): Int {
        val backgroundColor = notification.backgroundColor
        return if (backgroundColor == 0) defaultBackgroundColor else backgroundColor
    }

}