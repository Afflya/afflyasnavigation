package com.afflyas.afflyasnavigation;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class ANSpaceTop extends FrameLayout {

    private int topBarHeightMode = 1;

    private boolean translucentStatusThemeEnabled = false;

    /**
     *
     * system window inset values that has been set as padding
     *
     */
    private int insetTop = 0;

    /**
     * indicates that insets were set
     */
    private boolean isInsetsSet = false;

    public ANSpaceTop(@NonNull Context context) {
        super(context);
        init(context,null);
    }

    public ANSpaceTop(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    public ANSpaceTop(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }

    /**
     * Init
     *
     * @param context
     */
    private void init(Context context, @Nullable AttributeSet attrs) {
        /*
         * xml attributes
         */
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ANSpaceTop, 0, 0);
            try {
                topBarHeightMode = ta.getInt(R.styleable.ANSpaceTop_topBarHeightMode, 1);
            } finally {
                ta.recycle();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            translucentStatusThemeEnabled = ANHelper.isTranslucentStatusThemeEnabled(context);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            calcInsets();
        }
        if(!isInsetsSet){
            isInsetsSet = true;
            switch(topBarHeightMode){
                case 1: setPadding(0, insetTop + (int)getResources().getDimension(R.dimen.space_top_only_action_bar), 0, 0);
                    break;
                case 2: setPadding(0, insetTop + (int)getResources().getDimension(R.dimen.space_top_action_bar_with_tabs), 0, 0);
                    break;
                case 3: setPadding(0, insetTop, 0, 0);
                    break;
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void calcInsets(){
        int insetTop = 0;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            WindowInsets insets = getRootView().getRootWindowInsets();
            if(translucentStatusThemeEnabled) insetTop = insets.getSystemWindowInsetTop();
        }else{
            if(translucentStatusThemeEnabled) insetTop = getResources().getDimensionPixelOffset(R.dimen.android_status_bar_height);
        }
        /*
         * Check if these values are already set
         */
        if(insetTop != this.insetTop){
            this.insetTop = insetTop;
            isInsetsSet = false;
        }
    }

}
