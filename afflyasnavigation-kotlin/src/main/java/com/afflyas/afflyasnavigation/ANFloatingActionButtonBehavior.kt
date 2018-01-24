package com.afflyas.afflyasnavigation

import android.content.Context
import android.os.Build
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

class ANFloatingActionButtonBehavior<V : View>(
        context: Context,
        androidNavigationState: AndroidNavigationState,
        layoutHeight: Int) : CoordinatorLayout.Behavior<V>() {

    private val translucentNavigationEnabled: Boolean

    private val bottomNavigationHeight: Int
    private val translucentBarHeight: Int

    //fab margin from bottom of screen
    private val fabMarginBottom: Int

    private val displayHeight: Int
    //Y coordinate of TranslucentNavigationBar's top
    private val translucentBarY: Int

    private var initNavigationPosition: Float = 0f
    private var initFABPosition: Float = 0f
    private var initSnackBarPosition: Float = 0f

    private var currentNavigationPosition: Float = 0f

    /**
     * Current Y coordinate of Snack
     * Set to 0 when Snack is hidden ->
     */
    private var currentSnackBarPosition: Float = 0f

    init {
        bottomNavigationHeight = context.resources.getDimensionPixelOffset(R.dimen.bottom_navigation_height)
        translucentBarHeight = context.resources.getDimensionPixelOffset(R.dimen.navigation_bar_height)

        translucentNavigationEnabled = androidNavigationState == AndroidNavigationState.NAV_TRANSLUCENT_BOTTOM

        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

        val displayMetrics = DisplayMetrics()

        /**
         * Y coordinates calculating differently on android <= LOLLIPOP
         */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && translucentNavigationEnabled){
            display.getRealMetrics(displayMetrics)
            displayHeight = displayMetrics.heightPixels
            translucentBarY = displayHeight - translucentBarHeight
            fabMarginBottom = bottomNavigationHeight + translucentBarHeight

            initFABPosition = (displayHeight - bottomNavigationHeight - translucentBarHeight - layoutHeight).toFloat()
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
    }

    override fun layoutDependsOn(parent: CoordinatorLayout?, child: V?, dependency: View?): Boolean {

        if(dependency != null){

            if(child != null){
                if(initFABPosition == 0f && child.y != 0f){
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        initFABPosition = child.y
                    }
                }
            }

            when(dependency) {
                is ANBottomNavigation -> {
                    if(initNavigationPosition == 0f && dependency.y != 0f){
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                            if(translucentNavigationEnabled){
                                initNavigationPosition = (displayHeight - bottomNavigationHeight - translucentBarHeight).toFloat()
                            }else{
                                initNavigationPosition = (displayHeight - bottomNavigationHeight).toFloat()
                            }
                        }else{
                            initNavigationPosition = dependency.y

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

    override fun onDependentViewChanged(parent: CoordinatorLayout?, child: V?, dependency: View?): Boolean {
        updateFloatingActionButton(child, dependency)
        return super.onDependentViewChanged(parent, child, dependency)
    }

    /**
     * Update floating action button Y coordinate
     */
    private fun updateFloatingActionButton(child: V?, dependency: View?) {
        if(child != null && dependency != null){
            when(dependency) {
                is ANBottomNavigation -> {
                    currentNavigationPosition = dependency.y
                    /**
                     * Handle FAB only when Snack is hidden
                     */
                    if(currentSnackBarPosition == 0f){
                        val navPosOffset = initNavigationPosition - currentNavigationPosition
                        if(translucentNavigationEnabled){
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

                    if(translucentNavigationEnabled){
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
                         * Translucent navigation disabled
                         */
                        child.y = initFABPosition + bottomNavigationHeight - snackPosOffset
                    }
                }
            }
        }
    }

    /**
     * set FAB position when Snack disappears
     */
    override fun onDependentViewRemoved(parent: CoordinatorLayout?, child: V?, dependency: View?) {
        if(dependency != null && dependency is Snackbar.SnackbarLayout && child != null) {
            currentSnackBarPosition = 0f
            val navPosOffset = initNavigationPosition - currentNavigationPosition
            if (translucentNavigationEnabled) {
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





