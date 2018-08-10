package com.afflyas.afflyasnavigation

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RequiresApi

/**
 *
 * FrameLayout that adds left and right padding
 * based on the presence of a display cutout and translucent navigation bar
 *
 */
class ANVerticalContentContainer : FrameLayout {

    private var translucentNavigationThemeEnabled: Boolean = false

    /**
     * indicates that insets were set
     */
    private var isInsetsSet = false
    /**
     * system window inset values that has been set as padding
     */
    private var insetLeft = 0
    private var insetRight = 0

    constructor(context: Context) : super(context){
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr){
        init(context)
    }

    /**
     * Init
     *
     * @param context
     */
    private fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            translucentNavigationThemeEnabled = ANHelper.isTranslucentNavigationThemeEnabled(context)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            calcInsets()
        }
        if(!isInsetsSet){
            isInsetsSet = true
            setPadding(
                    insetLeft,
                    0,
                    insetRight,
                    0)
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun calcInsets(){
        var insetLeft = 0
        var insetRight = 0

        when{
        /**
         * P(and later) insets with cutout support
         */
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                val insets = rootView.rootWindowInsets

                val notch = insets.displayCutout

                if(ANHelper.isInMultiWindow(context)){
                    /**
                     *
                     * Set inset when in multi window mode
                     * Only for side with cutout but without navigation
                     *
                     */
                    if(notch != null){
                        insetLeft = insets.systemWindowInsetLeft
                        insetRight = insets.systemWindowInsetRight
                        /**
                         * stable insets -insets without notch
                         */
                        if(insets.stableInsetLeft != 0) insetLeft = 0
                        if(insets.stableInsetRight != 0) insetRight = 0
                    }
                }else{
                    if(translucentNavigationThemeEnabled){
                        insetLeft = insets.systemWindowInsetLeft
                        insetRight = insets.systemWindowInsetRight
                    }else{
                        if(notch != null){
                            insetLeft = notch.safeInsetLeft
                            insetRight = notch.safeInsetRight
                            /**
                             * stable insets -insets without notch
                             */
                            if(insets.stableInsetLeft != 0) insetLeft = 0
                            if(insets.stableInsetRight != 0) insetRight = 0
                        }
                    }
                }
            }
        /**
         * Nougat and Oreo insets
         */
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                val insets = rootView.rootWindowInsets
                /**
                 * No insets in multi window mode
                 */
                if(translucentNavigationThemeEnabled && !ANHelper.isInMultiWindow(context)){
                    insetLeft = insets.systemWindowInsetLeft
                    insetRight = insets.systemWindowInsetRight
                }
            }
        /**
         * Marshmallow insets
         */
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val insets = rootView.rootWindowInsets

                if(translucentNavigationThemeEnabled){
                    insetLeft = insets.systemWindowInsetLeft
                    insetRight = insets.systemWindowInsetRight
                }
            }
        /**
         * Lollipop insets
         */
            else -> {
                if(translucentNavigationThemeEnabled
                        && ANHelper.hasNavigationBar(context)
                        && (!resources.configuration.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)
                                && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                ){
                    insetRight = context.resources.getDimensionPixelOffset(R.dimen.navigation_bar_height)
                }
            }
        }
        /**
         * Check if these values are already set
         */
        if(insetLeft != this.insetLeft || insetRight != this.insetRight){
            this.insetLeft = insetLeft
            this.insetRight = insetRight
            isInsetsSet = false
        }
    }
}