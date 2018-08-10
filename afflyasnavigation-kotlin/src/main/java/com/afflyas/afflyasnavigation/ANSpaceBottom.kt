package com.afflyas.afflyasnavigation

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RequiresApi

class ANSpaceBottom : FrameLayout{

    private var translucentNavigationThemeEnabled: Boolean = false

    var withANBottomNavigation = true
        private set

    /**
     * indicates that insets were set
     */
    private var isInsetsSet = false
    /**
     * system window inset values that has been set as padding
     */
    private var insetBottom = 0

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
            val ta = context.obtainStyledAttributes(attrs, R.styleable.ANSpaceBottom, 0, 0)
            try {
                withANBottomNavigation = ta.getBoolean(R.styleable.ANSpaceBottom_withANBottomNavigation, true)
            } finally {
                ta.recycle()
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun calcInsets(){
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
                        insetBottom = insets.systemWindowInsetBottom
                        /**
                         * stable insets -insets without notch
                         */
                        if(insets.stableInsetBottom != 0) insetBottom = 0
                    }
                }else{
                    if(translucentNavigationThemeEnabled){
                        insetBottom = insets.systemWindowInsetBottom
                    }else{
                        if(notch != null){
                            insetBottom = notch.safeInsetBottom
                            /**
                             * stable insets -insets without notch
                             */
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
                }
            }
        /**
         * Marshmallow insets
         */
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val insets = rootView.rootWindowInsets
                if(translucentNavigationThemeEnabled){
                    insetBottom = insets.systemWindowInsetBottom
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
                        if(resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE){
                            insetBottom = context.resources.getDimensionPixelOffset(R.dimen.navigation_bar_height)
                        }
                    }
                }
            }
        }
        /**
         * Check if these values are already set
         */
        if(insetBottom != this.insetBottom){
            this.insetBottom = insetBottom
            isInsetsSet = false
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            calcInsets()
        }
        if(!isInsetsSet){
            isInsetsSet = true
            if(withANBottomNavigation){
                setPadding(0, 0, 0, insetBottom + resources.getDimension(R.dimen.bottom_navigation_height).toInt())
            }else{
                setPadding(0, 0, 0, insetBottom)
            }

        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}