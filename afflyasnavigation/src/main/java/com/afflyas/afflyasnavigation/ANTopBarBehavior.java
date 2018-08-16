package com.afflyas.afflyasnavigation;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;

public class ANTopBarBehavior<V extends View> extends VerticalScrollingBehavior<V> {

    private static final Interpolator INTERPOLATOR = new AccelerateDecelerateInterpolator();
    private static final int ANIM_DURATION = 300;

    private boolean hidden = false;
    private ViewPropertyAnimatorCompat translationAnimator = null;
    private ObjectAnimator translationObjectAnimator = null;

    private boolean behaviorTranslationEnabled = false;

    private int scrollingDeadZone = VerticalScrollingBehavior.DEFAULT_DEAD_ZONE;

    public void setBehaviorTranslationEnabled(boolean behaviorTranslationEnabled) {
        this.behaviorTranslationEnabled = behaviorTranslationEnabled;
    }

    public void setScrollingDeadZone(int scrollingDeadZone) {
        this.scrollingDeadZone = scrollingDeadZone;
    }

    /**
     * Constructor
     */
    public ANTopBarBehavior() {
        super();
    }

    public ANTopBarBehavior(boolean behaviorTranslationEnabled){
        super();
        this.behaviorTranslationEnabled = behaviorTranslationEnabled;
    }

    public ANTopBarBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onNestedVerticalOverScroll(CoordinatorLayout coordinatorLayout, V child, @ScrollDirection int direction, int currentOverScroll, int totalOverScroll) {
    }

    @Override
    public void onDirectionNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dx, int dy, int[] consumed, @ScrollDirection int scrollDirection) {
    }

    @Override
    protected boolean onNestedDirectionFling(CoordinatorLayout coordinatorLayout, V child, View target, float velocityX, float velocityY, @ScrollDirection int scrollDirection) {
        return false;
    }
    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type);
        if (dyConsumed < -scrollingDeadZone) {
            handleDirection(child, ScrollDirection.SCROLL_DIRECTION_DOWN);
        } else if (dyConsumed > scrollingDeadZone) {
            handleDirection(child, ScrollDirection.SCROLL_DIRECTION_UP);
        }
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL || super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type);
    }

    private void handleDirection(V child, int scrollDirection) {
        if (scrollDirection == ScrollDirection.SCROLL_DIRECTION_DOWN && hidden) {
            hidden = false;
            animateOffset(child, 0, false, true);
        } else if (scrollDirection == ScrollDirection.SCROLL_DIRECTION_UP && !hidden) {
            hidden = true;
            animateOffset(child, -child.getHeight(), false, true);
        }
    }

    private void animateOffset(V child, int offset, boolean forceAnimation, boolean withAnimation) {
        if (!behaviorTranslationEnabled && !forceAnimation) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            ensureOrCancelObjectAnimation(child, offset, withAnimation);
            translationObjectAnimator.start();
        } else {
            ensureOrCancelAnimator(child, withAnimation);
            translationAnimator.translationY(offset).start();
        }
    }

    /**
     * Manage animation for Android >= KITKAT
     *
     * @param child
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private void ensureOrCancelAnimator(V child, boolean withAnimation) {
        if (translationAnimator == null) {
            translationAnimator = ViewCompat.animate(child);
            translationAnimator.setDuration(withAnimation ? ANIM_DURATION : 0);
            translationAnimator.setInterpolator(INTERPOLATOR);
        } else {
            translationAnimator.setDuration(withAnimation ? ANIM_DURATION : 0);
            translationAnimator.cancel();
        }
    }

    /**
     * Manage animation for Android < KITKAT
     *
     * @param child
     */
    private void ensureOrCancelObjectAnimation(V child, int offset, boolean withAnimation) {
        if (translationObjectAnimator != null) {
            translationObjectAnimator.cancel();
        }
        translationObjectAnimator = ObjectAnimator.ofFloat(child, View.TRANSLATION_Y, offset);
        translationObjectAnimator.setDuration(withAnimation ? ANIM_DURATION : 0);
        translationAnimator.setInterpolator(INTERPOLATOR);
    }

    /**
     * Hide TopBar with animation
     * @param view
     */
    public void hideView(V view, boolean withAnimation) {
        if (!hidden) {
            hidden = true;
            animateOffset(view, -view.getHeight(), true, withAnimation);
        }
    }

    /**
     * Reset TopBar position with animation
     * @param view
     */
    public void resetOffset(V view, boolean withAnimation) {
        if (hidden) {
            hidden = false;
            animateOffset(view, 0, true, withAnimation);
        }
    }

    /**
     * Is hidden
     * @return
     */
    public boolean isHidden() {
        return hidden;
    }
}
