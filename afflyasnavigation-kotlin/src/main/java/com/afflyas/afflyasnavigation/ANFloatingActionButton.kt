package com.afflyas.afflyasnavigation

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager

//TODO manage translation behavior for Gravity.TOP(move to top) and Gravity.CENTER_VERTICAL(do not move)
//TODO manage bottom translation for large screens (when little snackbar appears in the middle)
@CoordinatorLayout.DefaultBehavior(ANFloatingActionButtonBehavior::class)
class ANFloatingActionButton : FloatingActionButton {

    private var isInitialized: Boolean = false

    private var isDrawn: Boolean = false

    private var layoutHeight: Int = 0

    private var fabBehavior: ANFloatingActionButtonBehavior<ANFloatingActionButton>? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var androidNavigationState = AndroidNavigationState.NAV_DEFAULT
    private var translucentNavigationThemeEnabled: Boolean = false

    init {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            translucentNavigationThemeEnabled = ANHelper.isTranslucentNavigationThemeEnabled(context)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            isDrawn = false

            val rect = Rect()
            getWindowVisibleDisplayFrame(rect)
            if(rect.width() != 0) {
                val requiredAndroidNavigationState: AndroidNavigationState
                if (translucentNavigationThemeEnabled) {
                    /**
                     * Detecting translucent navigation placement
                     */
                    val d = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
                    val realDisplayMetrics = DisplayMetrics()
                    d.getRealMetrics(realDisplayMetrics)
                    val realHeight = realDisplayMetrics.heightPixels
                    val realWidth = realDisplayMetrics.widthPixels
                    /**
                     * No translucent nav in multi-window mode
                     */
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !ANHelper.isInMultiWindow(context)) {
                        /**
                         * No side translucent nav on tablet
                         */
                        if (resources.configuration.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)) {
                            /**
                             * is there translucent navigation
                             */
                            if (rect.bottom >= realHeight) {
                                requiredAndroidNavigationState = AndroidNavigationState.NAV_DEFAULT
                            } else {
                                requiredAndroidNavigationState = AndroidNavigationState.NAV_TRANSLUCENT_BOTTOM
                            }
                        } else {
                            //check left side
                            if (rect.left != 0) {
                                requiredAndroidNavigationState = AndroidNavigationState.NAV_TRANSLUCENT_LEFT
                                //check right side
                            } else if (rect.right != realWidth) {
                                requiredAndroidNavigationState = AndroidNavigationState.NAV_TRANSLUCENT_RIGHT
                                //check bottom side
                            } else if (rect.bottom >= realHeight) {
                                requiredAndroidNavigationState = AndroidNavigationState.NAV_DEFAULT
                            } else {
                                requiredAndroidNavigationState = AndroidNavigationState.NAV_TRANSLUCENT_BOTTOM
                            }
                        }
                    } else {
                        requiredAndroidNavigationState = AndroidNavigationState.NAV_DEFAULT
                    }
                } else {
                    requiredAndroidNavigationState = AndroidNavigationState.NAV_DEFAULT
                }

                if (androidNavigationState != requiredAndroidNavigationState || !isInitialized) {
                    androidNavigationState = requiredAndroidNavigationState
                    isInitialized = true
                    setupBottomMargin()
                }
            }
        }else{
            if(!isInitialized){
                isInitialized = true
                bringToFront()
                setupBottomMargin()
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        layoutHeight = bottom - top
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if(!isDrawn){
            isDrawn = true
            setupBehaviorTranslation()
        }
    }

    private fun setupBehaviorTranslation(){
        if (parent is CoordinatorLayout) {
            val params = layoutParams
            //TODO add methods to avoid recreating fabBehavior when it is not null
            fabBehavior = ANFloatingActionButtonBehavior(
                    context,
                    androidNavigationState,
                    layoutHeight)
            (params as CoordinatorLayout.LayoutParams).behavior = fabBehavior
        }
    }

    private fun setupBottomMargin(){
        var fabBottomMargin = resources.getDimensionPixelOffset(R.dimen.bottom_navigation_height)
        var fabLeftMargin = 0
        var fabRightMargin = 0

        val params = (layoutParams as CoordinatorLayout.LayoutParams)

        if(androidNavigationState == AndroidNavigationState.NAV_TRANSLUCENT_BOTTOM){
            fabBottomMargin = resources.getDimensionPixelOffset(R.dimen.android_navigation_bar_height) + resources.getDimensionPixelOffset(R.dimen.bottom_navigation_height)
        }else if(androidNavigationState == AndroidNavigationState.NAV_TRANSLUCENT_RIGHT){

            if(params.gravity and Gravity.HORIZONTAL_GRAVITY_MASK != Gravity.LEFT ){
                fabRightMargin = resources.getDimensionPixelOffset(R.dimen.android_navigation_bar_height)
            }

        }else if(androidNavigationState == AndroidNavigationState.NAV_TRANSLUCENT_LEFT){
            if(params.gravity and Gravity.HORIZONTAL_GRAVITY_MASK != Gravity.RIGHT){
                fabLeftMargin = resources.getDimensionPixelOffset(R.dimen.android_navigation_bar_height)
            }
        }

        params.setMargins(fabLeftMargin,0, fabRightMargin, fabBottomMargin)

    }

}