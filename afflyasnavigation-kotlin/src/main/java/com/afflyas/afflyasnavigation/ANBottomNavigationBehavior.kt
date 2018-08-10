package com.afflyas.afflyasnavigation

import android.animation.ObjectAnimator
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.afflyas.afflyasnavigation.VerticalScrollingBehavior.Companion.ScrollDirection
import com.google.android.material.snackbar.Snackbar

class ANBottomNavigationBehavior<V : View> : VerticalScrollingBehavior<V> {

    companion object {
        private val INTERPOLATOR = AccelerateDecelerateInterpolator()
        private const val ANIM_DURATION = 350
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

    private var behaviorTranslationEnabled = false

    var scrollingDeadZone = VerticalScrollingBehavior.DEFAULT_DEAD_ZONE

    /**
     * system window inset values that has been set as padding
     */
    private var insetLeft = 0
    private var insetRight = 0
    private var insetBottom = 0

    constructor(): super()

    constructor(behaviorTranslationEnabled: Boolean): super(){
        this.behaviorTranslationEnabled = behaviorTranslationEnabled
    }

    constructor(behaviorTranslationEnabled: Boolean, insetLeft: Int, insetRight: Int, insetBottom: Int) : super() {
        this.behaviorTranslationEnabled = behaviorTranslationEnabled
        this.insetLeft = insetLeft
        this.insetRight = insetRight
        this.insetBottom = insetBottom
    }

    fun setInsets(insetLeft: Int, insetRight: Int, insetBottom: Int){
        this.insetLeft = insetLeft
        this.insetRight = insetRight
        this.insetBottom = insetBottom
    }

    /**
     * Enable or not the behavior translation
     * @param behaviorTranslationEnabled
     */
    fun setBehaviorTranslationEnabled(behaviorTranslationEnabled: Boolean) {
        this.behaviorTranslationEnabled = behaviorTranslationEnabled
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: V, dependency: View): Boolean {
        if (dependency is Snackbar.SnackbarLayout) {
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
        if (dyConsumed < -scrollingDeadZone) {
            handleDirection(child, VerticalScrollingBehavior.SCROLL_DIRECTION_DOWN)
        } else if (dyConsumed > scrollingDeadZone) {
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
                    targetOffset = if(insetBottom > 0){
                        val newOffset = (child.measuredHeight - child.translationY - insetBottom)
                        if(newOffset < 0){
                            0f
                        } else {
                            newOffset
                        }
                    }else{
                        child.measuredHeight - child.translationY
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
    private fun updateSnackbar(child: View?, dependency: View?) {
        if (dependency != null && dependency is Snackbar.SnackbarLayout) {

            val targetMargin: Int
            /**
             * bottom margin
             */
            if(insetBottom > 0){
                val newOffset = (child!!.measuredHeight - child.translationY - insetBottom).toInt()
                targetMargin = if(newOffset < 0){
                    0
                } else {
                    newOffset
                }
            }else{
                targetMargin = (child!!.measuredHeight - child.translationY).toInt()
                /**
                 * side padding for landscape translucent navigation
                 */
                dependency.setPadding(insetLeft, 0, insetRight, 0)
            }

            if (dependency.layoutParams is ViewGroup.MarginLayoutParams) {
                val p = dependency.layoutParams as ViewGroup.MarginLayoutParams
                p.bottomMargin = targetMargin
            }

            snackbarLayout = dependency
        }
    }
}