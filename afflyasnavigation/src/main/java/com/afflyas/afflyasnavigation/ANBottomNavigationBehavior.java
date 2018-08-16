package com.afflyas.afflyasnavigation;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;
import androidx.core.view.ViewPropertyAnimatorUpdateListener;

public class ANBottomNavigationBehavior<V extends View> extends VerticalScrollingBehavior<V> {

    private static final Interpolator INTERPOLATOR = new AccelerateDecelerateInterpolator();
    private static final int ANIM_DURATION = 300;

    public boolean isHidden() {
        return isHidden;
    }

    private boolean isHidden = false;

    private ViewPropertyAnimatorCompat translationAnimator = null;
    private ObjectAnimator translationObjectAnimator = null;
    private Snackbar.SnackbarLayout snackbarLayout = null;
    private float targetOffset = 0f;

    private boolean behaviorTranslationEnabled = false;

    public int getScrollingDeadZone() {
        return scrollingDeadZone;
    }

    public void setScrollingDeadZone(int scrollingDeadZone) {
        this.scrollingDeadZone = scrollingDeadZone;
    }

    private int scrollingDeadZone = VerticalScrollingBehavior.DEFAULT_DEAD_ZONE;

    /**
     * system window inset values that has been set as padding
     */
    private int insetLeft = 0;
    private int insetRight = 0;
    private int insetBottom = 0;

    public ANBottomNavigationBehavior() {
        super();
    }

    public ANBottomNavigationBehavior(boolean behaviorTranslationEnabled){
        super();
        this.behaviorTranslationEnabled = behaviorTranslationEnabled;
    }

    public ANBottomNavigationBehavior(boolean behaviorTranslationEnabled, int insetLeft, int insetRight, int insetBottom) {
        super();
        this.behaviorTranslationEnabled = behaviorTranslationEnabled;
        this.insetLeft = insetLeft;
        this.insetRight = insetRight;
        this.insetBottom = insetBottom;
    }

    public void setInsets(int insetLeft, int insetRight, int insetBottom){
        this.insetLeft = insetLeft;
        this.insetRight = insetRight;
        this.insetBottom = insetBottom;
    }

    /**
     * Enable or not the behavior translation
     * @param behaviorTranslationEnabled
     */
    public void setBehaviorTranslationEnabled(boolean behaviorTranslationEnabled) {
        this.behaviorTranslationEnabled = behaviorTranslationEnabled;
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull V child, @NonNull View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            updateSnackbar(child, dependency);
            return true;
        }
        return super.layoutDependsOn(parent, child, dependency);
    }

    @Override
    public void onNestedVerticalOverScroll(CoordinatorLayout coordinatorLayout, V child, @ScrollDirection int direction, int currentOverScroll, int totalOverScroll) { }

    @Override
    public void onDirectionNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dx, int dy, int[] consumed, @ScrollDirection int scrollDirection) { }

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

    /**
     * Handle scroll direction
     * @param child
     * @param scrollDirection
     */
    private void handleDirection(V child, int scrollDirection) {
        if (!behaviorTranslationEnabled) {
            return;
        }
        if (scrollDirection == ScrollDirection.SCROLL_DIRECTION_DOWN && isHidden) {
            isHidden = false;
            animateOffset(child, 0, false, true);
        } else if (scrollDirection == ScrollDirection.SCROLL_DIRECTION_UP && !isHidden) {
            isHidden = true;
            animateOffset(child, child.getHeight(), false, true);
        }
    }

    /**
     * Animate offset
     *
     * @param child
     * @param offset
     */
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
    private void ensureOrCancelAnimator(final V child, boolean withAnimation) {
        if (translationAnimator == null) {
            translationAnimator = ViewCompat.animate(child);
            translationAnimator.setDuration(withAnimation ? ANIM_DURATION : 0);
            translationAnimator.setUpdateListener(new ViewPropertyAnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(View view) {
                    if (snackbarLayout != null && snackbarLayout.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                        if(insetBottom > 0){
                            float newOffset = (child.getMeasuredHeight() - child.getTranslationY() - insetBottom);
                            if(newOffset < 0){
                                targetOffset = 0f;
                            } else {
                                targetOffset = newOffset;
                            }
                        }else{
                            targetOffset = child.getMeasuredHeight() - child.getTranslationY();
                        }
                        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams)snackbarLayout.getLayoutParams();
                        p.bottomMargin = (int) targetOffset;
                        snackbarLayout.requestLayout();
                    }
                }
            });
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
    private void ensureOrCancelObjectAnimation(final V child, int offset, boolean withAnimation) {

        if (translationObjectAnimator != null) {
            translationObjectAnimator.cancel();
        }

        translationObjectAnimator = ObjectAnimator.ofFloat(child, View.TRANSLATION_Y, offset);
        translationObjectAnimator.setDuration(withAnimation ? ANIM_DURATION : 0);
        translationObjectAnimator.setInterpolator(INTERPOLATOR);
        translationObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (snackbarLayout != null && snackbarLayout.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    targetOffset = child.getMeasuredHeight() - child.getTranslationY();
                    ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) snackbarLayout.getLayoutParams();
                    p.bottomMargin = (int) targetOffset;
                    snackbarLayout.requestLayout();
                }
            }
        });
    }

    /**
     * Hide AHBottomNavigation with animation
     * @param view
     * @param offset
     */
    public void hideView(V view, int offset, boolean withAnimation) {
        if (!isHidden) {
            isHidden = true;
            animateOffset(view, offset, true, withAnimation);
        }
    }

    /**
     * Reset AHBottomNavigation position with animation
     * @param view
     */
    public void resetOffset(V view, boolean withAnimation) {
        if (isHidden) {
            isHidden = false;
            animateOffset(view, 0, true, withAnimation);
        }
    }

    /**
     * Update Snackbar bottom margin and left/right padding
     */
    private void updateSnackbar(final View child, View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            int targetMargin;
            /*
             * bottom margin
             */
            if(insetBottom > 0){
                int newOffset = (int) (child.getMeasuredHeight() - child.getTranslationY() - insetBottom);
                if(newOffset < 0){
                    targetMargin = 0;
                } else {
                    targetMargin = newOffset;
                }
            }else{
                targetMargin = (int)(child.getMeasuredHeight() - child.getTranslationY());
                /*
                 * side padding for landscape translucent navigation
                 */
                dependency.setPadding(insetLeft, 0, insetRight, 0);
            }

            if (dependency.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) dependency.getLayoutParams();
                p.bottomMargin = targetMargin;
            }
            snackbarLayout = (Snackbar.SnackbarLayout)dependency;
        }
    }
}
