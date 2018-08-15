package com.afflyas.afflyasnavigation

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar


class ANFloatingActionButtonBehavior<V : View> : CoordinatorLayout.Behavior<V>() {

    /**
     * system window inset values that has been set as padding
     */
    private var insetBottom = 0

    private var bottomNavigationHeight: Int = 0
    //fab margin from bottom of screen
    private var fabMarginBottom: Int = 0

    private var displayHeight: Int = 0
    //Y coordinate of TranslucentNavigationBar's top
    private var translucentBarY: Int = 0

    private var initNavigationPosition: Float = 0f
    private var initFABPosition: Float = 0f
    private var initSnackBarPosition: Float = 0f

    private var currentNavigationPosition: Float = 0f

    /**
     * Current Y coordinate of Snack
     * Set to 0 when Snack is hidden ->
     */
    private var currentSnackBarPosition: Float = 0f

    var behaviorTranslationInitialized = false
        private set

    private var withBottomNavigation: Boolean = false

    fun updateTranslationBehavior(context: Context, insetBottom: Int, layoutHeight: Int, withBottomNavigation: Boolean){

        this.insetBottom = insetBottom

        if(withBottomNavigation) bottomNavigationHeight = context.resources.getDimensionPixelOffset(R.dimen.bottom_navigation_height)
        this.withBottomNavigation = withBottomNavigation

        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val displayMetrics = DisplayMetrics()

        /**
         * Y coordinates calculating differently on android <= LOLLIPOP
         */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && insetBottom > 0){
            display.getRealMetrics(displayMetrics)
            displayHeight = displayMetrics.heightPixels
            translucentBarY = displayHeight - insetBottom
            fabMarginBottom = bottomNavigationHeight + insetBottom

            initFABPosition = (displayHeight - bottomNavigationHeight - insetBottom - layoutHeight).toFloat()
            initSnackBarPosition = displayHeight.toFloat()

        }else{
            display.getMetrics(displayMetrics)
            displayHeight = displayMetrics.heightPixels
            translucentBarY = displayHeight
            fabMarginBottom = bottomNavigationHeight

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                initFABPosition = (displayHeight - bottomNavigationHeight - layoutHeight).toFloat()
                initSnackBarPosition = displayHeight.toFloat()
            }
        }

        behaviorTranslationInitialized = true
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: V, dependency: View): Boolean {
        if(behaviorTranslationInitialized){

            /**
             * set init FAB position for Pre LOLLIPOP devices
             */
            if(initFABPosition == 0f && child.y != 0f){
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    initFABPosition = child.y - bottomNavigationHeight
                }
            }

            when(dependency) {
                is ANBottomNavigation -> {
                    if(initNavigationPosition == 0f && dependency.y != 0f){
                        initNavigationPosition = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                            (displayHeight - bottomNavigationHeight - insetBottom).toFloat()
                        }else{
                            dependency.y - bottomNavigationHeight
                        }
                        currentNavigationPosition = dependency.y
                    }
                    return true
                }
                is Snackbar.SnackbarLayout -> {
                    if(initSnackBarPosition == 0f && dependency.y != 0f){
                        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            val p = dependency.layoutParams as ViewGroup.MarginLayoutParams
                            initSnackBarPosition = dependency.y + p.bottomMargin
                            currentSnackBarPosition = initSnackBarPosition
                        }
                    }
                    return true
                }
            }
        }
        return super.layoutDependsOn(parent, child, dependency)
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: V, dependency: View): Boolean {
        if(behaviorTranslationInitialized){
            updateFloatingActionButton(child, dependency)
        }
        return super.onDependentViewChanged(parent, child, dependency)
    }

    /**
     * Update floating action button Y coordinate
     */
    private fun updateFloatingActionButton(child: V, dependency: View) {
        when(dependency) {
            is ANBottomNavigation -> {
                currentNavigationPosition = dependency.y
                /**
                 * Handle FAB only when Snack is hidden
                 */
                if(currentSnackBarPosition == 0f){
                    val navPosOffset = initNavigationPosition - currentNavigationPosition
                    if(insetBottom > 0){
                        if(currentNavigationPosition > translucentBarY){
                            child.y = initFABPosition + bottomNavigationHeight
                        }else{
                            child.y = initFABPosition - navPosOffset
                        }
                    }else{
                        child.y = initFABPosition - navPosOffset
                    }
                }
            }
            is Snackbar.SnackbarLayout -> {
                currentSnackBarPosition = dependency.y

                val snackPosOffset = initSnackBarPosition - currentSnackBarPosition
                val navPosOffset = initNavigationPosition - currentNavigationPosition


                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                    if(withBottomNavigation){
                        /**
                         * Has Bottom Navigation
                         */
                        if (currentSnackBarPosition < currentNavigationPosition) {
                            /**
                             * Snack above bottom navigation
                             */
                            child.y = initFABPosition + bottomNavigationHeight + bottomNavigationHeight - snackPosOffset
                        }
                    }else{
                        /**
                         * No Bottom Navigation
                         */
                        child.y = initFABPosition - snackPosOffset
                    }
                }else{
                    if(insetBottom > 0){
                        /**
                         * Translucent navigation enabled
                         */
                        if(withBottomNavigation){
                            /**
                             * Has Bottom Navigation
                             */
                            if (currentNavigationPosition > translucentBarY) {
                                /**
                                 * Bottom navigation below android navigation bar
                                 */
                                if (currentSnackBarPosition > translucentBarY) {
                                    /**
                                     * Snack below android navigation bar
                                     */
                                    child.y = initFABPosition + bottomNavigationHeight
                                }else{
                                    /**
                                     * Snack above android navigation bar
                                     */
                                    child.y = initFABPosition + fabMarginBottom - snackPosOffset
                                }
                            } else {
                                /**
                                 * Bottom navigation above android navigation bar
                                 */
                                if (currentSnackBarPosition > currentNavigationPosition) {
                                    /**
                                     * Snack below bottom navigation
                                     */
                                    child.y = initFABPosition - navPosOffset
                                }else{
                                    /**
                                     * Snack above bottom navigation
                                     */
                                    child.y = initFABPosition + fabMarginBottom - snackPosOffset
                                }
                            }
                        }else{
                            /**
                             * No Bottom Navigation
                             */
                            if (currentSnackBarPosition > translucentBarY) {
                                /**
                                 * Snack below android navigation bar
                                 */
                                child.y = initFABPosition + bottomNavigationHeight
                            }else{
                                /**
                                 * Snack above android navigation bar
                                 */
                                child.y = initFABPosition + fabMarginBottom - snackPosOffset
                            }
                        }
                    }else{
                        /**
                         * Translucent navigation disabled
                         */
                        if(withBottomNavigation){
                            /**
                             * Has Bottom Navigation
                             */
                            if (currentSnackBarPosition > currentNavigationPosition) {
                                /**
                                 * Snack below bottom navigation
                                 */
                                child.y = initFABPosition - navPosOffset
                            }else{
                                /**
                                 * Snack above bottom navigation
                                 */
                                child.y = initFABPosition + fabMarginBottom - snackPosOffset
                            }
                        }else{
                            /**
                             * No Bottom Navigation
                             */
                            child.y = initFABPosition + fabMarginBottom - snackPosOffset
                        }
                    }
                }
            }
        }
    }


    /**
     * set FAB position when Snack disappears
     */
    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: V, dependency: View) {
        if(behaviorTranslationInitialized && dependency is Snackbar.SnackbarLayout) {

            /**
             * Handle if there no bottom navigation
             */
            if(!withBottomNavigation){
                child.y = initFABPosition
                return
            }

            currentSnackBarPosition = 0f
            val navPosOffset = initNavigationPosition - currentNavigationPosition
            if (insetBottom > 0) {
                if (currentNavigationPosition > translucentBarY) {
                    /**
                     * Bottom navigation below android navigation bar
                     */
                    child.y = initFABPosition + bottomNavigationHeight
                } else {
                    /**
                     * Bottom navigation above android navigation bar
                     */
                    child.y = initFABPosition - navPosOffset
                }
            } else {
                /**
                 * check if bottom navigation is visible
                 * not always returning 0.0 when fully visible
                 */
                if (navPosOffset >= -0.5) {
                    child.y = initFABPosition
                } else {
                    child.y = initFABPosition - navPosOffset
                }
            }
        }
        super.onDependentViewRemoved(parent, child, dependency)
    }
}