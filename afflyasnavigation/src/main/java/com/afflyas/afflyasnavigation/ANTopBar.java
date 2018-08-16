package com.afflyas.afflyasnavigation;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.DisplayCutout;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

public class ANTopBar extends FrameLayout {

    private boolean translucentStatusThemeEnabled = false;
    private boolean translucentNavigationThemeEnabled = false;

    /**
     * system window inset values that has been set as padding
     */
    private int insetLeft = 0;
    private int insetRight = 0;
    private int insetTop = 0;

    /**
     * indicates that insets were set
     */
    private boolean isInsetsSet = false;

    private ANTopBarBehavior<ANTopBar> topBarBehavior = null;

    private boolean behaviorTranslationEnabled = false;

    private int scrollingDeadZone = VerticalScrollingBehavior.DEFAULT_DEAD_ZONE;

    public void setScrollingDeadZone(int scrollingDeadZone) {
        this.scrollingDeadZone = scrollingDeadZone;
    }

    public ANTopBar(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public ANTopBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ANTopBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        /*
         * xml attributes
         */
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ANTopBar, 0, 0);
            try {
                behaviorTranslationEnabled = ta.getBoolean(R.styleable.ANTopBar_behaviorTranslationEnabled, false);
                scrollingDeadZone = ta.getInteger(R.styleable.ANTopBar_scrollingDeadZone, VerticalScrollingBehavior.DEFAULT_DEAD_ZONE);
            } finally {
                ta.recycle();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            translucentStatusThemeEnabled = ANHelper.isTranslucentStatusThemeEnabled(context);
            translucentNavigationThemeEnabled = ANHelper.isTranslucentNavigationThemeEnabled(context);
        }

        ViewCompat.setElevation(this, getResources().getDimension(R.dimen.top_bar_elevation));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        /*
         * apply padding and setup behavior
         */
        if(!isInsetsSet){
            isInsetsSet = true;

            setPadding(
                    insetLeft,
                    insetTop,
                    insetRight,
                    0);

            setupBehaviorTranslation();
        }
        super.onDraw(canvas);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void calcInsets(){
        int insetLeft = 0;
        int insetRight = 0;
        int insetTop = 0;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            /*
             * P(and later) insets with cutout support
             */
            WindowInsets insets = getRootView().getRootWindowInsets();

            if(translucentStatusThemeEnabled) insetTop = insets.getSystemWindowInsetTop();

            DisplayCutout notch = insets.getDisplayCutout();

            if(ANHelper.isInMultiWindow(getContext())){
                /*
                 *
                 * Set inset when in multi window mode
                 * Only for side with cutout but without navigation
                 *
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
            if(translucentStatusThemeEnabled) insetTop = insets.getSystemWindowInsetTop();
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
            if(translucentStatusThemeEnabled) insetTop = insets.getSystemWindowInsetTop();

            if(translucentNavigationThemeEnabled){
                insetLeft = insets.getSystemWindowInsetLeft();
                insetRight = insets.getSystemWindowInsetRight();
            }
        }else{
            /*
             * Lollipop insets
             */
            if(translucentStatusThemeEnabled) insetTop = getResources().getDimensionPixelOffset(R.dimen.android_status_bar_height);

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
        if(insetLeft != this.insetLeft || insetRight != this.insetRight || insetTop != this.insetTop){
            this.insetLeft = insetLeft;
            this.insetRight = insetRight;
            this.insetTop = insetTop;

            isInsetsSet = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            calcInsets();
        }else{
            /*
             * display top bar above other elements for Kitkat and older
             */
            bringToFront();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Setup showing/hiding behavior on scrolling
     */
    private void setupBehaviorTranslation(){
        if (getParent() instanceof CoordinatorLayout) {
            if (topBarBehavior == null) {
                topBarBehavior = new ANTopBarBehavior<>(behaviorTranslationEnabled);
                topBarBehavior.setScrollingDeadZone(scrollingDeadZone);
            } else {
                topBarBehavior.setBehaviorTranslationEnabled(behaviorTranslationEnabled);
                topBarBehavior.setScrollingDeadZone(scrollingDeadZone);
            }
            ((CoordinatorLayout.LayoutParams)getLayoutParams()).setBehavior(topBarBehavior);
        }
    }

    /**
     * change value and reset behavior
     */
    public void enableBehaviorTranslation(boolean value){
        behaviorTranslationEnabled = value;
        setupBehaviorTranslation();
    }

    /**
     * Return if the Top Bar is hidden or not
     */
    public boolean isHidden() {
        if (topBarBehavior != null) {
            return topBarBehavior.isHidden();
        } else {
            return false;
        }
    }

    /**
     * Hide Top Bar with animation
     */
    public void hideTopBar() {
        hideTopBar(true);
    }

    /**
     * Hide Top Bar with or without animation
     *
     * @param withAnimation Boolean
     */
    public void hideTopBar(boolean withAnimation) {
        if (topBarBehavior != null) {
            topBarBehavior.hideView(this, withAnimation);
        } else {
            ViewCompat.animate(this)
                    .translationY(this.getHeight())
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setDuration((withAnimation ? 300 : 0))
                    .start();
        }
    }

    /**
     * Restore Top Bar with animation
     */
    public void restoreTopBar() {
        restoreTopBar(true);
    }

    /**
     * Restore Top Bar with or without animation
     *
     * @param withAnimation Boolean
     */
    public void restoreTopBar(boolean withAnimation) {
        if (topBarBehavior != null) {
            topBarBehavior.resetOffset(this, withAnimation);
        } else {
            // Show bottom navigation
            ViewCompat.animate(this)
                    .translationY(0f)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setDuration((withAnimation ? 300 : 0))
                    .start();
        }
    }
}
