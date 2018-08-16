package com.afflyas.afflyasnavigation;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.WindowInsets;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

public class ANFloatingActionButton extends FloatingActionButton implements CoordinatorLayout.AttachedBehavior {

    @NonNull
    @Override
    public CoordinatorLayout.Behavior getBehavior() {
        return fabBehavior;
    }

    private ANFloatingActionButtonBehavior<ANFloatingActionButton> fabBehavior = new ANFloatingActionButtonBehavior<>();

    private boolean translucentNavigationThemeEnabled = false;

    /**
     * Add space to avoid bottom navigation
     */
    private boolean withANBottomNavigation = false;

    /**
     * system window insets
     */
    private int insetLeft = 0;
    private int insetRight = 0;
    private int insetBottom = 0;

    /**
     * indicates that insets were set
     */
    private boolean isInsetsSet = false;

    /**
     * layout height for calculate positions in translation behavior
     */
    private int layoutHeight = 0;

    public ANFloatingActionButton(Context context) {
        super(context);
        init(context,null);
    }

    public ANFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    public ANFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
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
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ANFloatingActionButton, 0, 0);
            try {
                withANBottomNavigation = ta.getBoolean(R.styleable.ANFloatingActionButton_withANBottomNavigation, false);
            } finally {
                ta.recycle();
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void calcInsets(){
        int insetLeft = 0;
        int insetRight = 0;
        int insetBottom = 0;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            /*
             * P(or later) insets with cutout support
             */
            WindowInsets insets = getRootView().getRootWindowInsets();
            DisplayCutout notch = insets.getDisplayCutout();

            if(ANHelper.isInMultiWindow(getContext())){
                if(notch != null){
                    insetLeft = insets.getSystemWindowInsetLeft();
                    insetRight = insets.getSystemWindowInsetRight();
                    insetBottom = insets.getSystemWindowInsetBottom();
                    /*
                     * stable insets -insets without notch
                     */
                    if(insets.getStableInsetLeft() != 0) insetLeft = 0;
                    if(insets.getStableInsetRight() != 0) insetRight = 0;
                    if(insets.getStableInsetBottom() != 0) insetBottom = 0;
                }
            }else{
                if(translucentNavigationThemeEnabled){
                    insetBottom = insets.getSystemWindowInsetBottom();
                    insetLeft = insets.getSystemWindowInsetLeft();
                    insetRight = insets.getSystemWindowInsetRight();
                }else{
                    if(notch != null){
                        insetLeft = notch.getSafeInsetLeft();
                        insetRight = notch.getSafeInsetRight();
                        insetBottom = notch.getSafeInsetBottom();
                        /*
                         * stable insets -insets without notch
                         */
                        if(insets.getStableInsetLeft() != 0) insetLeft = 0;
                        if(insets.getStableInsetRight() != 0) insetRight = 0;
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
                insetLeft = insets.getSystemWindowInsetLeft();
                insetRight = insets.getSystemWindowInsetRight();
            }
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            /*
             * Marshmallow insets
             */
            WindowInsets insets = getRootView().getRootWindowInsets();
            if(translucentNavigationThemeEnabled){
                insetBottom = insets.getSystemWindowInsetBottom();
                insetLeft = insets.getSystemWindowInsetLeft();
                insetRight = insets.getSystemWindowInsetRight();
            }
        }else{
            /*
             * Lollipop insets
             */
            if(translucentNavigationThemeEnabled && ANHelper.hasNavigationBar(getContext())){
                if(getResources().getConfiguration().isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)){
                    insetBottom = getResources().getDimensionPixelOffset(R.dimen.navigation_bar_height);
                }else{
                    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        insetRight = getResources().getDimensionPixelOffset(R.dimen.navigation_bar_height);
                    }else{
                        insetBottom = getResources().getDimensionPixelOffset(R.dimen.navigation_bar_height);
                    }
                }
            }
        }
        /*
         * Check if these values are already set
         */
        if(insetLeft != this.insetLeft || insetRight != this.insetRight || insetBottom != this.insetBottom){
            this.insetLeft = insetLeft;
            this.insetRight = insetRight;
            this.insetBottom = insetBottom;

            isInsetsSet = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            calcInsets();
        }else{
            /*
             * display bottom bar above other elements for Kitkat and older
             */
            bringToFront();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(!isInsetsSet){
            isInsetsSet = true;

            applyInsets();

            setupBehaviorTranslation();

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                bringToFront();
            }
        }
        super.onDraw(canvas);
    }

    /**
     * Setup behavior on scrolling
     */
    private void setupBehaviorTranslation(){
        if (getParent() instanceof CoordinatorLayout) {
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) getLayoutParams();
            if(!fabBehavior.isBehaviorTranslationInitialized()){
                fabBehavior.updateTranslationBehavior(
                        getContext(),
                        insetBottom,
                        layoutHeight,
                        withANBottomNavigation);
                params.setBehavior(fabBehavior);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        layoutHeight = bottom - top;
    }

    private void applyInsets(){
        int fabBottomMargin = 0;
        int fabLeftMargin = 0;
        int fabRightMargin = 0;
        int fabTopMargin = 0;

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)getLayoutParams();

        int gravity = params.gravity;

        /*
         * add bottom margin when gravity.BOTTOM
         */
        if((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM){
            fabBottomMargin = insetBottom;
            if(withANBottomNavigation){
                fabBottomMargin =+ getResources().getDimensionPixelOffset(R.dimen.bottom_navigation_height);
            }
        }

        /*
         * add left margin when Gravity.START(or Gravity.LEFT)
         *
         * add right margin when Gravity.END(or Gravity.RIGHT)
         *
         * in other cases add both left and right
         */
        switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK){
            case Gravity.START:
            case Gravity.LEFT:
                fabLeftMargin = insetLeft;
                break;
            case Gravity.END:
            case Gravity.RIGHT:
                fabRightMargin = insetRight;
                break;
            default:
                fabLeftMargin = insetLeft;
                fabRightMargin = insetRight;
                break;
        }

        params.setMargins(fabLeftMargin,fabTopMargin, fabRightMargin, fabBottomMargin);
    }

}
