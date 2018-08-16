package com.afflyas.afflyasnavigation;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.view.DisplayCutout;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class ANVerticalContentContainer extends FrameLayout {

    private boolean translucentNavigationThemeEnabled = false;

    /**
     * indicates that insets were set
     */
    private boolean isInsetsSet = false;
    /**
     * system window inset values that has been set as padding
     */
    private int insetLeft = 0;
    private int insetRight = 0;

    public ANVerticalContentContainer(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ANVerticalContentContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ANVerticalContentContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    /**
     * Init
     *
     * @param context
     */
    private void init(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            translucentNavigationThemeEnabled = ANHelper.isTranslucentNavigationThemeEnabled(context);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            calcInsets();
        }
        if(!isInsetsSet){
            isInsetsSet = true;
            setPadding(
                    insetLeft,
                    0,
                    insetRight,
                    0);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void calcInsets(){
        int insetLeft = 0;
        int insetRight = 0;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            /*
             * P(and later) insets with cutout support
             */
            WindowInsets insets = getRootView().getRootWindowInsets();
            DisplayCutout notch = insets.getDisplayCutout();
            if(ANHelper.isInMultiWindow(getContext())){
                /*
                 * Set inset when in multi window mode
                 * Only for side with cutout but without navigation
                 */
                if(notch != null){
                    insetLeft = insets.getSystemWindowInsetLeft();
                    insetRight = insets.getSystemWindowInsetRight();
                    /*
                     * stable insets -insets without notch
                     */
                    if(insets.getStableInsetLeft() != 0) insetLeft = 0;
                    if(insets.getStableInsetRight() != 0) insetRight = 0;
                }
            }else{
                if(translucentNavigationThemeEnabled){
                    insetLeft = insets.getSystemWindowInsetLeft();
                    insetRight = insets.getSystemWindowInsetRight();
                }else{
                    if(notch != null){
                        insetLeft = notch.getSafeInsetLeft();
                        insetRight = notch.getSafeInsetRight();
                        /*
                         * stable insets -insets without notch
                         */
                        if(insets.getStableInsetLeft() != 0) insetLeft = 0;
                        if(insets.getStableInsetRight() != 0) insetRight = 0;
                    }
                }
            }
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            /*
             * Nougat and Oreo insets
             */
            WindowInsets insets = getRootView().getRootWindowInsets();
            /*
             * No insets in multi window mode
             */
            if(translucentNavigationThemeEnabled && !ANHelper.isInMultiWindow(getContext())){
                insetLeft = insets.getSystemWindowInsetLeft();
                insetRight = insets.getSystemWindowInsetRight();
            }
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            /*
             * Marshmallow insets
             */
            WindowInsets insets = getRootView().getRootWindowInsets();

            if(translucentNavigationThemeEnabled){
                insetLeft = insets.getSystemWindowInsetLeft();
                insetRight = insets.getSystemWindowInsetRight();
            }
        }else{
            /*
             * Lollipop insets
             */
            if(translucentNavigationThemeEnabled
                    && ANHelper.hasNavigationBar(getContext())
                    && (!getResources().getConfiguration().isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)
                    && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            ){
                insetRight = getResources().getDimensionPixelOffset(R.dimen.navigation_bar_height);
            }
        }
        /*
         * Check if these values are already set
         */
        if(insetLeft != this.insetLeft || insetRight != this.insetRight){
            this.insetLeft = insetLeft;
            this.insetRight = insetRight;
            isInsetsSet = false;
        }
    }
}
