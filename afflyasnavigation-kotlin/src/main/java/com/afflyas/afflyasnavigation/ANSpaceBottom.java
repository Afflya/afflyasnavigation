package com.afflyas.afflyasnavigation;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.DisplayCutout;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class ANSpaceBottom extends FrameLayout {

    private boolean translucentNavigationThemeEnabled = false;

    /**
     * Add space to avoid bottom navigation
     */
    private boolean withANBottomNavigation = false;

    /**
     * indicates that insets were set
     */
    private boolean isInsetsSet = false;
    /**
     * system window inset values that has been set as padding
     */
    private int insetBottom = 0;

    public ANSpaceBottom(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public ANSpaceBottom(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ANSpaceBottom(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * Init
     *
     * @param context
     */
    private void init(Context context, @Nullable AttributeSet attrs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            translucentNavigationThemeEnabled = ANHelper.isTranslucentNavigationThemeEnabled(context);
        }
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ANSpaceBottom, 0, 0);
            try {
                withANBottomNavigation = ta.getBoolean(R.styleable.ANSpaceBottom_withANBottomNavigation, false);
            } finally {
                ta.recycle();
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void calcInsets(){
        int insetBottom = 0;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            /*
             * P(or later) insets with cutout support
             */
            WindowInsets insets = getRootView().getRootWindowInsets();
            DisplayCutout notch = insets.getDisplayCutout();

            if(ANHelper.isInMultiWindow(getContext())){
                if(notch != null){
                    insetBottom = insets.getSystemWindowInsetBottom();
                    /*
                     * stable insets - insets without notch
                     */
                    if(insets.getStableInsetBottom() != 0) insetBottom = 0;
                }
            }else{
                if(translucentNavigationThemeEnabled){
                    insetBottom = insets.getSystemWindowInsetBottom();
                }else{
                    if(notch != null){
                        insetBottom = notch.getSafeInsetBottom();
                        /*
                         * stable insets - insets without notch
                         */
                        if(insets.getStableInsetBottom() != 0) insetBottom = 0;
                    }
                }
            }
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            /*
             * Nougat and Oreo insets
             */
            WindowInsets insets = getRootView().getRootWindowInsets();
            if(translucentNavigationThemeEnabled && !ANHelper.isInMultiWindow(getContext())){
                insetBottom = insets.getSystemWindowInsetBottom();
            }
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            /*
             * Marshmallow insets
             */
            WindowInsets insets = getRootView().getRootWindowInsets();
            if(translucentNavigationThemeEnabled){
                insetBottom = insets.getSystemWindowInsetBottom();
            }
        }else{
            /*
             * Lollipop insets
             */
            if(translucentNavigationThemeEnabled && ANHelper.hasNavigationBar(getContext())){
                if(getResources().getConfiguration().isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)){
                    insetBottom = getResources().getDimensionPixelOffset(R.dimen.navigation_bar_height);
                }else{
                    if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
                        insetBottom = getResources().getDimensionPixelOffset(R.dimen.navigation_bar_height);
                    }
                }
            }
        }

        /*
         * Check if these values are already set
         */
        if(insetBottom != this.insetBottom){
            this.insetBottom = insetBottom;
            isInsetsSet = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            calcInsets();
        }
        if(!isInsetsSet){
            isInsetsSet = true;
            if(withANBottomNavigation){
                setPadding(0, 0, 0, insetBottom + (int)getResources().getDimension(R.dimen.bottom_navigation_height));
            }else{
                setPadding(0, 0, 0, insetBottom);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
