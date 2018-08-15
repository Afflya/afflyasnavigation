package com.afflyas.afflyasnavigation

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

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
     * Add space to avoid bottom navigation
     */
    var withANBottomNavigation = false
        private set

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

    constructor(context: Context) : super(context){
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr){
        init(context, attrs)
    }

    /**
     * Init
     *
     * @param context
     */
    private fun init(context: Context, attrs: AttributeSet?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            translucentNavigationThemeEnabled = ANHelper.isTranslucentNavigationThemeEnabled(context)
        }

        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.ANFloatingActionButton, 0, 0)
            try {
                withANBottomNavigation = ta.getBoolean(R.styleable.ANFloatingActionButton_withANBottomNavigation, false)
            } finally {
                ta.recycle()
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun calcInsets(){
        var insetLeft = 0
        var insetRight = 0
        var insetBottom = 0

        when{
            /**
             * P(or later) insets with cutout support
             */
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                val insets = rootView.rootWindowInsets
                val notch = insets.displayCutout

                if(ANHelper.isInMultiWindow(context)){
                    if(notch != null){
                        insetLeft = insets.systemWindowInsetLeft
                        insetRight = insets.systemWindowInsetRight
                        insetBottom = insets.systemWindowInsetBottom
                        /**
                         * stable insets -insets without notch
                         */
                        if(insets.stableInsetLeft != 0) insetLeft = 0
                        if(insets.stableInsetRight != 0) insetRight = 0
                        if(insets.stableInsetBottom != 0) insetBottom = 0
                    }
                }else{
                    if(translucentNavigationThemeEnabled){
                        insetBottom = insets.systemWindowInsetBottom
                        insetLeft = insets.systemWindowInsetLeft
                        insetRight = insets.systemWindowInsetRight
                    }else{
                        if(notch != null){
                            insetLeft = notch.safeInsetLeft
                            insetRight = notch.safeInsetRight
                            insetBottom = notch.safeInsetBottom
                            /**
                             * stable insets -insets without notch
                             */
                            if(insets.stableInsetLeft != 0) insetLeft = 0
                            if(insets.stableInsetRight != 0) insetRight = 0
                            if(insets.stableInsetBottom != 0) insetBottom = 0
                        }
                    }
                }
            }
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

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            calcInsets()
        }else{
            /**
             * display bottom bar above other elements for Kitkat and older
             */
            bringToFront()
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
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
            if(!fabBehavior.isBehaviorTranslationInitialized){
                fabBehavior.updateTranslationBehavior(
                        context,
                        insetBottom,
                        layoutHeight,
                        withANBottomNavigation)
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
                fabBottomMargin = insetBottom
                if(withANBottomNavigation){
                    fabBottomMargin =+ resources.getDimensionPixelOffset(R.dimen.bottom_navigation_height)
                }
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