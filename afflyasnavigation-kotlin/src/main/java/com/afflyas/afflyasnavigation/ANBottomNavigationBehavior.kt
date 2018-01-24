package com.afflyas.afflyasnavigation

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPropertyAnimatorCompat
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.view.View
import android.view.ViewGroup

class ANBottomNavigationBehavior<V : View> : VerticalScrollingBehavior<V> {

    companion object {
        private val INTERPOLATOR = LinearOutSlowInInterpolator()
        private val ANIM_DURATION = 300
    }
    /**
     * Is hidden
     * @return
     */
    var isHidden = false
        private set
    private var translationAnimator: ViewPropertyAnimatorCompat? = null
    private var translationObjectAnimator: ObjectAnimator? = null
    private var snackbarLayout: Snackbar.SnackbarLayout? = null
    private var targetOffset = 0f
    private var behaviorTranslationEnabled = true

    private var translucentNavigationHeight: Int = 0

    var androidNavigationState = AndroidNavigationState.NAV_DEFAULT

    constructor(context: Context): super() {
        translucentNavigationHeight = context.resources.getDimensionPixelOffset(R.dimen.navigation_bar_height)
    }

    constructor(context: Context, behaviorTranslationEnabled: Boolean, navigationState: AndroidNavigationState) : super() {
        this.androidNavigationState = navigationState
        this.behaviorTranslationEnabled = behaviorTranslationEnabled
        translucentNavigationHeight = context.resources.getDimensionPixelOffset(R.dimen.navigation_bar_height)
    }

    override fun layoutDependsOn(parent: CoordinatorLayout?, child: V?, dependency: View?): Boolean {
        if (dependency != null && dependency is Snackbar.SnackbarLayout) {
            updateSnackbar(child, dependency)
            return true
        }
        return super.layoutDependsOn(parent, child, dependency)
    }

    override fun onNestedVerticalOverScroll(coordinatorLayout: CoordinatorLayout, child: V, @ScrollDirection direction: Int, currentOverScroll: Int, totalOverScroll: Int) {}

    override fun onDirectionNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dx: Int, dy: Int, consumed: IntArray, @ScrollDirection scrollDirection: Int) {}

    override fun onNestedDirectionFling(coordinatorLayout: CoordinatorLayout, child: V, target: View, velocityX: Float, velocityY: Float, @ScrollDirection scrollDirection: Int): Boolean {
        return false
    }

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
        if (dyConsumed < 0) {
            handleDirection(child, VerticalScrollingBehavior.SCROLL_DIRECTION_DOWN)
        } else if (dyConsumed > 0) {
            handleDirection(child, VerticalScrollingBehavior.SCROLL_DIRECTION_UP)
        }
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, directTargetChild: View, target: View, axes: Int, type: Int): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL || super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type)
    }

    /**
     * Handle scroll direction
     * @param child
     * @param scrollDirection
     */
    private fun handleDirection(child: V, scrollDirection: Int) {
        if (!behaviorTranslationEnabled) {
            return
        }
        if (scrollDirection == VerticalScrollingBehavior.SCROLL_DIRECTION_DOWN && isHidden) {
            isHidden = false
            animateOffset(child, 0, false, true)
        } else if (scrollDirection == VerticalScrollingBehavior.SCROLL_DIRECTION_UP && !isHidden) {
            isHidden = true
            animateOffset(child, child.height, false, true)
        }
    }

    /**
     * Animate offset
     *
     * @param child
     * @param offset
     */
    private fun animateOffset(child: V, offset: Int, forceAnimation: Boolean, withAnimation: Boolean) {
        if (!behaviorTranslationEnabled && !forceAnimation) {
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            ensureOrCancelObjectAnimation(child, offset, withAnimation)
            translationObjectAnimator!!.start()
        } else {
            ensureOrCancelAnimator(child, withAnimation)
            translationAnimator!!.translationY(offset.toFloat()).start()
        }
    }

    /**
     * Manage animation for Android >= KITKAT
     *
     * @param child
     */
    private fun ensureOrCancelAnimator(child: V, withAnimation: Boolean) {
        if (translationAnimator == null) {
            translationAnimator = ViewCompat.animate(child)
            translationAnimator!!.duration = (if (withAnimation) ANIM_DURATION else 0).toLong()
            translationAnimator!!.setUpdateListener {
                if (snackbarLayout != null && snackbarLayout!!.layoutParams is ViewGroup.MarginLayoutParams) {
                    //if(translucentNavigationHeight > 0){
                    if(androidNavigationState == AndroidNavigationState.NAV_TRANSLUCENT_BOTTOM){
                        val newOffset = (child.measuredHeight - child.translationY - translucentNavigationHeight)
                        if(newOffset < 0){
                            targetOffset = 0f
                        } else {
                            targetOffset = newOffset
                        }
                    }else{
                        targetOffset = child.measuredHeight - child.translationY
                    }
                    val p = snackbarLayout!!.layoutParams as ViewGroup.MarginLayoutParams
                    p.bottomMargin = targetOffset.toInt()
                    snackbarLayout!!.requestLayout()
                }
            }
            translationAnimator!!.interpolator = INTERPOLATOR
        } else {
            translationAnimator!!.duration = (if (withAnimation) ANIM_DURATION else 0).toLong()
            translationAnimator!!.cancel()
        }
    }

    /**
     * Manage animation for Android < KITKAT
     *
     * @param child
     */
    private fun ensureOrCancelObjectAnimation(child: V, offset: Int, withAnimation: Boolean) {

        if (translationObjectAnimator != null) {
            translationObjectAnimator!!.cancel()
        }

        translationObjectAnimator = ObjectAnimator.ofFloat<View>(child, View.TRANSLATION_Y, offset.toFloat())
        translationObjectAnimator!!.duration = (if (withAnimation) ANIM_DURATION else 0).toLong()
        translationObjectAnimator!!.interpolator = INTERPOLATOR
        translationObjectAnimator!!.addUpdateListener {
            if (snackbarLayout != null && snackbarLayout!!.layoutParams is ViewGroup.MarginLayoutParams) {
                targetOffset = child.measuredHeight - child.translationY
                val p = snackbarLayout!!.layoutParams as ViewGroup.MarginLayoutParams
                p.bottomMargin = targetOffset.toInt()
                snackbarLayout!!.requestLayout()
            }
        }
    }



    /**
     * Enable or not the behavior translation
     * @param behaviorTranslationEnabled
     */
    fun setBehaviorTranslationEnabled(behaviorTranslationEnabled: Boolean) {
        this.behaviorTranslationEnabled = behaviorTranslationEnabled
    }

    /**
     * Hide AHBottomNavigation with animation
     * @param view
     * @param offset
     */
    fun hideView(view: V, offset: Int, withAnimation: Boolean) {
        if (!isHidden) {
            isHidden = true
            animateOffset(view, offset, true, withAnimation)
        }
    }

    /**
     * Reset AHBottomNavigation position with animation
     * @param view
     */
    fun resetOffset(view: V, withAnimation: Boolean) {
        if (isHidden) {
            isHidden = false
            animateOffset(view, 0, true, withAnimation)
        }
    }

    /**
     * Update Snackbar bottom margin and left/right padding
     */
    fun updateSnackbar(child: View?, dependency: View?) {
        if (dependency != null && dependency is Snackbar.SnackbarLayout) {

            val targetMargin: Int
            /**
             * bottom margin
             */
            if(androidNavigationState == AndroidNavigationState.NAV_TRANSLUCENT_BOTTOM){
                val newOffset = (child!!.measuredHeight - child.translationY - translucentNavigationHeight).toInt()
                if(newOffset < 0){
                    targetMargin = 0
                } else {
                    targetMargin = newOffset
                }
            }else{
                targetMargin = (child!!.measuredHeight - child.translationY).toInt()
                /**
                 * side padding for landscape translucent navigation
                 */
                if(androidNavigationState == AndroidNavigationState.NAV_TRANSLUCENT_LEFT){
                    dependency.setPadding(translucentNavigationHeight, 0, 0, 0)
                }else if(androidNavigationState == AndroidNavigationState.NAV_TRANSLUCENT_RIGHT){
                    dependency.setPadding(0, 0, translucentNavigationHeight, 0)
                }
            }

            if (dependency.layoutParams is ViewGroup.MarginLayoutParams) {
                val p = dependency.layoutParams as ViewGroup.MarginLayoutParams
                p.bottomMargin = targetMargin
            }

            snackbarLayout = dependency
        }
    }


}