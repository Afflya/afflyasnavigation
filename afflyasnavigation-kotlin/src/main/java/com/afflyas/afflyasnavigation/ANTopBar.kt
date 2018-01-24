package com.afflyas.afflyasnavigation

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.FrameLayout

class ANTopBar : FrameLayout {

    companion object {
        private const val MULTI_WINDOW_TOP_DISTANCE = 300
    }

    private var translucentStatusThemeEnabled: Boolean = false
    private var translucentNavigationThemeEnabled: Boolean = false

    var behaviorTranslationEnabled: Boolean = false
        set(value) {
            field = value
            enableBehaviorTranslation()
        }

    private var topBarBehavior: ANTopBarBehavior<ANTopBar>? = null

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
            val ta = context.obtainStyledAttributes(attrs, R.styleable.ANTopBar_Params, 0, 0)
            try {
                behaviorTranslationEnabled = ta.getBoolean(R.styleable.ANTopBar_Params_tbBehaviorTranslationEnabled, false)
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        /**
         * Translucent navigation available for Android >= LOLLIPOP
         */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            val rect = Rect()
            getWindowVisibleDisplayFrame(rect)
            /**
             * Add padding if needed when element is ready
             */
            if(rect.width() != 0){
                if(!translucentStatusThemeEnabled && !translucentNavigationThemeEnabled){
                    setTranslucentPadding(false)
                    enableBehaviorTranslation()
                }else{
                    /**
                     * addPaddingLeft translucent navigation on left side
                     * addPaddingTop translucent status bar on left side
                     * addPaddingRight translucent navigation on right side
                     */
                    var addPaddingTop = false
                    var addPaddingLeft = false
                    var addPaddingRight = false

                    /**
                     * Top padding to avoid overlapping translucent status bar
                     */
                    if(translucentStatusThemeEnabled){
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            //Not add top padding if app is in bottom window of multi-window mode
                            addPaddingTop = rect.top <= MULTI_WINDOW_TOP_DISTANCE
                        }else{
                            addPaddingTop = true
                        }
                    }
                    /**
                     * Side padding to avoid overlapping translucent navigation
                     * Could be only in single window mode and landscape orientation (except tablets)
                     */
                    if(translucentNavigationThemeEnabled){
                        /**
                         * No side translucent navigation in multi-window or on tablet
                         */
                        if((Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !ANHelper.isInMultiWindow(context)) && !resources.configuration.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)) {
                            /**
                             * Check left side
                             */
                            addPaddingLeft = rect.left != 0
                            if(!addPaddingLeft){
                                /**
                                 * Check right side
                                 */
                                val d = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
                                val realDisplayMetrics = DisplayMetrics()
                                d.getRealMetrics(realDisplayMetrics)
                                val realWidth = realDisplayMetrics.widthPixels
                                /**
                                 * right position of element should not be equal screen size
                                 */
                                addPaddingRight = rect.right != realWidth
                            }
                        }
                    }
                    setTranslucentPadding(addPaddingLeft, addPaddingTop, addPaddingRight)
                    enableBehaviorTranslation()
                }
            }
        }else{
            setTranslucentPadding(false, false, false)
            enableBehaviorTranslation()
            //display above other elements on Android <= LOLLIPOP
            bringToFront()
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * Setup showing/hiding behavior on scrolling
     */
    private fun enableBehaviorTranslation(){
        if (parent is CoordinatorLayout) {
            val params = layoutParams
            if (topBarBehavior == null) {
                topBarBehavior = ANTopBarBehavior<ANTopBar>(behaviorTranslationEnabled)
            } else {
                topBarBehavior!!.behaviorTranslationEnabled = behaviorTranslationEnabled
            }
            (params as CoordinatorLayout.LayoutParams).behavior = topBarBehavior
        }
    }

    /**
     *
     * Set padding to avoid overlapping
     *
     * @param addPaddingLeft translucent navigation on left side
     * @param addPaddingTop translucent status bar on left side
     * @param addPaddingRight translucent navigation on right side
     *
     */
    private fun setTranslucentPadding(addPaddingLeft: Boolean, addPaddingTop: Boolean, addPaddingRight: Boolean){
        setPadding(
                if(addPaddingLeft) resources.getDimensionPixelOffset(R.dimen.navigation_bar_height) else 0,
                if(addPaddingTop) resources.getDimensionPixelOffset(R.dimen.android_status_bar_height) else 0,
                if(addPaddingRight)resources.getDimensionPixelOffset(R.dimen.navigation_bar_height) else 0,
                0)
    }

    private fun setTranslucentPadding(topPadding: Boolean){
        setTranslucentPadding( false, topPadding, false)
    }

    /**
     * Return if the Bottom Navigation is hidden or not
     */
    fun isHidden(): Boolean {
        return if (topBarBehavior != null) {
            topBarBehavior!!.isHidden()
        } else false
    }

    /**
     * Hide Bottom Navigation with animation
     */
    fun hideTopBar() {
        hideTopBar(true)
    }

    /**
     * Hide Bottom Navigation with or without animation
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
     * Restore Bottom Navigation with animation
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