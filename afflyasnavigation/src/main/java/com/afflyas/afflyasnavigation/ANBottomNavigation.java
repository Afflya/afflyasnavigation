package com.afflyas.afflyasnavigation;

import android.animation.Animator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afflyas.afflyasnavigation.notification.ANNotification;
import com.afflyas.afflyasnavigation.notification.ANNotificationHelper;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

public class ANBottomNavigation extends FrameLayout {

    // Constant
    public static final int CURRENT_ITEM_NONE = -1;
    public static final int UPDATE_ALL_NOTIFICATIONS = -1;

    // Title state
    public enum TitleState {
        SHOW_WHEN_ACTIVE,
        ALWAYS_SHOW,
        ALWAYS_HIDE
    }

    // Static
    private static String TAG = "AHBottomNavigation";
    private static final String EXCEPTION_INDEX_OUT_OF_BOUNDS = "The position (%d) is out of bounds of the items (%d elements)";
    private static final int MIN_ITEMS = 3;
    private static final int MAX_ITEMS = 5;

    // Listener
    private OnTabSelectedListener tabSelectedListener = null;

    // Variables
    private Context context;
    private Resources resources;
    // Variables
    private boolean translucentNavigationThemeEnabled = false;
    private final ArrayList<ANBottomNavigationItem> items = new ArrayList<>();
    private final ArrayList<View> views = new ArrayList<>();
    private ANBottomNavigationBehavior<ANBottomNavigation> bottomNavigationBehavior = null;
    private LinearLayout linearLayoutContainer = null;
    private View backgroundColorView = null;
    private Animator circleRevealAnim = null;
    private boolean colored = false;
    private boolean selectedBackgroundVisible = false;
    private List<ANNotification> notifications = ANNotification.generateEmptyList(MAX_ITEMS);
    private final boolean[] itemsEnabledStates = {true, true, true, true, true};
    private int currentItem = 0;
    private int currentColor = 0;
    private boolean needHideBottomNavigation = false;
    private boolean hideBottomNavigationWithAnimation = false;
    private boolean soundEffectsEnabled = true;

    private boolean behaviorTranslationEnabled = true;

    public int getScrollingDeadZone() {
        return scrollingDeadZone;
    }

    public void setScrollingDeadZone(int scrollingDeadZone) {
        this.scrollingDeadZone = scrollingDeadZone;
    }

    private int scrollingDeadZone = VerticalScrollingBehavior.DEFAULT_DEAD_ZONE;

    /**
     * indicates that insets were set
     */
    private boolean isInsetsSet = false;
    /**
     * system window inset values that has been set as padding
     */
    private int insetLeft = 0;
    private int insetRight = 0;
    private int insetBottom = 0;

    // Variables (Styles)
    private Typeface titleTypeface = null;
    private int defaultBackgroundColor = Color.WHITE;
    private int defaultBackgroundResource = 0;
    @ColorInt private int itemActiveColor = 0;
    @ColorInt private int itemInactiveColor = 0;
    @ColorInt private int titleColorActive = 0;
    @ColorInt private int itemDisableColor = 0;
    @ColorInt private int titleColorInactive = 0;
    @ColorInt private int coloredTitleColorActive = 0;
    @ColorInt private int coloredTitleColorInactive = 0;
    private float titleActiveTextSize = 0f;
    private float titleInactiveTextSize = 0f;
    private int bottomNavigationHeight = 0;
    private int navigationBarHeight = 0;
    private float selectedItemWidth = 0f;
    private float notSelectedItemWidth = 0f;
    private boolean forceTint = false;
    private TitleState titleState = TitleState.SHOW_WHEN_ACTIVE;

    // Notifications
    @ColorInt private int notificationTextColor = 0;
    @ColorInt private int notificationBackgroundColor = 0;
    private Drawable notificationBackgroundDrawable = null;
    private Typeface notificationTypeface = null;
    private int notificationActiveMarginLeft = 0;
    private int notificationInactiveMarginLeft = 0;
    private int notificationActiveMarginTop = 0;
    private int notificationInactiveMarginTop = 0;
    private long notificationAnimationDuration = 0;

    public ANBottomNavigation(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public ANBottomNavigation(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ANBottomNavigation(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    public void setSoundEffectsEnabled(boolean soundEffectsEnabled) {
        super.setSoundEffectsEnabled(soundEffectsEnabled);
        this.soundEffectsEnabled = soundEffectsEnabled;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        createItems();
    }

    /**
     * Setup showing/hiding behavior on scrolling
     */
    private void setupBehaviorTranslation(){
        if (getParent() instanceof CoordinatorLayout) {
            if (bottomNavigationBehavior == null) {
                bottomNavigationBehavior = new ANBottomNavigationBehavior<>(behaviorTranslationEnabled, insetLeft, insetRight, insetBottom);
                bottomNavigationBehavior.setScrollingDeadZone(scrollingDeadZone);
            } else {
                bottomNavigationBehavior.setBehaviorTranslationEnabled(behaviorTranslationEnabled);
                bottomNavigationBehavior.setInsets(insetLeft, insetRight, insetBottom);
                bottomNavigationBehavior.setScrollingDeadZone(scrollingDeadZone);
            }

            ((CoordinatorLayout.LayoutParams)getLayoutParams()).setBehavior(bottomNavigationBehavior);

            if (needHideBottomNavigation) {
                needHideBottomNavigation = false;
                bottomNavigationBehavior.hideView(this, bottomNavigationHeight, hideBottomNavigationWithAnimation);
            }
        }
    }

    /**
     * Set the behavior translation value
     *
     * @param behaviorTranslationEnabled boolean for the state
     */
    public void enableBehaviorTranslation(boolean behaviorTranslationEnabled) {
        this.behaviorTranslationEnabled = behaviorTranslationEnabled;
        setupBehaviorTranslation();
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

            if(ANHelper.isInMultiWindow(context)){
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
            if(translucentNavigationThemeEnabled && !ANHelper.isInMultiWindow(context)){
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
            if(translucentNavigationThemeEnabled && ANHelper.hasNavigationBar(context)){
                if(resources.getConfiguration().isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)){
                    insetBottom = resources.getDimensionPixelOffset(R.dimen.navigation_bar_height);
                }else{
                    if(resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        insetRight = resources.getDimensionPixelOffset(R.dimen.navigation_bar_height);
                    }else{
                        insetBottom = resources.getDimensionPixelOffset(R.dimen.navigation_bar_height);
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

            createItems();

            setupBehaviorTranslation();

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                bringToFront();
            }
        }
        super.onDraw(canvas);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putInt("current_item", currentItem);
        bundle.putParcelableArrayList("notifications", new ArrayList<> (notifications));
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            currentItem = bundle.getInt("current_item");
            notifications = bundle.getParcelableArrayList("notifications");
            state = bundle.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }

    /**
     * Init
     *
     * @param context
     */
    private void init(Context context, AttributeSet attrs) {
        this.context = context;
        resources = this.context.getResources();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            translucentNavigationThemeEnabled = ANHelper.isTranslucentNavigationThemeEnabled(context);
        }

        setClickable(true);

        // Item colors
        titleColorActive = ContextCompat.getColor(context, R.color.colorBottomNavigationAccent);
        titleColorInactive = ContextCompat.getColor(context, R.color.colorBottomNavigationInactive);
        itemDisableColor = ContextCompat.getColor(context, R.color.colorBottomNavigationDisable);

        // Colors for colored bottom navigation
        coloredTitleColorActive = ContextCompat.getColor(context, R.color.colorBottomNavigationActiveColored);
        coloredTitleColorInactive = ContextCompat.getColor(context, R.color.colorBottomNavigationInactiveColored);

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ANBottomNavigation, 0, 0);
            try {
                selectedBackgroundVisible = ta.getBoolean(R.styleable.ANBottomNavigation_selectedBackgroundVisible, false);

                behaviorTranslationEnabled = ta.getBoolean(R.styleable.ANBottomNavigation_behaviorTranslationEnabled, false);
                scrollingDeadZone = ta.getInteger(R.styleable.ANBottomNavigation_scrollingDeadZone, VerticalScrollingBehavior.DEFAULT_DEAD_ZONE);

                switch (ta.getInt(R.styleable.ANBottomNavigation_titleState, 1)){
                    case 1:
                        titleState = TitleState.SHOW_WHEN_ACTIVE;
                        break;
                    case 2:
                        titleState = TitleState.ALWAYS_SHOW;
                        break;
                    case 3:
                        titleState = TitleState.ALWAYS_HIDE;
                        break;
                }

                titleColorActive = ta.getColor(R.styleable.ANBottomNavigation_accentColor,
                        ContextCompat.getColor(context, R.color.colorBottomNavigationAccent));
                titleColorInactive = ta.getColor(R.styleable.ANBottomNavigation_inactiveColor,
                        ContextCompat.getColor(context, R.color.colorBottomNavigationInactive));

                itemDisableColor = ta.getColor(R.styleable.ANBottomNavigation_disableColor,
                        ContextCompat.getColor(context, R.color.colorBottomNavigationDisable));

                coloredTitleColorActive = ta.getColor(R.styleable.ANBottomNavigation_coloredActive,
                        ContextCompat.getColor(context, R.color.colorBottomNavigationActiveColored));
                coloredTitleColorInactive = ta.getColor(R.styleable.ANBottomNavigation_coloredInactive,
                        ContextCompat.getColor(context, R.color.colorBottomNavigationInactiveColored));

                colored = ta.getBoolean(R.styleable.ANBottomNavigation_colored, false);

                /*
                 * parse items from xml-menu resource
                 */
                int menuRes = ta.getResourceId(R.styleable.ANBottomNavigation_itemsMenu, 0);
                parseItemsFromXmlMenu(menuRes);

            } finally {
                ta.recycle();
            }
        }

        notificationTextColor = ContextCompat.getColor(context, android.R.color.white);
        bottomNavigationHeight = (int) resources.getDimension(R.dimen.bottom_navigation_height);

        if (colored)
            itemActiveColor = coloredTitleColorActive;
        else
            itemActiveColor = titleColorActive;

        if (colored)
            itemInactiveColor = coloredTitleColorInactive;
        else
            itemInactiveColor = titleColorInactive;

        // Notifications
        notificationActiveMarginLeft = (int) resources.getDimension(R.dimen.bottom_navigation_notification_margin_left_active);
        notificationInactiveMarginLeft = (int) resources.getDimension(R.dimen.bottom_navigation_notification_margin_left);
        notificationActiveMarginTop = (int) resources.getDimension(R.dimen.bottom_navigation_notification_margin_top_active);
        notificationInactiveMarginTop = (int) resources.getDimension(R.dimen.bottom_navigation_notification_margin_top);
        notificationAnimationDuration = 150;

        ViewCompat.setElevation(this, resources.getDimension(R.dimen.bottom_navigation_elevation));
        setClipToPadding(false);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, bottomNavigationHeight);
        setLayoutParams(params);
    }

    /**
     * parse items from xml-menu resource
     */
    @SuppressWarnings("deprecation")
    private void parseItemsFromXmlMenu(int menuRes){
        if(menuRes != 0){
            try {
                XmlResourceParser xml = resources.getXml(menuRes);

                int eventType = xml.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if(eventType == XmlPullParser.START_TAG) {
                        Integer itemTitleRes = null;
                        Integer itemIconRes = null;
                        Integer itemColorRes = null;
                        for (int i = 0; i < xml.getAttributeCount(); i++){
                            switch (xml.getAttributeName(i)){
                                case "title":
                                    itemTitleRes = Integer.parseInt(xml.getAttributeValue(i).substring(1));
                                    break;
                                case "icon":
                                    itemIconRes = Integer.parseInt(xml.getAttributeValue(i).substring(1));
                                    break;
                                case "color":
                                    itemColorRes = Integer.parseInt(xml.getAttributeValue(i).substring(1));
                                    break;
                            }
                        }

                        if(itemTitleRes != null && itemIconRes != null){
                            final ANBottomNavigationItem newItem;

                            if(itemColorRes == null){
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    newItem = new ANBottomNavigationItem(resources.getString(itemTitleRes), resources.getDrawable(itemIconRes, null));
                                }else{
                                    newItem = new ANBottomNavigationItem(resources.getString(itemTitleRes), resources.getDrawable(itemIconRes));
                                }
                            }else{
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        newItem = new ANBottomNavigationItem(resources.getString(itemTitleRes), resources.getDrawable(itemIconRes, null), resources.getColor(itemColorRes,null));
                                    }else{
                                        newItem = new ANBottomNavigationItem(resources.getString(itemTitleRes), resources.getDrawable(itemIconRes, null), resources.getColor(itemColorRes));
                                    }
                                }else{
                                    newItem = new ANBottomNavigationItem(resources.getString(itemTitleRes), resources.getDrawable(itemIconRes), resources.getColor(itemColorRes));
                                }
                            }
                            items.add(newItem);
                        }
                    }
                    eventType = xml.next();
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Wrong ANBottomNavigation itemsMenu attribute format");
            }
        }
    }


    /**
     * Create the items in the bottom navigation
     */
    private void createItems() {

        if (items.size() < MIN_ITEMS) {
            Log.w(TAG, "The items list should have at least 3 items");
        } else if (items.size() > MAX_ITEMS) {
            Log.w(TAG, "The items list should not have more than 5 items");
        }

        removeAllViews();
        views.clear();

        int layoutHeight = (int) resources.getDimension(R.dimen.bottom_navigation_height);

        backgroundColorView = new View(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout.LayoutParams backgroundLayoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, layoutHeight + insetBottom);
            addView(backgroundColorView, backgroundLayoutParams);
            bottomNavigationHeight = layoutHeight;
        }

        linearLayoutContainer = new LinearLayout(context);
        linearLayoutContainer.setOrientation(LinearLayout.HORIZONTAL);
        linearLayoutContainer.setGravity(Gravity.CENTER);

        linearLayoutContainer.setPadding(insetLeft,0,insetRight,0);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, layoutHeight);

        addView(linearLayoutContainer, layoutParams);

        if (titleState != TitleState.ALWAYS_HIDE && (items.size() == MIN_ITEMS || titleState == TitleState.ALWAYS_SHOW)) {
            createClassicItems(linearLayoutContainer);
        } else {
            createSmallItems(linearLayoutContainer);
        }

        // Force a request layout after all the items have been created
        post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
            }
        });
    }

    /**
     * Check if items must be classic
     *
     * @return true if classic (icon + title)
     */
    private boolean isClassic() {
        return (titleState == TitleState.ALWAYS_SHOW || items.size() <= MIN_ITEMS) && titleState != TitleState.ALWAYS_SHOW;
    }

    /**
     * Create classic items (only 3 items in the bottom navigation)
     *
     * @param linearLayout The layout where the items are added
     */
    private void createClassicItems(LinearLayout linearLayout) {

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        float height = resources.getDimension(R.dimen.bottom_navigation_height);
        float minWidth = resources.getDimension(R.dimen.bottom_navigation_min_width);
        float maxWidth = resources.getDimension(R.dimen.bottom_navigation_max_width);

        if (titleState == TitleState.ALWAYS_SHOW && items.size() > MIN_ITEMS) {
            minWidth = resources.getDimension(R.dimen.bottom_navigation_small_inactive_min_width);
            maxWidth = resources.getDimension(R.dimen.bottom_navigation_small_inactive_max_width);
        }

        int layoutWidth = getWidth();
        if (layoutWidth == 0 || items.size() == 0) {
            return;
        }

        float itemWidth = (float) layoutWidth / items.size();
        if (itemWidth < minWidth) {
            itemWidth = minWidth;
        } else if (itemWidth > maxWidth) {
            itemWidth = maxWidth;
        }

        float activeSize = resources.getDimension(R.dimen.bottom_navigation_text_size_active);
        float inactiveSize = resources.getDimension(R.dimen.bottom_navigation_text_size_inactive);
        int activePaddingTop = (int)resources.getDimension(R.dimen.bottom_navigation_margin_top_active);

        if (titleActiveTextSize != 0f && titleInactiveTextSize != 0f) {
            activeSize = titleActiveTextSize;
            inactiveSize = titleInactiveTextSize;
        } else if (titleState == TitleState.ALWAYS_SHOW && items.size() > MIN_ITEMS) {
            activeSize = resources.getDimension(R.dimen.bottom_navigation_text_size_forced_active);
            inactiveSize = resources.getDimension(R.dimen.bottom_navigation_text_size_forced_inactive);
        }

        for (int i = 0; i < items.size(); i++) {
            final boolean current = currentItem == i;
            final int itemIndex = i;
            ANBottomNavigationItem item = items.get(itemIndex);

            View view = inflater.inflate(R.layout.bottom_navigation_item, this, false);
            FrameLayout container = view.findViewById(R.id.bottom_navigation_container);
            ImageView icon = view.findViewById(R.id.bottom_navigation_item_icon);
            TextView title = view.findViewById(R.id.bottom_navigation_item_title);
            TextView notification = view.findViewById(R.id.bottom_navigation_notification);

            icon.setImageDrawable(item.getDrawable(context));
            title.setText(item.getTitle(context));

            if (titleTypeface != null) {
                title.setTypeface(titleTypeface);
            }

            if (titleState == TitleState.ALWAYS_SHOW && items.size() > MIN_ITEMS) {
                container.setPadding(0, container.getPaddingTop(), 0, container.getPaddingBottom());
            }

            if (current) {
                if (selectedBackgroundVisible) {
                    view.setSelected(true);
                }
                icon.setSelected(true);
                // Update margins (icon & notification)
                if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams)icon.getLayoutParams();
                    p.setMargins(p.leftMargin, activePaddingTop, p.rightMargin, p.bottomMargin);

                    ViewGroup.MarginLayoutParams paramsNotification = (ViewGroup.MarginLayoutParams)notification.getLayoutParams();
                    paramsNotification.setMargins(notificationActiveMarginLeft, paramsNotification.topMargin,
                            paramsNotification.rightMargin, paramsNotification.bottomMargin);

                    view.requestLayout();
                }
            } else {
                icon.setSelected(false);
                ViewGroup.MarginLayoutParams paramsNotification = (ViewGroup.MarginLayoutParams)notification.getLayoutParams();
                paramsNotification.setMargins(notificationInactiveMarginLeft, paramsNotification.topMargin,
                        paramsNotification.rightMargin, paramsNotification.bottomMargin);
            }

            if (colored) {
                if (current) {
                    setBackgroundColor(item.getColor(context));
                    currentColor = item.getColor(context);
                }
            } else {
                if (defaultBackgroundResource != 0) {
                    setBackgroundResource(defaultBackgroundResource);
                } else {
                    setBackgroundColor(defaultBackgroundColor);
                }
            }

            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, current ? activeSize : inactiveSize);

            if (itemsEnabledStates[i]) {
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        updateItems(itemIndex, true);
                    }
                });
                icon.setImageDrawable(ANHelper.getTintDrawable(items.get(i).getDrawable(context),
                        current ? itemActiveColor : itemInactiveColor, forceTint));
                title.setTextColor(current ? itemActiveColor : itemInactiveColor);
                view.setSoundEffectsEnabled(soundEffectsEnabled);
            } else {
                icon.setImageDrawable(ANHelper.getTintDrawable(items.get(i).getDrawable(context),
                        itemDisableColor, forceTint));
                title.setTextColor(itemDisableColor);
            }

            LayoutParams params = new LayoutParams((int) itemWidth, (int) height);
            linearLayout.addView(view, params);
            views.add(view);
        }

        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS);
    }

    /**
     * Create small items (more than 3 items in the bottom navigation)
     *
     * @param linearLayout The layout where the items are added
     */
    private void createSmallItems(LinearLayout linearLayout) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        float height = resources.getDimension(R.dimen.bottom_navigation_height);
        float minWidth = resources.getDimension(R.dimen.bottom_navigation_small_inactive_min_width);
        float maxWidth = resources.getDimension(R.dimen.bottom_navigation_small_inactive_max_width);

        int layoutWidth = getWidth();
        if (layoutWidth == 0 || items.size() == 0) {
            return;
        }

        float itemWidth = (float) layoutWidth / items.size();

        if (itemWidth < minWidth) {
            itemWidth = minWidth;
        } else if (itemWidth > maxWidth) {
            itemWidth = maxWidth;
        }

        int activeMarginTop = (int) resources.getDimension(R.dimen.bottom_navigation_small_margin_top_active);
        float difference = resources.getDimension(R.dimen.bottom_navigation_small_selected_width_difference);

        selectedItemWidth = itemWidth + items.size() * difference;
        itemWidth -= difference;
        notSelectedItemWidth = itemWidth;


        for (int i = 0; i < items.size(); i++) {

            final int itemIndex = i;
            ANBottomNavigationItem item = items.get(itemIndex);

            View view = inflater.inflate(R.layout.bottom_navigation_small_item, this, false);
            ImageView icon = view.findViewById(R.id.bottom_navigation_small_item_icon);
            TextView title = view.findViewById(R.id.bottom_navigation_small_item_title);
            TextView notification = view.findViewById(R.id.bottom_navigation_notification);
            icon.setImageDrawable(item.getDrawable(context));

            if (titleState != TitleState.ALWAYS_HIDE) {
                title.setText(item.getTitle(context));
            }

            if (titleActiveTextSize != 0) {
                title.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleActiveTextSize);
            }

            if (titleTypeface != null) {
                title.setTypeface(titleTypeface);
            }

            if (i == currentItem) {
                if (selectedBackgroundVisible) {
                    view.setSelected(true);
                }
                icon.setSelected(true);
                // Update margins (icon & notification)

                if (titleState != TitleState.ALWAYS_HIDE) {
                    if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) icon.getLayoutParams();
                        p.setMargins(p.leftMargin, activeMarginTop, p.rightMargin, p.bottomMargin);

                        ViewGroup.MarginLayoutParams paramsNotification = (ViewGroup.MarginLayoutParams)
                                notification.getLayoutParams();
                        paramsNotification.setMargins(notificationActiveMarginLeft, notificationActiveMarginTop,
                                paramsNotification.rightMargin, paramsNotification.bottomMargin);

                        view.requestLayout();
                    }
                }
            } else {
                icon.setSelected(false);
                ViewGroup.MarginLayoutParams paramsNotification = (ViewGroup.MarginLayoutParams)
                        notification.getLayoutParams();
                paramsNotification.setMargins(notificationInactiveMarginLeft, notificationInactiveMarginTop,
                        paramsNotification.rightMargin, paramsNotification.bottomMargin);
            }

            if (colored) {
                if (i == currentItem) {
                    setBackgroundColor(item.getColor(context));
                    currentColor = item.getColor(context);
                }
            } else {
                if (defaultBackgroundResource != 0) {
                    setBackgroundResource(defaultBackgroundResource);
                } else {
                    setBackgroundColor(defaultBackgroundColor);
                }
            }

            if (itemsEnabledStates[i]) {
                icon.setImageDrawable(ANHelper.getTintDrawable(items.get(i).getDrawable(context),
                        currentItem == i ? itemActiveColor : itemInactiveColor, forceTint));
                title.setTextColor(currentItem == i ? itemActiveColor : itemInactiveColor);
                title.setAlpha(currentItem == i ? 1 : 0);
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        updateSmallItems(itemIndex, true);
                    }
                });
                view.setSoundEffectsEnabled(soundEffectsEnabled);
            } else {
                icon.setImageDrawable(ANHelper.getTintDrawable(items.get(i).getDrawable(context),
                        itemDisableColor, forceTint));
                title.setTextColor(itemDisableColor);
                title.setAlpha(0);
            }

            int width = i == currentItem ? (int) selectedItemWidth :
                    (int) itemWidth;

            if (titleState == TitleState.ALWAYS_HIDE) {
                width = (int) (itemWidth * 1.16);
            }

            LayoutParams params = new LayoutParams(width, (int) height);
            linearLayout.addView(view, params);
            views.add(view);
        }

        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS);
    }

    /**
     * Update Items UI
     *
     * @param itemIndex   int: Selected item position
     * @param useCallback boolean: Use or not the callback
     */
    private void updateItems(final int itemIndex, boolean useCallback) {

        if (currentItem == itemIndex) {
            if (tabSelectedListener != null && useCallback) {
                tabSelectedListener.onTabSelected(itemIndex, true);
            }
            return;
        }

        if (tabSelectedListener != null && useCallback) {
            boolean selectionAllowed = tabSelectedListener.onTabSelected(itemIndex, false);
            if (!selectionAllowed) return;
        }

        int activeMarginTop = (int) resources.getDimension(R.dimen.bottom_navigation_margin_top_active);
        int inactiveMarginTop = (int) resources.getDimension(R.dimen.bottom_navigation_margin_top_inactive);
        float activeSize = resources.getDimension(R.dimen.bottom_navigation_text_size_active);
        float inactiveSize = resources.getDimension(R.dimen.bottom_navigation_text_size_inactive);

        if (titleActiveTextSize != 0 && titleInactiveTextSize != 0) {
            activeSize = titleActiveTextSize;
            inactiveSize = titleInactiveTextSize;
        } else if (titleState == TitleState.ALWAYS_SHOW && items.size() > MIN_ITEMS) {
            activeSize = resources.getDimension(R.dimen.bottom_navigation_text_size_forced_active);
            inactiveSize = resources.getDimension(R.dimen.bottom_navigation_text_size_forced_inactive);
        }

        for (int i = 0; i < views.size(); i++) {

            final View view = views.get(i);
            if (selectedBackgroundVisible) {
                view.setSelected(i == itemIndex);
            }

            if (i == itemIndex) {

                final TextView title = view.findViewById(R.id.bottom_navigation_item_title);
                final ImageView icon = view.findViewById(R.id.bottom_navigation_item_icon);
                final TextView notification = view.findViewById(R.id.bottom_navigation_notification);

                icon.setSelected(true);
                ANHelper.updateTopMargin(icon, inactiveMarginTop, activeMarginTop);
                ANHelper.updateLeftMargin(notification, notificationInactiveMarginLeft, notificationActiveMarginLeft);
                ANHelper.updateTextColor(title, itemInactiveColor, itemActiveColor);
                ANHelper.updateTextSize(title, inactiveSize, activeSize);
                ANHelper.updateDrawableColor(items.get(itemIndex).getDrawable(context), icon,
                        itemInactiveColor, itemActiveColor, forceTint);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && colored) {

                    int finalRadius = Math.max(getWidth(), getHeight());
                    int cx = (int) view.getX() + view.getWidth() / 2;
                    int cy = view.getHeight() / 2;

                    if (circleRevealAnim != null && circleRevealAnim.isRunning()) {
                        circleRevealAnim.cancel();
                        setBackgroundColor(items.get(itemIndex).getColor(context));
                        backgroundColorView.setBackgroundColor(Color.TRANSPARENT);
                    }

                    circleRevealAnim = ViewAnimationUtils.createCircularReveal(backgroundColorView, cx, cy, 0, finalRadius);
                    circleRevealAnim.setStartDelay(5);
                    circleRevealAnim.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            backgroundColorView.setBackgroundColor(items.get(itemIndex).getColor(context));
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            setBackgroundColor(items.get(itemIndex).getColor(context));
                            backgroundColorView.setBackgroundColor(Color.TRANSPARENT);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
                    circleRevealAnim.start();
                } else if (colored) {
                    ANHelper.updateViewBackgroundColor(this, currentColor,
                            items.get(itemIndex).getColor(context));
                } else {
                    if (defaultBackgroundResource != 0) {
                        setBackgroundResource(defaultBackgroundResource);
                    } else {
                        setBackgroundColor(defaultBackgroundColor);
                    }
                    backgroundColorView.setBackgroundColor(Color.TRANSPARENT);
                }

            } else if (i == currentItem) {

                final TextView title = view.findViewById(R.id.bottom_navigation_item_title);
                final ImageView icon = view.findViewById(R.id.bottom_navigation_item_icon);
                final TextView notification = view.findViewById(R.id.bottom_navigation_notification);

                icon.setSelected(false);
                ANHelper.updateTopMargin(icon, activeMarginTop, inactiveMarginTop);
                ANHelper.updateLeftMargin(notification, notificationActiveMarginLeft, notificationInactiveMarginLeft);
                ANHelper.updateTextColor(title, itemActiveColor, itemInactiveColor);
                ANHelper.updateTextSize(title, activeSize, inactiveSize);
                ANHelper.updateDrawableColor(items.get(currentItem).getDrawable(context), icon,
                        itemActiveColor, itemInactiveColor, forceTint);
            }
        }

        currentItem = itemIndex;
        if (currentItem > 0 && currentItem < items.size()) {
            currentColor = items.get(currentItem).getColor(context);
        } else if (currentItem == CURRENT_ITEM_NONE) {
            if (defaultBackgroundResource != 0) {
                setBackgroundResource(defaultBackgroundResource);
            } else {
                setBackgroundColor(defaultBackgroundColor);
            }
            backgroundColorView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    /**
     * Update Small items UI
     *
     * @param itemIndex   int: Selected item position
     * @param useCallback boolean: Use or not the callback
     */
    private void updateSmallItems(final int itemIndex, boolean useCallback) {

        if (currentItem == itemIndex) {
            if (tabSelectedListener != null && useCallback) {
                tabSelectedListener.onTabSelected(itemIndex, true);
            }
            return;
        }

        if (tabSelectedListener != null && useCallback) {
            boolean selectionAllowed = tabSelectedListener.onTabSelected(itemIndex, false);
            if (!selectionAllowed) return;
        }

        int activeMarginTop = (int) resources.getDimension(R.dimen.bottom_navigation_small_margin_top_active);
        int inactiveMargin = (int) resources.getDimension(R.dimen.bottom_navigation_small_margin_top);

        for (int i = 0; i < views.size(); i++) {

            View view = views.get(i);
            if (selectedBackgroundVisible) {
                view.setSelected(i == itemIndex);
            }

            if (i == itemIndex) {

                FrameLayout container = view.findViewById(R.id.bottom_navigation_small_container);
                TextView title = view.findViewById(R.id.bottom_navigation_small_item_title);
                ImageView icon = view.findViewById(R.id.bottom_navigation_small_item_icon);
                TextView notification = view.findViewById(R.id.bottom_navigation_notification);

                icon.setSelected(true);

                if (titleState != TitleState.ALWAYS_HIDE) {
                    ANHelper.updateTopMargin(icon, inactiveMargin, activeMarginTop);
                    ANHelper.updateLeftMargin(notification, notificationInactiveMarginLeft, notificationActiveMarginLeft);
                    ANHelper.updateTopMargin(notification, notificationInactiveMarginTop, notificationActiveMarginTop);
                    ANHelper.updateTextColor(title, itemInactiveColor, itemActiveColor);
                    ANHelper.updateWidth(container, notSelectedItemWidth, selectedItemWidth);
                }

                ANHelper.updateAlpha(title, 0f, 1f);
                ANHelper.updateDrawableColor(items.get(itemIndex).getDrawable(context), icon, itemInactiveColor,
                        itemActiveColor, forceTint);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && colored) {
                    int finalRadius = Math.max(getWidth(), getHeight());
                    int cx = (int) views.get(itemIndex).getX() + views.get(itemIndex).getWidth() / 2;
                    int cy = views.get(itemIndex).getHeight() / 2;

                    if (circleRevealAnim != null && circleRevealAnim.isRunning()) {
                        circleRevealAnim.cancel();
                        setBackgroundColor(items.get(itemIndex).getColor(context));
                        backgroundColorView.setBackgroundColor(Color.TRANSPARENT);
                    }

                    circleRevealAnim = ViewAnimationUtils.createCircularReveal(backgroundColorView, cx, cy, 0f, finalRadius);
                    circleRevealAnim.setStartDelay(5);
                    circleRevealAnim.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                            backgroundColorView.setBackgroundColor(items.get(itemIndex).getColor(context));
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            setBackgroundColor(items.get(itemIndex).getColor(context));
                            backgroundColorView.setBackgroundColor(Color.TRANSPARENT);
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });
                    circleRevealAnim.start();
                } else if (colored) {
                    ANHelper.updateViewBackgroundColor(this, currentColor,
                            items.get(itemIndex).getColor(context));
                } else {
                    if (defaultBackgroundResource != 0) {
                        setBackgroundResource(defaultBackgroundResource);
                    } else {
                        setBackgroundColor(defaultBackgroundColor);
                    }
                    backgroundColorView.setBackgroundColor(Color.TRANSPARENT);
                }

            } else if (i == currentItem) {

                FrameLayout container = view.findViewById(R.id.bottom_navigation_small_container);
                        TextView title = view.findViewById(R.id.bottom_navigation_small_item_title);
                ImageView icon = view.findViewById(R.id.bottom_navigation_small_item_icon);
                TextView notification = view.findViewById(R.id.bottom_navigation_notification);

                icon.setSelected(false);

                if (titleState != TitleState.ALWAYS_HIDE) {
                    ANHelper.updateTopMargin(icon, activeMarginTop, inactiveMargin);
                    ANHelper.updateLeftMargin(notification, notificationActiveMarginLeft, notificationInactiveMarginLeft);
                    ANHelper.updateTopMargin(notification, notificationActiveMarginTop, notificationInactiveMarginTop);
                    ANHelper.updateTextColor(title, itemActiveColor, itemInactiveColor);
                    ANHelper.updateWidth(container, selectedItemWidth, notSelectedItemWidth);
                }

                ANHelper.updateAlpha(title, 1f, 0f);
                ANHelper.updateDrawableColor(items.get(currentItem).getDrawable(context), icon, itemActiveColor,
                        itemInactiveColor, forceTint);
            }
        }

        currentItem = itemIndex;
        if (currentItem > 0 && currentItem < items.size()) {
            currentColor = items.get(currentItem).getColor(context);
        } else if (currentItem == CURRENT_ITEM_NONE) {
            if (defaultBackgroundResource != 0) {
                setBackgroundResource(defaultBackgroundResource);
            } else {
                setBackgroundColor(defaultBackgroundColor);
            }
            backgroundColorView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    /**
     * Update notifications
     */
    @SuppressWarnings("deprecation")
    private void updateNotifications(boolean updateStyle, int itemPosition) {

        for (int i = 0; i < views.size(); i++) {

            if (i >= notifications.size()) {
                break;
            }

            if (itemPosition != UPDATE_ALL_NOTIFICATIONS && itemPosition != i) {
                continue;
            }

            final ANNotification notificationItem = notifications.get(i);
            final int currentTextColor = ANNotificationHelper.getTextColor(notificationItem, notificationTextColor);
            final int currentBackgroundColor = ANNotificationHelper.getBackgroundColor(notificationItem, notificationBackgroundColor);

            TextView notification = views.get(i).findViewById(R.id.bottom_navigation_notification);

            String currentValue = notification.getText().toString();
            boolean animate = !currentValue.equals(String.valueOf(notificationItem.getText()));

            if (updateStyle) {
                notification.setTextColor(currentTextColor);
                if (notificationTypeface != null) {
                    notification.setTypeface(notificationTypeface);
                } else {
                    notification.setTypeface(null, Typeface.BOLD);
                }

                if (notificationBackgroundDrawable != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        Drawable drawable = notificationBackgroundDrawable.getConstantState().newDrawable();
                        notification.setBackground(drawable);
                    } else {
                        notification.setBackgroundDrawable(notificationBackgroundDrawable);
                    }

                } else if (currentBackgroundColor != 0) {
                    Drawable defautlDrawable = ContextCompat.getDrawable(context, R.drawable.notification_background);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        notification.setBackground(ANHelper.getTintDrawable(defautlDrawable,
                                currentBackgroundColor, forceTint));
                    } else {
                        notification.setBackgroundDrawable(ANHelper.getTintDrawable(defautlDrawable,
                                currentBackgroundColor, forceTint));
                    }
                }
            }

            if (notificationItem.isEmpty() && notification.getText().length() > 0) {
                notification.setText("");
                if (animate) {
                    notification.animate()
                            .scaleX(0)
                            .scaleY(0)
                            .alpha(0)
                            .setInterpolator(new AccelerateInterpolator())
                            .setDuration(notificationAnimationDuration)
                            .start();
                }
            } else if (!notificationItem.isEmpty()) {
                notification.setText(String.valueOf(notificationItem.getText()));
                if (animate) {
                    notification.setScaleX(0);
                    notification.setScaleY(0);
                    notification.animate()
                            .scaleX(1)
                            .scaleY(1)
                            .alpha(1)
                            .setInterpolator(new OvershootInterpolator())
                            .setDuration(notificationAnimationDuration)
                            .start();
                }
            }
        }
    }


    ////////////
    // PUBLIC //
    ////////////

    /**
     * Add an item
     */
    public void addItem(ANBottomNavigationItem item) {
        if (this.items.size() > MAX_ITEMS) {
            Log.w(TAG, "The items list should not have more than 5 items");
        }
        items.add(item);
        createItems();
    }

    /**
     * Add all items
     */
    public void addItems(List<ANBottomNavigationItem> items) {
        if (items.size() > MAX_ITEMS || this.items.size() + items.size() > MAX_ITEMS) {
            Log.w(TAG, "The items list should not have more than 5 items");
        }
        this.items.addAll(items);
        createItems();
    }

    /**
     * Remove an item at the given index
     */
    public void removeItemAtIndex(int index) {
        if (index < items.size()) {
            this.items.remove(index);
            createItems();
        }
    }

    /**
     * Remove all items
     */
    public void removeAllItems() {
        this.items.clear();
        createItems();
    }

    /**
     * Refresh the AHBottomView
     */
    public void refresh() {
        createItems();
    }

    /**
     * Return the number of items
     *
     * @return int
     */
    public int getItemsCount() {
        return items.size();
    }

    /**
     * Return if the Bottom Navigation is colored
     */
    public boolean isColored() {
        return colored;
    }

    /**
     * Set if the Bottom Navigation is colored
     */
    public void setColored(boolean colored) {
        this.colored = colored;
        this.itemActiveColor = colored ? coloredTitleColorActive : titleColorActive;
        this.itemInactiveColor = colored ? coloredTitleColorInactive : titleColorInactive;
        createItems();
    }

    /**
     * Return the bottom navigation background color
     *
     * @return The bottom navigation background color
     */
    public int getDefaultBackgroundColor() {
        return defaultBackgroundColor;
    }

    /**
     * Set the bottom navigation background color
     *
     * @param defaultBackgroundColor The bottom navigation background color
     */
    public void setDefaultBackgroundColor(@ColorInt int defaultBackgroundColor) {
        this.defaultBackgroundColor = defaultBackgroundColor;
        createItems();
    }

    /**
     * Set the bottom navigation background resource
     *
     * @param defaultBackgroundResource The bottom navigation background resource
     */
    public void setDefaultBackgroundResource(@DrawableRes int defaultBackgroundResource) {
        this.defaultBackgroundResource = defaultBackgroundResource;
        createItems();
    }

    /**
     * Get the accent color (used when the view contains 3 items)
     *
     * @return The default accent color
     */
    public int getAccentColor() {
        return itemActiveColor;
    }

    /**
     * Set the accent color (used when the view contains 3 items)
     *
     * @param accentColor The new accent color
     */
    public void setAccentColor(int accentColor) {
        this.titleColorActive = accentColor;
        this.itemActiveColor = accentColor;
        createItems();
    }

    /**
     * Get the inactive color (used when the view contains 3 items)
     *
     * @return The inactive color
     */
    public int getInactiveColor() {
        return itemInactiveColor;
    }

    /**
     * Set the inactive color (used when the view contains 3 items)
     *
     * @param inactiveColor The inactive color
     */
    public void setInactiveColor(int inactiveColor) {
        this.titleColorInactive = inactiveColor;
        this.itemInactiveColor = inactiveColor;
        createItems();
    }

    /**
     * Set the colors used when the bottom bar uses the colored mode
     *
     * @param colorActive   The active color
     * @param colorInactive The inactive color
     */
    public void setColoredModeColors(@ColorInt int colorActive, @ColorInt int colorInactive) {
        this.coloredTitleColorActive = colorActive;
        this.coloredTitleColorInactive = colorInactive;
        createItems();
    }

    /**
     * Set selected background visibility
     */
    public void setSelectedBackgroundVisible(boolean visible) {
        this.selectedBackgroundVisible = visible;
        createItems();
    }

    /**
     * Set notification typeface
     *
     * @param typeface Typeface
     */
    public void setTitleTypeface(Typeface typeface) {
        this.titleTypeface = typeface;
        createItems();
    }

    /**
     * Set title text size in pixels
     *
     * @param activeSize
     * @param inactiveSize
     */
    public void setTitleTextSize(float activeSize, float inactiveSize) {
        this.titleActiveTextSize = activeSize;
        this.titleInactiveTextSize = inactiveSize;
        createItems();
    }

    /**
     * Set title text size in SP
     *
     * +	 * @param activeSize in sp
     * +	 * @param inactiveSize in sp
     */
    public void setTitleTextSizeInSp(float activeSize, float inactiveSize) {
        this.titleActiveTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, activeSize, resources.getDisplayMetrics());
        this.titleInactiveTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, inactiveSize, resources.getDisplayMetrics());
        createItems();
    }

    /**
     * Get item at the given index
     *
     * @param position int: item position
     * @return The item at the given position
     */
    @Nullable
    public ANBottomNavigationItem getItem(int position) {
        if (position < 0 || position > items.size() - 1) {
            Log.w(TAG, "The position is out of bounds of the items (" + items.size() + " elements)");
            return null;
        }
        return items.get(position);
    }

    /**
     * Get the current item
     *
     * @return The current item position
     */
    public int getCurrentItem() {
        return currentItem;
    }

    /**
     * Set the current item
     *
     * @param position int: position
     */
    public void setCurrentItem(int position) {
        setCurrentItem(position, true);
    }

    /**
     * Set the current item
     *
     * @param position    int: item position
     * @param useCallback boolean: use or not the callback
     */
    public void setCurrentItem(int position, boolean useCallback) {
        if (position >= items.size()) {
            Log.w(TAG, "The position is out of bounds of the items (" + items.size() + " elements)");
            return;
        }

        if (titleState != TitleState.ALWAYS_HIDE && (items.size() == MIN_ITEMS || titleState == TitleState.ALWAYS_SHOW)) {
            updateItems(position, useCallback);
        } else {
            updateSmallItems(position, useCallback);
        }
    }

    /**
     * Hide Bottom Navigation with animation
     */
    public void hideBottomNavigation() {
        hideBottomNavigation(true);
    }

    /**
     * Hide Bottom Navigation with or without animation
     *
     * @param withAnimation Boolean
     */
    public void hideBottomNavigation(boolean withAnimation) {
        if (bottomNavigationBehavior != null) {
            bottomNavigationBehavior.hideView(this, bottomNavigationHeight + insetBottom, withAnimation);
        } else if (getParent() instanceof CoordinatorLayout) {
            needHideBottomNavigation = true;
            hideBottomNavigationWithAnimation = withAnimation;
        } else {
            // Hide bottom navigation
            ViewCompat.animate(this)
                    .translationY(bottomNavigationHeight)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setDuration(withAnimation ? 300 : 0)
                    .start();
        }
    }

    /**
     * Restore Bottom Navigation with animation
     */
    public void restoreBottomNavigation() {
        restoreBottomNavigation(true);
    }

    /**
     * Restore Bottom Navigation with or without animation
     *
     * @param withAnimation Boolean
     */
    public void restoreBottomNavigation(boolean withAnimation) {
        if (bottomNavigationBehavior != null) {
            bottomNavigationBehavior.resetOffset(this, withAnimation);
        } else {
            // Show bottom navigation
            ViewCompat.animate(this)
                    .translationY(0f)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setDuration(withAnimation ? 300 : 0)
                    .start();
        }
    }

    /**
     * Return if the tint should be forced (with setColorFilter)
     *
     * @return Boolean
     */
    public boolean isForceTint() {
        return forceTint;
    }

    /**
     * Set the force tint value
     * If forceTint = true, the tint is made with drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
     *
     * @param forceTint Boolean
     */
    public void  setForceTint(boolean forceTint) {
        this.forceTint = forceTint;
        createItems();
    }

    /**
     * Return the title state for display
     *
     * @return TitleState
     */
    public TitleState getTitleState() {
        return titleState;
    }

    /**
     * Sets the title state for each tab
     * SHOW_WHEN_ACTIVE: when a tab is focused
     * ALWAYS_SHOW: show regardless of which tab is in focus
     * ALWAYS_HIDE: never show tab titles
     * Note: Always showing the title is against Material Design guidelines
     *
     * @param titleState TitleState
     */
    public void  setTitleState(TitleState titleState) {
        this.titleState = titleState;
        createItems();
    }

    /**
     * Set AHOnTabSelectedListener
     */
    public void  setOnTabSelectedListener(OnTabSelectedListener tabSelectedListener) {
        this.tabSelectedListener = tabSelectedListener;
    }

    /**
     * Remove AHOnTabSelectedListener
     */
    public void removeOnTabSelectedListener() {
        this.tabSelectedListener = null;
    }

    /**
     * Set notification text
     *
     * @param title        String
     * @param itemPosition int
     */
    public void setNotification(String title, int itemPosition) {
        if (itemPosition < 0 || itemPosition > items.size() - 1) {
            throw new IndexOutOfBoundsException(String.format(Locale.US, EXCEPTION_INDEX_OUT_OF_BOUNDS, itemPosition, items.size()));
        }

        notifications.set(itemPosition, ANNotification.justText(title));
        updateNotifications(false, itemPosition);
    }

    /**
     * Set fully customized Notification
     *
     * @param inputNotification AHNotification
     * @param itemPosition Int
     */
    public void setNotification(@Nullable ANNotification inputNotification, int itemPosition) {
        ANNotification notification = inputNotification;
        if (itemPosition < 0 || itemPosition > items.size() - 1) {
            throw new IndexOutOfBoundsException(String.format(Locale.US, EXCEPTION_INDEX_OUT_OF_BOUNDS, itemPosition, items.size()));
        }
        if (notification == null) {
            notification = new ANNotification(); // instead of null, use empty notification
        }
        notifications.set(itemPosition, notification);
        updateNotifications(true, itemPosition);
    }

    /**
     * Set notification text color
     *
     * @param textColor int
     */
    public void setNotificationTextColor(@ColorInt int textColor) {
        this.notificationTextColor = textColor;
        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS);
    }

    /**
     * Set notification text color
     *
     * @param textColor int
     */
    public void setNotificationTextColorResource(@ColorRes int textColor) {
        this.notificationTextColor = ContextCompat.getColor(context, textColor);
        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS);
    }

    /**
     * Set notification background resource
     *
     * @param drawable Drawable
     */
    public void setNotificationBackground(Drawable drawable) {
        this.notificationBackgroundDrawable = drawable;
        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS);
    }

    /**
     * Set notification background color
     *
     * @param color int
     */
    public void setNotificationBackgroundColor(@ColorInt int color) {
        this.notificationBackgroundColor = color;
        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS);
    }

    /**
     * Set notification background color
     *
     * @param color int
     */
    public void setNotificationBackgroundColorResource(@ColorRes int color) {
        this.notificationBackgroundColor = ContextCompat.getColor(context, color);
        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS);
    }

    /**
     * Set notification typeface
     *
     * @param typeface Typeface
     */
    public void setNotificationTypeface(Typeface typeface) {
        this.notificationTypeface = typeface;
        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS);
    }

    public void setNotificationAnimationDuration(long notificationAnimationDuration) {
        this.notificationAnimationDuration = notificationAnimationDuration;
        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS);
    }

    /**
     * Set the notification margin left
     *
     * @param activeMargin
     * @param inactiveMargin
     */
    public void setNotificationMarginLeft(int activeMargin, int inactiveMargin) {
        this.notificationActiveMarginLeft = activeMargin;
        this.notificationInactiveMarginLeft = inactiveMargin;
        createItems();
    }

    /**
     * Activate or not the elevation
     *
     * @param useElevation boolean
     */
    public void setUseElevation(boolean useElevation) {
        ViewCompat.setElevation(this, useElevation ? resources.getDimension(R.dimen.bottom_navigation_elevation) : 0f);
        setClipToPadding(false);
    }

    /**
     * Activate or not the elevation, and set the value
     *
     * @param useElevation boolean
     * @param elevation    float
     */
    public void setUseElevation(boolean useElevation, float elevation) {
        ViewCompat.setElevation(this, useElevation ? elevation : 0f);
        setClipToPadding(false);
    }

    /**
     * Return if the Bottom Navigation is hidden or not
     */
    public boolean isHidden() {
        if (bottomNavigationBehavior != null) {
            return bottomNavigationBehavior.isHidden();
        } else
            return false;
    }

    /**
     * Get the view at the given position
     * @param position int
     * @return The view at the position, or null
     */
    public View getViewAtPosition(int position) {
        if (linearLayoutContainer != null && position >= 0
                && position < linearLayoutContainer.getChildCount()) {
            return linearLayoutContainer.getChildAt(position);
        } else
            return null;
    }

    /**
     * Enable the tab item at the given position
     * @param position int
     */
    public void enableItemAtPosition(int position) {
        if (position < 0 || position > items.size() - 1) {
            Log.w(TAG, "The position is out of bounds of the items (" + items.size() + " elements)");
            return;
        }
        itemsEnabledStates[position] = true;
        createItems();
    }

    /**
     * Disable the tab item at the given position
     * @param position int
     */
    public void disableItemAtPosition(int position) {
        if (position < 0 || position > items.size() - 1) {
            Log.w(TAG, "The position is out of bounds of the items (" + items.size() + " elements)");
            return;
        }
        itemsEnabledStates[position] = false;
        createItems();
    }

    /**
     * Set the item disable color
     * @param itemDisableColor int
     */
    public void setItemDisableColor(@ColorInt int itemDisableColor) {
        this.itemDisableColor = itemDisableColor;
    }

    public interface OnTabSelectedListener {
        /**
         * Called when a tab has been selected (clicked)
         *
         * @param position    int: Position of the selected tab
         * @param wasSelected boolean: true if the tab was already selected
         * @return boolean: true for updating the tab UI, false otherwise
         */
        boolean onTabSelected(int position, boolean wasSelected);
    }
}
