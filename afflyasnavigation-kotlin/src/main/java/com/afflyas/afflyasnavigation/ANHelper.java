package com.afflyas.afflyasnavigation;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.drawable.DrawableCompat;

public class ANHelper {

    private ANHelper(){}
    /**
     * Check if app is in multi window mode
     */
    @RequiresApi(Build.VERSION_CODES.N)
    public static boolean isInMultiWindow(Context context){

        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        if(wm == null) return false;

        Display d = wm.getDefaultDisplay();

        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
        d.getRealMetrics(realDisplayMetrics);

        int realHeight = realDisplayMetrics.heightPixels;
        int realWidth = realDisplayMetrics.widthPixels;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);

        int displayHeight = displayMetrics.heightPixels;
        int displayWidth = displayMetrics.widthPixels;

        return realWidth - displayWidth > realWidth / 5 || realHeight - displayHeight > realHeight / 5;
    }

    /**
     *
     * Check if device has navigation bar
     *
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean  hasNavigationBar(Context context){

        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        if(wm == null) return false;

        Display d = wm.getDefaultDisplay();

        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
        d.getRealMetrics(realDisplayMetrics);

        int realHeight = realDisplayMetrics.heightPixels;
        int realWidth = realDisplayMetrics.widthPixels;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);

        int displayHeight = displayMetrics.heightPixels;
        int displayWidth = displayMetrics.widthPixels;

        return realWidth > displayWidth || realHeight > displayHeight;
    }

    /**
     * Check if translucent navigation enabled in the theme
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean  isTranslucentNavigationThemeEnabled(Context context){
        int[] themeAttrs = { android.R.attr.windowTranslucentNavigation };
        TypedArray typedValue = context.getTheme().obtainStyledAttributes(themeAttrs);
        boolean translucentThemeEnabled;
        try {
            translucentThemeEnabled = typedValue.getBoolean(0, false);
        } finally {
            typedValue.recycle();
        }
        return translucentThemeEnabled;
    }

    /**
     * Check if translucent status enabled in the theme
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean  isTranslucentStatusThemeEnabled(Context context){
        int[] themeAttrs = { android.R.attr.windowTranslucentStatus };
        TypedArray typedValue = context.getTheme().obtainStyledAttributes(themeAttrs);
        boolean translucentStatusEnabled;
        try {
            translucentStatusEnabled = typedValue.getBoolean(0, false);
        } finally {
            typedValue.recycle();
        }
        return translucentStatusEnabled;
    }

    /**
     * Return a tint drawable
     *
     * @param drawable
     * @param color
     * @param forceTint
     * @return
     */
    public static Drawable getTintDrawable(Drawable drawable, @ColorInt int color, Boolean forceTint) {
        if (forceTint) {
            drawable.clearColorFilter();
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            drawable.invalidateSelf();
            return drawable;
        }
        Drawable wrapDrawable = DrawableCompat.wrap(drawable).mutate();
        DrawableCompat.setTint(wrapDrawable, color);
        return wrapDrawable;
    }

    /**
     * Update top margin with animation
     */
    public static void updateTopMargin(final View view, int fromMargin, int toMargin) {
        ValueAnimator animator = ValueAnimator.ofFloat((float)fromMargin, (float)toMargin);
        animator.setDuration(150);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedValue = (float) valueAnimator.getAnimatedValue();
                if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                    p.setMargins(p.leftMargin, (int)animatedValue, p.rightMargin, p.bottomMargin);
                    view.requestLayout();
                }
            }
        });
        animator.start();
    }

    /**
     * Update bottom margin with animation
     */
    public static void updateBottomMargin(final View view, int fromMargin, int toMargin, int duration) {
        ValueAnimator animator = ValueAnimator.ofFloat((float)fromMargin, (float)toMargin);
        animator.setDuration(duration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedValue = (float) valueAnimator.getAnimatedValue();
                if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                    p.setMargins(p.leftMargin, p.topMargin, p.rightMargin, (int)animatedValue);
                    view.requestLayout();
                }
            }
        });
        animator.start();
    }

    /**
     * Update left margin with animation
     */
    public static void updateLeftMargin(final View view, int fromMargin, int toMargin) {
        ValueAnimator animator = ValueAnimator.ofFloat((float)fromMargin, (float)toMargin);
        animator.setDuration(150);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedValue = (float) valueAnimator.getAnimatedValue();
                if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                    p.setMargins((int) animatedValue, p.topMargin, p.rightMargin, p.bottomMargin);
                    view.requestLayout();
                }
            }
        });
        animator.start();
    }

    /**
     * Update text size with animation
     */
    public static void updateTextSize(final TextView textView, float fromSize, float toSize) {
        ValueAnimator animator = ValueAnimator.ofFloat(fromSize, toSize);
        animator.setDuration(150);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedValue = (float) valueAnimator.getAnimatedValue();
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, animatedValue);
            }
        });
        animator.start();
    }

    /**
     * Update alpha
     */
    public static void updateAlpha(final View view, float fromValue, float toValue) {
        ValueAnimator animator = ValueAnimator.ofFloat(fromValue, toValue);
        animator.setDuration(150);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedValue = (float) valueAnimator.getAnimatedValue();
                view.setAlpha(animatedValue);
            }
        });
        animator.start();
    }

    /**
     * Update text color with animation
     */
    public static void updateTextColor(final TextView textView, @ColorInt int fromColor,
                                       @ColorInt int toColor) {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
        colorAnimation.setDuration(150);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                textView.setTextColor((Integer) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();
    }

    /**
     * Update text color with animation
     */
    public static void updateViewBackgroundColor(final View view, @ColorInt int fromColor,
                                                 @ColorInt int toColor) {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
        colorAnimation.setDuration(150);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                view.setBackgroundColor((Integer) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();
    }

    /**
     * Update image view color with animation
     */
    public static void updateDrawableColor(final Drawable drawable, final ImageView imageView, @ColorInt int fromColor,
                                           @ColorInt int toColor, final boolean forceTint) {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
        colorAnimation.setDuration(150);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                imageView.setImageDrawable(getTintDrawable(drawable,
                        (Integer) animator.getAnimatedValue(), forceTint));
                imageView.requestLayout();
            }
        });
        colorAnimation.start();
    }

    /**
     * Update width
     */
    public static void updateWidth(final View view, float fromWidth, float toWidth) {
        ValueAnimator animator = ValueAnimator.ofFloat(fromWidth, toWidth);
        animator.setDuration(150);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.width = Math.round((float) animator.getAnimatedValue());
                view.setLayoutParams(params);
            }
        });
        animator.start();
    }
}
