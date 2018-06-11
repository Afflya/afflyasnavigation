package com.afflyas.afflyasnavigation

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPropertyAnimatorCompat
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.util.AttributeSet
import android.view.View
import com.afflyas.afflyasnavigation.VerticalScrollingBehavior.Companion.ScrollDirection

class ANTopBarBehavior<V : View> : VerticalScrollingBehavior<V> {

    companion object {
        private val INTERPOLATOR = LinearOutSlowInInterpolator()
        private const val ANIM_DURATION = 300
    }

    private var hidden = false
    private var translationAnimator: ViewPropertyAnimatorCompat? = null
    private var translationObjectAnimator: ObjectAnimator? = null
    var behaviorTranslationEnabled = false

    /**
     * Constructor
     */
    constructor() : super()

    constructor(behaviorTranslationEnabled: Boolean) : super(){
        this.behaviorTranslationEnabled = behaviorTranslationEnabled
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onDirectionNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dx: Int, dy: Int, consumed: IntArray, @ScrollDirection scrollDirection: Int) {}

    override fun onNestedDirectionFling(coordinatorLayout: CoordinatorLayout, child: V, target: View, velocityX: Float, velocityY: Float, @ScrollDirection scrollDirection: Int): Boolean {
        return false
    }

    override fun onNestedVerticalOverScroll(coordinatorLayout: CoordinatorLayout, child: V, @ScrollDirection direction: Int, currentOverScroll: Int, totalOverScroll: Int) {}

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
        if (dyConsumed < 0) {
            handleDirection(child, SCROLL_DIRECTION_DOWN)
        } else if (dyConsumed > 0) {
            handleDirection(child, SCROLL_DIRECTION_UP)
        }
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, directTargetChild: View, target: View, axes: Int, type: Int): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL || super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type)
    }

    private fun handleDirection(child: V, scrollDirection: Int) {
        if (scrollDirection == SCROLL_DIRECTION_DOWN && hidden) {
            hidden = false
            animateOffset(child, 0, false, true)
        } else if (scrollDirection == SCROLL_DIRECTION_UP && !hidden) {
            hidden = true
            animateOffset(child, -child.height, false, true)
        }
    }

    private fun animateOffset(child: V, offset: Int, forceAnimation: Boolean, withAnimation: Boolean) {
        if (!behaviorTranslationEnabled && !forceAnimation) {
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            ensureOrCancelObjectAnimation(child, offset, withAnimation)
            translationObjectAnimator?.start()
        } else {
            ensureOrCancelAnimator(child, withAnimation)
            translationAnimator?.translationY(offset.toFloat())?.start()
        }
    }

    /**
     * Manage animation for Android >= KITKAT
     *
     * @param child
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun ensureOrCancelAnimator(child: V, withAnimation: Boolean) {
        if (translationAnimator == null) {
            translationAnimator = ViewCompat.animate(child)
            translationAnimator?.duration = (if (withAnimation) ANIM_DURATION else 0).toLong()
            translationAnimator?.interpolator = INTERPOLATOR
        } else {
            translationAnimator?.duration = (if (withAnimation) ANIM_DURATION else 0).toLong()
            translationAnimator?.cancel()
        }
    }

    /**
     * Manage animation for Android < KITKAT
     *
     * @param child
     */
    private fun ensureOrCancelObjectAnimation(child: V, offset: Int, withAnimation: Boolean) {
        if (translationObjectAnimator != null) {
            translationObjectAnimator?.cancel()
        }
        translationObjectAnimator = ObjectAnimator.ofFloat<View>(child, View.TRANSLATION_Y, offset.toFloat())
        translationObjectAnimator?.duration = (if (withAnimation) ANIM_DURATION else 0).toLong()
        translationObjectAnimator?.interpolator = INTERPOLATOR
    }

    /**
     * Hide TopBar with animation
     * @param view
     */
    fun hideView(view: V, withAnimation: Boolean) {
        if (!hidden) {
            hidden = true
            animateOffset(view, -view.height, true, withAnimation)
        }
    }

    /**
     * Reset TopBar position with animation
     * @param view
     */
    fun resetOffset(view: V, withAnimation: Boolean) {
        if (hidden) {
            hidden = false
            animateOffset(view, 0, true, withAnimation)
        }
    }

    /**
     * Is hidden
     * @return
     */
    fun isHidden(): Boolean {
        return hidden
    }
}