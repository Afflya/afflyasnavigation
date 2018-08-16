package com.afflyas.afflyasnavigation;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

public class ANFloatingActionButtonBehavior <V extends View> extends CoordinatorLayout.Behavior<V> {

    /**
     * system window inset values that has been set as padding
     */
    private int insetBottom = 0;

    private int bottomNavigationHeight = 0;
    //fab margin from bottom of screen
    private int fabMarginBottom = 0;

    private int displayHeight = 0;
    //Y coordinate of TranslucentNavigationBar's top
    private int translucentBarY = 0;

    private float initNavigationPosition = 0f;
    private float initFABPosition = 0f;
    private float initSnackBarPosition = 0f;

    private float currentNavigationPosition = 0f;

    /**
     * Current Y coordinate of Snack
     * Set to 0 when Snack is hidden ->
     */
    private float currentSnackBarPosition = 0f;

    public boolean isBehaviorTranslationInitialized() {
        return behaviorTranslationInitialized;
    }

    private boolean behaviorTranslationInitialized = false;


    private boolean withBottomNavigation = false;

    public void updateTranslationBehavior(Context context, int insetBottom, int layoutHeight, boolean withBottomNavigation){

        this.insetBottom = insetBottom;

        if(withBottomNavigation) bottomNavigationHeight = context.getResources().getDimensionPixelOffset(R.dimen.bottom_navigation_height);
        this.withBottomNavigation = withBottomNavigation;

        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        if(wm == null) return;

        Display display = wm.getDefaultDisplay();

        DisplayMetrics displayMetrics = new DisplayMetrics();

        /*
         * Y coordinates calculating differently on android <= LOLLIPOP
         */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && insetBottom > 0){
            display.getRealMetrics(displayMetrics);
            displayHeight = displayMetrics.heightPixels;
            translucentBarY = displayHeight - insetBottom;
            fabMarginBottom = bottomNavigationHeight + insetBottom;

            initFABPosition = (displayHeight - bottomNavigationHeight - insetBottom - layoutHeight);
            initSnackBarPosition = displayHeight;

        }else{
            display.getMetrics(displayMetrics);
            displayHeight = displayMetrics.heightPixels;
            translucentBarY = displayHeight;
            fabMarginBottom = bottomNavigationHeight;

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                initFABPosition = (displayHeight - bottomNavigationHeight - layoutHeight);
                initSnackBarPosition = displayHeight;
            }
        }

        behaviorTranslationInitialized = true;
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull V child, @NonNull View dependency) {
        if(behaviorTranslationInitialized){

            /*
             * set init FAB position for Pre LOLLIPOP devices
             */
            if(initFABPosition == 0f && child.getY() != 0f){
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    initFABPosition = child.getY() - bottomNavigationHeight;
                }
            }

            if(dependency instanceof ANBottomNavigation){
                if(initNavigationPosition == 0f && dependency.getY() != 0f){
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                        initNavigationPosition = (displayHeight - bottomNavigationHeight - insetBottom);
                    }else{
                        initNavigationPosition = dependency.getY() - bottomNavigationHeight;
                    }
                    currentNavigationPosition = dependency.getY();
                }
                return true;
            }else if(dependency instanceof Snackbar.SnackbarLayout){
                if(initSnackBarPosition == 0f && dependency.getY() != 0f){
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams)dependency.getLayoutParams();
                                initSnackBarPosition = dependency.getY() + p.bottomMargin;
                        currentSnackBarPosition = initSnackBarPosition;
                    }
                }
                return true;
            }
        }
        return super.layoutDependsOn(parent, child, dependency);
    }

    @Override
    public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, @NonNull V child, @NonNull View dependency) {
        if(behaviorTranslationInitialized){
            updateFloatingActionButton(child, dependency);
        }
        return super.onDependentViewChanged(parent, child, dependency);
    }

    /**
     * Update floating action button Y coordinate
     */
    private void updateFloatingActionButton(V child, View dependency) {

        if(dependency instanceof ANBottomNavigation){
            currentNavigationPosition = dependency.getY();
            /*
             * Handle FAB only when Snack is hidden
             */
            if(currentSnackBarPosition == 0f){
                float navPosOffset = initNavigationPosition - currentNavigationPosition;
                if(insetBottom > 0){
                    if(currentNavigationPosition > translucentBarY){
                        child.setY(initFABPosition + bottomNavigationHeight);
                    }else{
                        child.setY(initFABPosition - navPosOffset);
                    }
                }else{
                    child.setY(initFABPosition - navPosOffset);
                }
            }
        }else if(dependency instanceof Snackbar.SnackbarLayout){
            currentSnackBarPosition = dependency.getY();

            float snackPosOffset = initSnackBarPosition - currentSnackBarPosition;
            float navPosOffset = initNavigationPosition - currentNavigationPosition;

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                if(withBottomNavigation){
                    /*
                     * Has Bottom Navigation
                     */
                    if (currentSnackBarPosition < currentNavigationPosition) {
                        /*
                         * Snack above bottom navigation
                         */
                        child.setY(initFABPosition + bottomNavigationHeight + bottomNavigationHeight - snackPosOffset);
                    }
                }else{
                    /*
                     * No Bottom Navigation
                     */
                    child.setY(initFABPosition - snackPosOffset);
                }
            }else{
                if(insetBottom > 0){
                    /*
                     * Translucent navigation enabled
                     */
                    if(withBottomNavigation){
                        /*
                         * Has Bottom Navigation
                         */
                        if (currentNavigationPosition > translucentBarY) {
                            /*
                             * Bottom navigation below android navigation bar
                             */
                            if (currentSnackBarPosition > translucentBarY) {
                                /*
                                 * Snack below android navigation bar
                                 */
                                child.setY(initFABPosition + bottomNavigationHeight);
                            }else{
                                /*
                                 * Snack above android navigation bar
                                 */
                                child.setY(initFABPosition + fabMarginBottom - snackPosOffset);
                            }
                        } else {
                            /*
                             * Bottom navigation above android navigation bar
                             */
                            if (currentSnackBarPosition > currentNavigationPosition) {
                                /*
                                 * Snack below bottom navigation
                                 */
                                child.setY(initFABPosition - navPosOffset);
                            }else{
                                /*
                                 * Snack above bottom navigation
                                 */
                                child.setY(initFABPosition + fabMarginBottom - snackPosOffset);
                            }
                        }
                    }else{
                        /*
                         * No Bottom Navigation
                         */
                        if (currentSnackBarPosition > translucentBarY) {
                            /*
                             * Snack below android navigation bar
                             */
                            child.setY(initFABPosition + bottomNavigationHeight);
                        }else{
                            /*
                             * Snack above android navigation bar
                             */
                            child.setY(initFABPosition + fabMarginBottom - snackPosOffset);
                        }
                    }
                }else{
                    /*
                     * Translucent navigation disabled
                     */
                    if(withBottomNavigation){
                        /*
                         * Has Bottom Navigation
                         */
                        if (currentSnackBarPosition > currentNavigationPosition) {
                            /*
                             * Snack below bottom navigation
                             */
                            child.setY(initFABPosition - navPosOffset);
                        }else{
                            /*
                             * Snack above bottom navigation
                             */
                            child.setY(initFABPosition + fabMarginBottom - snackPosOffset);
                        }
                    }else{
                        /*
                         * No Bottom Navigation
                         */
                        child.setY(initFABPosition + fabMarginBottom - snackPosOffset);
                    }
                }
            }
        }
    }

    /**
     * set FAB position when Snack disappears
     */
    @Override
    public void onDependentViewRemoved(@NonNull CoordinatorLayout parent, @NonNull V child, @NonNull View dependency) {
        if(behaviorTranslationInitialized && dependency instanceof Snackbar.SnackbarLayout) {

            /*
             * Handle if there no bottom navigation
             */
            if(!withBottomNavigation){
                child.setY(initFABPosition);
                return;
            }

            currentSnackBarPosition = 0f;
            float navPosOffset = initNavigationPosition - currentNavigationPosition;
            if (insetBottom > 0) {
                if (currentNavigationPosition > translucentBarY) {
                    /*
                     * Bottom navigation below android navigation bar
                     */
                    child.setY(initFABPosition + bottomNavigationHeight);
                } else {
                    /*
                     * Bottom navigation above android navigation bar
                     */
                    child.setY(initFABPosition - navPosOffset);
                }
            } else {
                /*
                 * check if bottom navigation is visible
                 * not always returning 0.0 when fully visible
                 */
                if (navPosOffset >= -0.5) {
                    child.setY(initFABPosition);
                } else {
                    child.setY(initFABPosition - navPosOffset);
                }
            }
        }
        super.onDependentViewRemoved(parent, child, dependency);
    }
}
