package com.afflyas.afflyasnavigation.notification

import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.ColorInt
import android.text.TextUtils
import java.util.ArrayList

class ANNotification : Parcelable {

    constructor() {
        // empty
    }

    var text: String? = null
        private set // can be null, so notification will not be shown

    @ColorInt
    var textColor: Int = 0
        private set // if 0 then use default value

    @ColorInt
    var backgroundColor: Int = 0
        private set // if 0 then use default value

    val isEmpty: Boolean
        get() = TextUtils.isEmpty(text)

    private constructor(inputParcel: Parcel) {
        text = inputParcel.readString()
        textColor = inputParcel.readInt()
        backgroundColor = inputParcel.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(text)
        dest.writeInt(textColor)
        dest.writeInt(backgroundColor)
    }

    companion object {

        fun justText(text: String): ANNotification {
            return Builder().setText(text).build()
        }

        fun generateEmptyList(size: Int): ArrayList<ANNotification> {
            val notificationList = ArrayList<ANNotification>()
            for (i in 0 until size) {
                notificationList.add(ANNotification())
            }
            return notificationList
        }

        val CREATOR: Parcelable.Creator<ANNotification> = object : Parcelable.Creator<ANNotification> {
            override fun createFromParcel(inputParcel: Parcel): ANNotification {
                return ANNotification(inputParcel)
            }

            override fun newArray(size: Int): Array<ANNotification?> {
                return arrayOfNulls(size)
            }
        }
    }

    class Builder {
        private var text: String? = null
        @ColorInt
        private var textColor: Int = 0
        @ColorInt
        private var backgroundColor: Int = 0

        fun setText(text: String?): Builder {
            this.text = text
            return this
        }

        fun setTextColor(@ColorInt textColor: Int): Builder {
            this.textColor = textColor
            return this
        }

        fun setBackgroundColor(@ColorInt backgroundColor: Int): Builder {
            this.backgroundColor = backgroundColor
            return this
        }

        fun build(): ANNotification {
            val notification = ANNotification()
            notification.text = text
            notification.textColor = textColor
            notification.backgroundColor = backgroundColor
            return notification
        }
    }


}