package com.afflyas.afflyasnavigation

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.DrawableCompat

object ANHelper {


    /**
     * Check if app is in multi window mode
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun isInMultiWindow(context: Context): Boolean {
        val d = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

        val realDisplayMetrics = DisplayMetrics()
        d.getRealMetrics(realDisplayMetrics)

        val realHeight = realDisplayMetrics.heightPixels
        val realWidth = realDisplayMetrics.widthPixels

        val displayMetrics = DisplayMetrics()
        d.getMetrics(displayMetrics)

        val displayHeight = displayMetrics.heightPixels
        val displayWidth = displayMetrics.widthPixels

        return realWidth - displayWidth > realWidth / 5 || realHeight - displayHeight > realHeight / 5
    }

    /**
     *
     * Check if device has navigation bar
     *
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun hasNavigationBar(context: Context): Boolean {
        val d = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

        val realDisplayMetrics = DisplayMetrics()
        d.getRealMetrics(realDisplayMetrics)

        val realHeight = realDisplayMetrics.heightPixels
        val realWidth = realDisplayMetrics.widthPixels

        val displayMetrics = DisplayMetrics()
        d.getMetrics(displayMetrics)

        val displayHeight = displayMetrics.heightPixels
        val displayWidth = displayMetrics.widthPixels

        return realWidth > displayWidth || realHeight > displayHeight
    }

    /**
     * Check if translucent navigation enabled in the theme
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun isTranslucentNavigationThemeEnabled(context: Context): Boolean{
        val themeAttrs = intArrayOf(android.R.attr.windowTranslucentNavigation)
        val typedValue = context.theme.obtainStyledAttributes(themeAttrs)
        val translucentThemeEnabled: Boolean
        try {
            translucentThemeEnabled = typedValue.getBoolean(0, false)
        } finally {
            typedValue.recycle()
        }
        return translucentThemeEnabled
    }

    /**
     * Check if translucent status enabled in the theme
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun isTranslucentStatusThemeEnabled(context: Context): Boolean{
        val themeAttrs = intArrayOf(android.R.attr.windowTranslucentStatus)
        val typedValue = context.theme.obtainStyledAttributes(themeAttrs)
        val translucentStatusEnabled: Boolean
        try {
            translucentStatusEnabled = typedValue.getBoolean(0, false)
        } finally {
            typedValue.recycle()
        }
        return translucentStatusEnabled
    }

    /**
     * Return a tint drawable
     *
     * @param drawable
     * @param color
     * @param forceTint
     * @return
     */
    fun getTintDrawable(drawable: Drawable, @ColorInt color: Int, forceTint: Boolean): Drawable {
        if (forceTint) {
            drawable.clearColorFilter()
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            drawable.invalidateSelf()
            return drawable
        }
        val wrapDrawable = DrawableCompat.wrap(drawable).mutate()
        DrawableCompat.setTint(wrapDrawable, color)
        return wrapDrawable
    }

    /**
     * Update top margin with animation
     */
    fun updateTopMargin(view: View, fromMargin: Int, toMargin: Int) {
        val animator = ValueAnimator.ofFloat(fromMargin.toFloat(), toMargin.toFloat())
        animator.duration = 150
        animator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Float
            if (view.layoutParams is ViewGroup.MarginLayoutParams) {
                val p = view.layoutParams as ViewGroup.MarginLayoutParams
                p.setMargins(p.leftMargin, animatedValue.toInt(), p.rightMargin, p.bottomMargin)
                view.requestLayout()
            }
        }
        animator.start()
    }

    /**
     * Update bottom margin with animation
     */
    fun updateBottomMargin(view: View, fromMargin: Int, toMargin: Int, duration: Int) {
        val animator = ValueAnimator.ofFloat(fromMargin.toFloat(), toMargin.toFloat())
        animator.duration = duration.toLong()
        animator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Float
            if (view.layoutParams is ViewGroup.MarginLayoutParams) {
                val p = view.layoutParams as ViewGroup.MarginLayoutParams
                p.setMargins(p.leftMargin, p.topMargin, p.rightMargin, animatedValue.toInt())
                view.requestLayout()
            }
        }
        animator.start()
    }

    /**
     * Update left margin with animation
     */
    fun updateLeftMargin(view: View, fromMargin: Int, toMargin: Int) {
        val animator = ValueAnimator.ofFloat(fromMargin.toFloat(), toMargin.toFloat())
        animator.duration = 150
        animator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Float
            if (view.layoutParams is ViewGroup.MarginLayoutParams) {
                val p = view.layoutParams as ViewGroup.MarginLayoutParams
                p.setMargins(animatedValue.toInt(), p.topMargin, p.rightMargin, p.bottomMargin)
                view.requestLayout()
            }
        }
        animator.start()
    }

    /**
     * Update text size with animation
     */
    fun updateTextSize(textView: TextView, fromSize: Float, toSize: Float) {
        val animator = ValueAnimator.ofFloat(fromSize, toSize)
        animator.duration = 150
        animator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Float
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, animatedValue)
        }
        animator.start()
    }

    /**
     * Update alpha
     */
    fun updateAlpha(view: View, fromValue: Float, toValue: Float) {
        val animator = ValueAnimator.ofFloat(fromValue, toValue)
        animator.duration = 150
        animator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Float
            view.alpha = animatedValue
        }
        animator.start()
    }

    /**
     * Update text color with animation
     */
    fun updateTextColor(textView: TextView, @ColorInt fromColor: Int,
                        @ColorInt toColor: Int) {
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        colorAnimation.duration = 150
        colorAnimation.addUpdateListener { valueAnimator -> textView.setTextColor(valueAnimator.animatedValue as Int) }
        colorAnimation.start()
    }

    /**
     * Update text color with animation
     */
    fun updateViewBackgroundColor(view: View, @ColorInt fromColor: Int,
                                  @ColorInt toColor: Int) {
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        colorAnimation.duration = 150
        colorAnimation.addUpdateListener { valueAnimator -> view.setBackgroundColor(valueAnimator.animatedValue as Int) }
        colorAnimation.start()
    }

    /**
     * Update image view color with animation
     */
    fun updateDrawableColor(drawable: Drawable, imageView: ImageView,
                            @ColorInt fromColor: Int, @ColorInt toColor: Int,
                            forceTint: Boolean) {
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        colorAnimation.duration = 150
        colorAnimation.addUpdateListener { valueAnimator ->
            imageView.setImageDrawable(getTintDrawable(drawable,
                    valueAnimator.animatedValue as Int, forceTint))
            imageView.requestLayout()
        }
        colorAnimation.start()
    }

    /**
     * Update width
     */
    fun updateWidth(view: View, fromWidth: Float, toWidth: Float) {
        val animator = ValueAnimator.ofFloat(fromWidth, toWidth)
        animator.duration = 150
        animator.addUpdateListener { valueAnimator ->
            val params = view.layoutParams
            params.width = Math.round(valueAnimator.animatedValue as Float)
            view.layoutParams = params
        }
        animator.start()
    }

}