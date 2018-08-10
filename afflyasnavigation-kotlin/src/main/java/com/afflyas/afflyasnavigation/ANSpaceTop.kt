package com.afflyas.afflyasnavigation

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RequiresApi

class ANSpaceTop : FrameLayout {

    private var topBarHeightMode = 1

    private var translucentStatusThemeEnabled: Boolean = false

    /**
     *
     * system window inset values that has been set as padding
     *
     */
    private var insetTop = 0

    /**
     * indicates that insets were set
     */
    private var isInsetsSet = false

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
        /**
         * xml attributes
         */
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.ANSpaceTop, 0, 0)
            try {
                topBarHeightMode = ta.getInt(R.styleable.ANSpaceTop_topBarHeightMode, 1)
            } finally {
                ta.recycle()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            translucentStatusThemeEnabled = ANHelper.isTranslucentStatusThemeEnabled(context)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            calcInsets()
        }
        if(!isInsetsSet){
            isInsetsSet = true
            when(topBarHeightMode){
                1 -> setPadding(0, insetTop + resources.getDimension(R.dimen.space_top_only_action_bar).toInt(), 0, 0)
                2 -> setPadding(0, insetTop + resources.getDimension(R.dimen.space_top_action_bar_with_tabs).toInt(), 0, 0)
                3 -> setPadding(0, insetTop, 0, 0)
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun calcInsets(){
        var insetTop = 0

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val insets = rootView.rootWindowInsets
            if(translucentStatusThemeEnabled) insetTop = insets.systemWindowInsetTop
        }else{
            if(translucentStatusThemeEnabled) insetTop = resources.getDimensionPixelOffset(R.dimen.android_status_bar_height)
        }
        /**
         * Check if these values are already set
         */
        if(insetTop != this.insetTop){
            this.insetTop = insetTop
            isInsetsSet = false
        }
    }
}