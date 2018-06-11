package com.afflyas.afflyasnavigation

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.util.AttributeSet
import android.widget.FrameLayout


class ANTopBar : FrameLayout {

    private var translucentStatusThemeEnabled: Boolean = false
    private var translucentNavigationThemeEnabled: Boolean = false

    /**
     *
     * system window inset values that has been set as padding
     *
     */
    private var insetLeft = 0
    private var insetRight = 0
    private var insetTop = 0

    /**
     * indicates that insets were set
     */
    private var isInsetsSet = false

    private var topBarBehavior: ANTopBarBehavior<ANTopBar>? = null

    var behaviorTranslationEnabled: Boolean = false
        private set


    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {

        /**
         * xml attributes
         */
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.ANTopBar, 0, 0)
            try {
                behaviorTranslationEnabled = ta.getBoolean(R.styleable.ANTopBar_ANTopBar_BehaviorTranslationEnabled, false)
            } finally {
                ta.recycle()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            translucentStatusThemeEnabled = ANHelper.isTranslucentStatusThemeEnabled(context)
            translucentNavigationThemeEnabled = ANHelper.isTranslucentNavigationThemeEnabled(context)
        }

        ViewCompat.setElevation(this, resources.getDimension(R.dimen.top_bar_elevation))

    }

    override fun onDraw(canvas: Canvas?) {
        /**
         * apply padding and setup behavior
         */
        if(!isInsetsSet){
            isInsetsSet = true

            setPadding(
                    insetLeft,
                    insetTop,
                    insetRight,
                    0)

            setupBehaviorTranslation()
        }
        super.onDraw(canvas)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun calcInsets(){
        var insetLeft = 0
        var insetRight = 0
        var insetTop = 0

        when{
//        /**
//         * P(and later) insets with cutout support
//         * TODO uncomment these lines if your target sdk version >= 28
//         */
//            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
//                val insets = rootView.rootWindowInsets
//
//                if(translucentStatusThemeEnabled) insetTop = insets.systemWindowInsetTop
//
//                val notch = insets.displayCutout
//
//                if(ANHelper.isInMultiWindow(context)){
//                    /**
//                     *
//                     * Set inset when in multi window mode
//                     * Only for side with cutout but without navigation
//                     *
//                     */
//                    if(notch != null){
//                        insetLeft = insets.systemWindowInsetLeft
//                        insetRight = insets.systemWindowInsetRight
//                        /**
//                         * stable insets -insets without notch
//                         */
//                        if(insets.stableInsetLeft != 0) insetLeft = 0
//                        if(insets.stableInsetRight != 0) insetRight = 0
//                    }
//                }else{
//                    if(translucentNavigationThemeEnabled){
//                        insetLeft = insets.systemWindowInsetLeft
//                        insetRight = insets.systemWindowInsetRight
//                    }else{
//                        if(notch != null){
//                            insetLeft = notch.safeInsetLeft
//                            insetRight = notch.safeInsetRight
//                            /**
//                             * stable insets -insets without notch
//                             */
//                            if(insets.stableInsetLeft != 0) insetLeft = 0
//                            if(insets.stableInsetRight != 0) insetRight = 0
//                        }
//                    }
//                }
//            }
        /**
         * Nougat and Oreo insets
         */
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                val insets = rootView.rootWindowInsets
                if(translucentStatusThemeEnabled) insetTop = insets.systemWindowInsetTop
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
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                val insets = rootView.rootWindowInsets
                if(translucentStatusThemeEnabled) insetTop = insets.systemWindowInsetTop

                if(translucentNavigationThemeEnabled){
                    insetLeft = insets.systemWindowInsetLeft
                    insetRight = insets.systemWindowInsetRight
                }
            }
        /**
         * Lollipop insets
         */
            else -> {
                if(translucentStatusThemeEnabled) insetTop = resources.getDimensionPixelOffset(R.dimen.android_status_bar_height)

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
        if(insetLeft != this.insetLeft || insetRight != this.insetRight || insetTop != this.insetTop){
            this.insetLeft = insetLeft
            this.insetRight = insetRight
            this.insetTop = insetTop

            isInsetsSet = false
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            calcInsets()
        }else{
            /**
             * display top bar above other elements for Kitkat and older
             */
            bringToFront()
        }
    }

    /**
     * Setup showing/hiding behavior on scrolling
     */
    private fun setupBehaviorTranslation(){
        //Log.d("development", "setupBehaviorTranslation")
        if (parent is CoordinatorLayout) {
            if (topBarBehavior == null) {
                topBarBehavior = ANTopBarBehavior(behaviorTranslationEnabled)
            } else {
                topBarBehavior!!.behaviorTranslationEnabled = behaviorTranslationEnabled
            }
            (layoutParams as CoordinatorLayout.LayoutParams).behavior = topBarBehavior
        }
    }

    /**
     * change value and reset behavior
     */
    fun enableBehaviorTranslation(value: Boolean){
        behaviorTranslationEnabled = value
        setupBehaviorTranslation()
    }

    /**
     * Return if the Top Bar is hidden or not
     */
    fun isHidden(): Boolean {
        return if (topBarBehavior != null) {
            topBarBehavior!!.isHidden()
        } else false
    }

    /**
     * Hide Top Bar with animation
     */
    fun hideTopBar() {
        hideTopBar(true)
    }

    /**
     * Hide Top Bar with or without animation
     *
     * @param withAnimation Boolean
     */
    fun hideTopBar(withAnimation: Boolean) {
        if (topBarBehavior != null) {
            topBarBehavior!!.hideView(this, withAnimation)
        } else {
            ViewCompat.animate(this)
                    .translationY(this.height.toFloat())
                    .setInterpolator(LinearOutSlowInInterpolator())
                    .setDuration((if (withAnimation) 300 else 0).toLong())
                    .start()
        }
    }

    /**
     * Restore Top Bar with animation
     */
    fun restoreTopBar() {
        restoreTopBar(true)
    }

    /**
     * Restore Top Bar with or without animation
     *
     * @param withAnimation Boolean
     */
    fun restoreTopBar(withAnimation: Boolean) {
        if (topBarBehavior != null) {
            topBarBehavior!!.resetOffset(this, withAnimation)
        } else {
            // Show bottom navigation
            ViewCompat.animate(this)
                    .translationY(0f)
                    .setInterpolator(LinearOutSlowInInterpolator())
                    .setDuration((if (withAnimation) 300 else 0).toLong())
                    .start()
        }
    }

}