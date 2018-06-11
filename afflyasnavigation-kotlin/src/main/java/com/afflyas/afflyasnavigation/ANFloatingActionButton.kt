package com.afflyas.afflyasnavigation

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.util.AttributeSet
import android.view.Gravity

//TODO add support for KITKAT's translucent navigation
//TODO manage translation behavior for Gravity.TOP(move to top) and Gravity.CENTER_VERTICAL(do not move)
//TODO manage bottom translation for large screens (when little snackbar appears in the middle)
class ANFloatingActionButton : FloatingActionButton, CoordinatorLayout.AttachedBehavior {

    override fun getBehavior(): ANFloatingActionButtonBehavior<ANFloatingActionButton> {
        return fabBehavior
    }

    private val fabBehavior: ANFloatingActionButtonBehavior<ANFloatingActionButton> = ANFloatingActionButtonBehavior()

    private var translucentNavigationThemeEnabled: Boolean = false

    /**
     * system window insets
     */
    private var insetLeft = 0
    private var insetRight = 0
    private var insetBottom = 0

    /**
     * indicates that insets were set
     */
    private var isInsetsSet = false

    /**
     * layout height for calculate positions in translation behavior
     */
    private var layoutHeight: Int = 0

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            translucentNavigationThemeEnabled = ANHelper.isTranslucentNavigationThemeEnabled(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun calcInsets(){
        var insetLeft = 0
        var insetRight = 0
        var insetBottom = 0

        when{
//            /**
//             * P(or later) insets with cutout support
//             * TODO uncomment these lines if your target sdk version >= 28
//             */
//            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
//                val insets = rootView.rootWindowInsets
//                val notch = insets.displayCutout
//
//                if(ANHelper.isInMultiWindow(context)){
//                    if(notch != null){
//                        insetLeft = insets.systemWindowInsetLeft
//                        insetRight = insets.systemWindowInsetRight
//                        insetBottom = insets.systemWindowInsetBottom
//                        /**
//                         * stable insets -insets without notch
//                         */
//                        if(insets.stableInsetLeft != 0) insetLeft = 0
//                        if(insets.stableInsetRight != 0) insetRight = 0
//                        if(insets.stableInsetBottom != 0) insetBottom = 0
//                    }
//                }else{
//                    if(translucentNavigationThemeEnabled){
//                        insetBottom = insets.systemWindowInsetBottom
//                        insetLeft = insets.systemWindowInsetLeft
//                        insetRight = insets.systemWindowInsetRight
//                    }else{
//                        if(notch != null){
//                            insetLeft = notch.safeInsetLeft
//                            insetRight = notch.safeInsetRight
//                            insetBottom = notch.safeInsetBottom
//                            /**
//                             * stable insets -insets without notch
//                             */
//                            if(insets.stableInsetLeft != 0) insetLeft = 0
//                            if(insets.stableInsetRight != 0) insetRight = 0
//                            if(insets.stableInsetBottom != 0) insetBottom = 0
//                        }
//                    }
//                }
//            }
            /**
             * Nougat and Oreo insets
             */
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                val insets = rootView.rootWindowInsets
                if(translucentNavigationThemeEnabled && !ANHelper.isInMultiWindow(context)){
                    insetBottom = insets.systemWindowInsetBottom
                    insetLeft = insets.systemWindowInsetLeft
                    insetRight = insets.systemWindowInsetRight
                }
            }
            /**
             * Marshmallow insets
             */
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                val insets = rootView.rootWindowInsets
                if(translucentNavigationThemeEnabled){
                    insetBottom = insets.systemWindowInsetBottom
                    insetLeft = insets.systemWindowInsetLeft
                    insetRight = insets.systemWindowInsetRight
                }
            }
            /**
             * Lollipop insets
             */
            else -> {
                if(translucentNavigationThemeEnabled && ANHelper.hasNavigationBar(context)){
                    if(resources.configuration.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)){
                        insetBottom = context.resources.getDimensionPixelOffset(R.dimen.navigation_bar_height)
                    }else{
                        if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                            insetRight = context.resources.getDimensionPixelOffset(R.dimen.navigation_bar_height)
                        }else{
                            insetBottom = context.resources.getDimensionPixelOffset(R.dimen.navigation_bar_height)
                        }
                    }
                }
            }
        }
        /**
         * Check if these values are already set
         */
        if(insetLeft != this.insetLeft || insetRight != this.insetRight || insetBottom != this.insetBottom){
            this.insetLeft = insetLeft
            this.insetRight = insetRight
            this.insetBottom = insetBottom

            isInsetsSet = false
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            calcInsets()
        }else{
            /**
             * display bottom bar above other elements for Kitkat and older
             */
            bringToFront()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        if(!isInsetsSet){
            isInsetsSet = true

            applyInsets()

            setupBehaviorTranslation()

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                bringToFront()
            }
        }
        super.onDraw(canvas)
    }

    /**
     * Setup behavior on scrolling
     */
    private fun setupBehaviorTranslation(){
        if (parent is CoordinatorLayout) {
            val params = layoutParams
            if(!fabBehavior.behaviorTranslationInitialized){
                fabBehavior.updateTranslationBehavior(
                        context,
                        insetBottom,
                        layoutHeight)
                (params as CoordinatorLayout.LayoutParams).behavior = fabBehavior
            }
        }
    }



    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        layoutHeight = bottom - top
    }

    private fun applyInsets(){
        var fabBottomMargin = 0
        var fabLeftMargin = 0
        var fabRightMargin = 0
        var fabTopMargin = 0

        val params = (layoutParams as CoordinatorLayout.LayoutParams)

        val gravity = params.gravity

        /**
         * add top margin when gravity.TOP (coming later)
         *
         * add bottom margin when gravity.BOTTOM
         *
         * do nothing in other cases
         */
        when(gravity and Gravity.VERTICAL_GRAVITY_MASK){
//            Gravity.TOP -> {
//                Log.d("development", "VERTICAL GRAVITY: TOP")
//                //TODO: Handle Gravity.TOP
//            }
            Gravity.BOTTOM -> {
                fabBottomMargin = insetBottom + resources.getDimensionPixelOffset(R.dimen.bottom_navigation_height)
            }
        }

        /**
         * add left margin when Gravity.START(or Gravity.LEFT)
         *
         * add right margin when Gravity.END(or Gravity.RIGHT)
         *
         * in other cases add both left and right
         */
        when(gravity and Gravity.HORIZONTAL_GRAVITY_MASK){
            Gravity.START, Gravity.LEFT -> {
                //Log.d("development", "HORIZONTAL GRAVITY: START")
                fabLeftMargin = insetLeft
                //Добавить слева
            }
            Gravity.END, Gravity.RIGHT -> {
                //Log.d("development", "HORIZONTAL GRAVITY: END")
                fabRightMargin = insetRight
                //Добавить справа
            }
            else -> {
                //Log.d("development", "HORIZONTAL GRAVITY: CENTER_HORIZONTAL")
                fabLeftMargin = insetLeft
                fabRightMargin = insetRight
                //Добавитьс слева и справа, чтобы было по центру
            }
        }
        params.setMargins(fabLeftMargin,fabTopMargin, fabRightMargin, fabBottomMargin)
    }
}