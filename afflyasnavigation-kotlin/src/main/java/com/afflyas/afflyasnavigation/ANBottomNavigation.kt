package com.afflyas.afflyasnavigation

import android.animation.Animator
import android.content.Context
import android.content.res.Configuration
import android.content.res.XmlResourceParser
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.afflyas.afflyasnavigation.notification.ANNotification
import com.afflyas.afflyasnavigation.notification.ANNotificationHelper
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.util.*

class ANBottomNavigation : FrameLayout {

    companion object {
        private const val CURRENT_ITEM_NONE = -1
        private const val UPDATE_ALL_NOTIFICATIONS = -1

        private const val TAG = "AHBottomNavigation"
        private const val EXCEPTION_INDEX_OUT_OF_BOUNDS = "The position (%d) is out of bounds of the items (%d elements)"
        private const val MIN_ITEMS = 3
        private const val MAX_ITEMS = 5

    }

    // Title state
    enum class TitleState {
        SHOW_WHEN_ACTIVE,
        ALWAYS_SHOW,
        ALWAYS_HIDE
    }

    // Listener
    private var tabSelectedListener: OnTabSelectedListener? = null

    // Variables
    private var translucentNavigationThemeEnabled: Boolean = false
    private val items = ArrayList<ANBottomNavigationItem>()
    private val views = ArrayList<View>()
    private var bottomNavigationBehavior: ANBottomNavigationBehavior<ANBottomNavigation>? = null
    private var linearLayoutContainer: LinearLayout? = null
    private var backgroundColorView: View? = null
    private var circleRevealAnim: Animator? = null
    private var colored = false
    private var selectedBackgroundVisible = false
    private var notifications: ArrayList<ANNotification>? = ANNotification.generateEmptyList(MAX_ITEMS)
    private val itemsEnabledStates = arrayOf(true, true, true, true, true)
    private var currentItem = 0
    private var currentColor = 0
    private var needHideBottomNavigation = false
    private var hideBottomNavigationWithAnimation = false
    private var soundEffectsEnabled = true

    var behaviorTranslationEnabled = true
        private set

    var scrollingDeadZone = VerticalScrollingBehavior.DEFAULT_DEAD_ZONE

    /**
     * indicates that insets were set
     */
    private var isInsetsSet = false
    /**
     * system window inset values that has been set as padding
     */
    private var insetLeft = 0
    private var insetRight = 0
    private var insetBottom = 0

    // Variables (Styles)
    private var titleTypeface: Typeface? = null
    private var defaultBackgroundColor = Color.WHITE
    private var defaultBackgroundResource = 0
    @ColorInt private var itemActiveColor: Int = 0
    @ColorInt private var itemInactiveColor: Int = 0
    @ColorInt private var titleColorActive: Int = 0
    @ColorInt private var itemDisableColor: Int = 0
    @ColorInt private var titleColorInactive: Int = 0
    @ColorInt private var coloredTitleColorActive: Int = 0
    @ColorInt private var coloredTitleColorInactive: Int = 0
    private var titleActiveTextSize: Float = 0.toFloat()
    private var titleInactiveTextSize:Float = 0.toFloat()
    private var bottomNavigationHeight: Int = 0
    private var navigationBarHeight = 0
    private var selectedItemWidth: Float = 0.toFloat()
    private var notSelectedItemWidth:Float = 0.toFloat()
    private var forceTint = false
    private var titleState = TitleState.SHOW_WHEN_ACTIVE

    // Notifications
    @ColorInt private var notificationTextColor: Int = 0
    @ColorInt private var notificationBackgroundColor: Int = 0
    private var notificationBackgroundDrawable: Drawable? = null
    private var notificationTypeface: Typeface? = null
    private var notificationActiveMarginLeft: Int = 0
    private var notificationInactiveMarginLeft:Int = 0
    private var notificationActiveMarginTop: Int = 0
    private var notificationInactiveMarginTop:Int = 0
    private var notificationAnimationDuration: Long = 0

    /**
     * Constructors
     */
    constructor(context: Context) : super(context){
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs){
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr){
        init(context, attrs)
    }

    override fun setSoundEffectsEnabled(soundEffectsEnabled: Boolean) {
        super.setSoundEffectsEnabled(soundEffectsEnabled)
        this.soundEffectsEnabled = soundEffectsEnabled
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        createItems()
    }

    /**
     * Setup showing/hiding behavior on scrolling
     */
    private fun setupBehaviorTranslation(){
        if (parent is CoordinatorLayout) {
            if (bottomNavigationBehavior == null) {
                bottomNavigationBehavior = ANBottomNavigationBehavior(behaviorTranslationEnabled, insetLeft, insetRight, insetBottom)
                bottomNavigationBehavior!!.scrollingDeadZone = scrollingDeadZone
            } else {
                bottomNavigationBehavior!!.setBehaviorTranslationEnabled(behaviorTranslationEnabled)
                bottomNavigationBehavior!!.setInsets(insetLeft, insetRight, insetBottom)
                bottomNavigationBehavior!!.scrollingDeadZone = scrollingDeadZone
            }

            (layoutParams as CoordinatorLayout.LayoutParams).behavior = bottomNavigationBehavior

            if (needHideBottomNavigation) {
                needHideBottomNavigation = false
                bottomNavigationBehavior!!.hideView(this, bottomNavigationHeight, hideBottomNavigationWithAnimation)
            }
        }
    }

    /**
     * Set the behavior translation value
     *
     * @param behaviorTranslationEnabled boolean for the state
     */
    fun enableBehaviorTranslation(behaviorTranslationEnabled: Boolean) {
        this.behaviorTranslationEnabled = behaviorTranslationEnabled
        setupBehaviorTranslation()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun calcInsets(){
        var insetLeft = 0
        var insetRight = 0
        var insetBottom = 0

        when{
        /**
         * P(or later) insets with cutout support
         */
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                val insets = rootView.rootWindowInsets
                val notch = insets.displayCutout

//                    Log.d("development", "stable insets\n" +
//                            "systemWindowInsetTop = ${insets.stableInsetTop}\n" +
//                            "systemWindowInsetBottom = ${insets.stableInsetBottom}\n" +
//                            "systemWindowInsetLeft = ${insets.stableInsetLeft}\n" +
//                            "systemWindowInsetRight = ${insets.stableInsetRight}\n" +
//                            "")
//                    Log.d("development", "system insets\n" +
//                            "systemWindowInsetTop = ${insets.systemWindowInsetTop}\n" +
//                            "systemWindowInsetBottom = ${insets.systemWindowInsetBottom}\n" +
//                            "systemWindowInsetLeft = ${insets.systemWindowInsetLeft}\n" +
//                            "systemWindowInsetRight = ${insets.systemWindowInsetRight}\n" +
//                            "")
//                    Log.d("development", "displayCutout\n" +
//                            "systemWindowInsetTop = ${insets.displayCutout.safeInsetTop}\n" +
//                            "systemWindowInsetBottom = ${insets.displayCutout.safeInsetBottom}\n" +
//                            "systemWindowInsetLeft = ${insets.displayCutout.safeInsetLeft}\n" +
//                            "systemWindowInsetRight = ${insets.displayCutout.safeInsetRight}\n" +
//                            "")

                if(ANHelper.isInMultiWindow(context)){
                    if(notch != null){
                        insetLeft = insets.systemWindowInsetLeft
                        insetRight = insets.systemWindowInsetRight
                        insetBottom = insets.systemWindowInsetBottom
                        /**
                         * stable insets -insets without notch
                         */
                        if(insets.stableInsetLeft != 0) insetLeft = 0
                        if(insets.stableInsetRight != 0) insetRight = 0
                        if(insets.stableInsetBottom != 0) insetBottom = 0
                    }
                }else{
                    if(translucentNavigationThemeEnabled){
                        insetBottom = insets.systemWindowInsetBottom
                        insetLeft = insets.systemWindowInsetLeft
                        insetRight = insets.systemWindowInsetRight
                    }else{
                        if(notch != null){
                            insetLeft = notch.safeInsetLeft
                            insetRight = notch.safeInsetRight
                            insetBottom = notch.safeInsetBottom
                            /**
                             * stable insets -insets without notch
                             */
                            if(insets.stableInsetLeft != 0) insetLeft = 0
                            if(insets.stableInsetRight != 0) insetRight = 0
                            if(insets.stableInsetBottom != 0) insetBottom = 0
                        }
                    }
                }
            }
        /**
         * Nougat and Oreo insets
         */
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                val insets = rootView.rootWindowInsets
                if(translucentNavigationThemeEnabled && !ANHelper.isInMultiWindow(context)){
                    insetBottom = insets.systemWindowInsetBottom
                    insetLeft = insets.systemWindowInsetLeft
                    insetRight = insets.systemWindowInsetRight
                }
            }
        /**
         * Marshmallow insets
         */
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val insets = rootView.rootWindowInsets
                if(translucentNavigationThemeEnabled){
                    insetBottom = insets.systemWindowInsetBottom
                    insetLeft = insets.systemWindowInsetLeft
                    insetRight = insets.systemWindowInsetRight
                }
            }
        /**
         * Lollipop insets
         */
            else -> {
                if(translucentNavigationThemeEnabled && ANHelper.hasNavigationBar(context)){
                    if(resources.configuration.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)){
                        insetBottom = context.resources.getDimensionPixelOffset(R.dimen.navigation_bar_height)
                    }else{
                        if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                            insetRight = context.resources.getDimensionPixelOffset(R.dimen.navigation_bar_height)
                        }else{
                            insetBottom = context.resources.getDimensionPixelOffset(R.dimen.navigation_bar_height)
                        }
                    }
                }
            }
        }
        /**
         * Check if these values are already set
         */
        if(insetLeft != this.insetLeft || insetRight != this.insetRight || insetBottom != this.insetBottom){
            this.insetLeft = insetLeft
            this.insetRight = insetRight
            this.insetBottom = insetBottom

            //createItems()
            isInsetsSet = false
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            calcInsets()
        }else{
            /**
             * display bottom bar above other elements for Kitkat and older
             */
            bringToFront()
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        if(!isInsetsSet){
            isInsetsSet = true

            createItems()

            setupBehaviorTranslation()

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                bringToFront()
            }
        }
        super.onDraw(canvas)
    }



    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())
        bundle.putInt("current_item", currentItem)
        bundle.putParcelableArrayList("notifications", ArrayList<ANNotification>(notifications!!))
        return bundle
    }

    override fun onRestoreInstanceState(inputState: Parcelable?) {
        var state = inputState
        if (state is Bundle) {
            val bundle = state as Bundle?
            currentItem = bundle!!.getInt("current_item")

            notifications = bundle.getParcelableArrayList<ANNotification>("notifications")
            state = bundle.getParcelable("superState")
        }
        super.onRestoreInstanceState(state)
    }

    /////////////
    // PRIVATE //
    /////////////

    /**
     * Init
     *
     * @param context
     */
    private fun init(context: Context, attrs: AttributeSet?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            translucentNavigationThemeEnabled = ANHelper.isTranslucentNavigationThemeEnabled(context)
        }

        isClickable = true

        // Item colors
        titleColorActive = ContextCompat.getColor(context, R.color.colorBottomNavigationAccent)
        titleColorInactive = ContextCompat.getColor(context, R.color.colorBottomNavigationInactive)
        itemDisableColor = ContextCompat.getColor(context, R.color.colorBottomNavigationDisable)

        // Colors for colored bottom navigation
        coloredTitleColorActive = ContextCompat.getColor(context, R.color.colorBottomNavigationActiveColored)
        coloredTitleColorInactive = ContextCompat.getColor(context, R.color.colorBottomNavigationInactiveColored)

        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.ANBottomNavigation, 0, 0)
            try {
                selectedBackgroundVisible = ta.getBoolean(R.styleable.ANBottomNavigation_selectedBackgroundVisible, false)

                behaviorTranslationEnabled = ta.getBoolean(R.styleable.ANBottomNavigation_behaviorTranslationEnabled, false)
                scrollingDeadZone = ta.getInteger(R.styleable.ANBottomNavigation_scrollingDeadZone, VerticalScrollingBehavior.DEFAULT_DEAD_ZONE)


                when(ta.getInt(R.styleable.ANBottomNavigation_titleState, 1)){
                    1 -> titleState = TitleState.SHOW_WHEN_ACTIVE
                    2 -> titleState = TitleState.ALWAYS_SHOW
                    3 -> titleState = TitleState.ALWAYS_HIDE
                }

                titleColorActive = ta.getColor(R.styleable.ANBottomNavigation_accentColor,
                        ContextCompat.getColor(context, R.color.colorBottomNavigationAccent))
                titleColorInactive = ta.getColor(R.styleable.ANBottomNavigation_inactiveColor,
                        ContextCompat.getColor(context, R.color.colorBottomNavigationInactive))

                itemDisableColor = ta.getColor(R.styleable.ANBottomNavigation_disableColor,
                        ContextCompat.getColor(context, R.color.colorBottomNavigationDisable))

                coloredTitleColorActive = ta.getColor(R.styleable.ANBottomNavigation_coloredActive,
                        ContextCompat.getColor(context, R.color.colorBottomNavigationActiveColored))
                coloredTitleColorInactive = ta.getColor(R.styleable.ANBottomNavigation_coloredInactive,
                        ContextCompat.getColor(context, R.color.colorBottomNavigationInactiveColored))

                colored = ta.getBoolean(R.styleable.ANBottomNavigation_colored, false)

                /**
                 * parse items from xml-menu resource
                 */
                val menuRes = ta.getResourceId(R.styleable.ANBottomNavigation_itemsMenu, 0)
                parseItemsFromXmlMenu(menuRes)

            } finally {
                ta.recycle()
            }
        }

        notificationTextColor = ContextCompat.getColor(context, android.R.color.white)
        bottomNavigationHeight = resources.getDimension(R.dimen.bottom_navigation_height).toInt()


        itemActiveColor = if (colored) coloredTitleColorActive else titleColorActive
        itemInactiveColor = if (colored) coloredTitleColorInactive else titleColorInactive


        // Notifications
        notificationActiveMarginLeft = resources.getDimension(R.dimen.bottom_navigation_notification_margin_left_active).toInt()
        notificationInactiveMarginLeft = resources.getDimension(R.dimen.bottom_navigation_notification_margin_left).toInt()
        notificationActiveMarginTop = resources.getDimension(R.dimen.bottom_navigation_notification_margin_top_active).toInt()
        notificationInactiveMarginTop = resources.getDimension(R.dimen.bottom_navigation_notification_margin_top).toInt()
        notificationAnimationDuration = 150

        ViewCompat.setElevation(this, resources.getDimension(R.dimen.bottom_navigation_elevation))
        clipToPadding = false

        val params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, bottomNavigationHeight)
        layoutParams = params
    }

    /**
     * parse items from xml-menu resource
     */
    @Suppress("DEPRECATION")
    private fun parseItemsFromXmlMenu(menuRes: Int){
        if(menuRes != 0){
            try {
                val xml: XmlResourceParser = resources.getXml(menuRes)

                var eventType = xml.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if(eventType == XmlPullParser.START_TAG) {
                        var itemTitleRes: Int? = null
                        var itemIconRes: Int? = null
                        var itemColorRes: Int? = null
                        for (i in 0..(xml.attributeCount - 1)){
                            when(xml.getAttributeName(i)){
                                "title" -> {
                                    itemTitleRes = Integer.parseInt(xml.getAttributeValue(i).substring(1))
                                }
                                "icon" -> {
                                    itemIconRes = Integer.parseInt(xml.getAttributeValue(i).substring(1))
                                }
                                "color" -> {
                                    itemColorRes = Integer.parseInt(xml.getAttributeValue(i).substring(1))
                                }
                            }
                        }

                        if(itemTitleRes != null && itemIconRes != null){
                            val newItem: ANBottomNavigationItem

                            if(itemColorRes == null){
                                newItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    ANBottomNavigationItem(resources.getString(itemTitleRes), resources.getDrawable(itemIconRes, null))
                                }else{
                                    ANBottomNavigationItem(resources.getString(itemTitleRes), resources.getDrawable(itemIconRes))
                                }
                            }else{
                                newItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        ANBottomNavigationItem(resources.getString(itemTitleRes), resources.getDrawable(itemIconRes, null), resources.getColor(itemColorRes,null))
                                    }else{
                                        ANBottomNavigationItem(resources.getString(itemTitleRes), resources.getDrawable(itemIconRes, null), resources.getColor(itemColorRes))
                                    }
                                }else{

                                    ANBottomNavigationItem(resources.getString(itemTitleRes), resources.getDrawable(itemIconRes), resources.getColor(itemColorRes))
                                }
                            }
                            items.add(newItem)
                        }
                    }
                    eventType = xml.next()
                }
            } catch (e: XmlPullParserException) {
                throw IllegalArgumentException("Wrong ANBottomNavigation itemsMenu attribute format")
            }
        }
    }


    /**
     * Create the items in the bottom navigation
     */
    private fun createItems() {

        if (items.size < MIN_ITEMS) {
            Log.w(TAG, "The items list should have at least 3 items")
        } else if (items.size > MAX_ITEMS) {
            Log.w(TAG, "The items list should not have more than 5 items")
        }

        removeAllViews()
        views.clear()

        val layoutHeight = resources.getDimension(R.dimen.bottom_navigation_height).toInt()

        backgroundColorView = View(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val backgroundLayoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, layoutHeight + insetBottom)
            addView(backgroundColorView, backgroundLayoutParams)
            bottomNavigationHeight = layoutHeight
        }

        linearLayoutContainer = LinearLayout(context)
        linearLayoutContainer!!.orientation = LinearLayout.HORIZONTAL
        linearLayoutContainer!!.gravity = Gravity.CENTER

        linearLayoutContainer!!.setPadding(insetLeft,0,insetRight,0)

        val layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, layoutHeight)

        addView(linearLayoutContainer, layoutParams)

        if (titleState != TitleState.ALWAYS_HIDE && (items.size == MIN_ITEMS || titleState == TitleState.ALWAYS_SHOW)) {
            createClassicItems(linearLayoutContainer!!)
        } else {
            createSmallItems(linearLayoutContainer!!)
        }

        // Force a request layout after all the items have been created
        post { requestLayout() }
    }

    /**
     * Check if items must be classic
     *
     * @return true if classic (icon + title)
     */
    private fun isClassic(): Boolean {
        return titleState == TitleState.ALWAYS_SHOW || items.size <= MIN_ITEMS && titleState != TitleState.ALWAYS_SHOW
    }

    /**
     * Create classic items (only 3 items in the bottom navigation)
     *
     * @param linearLayout The layout where the items are added
     */
    private fun createClassicItems(linearLayout: LinearLayout) {

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val height = resources.getDimension(R.dimen.bottom_navigation_height)
        var minWidth = resources.getDimension(R.dimen.bottom_navigation_min_width)
        var maxWidth = resources.getDimension(R.dimen.bottom_navigation_max_width)

        if (titleState == TitleState.ALWAYS_SHOW && items.size > MIN_ITEMS) {
            minWidth = resources.getDimension(R.dimen.bottom_navigation_small_inactive_min_width)
            maxWidth = resources.getDimension(R.dimen.bottom_navigation_small_inactive_max_width)
        }

        val layoutWidth = width
        if (layoutWidth == 0 || items.size == 0) {
            return
        }

        var itemWidth = (layoutWidth / items.size).toFloat()
        if (itemWidth < minWidth) {
            itemWidth = minWidth
        } else if (itemWidth > maxWidth) {
            itemWidth = maxWidth
        }

        var activeSize = resources.getDimension(R.dimen.bottom_navigation_text_size_active)
        var inactiveSize = resources.getDimension(R.dimen.bottom_navigation_text_size_inactive)
        val activePaddingTop = resources.getDimension(R.dimen.bottom_navigation_margin_top_active).toInt()

        if (titleActiveTextSize != 0f && titleInactiveTextSize != 0f) {
            activeSize = titleActiveTextSize
            inactiveSize = titleInactiveTextSize
        } else if (titleState == TitleState.ALWAYS_SHOW && items.size > MIN_ITEMS) {
            activeSize = resources.getDimension(R.dimen.bottom_navigation_text_size_forced_active)
            inactiveSize = resources.getDimension(R.dimen.bottom_navigation_text_size_forced_inactive)
        }

        for (i in items.indices) {
            val current = currentItem == i
            val item = items[i]

            val view = inflater.inflate(R.layout.bottom_navigation_item, this, false)
            val container = view.findViewById(R.id.bottom_navigation_container) as FrameLayout
            val icon = view.findViewById(R.id.bottom_navigation_item_icon) as ImageView
            val title = view.findViewById(R.id.bottom_navigation_item_title) as TextView
            val notification = view.findViewById(R.id.bottom_navigation_notification) as TextView

            icon.setImageDrawable(item.getDrawable(context))
            title.text = item.getTitle(context)

            if (titleTypeface != null) {
                title.typeface = titleTypeface
            }

            if (titleState == TitleState.ALWAYS_SHOW && items.size > MIN_ITEMS) {
                container.setPadding(0, container.paddingTop, 0, container.paddingBottom)
            }

            if (current) {
                if (selectedBackgroundVisible) {
                    view.setSelected(true)
                }
                icon.isSelected = true
                // Update margins (icon & notification)
                if (view.getLayoutParams() is ViewGroup.MarginLayoutParams) {
                    val p = icon.layoutParams as ViewGroup.MarginLayoutParams
                    p.setMargins(p.leftMargin, activePaddingTop, p.rightMargin, p.bottomMargin)

                    val paramsNotification = notification.layoutParams as ViewGroup.MarginLayoutParams
                    paramsNotification.setMargins(notificationActiveMarginLeft, paramsNotification.topMargin,
                            paramsNotification.rightMargin, paramsNotification.bottomMargin)

                    view.requestLayout()
                }
            } else {
                icon.isSelected = false
                val paramsNotification = notification.layoutParams as ViewGroup.MarginLayoutParams
                paramsNotification.setMargins(notificationInactiveMarginLeft, paramsNotification.topMargin,
                        paramsNotification.rightMargin, paramsNotification.bottomMargin)
            }

            if (colored) {
                if (current) {
                    setBackgroundColor(item.getColor(context))
                    currentColor = item.getColor(context)
                }
            } else {
                if (defaultBackgroundResource != 0) {
                    setBackgroundResource(defaultBackgroundResource)
                } else {
                    setBackgroundColor(defaultBackgroundColor)
                }
            }

            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, if (current) activeSize else inactiveSize)

            if (itemsEnabledStates[i]) {
                view.setOnClickListener { updateItems(i, true) }
                icon.setImageDrawable(ANHelper.getTintDrawable(items[i].getDrawable(context)!!,
                        if (current) itemActiveColor else itemInactiveColor, forceTint))
                title.setTextColor(if (current) itemActiveColor else itemInactiveColor)
                view.isSoundEffectsEnabled = soundEffectsEnabled
            } else {
                icon.setImageDrawable(ANHelper.getTintDrawable(items[i].getDrawable(context)!!,
                        itemDisableColor, forceTint))
                title.setTextColor(itemDisableColor)
            }

            val params = FrameLayout.LayoutParams(itemWidth.toInt(), height.toInt())

            linearLayout.addView(view, params)
            views.add(view)
        }

        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS)
    }

    /**
     * Create small items (more than 3 items in the bottom navigation)
     *
     * @param linearLayout The layout where the items are added
     */
    private fun createSmallItems(linearLayout: LinearLayout) {

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val height = resources.getDimension(R.dimen.bottom_navigation_height)
        val minWidth = resources.getDimension(R.dimen.bottom_navigation_small_inactive_min_width)
        val maxWidth = resources.getDimension(R.dimen.bottom_navigation_small_inactive_max_width)

        val layoutWidth = width
        if (layoutWidth == 0 || items.size == 0) {
            return
        }

        var itemWidth = (layoutWidth / items.size).toFloat()

        if (itemWidth < minWidth) {
            itemWidth = minWidth
        } else if (itemWidth > maxWidth) {
            itemWidth = maxWidth
        }

        val activeMarginTop = resources.getDimension(R.dimen.bottom_navigation_small_margin_top_active).toInt()
        val difference = resources.getDimension(R.dimen.bottom_navigation_small_selected_width_difference)

        selectedItemWidth = itemWidth + items.size * difference
        itemWidth -= difference
        notSelectedItemWidth = itemWidth


        for (i in items.indices) {

            val item = items[i]

            val view = inflater.inflate(R.layout.bottom_navigation_small_item, this, false)
            val icon = view.findViewById(R.id.bottom_navigation_small_item_icon) as ImageView
            val title = view.findViewById(R.id.bottom_navigation_small_item_title) as TextView
            val notification = view.findViewById(R.id.bottom_navigation_notification) as TextView
            icon.setImageDrawable(item.getDrawable(context))

            if (titleState != TitleState.ALWAYS_HIDE) {
                title.text = item.getTitle(context)
            }

            if (titleActiveTextSize != 0f) {
                title.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleActiveTextSize)
            }

            if (titleTypeface != null) {
                title.typeface = titleTypeface
            }

            if (i == currentItem) {
                if (selectedBackgroundVisible) {
                    view.isSelected = true
                }
                icon.isSelected = true
                // Update margins (icon & notification)

                if (titleState != TitleState.ALWAYS_HIDE) {
                    if (view.layoutParams is ViewGroup.MarginLayoutParams) {
                        val p = icon.layoutParams as ViewGroup.MarginLayoutParams
                        p.setMargins(p.leftMargin, activeMarginTop, p.rightMargin, p.bottomMargin)

                        val paramsNotification = notification.layoutParams as ViewGroup.MarginLayoutParams
                        paramsNotification.setMargins(notificationActiveMarginLeft, notificationActiveMarginTop,
                                paramsNotification.rightMargin, paramsNotification.bottomMargin)

                        view.requestLayout()
                    }
                }
            } else {
                icon.isSelected = false
                val paramsNotification = notification.layoutParams as ViewGroup.MarginLayoutParams
                paramsNotification.setMargins(notificationInactiveMarginLeft, notificationInactiveMarginTop,
                        paramsNotification.rightMargin, paramsNotification.bottomMargin)
            }

            if (colored) {
                if (i == currentItem) {
                    setBackgroundColor(item.getColor(context))
                    currentColor = item.getColor(context)
                }
            } else {
                if (defaultBackgroundResource != 0) {
                    setBackgroundResource(defaultBackgroundResource)
                } else {
                    setBackgroundColor(defaultBackgroundColor)
                }
            }

            if (itemsEnabledStates[i]) {
                icon.setImageDrawable(ANHelper.getTintDrawable(items[i].getDrawable(context)!!,
                        if (currentItem == i) itemActiveColor else itemInactiveColor, forceTint))
                title.setTextColor(if (currentItem == i) itemActiveColor else itemInactiveColor)
                title.alpha = (if (currentItem == i) 1 else 0).toFloat()
                view.setOnClickListener { updateSmallItems(i, true) }
                view.isSoundEffectsEnabled = soundEffectsEnabled
            } else {
                icon.setImageDrawable(ANHelper.getTintDrawable(items[i].getDrawable(context)!!,
                        itemDisableColor, forceTint))
                title.setTextColor(itemDisableColor)
                title.alpha = 0f
            }

            var width = if (i == currentItem)
                selectedItemWidth.toInt()
            else
                itemWidth.toInt()

            if (titleState == TitleState.ALWAYS_HIDE) {
                width = (itemWidth * 1.16).toInt()
            }

            val params = FrameLayout.LayoutParams(width, height.toInt())

            linearLayout.addView(view, params)
            views.add(view)
        }

        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS)
    }

    /**
     * Update Items UI
     *
     * @param itemIndex   int: Selected item position
     * @param useCallback boolean: Use or not the callback
     */
    private fun updateItems(itemIndex: Int, useCallback: Boolean) {

        if (currentItem == itemIndex) {
            if (tabSelectedListener != null && useCallback) {
                tabSelectedListener!!.onTabSelected(itemIndex, true)
            }
            return
        }

        if (tabSelectedListener != null && useCallback) {
            val selectionAllowed = tabSelectedListener!!.onTabSelected(itemIndex, false)
            if (!selectionAllowed) return
        }

        val activeMarginTop = resources.getDimension(R.dimen.bottom_navigation_margin_top_active).toInt()
        val inactiveMarginTop = resources.getDimension(R.dimen.bottom_navigation_margin_top_inactive).toInt()
        var activeSize = resources.getDimension(R.dimen.bottom_navigation_text_size_active)
        var inactiveSize = resources.getDimension(R.dimen.bottom_navigation_text_size_inactive)

        if (titleActiveTextSize != 0f && titleInactiveTextSize != 0f) {
            activeSize = titleActiveTextSize
            inactiveSize = titleInactiveTextSize
        } else if (titleState == TitleState.ALWAYS_SHOW && items.size > MIN_ITEMS) {
            activeSize = resources.getDimension(R.dimen.bottom_navigation_text_size_forced_active)
            inactiveSize = resources.getDimension(R.dimen.bottom_navigation_text_size_forced_inactive)
        }

        for (i in views.indices) {

            val view = views[i]
            if (selectedBackgroundVisible) {
                view.isSelected = i == itemIndex
            }

            if (i == itemIndex) {

                val title = view.findViewById<View>(R.id.bottom_navigation_item_title) as TextView
                val icon = view.findViewById<View>(R.id.bottom_navigation_item_icon) as ImageView
                val notification = view.findViewById<View>(R.id.bottom_navigation_notification) as TextView

                icon.isSelected = true
                ANHelper.updateTopMargin(icon, inactiveMarginTop, activeMarginTop)
                ANHelper.updateLeftMargin(notification, notificationInactiveMarginLeft, notificationActiveMarginLeft)
                ANHelper.updateTextColor(title, itemInactiveColor, itemActiveColor)
                ANHelper.updateTextSize(title, inactiveSize, activeSize)
                ANHelper.updateDrawableColor(items[itemIndex].getDrawable(context)!!, icon, itemInactiveColor,
                        itemActiveColor, forceTint)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && colored) {

                    val finalRadius = Math.max(width, height)
//                    val finalRadius = Math.max(realWidth, realHeight)
                    val cx = view.x.toInt() + view.width / 2
                    val cy = view.height / 2

                    if (circleRevealAnim != null && circleRevealAnim!!.isRunning) {
                        circleRevealAnim!!.cancel()
                        setBackgroundColor(items[itemIndex].getColor(context))
                        backgroundColorView!!.setBackgroundColor(Color.TRANSPARENT)
                    }

                    circleRevealAnim = ViewAnimationUtils.createCircularReveal(backgroundColorView, cx, cy, 0f, finalRadius.toFloat())
                    circleRevealAnim!!.startDelay = 5

                    circleRevealAnim!!.addListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {
                            backgroundColorView!!.setBackgroundColor(items[itemIndex].getColor(context))
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            setBackgroundColor(items[itemIndex].getColor(context))
                            backgroundColorView!!.setBackgroundColor(Color.TRANSPARENT)
                        }

                        override fun onAnimationCancel(animation: Animator) {}

                        override fun onAnimationRepeat(animation: Animator) {}
                    })


                    circleRevealAnim!!.start()
                } else if (colored) {
                    ANHelper.updateViewBackgroundColor(this, currentColor,
                            items[itemIndex].getColor(context))
                } else {
                    if (defaultBackgroundResource != 0) {
                        setBackgroundResource(defaultBackgroundResource)
                    } else {
                        setBackgroundColor(defaultBackgroundColor)
                    }
                    backgroundColorView!!.setBackgroundColor(Color.TRANSPARENT)
                }

            } else if (i == currentItem) {

                val title = view.findViewById<View>(R.id.bottom_navigation_item_title) as TextView
                val icon = view.findViewById<View>(R.id.bottom_navigation_item_icon) as ImageView
                val notification = view.findViewById<View>(R.id.bottom_navigation_notification) as TextView

                icon.isSelected = false
                ANHelper.updateTopMargin(icon, activeMarginTop, inactiveMarginTop)
                ANHelper.updateLeftMargin(notification, notificationActiveMarginLeft, notificationInactiveMarginLeft)
                ANHelper.updateTextColor(title, itemActiveColor, itemInactiveColor)
                ANHelper.updateTextSize(title, activeSize, inactiveSize)
                ANHelper.updateDrawableColor(items[currentItem].getDrawable(context)!!, icon, itemActiveColor,
                        itemInactiveColor, forceTint)
            }
        }

        currentItem = itemIndex
        if (currentItem > 0 && currentItem < items.size) {
            currentColor = items[currentItem].getColor(context)
        } else if (currentItem == CURRENT_ITEM_NONE) {
            if (defaultBackgroundResource != 0) {
                setBackgroundResource(defaultBackgroundResource)
            } else {
                setBackgroundColor(defaultBackgroundColor)
            }
            backgroundColorView!!.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    /**
     * Update Small items UI
     *
     * @param itemIndex   int: Selected item position
     * @param useCallback boolean: Use or not the callback
     */
    private fun updateSmallItems(itemIndex: Int, useCallback: Boolean) {

        if (currentItem == itemIndex) {
            if (tabSelectedListener != null && useCallback) {
                tabSelectedListener!!.onTabSelected(itemIndex, true)
            }
            return
        }

        if (tabSelectedListener != null && useCallback) {
            val selectionAllowed = tabSelectedListener!!.onTabSelected(itemIndex, false)
            if (!selectionAllowed) return
        }

        val activeMarginTop = resources.getDimension(R.dimen.bottom_navigation_small_margin_top_active).toInt()
        val inactiveMargin = resources.getDimension(R.dimen.bottom_navigation_small_margin_top).toInt()

        for (i in views.indices) {

            val view = views[i]
            if (selectedBackgroundVisible) {
                view.isSelected = i == itemIndex
            }

            if (i == itemIndex) {

                val container = view.findViewById<View>(R.id.bottom_navigation_small_container) as FrameLayout
                val title = view.findViewById<View>(R.id.bottom_navigation_small_item_title) as TextView
                val icon = view.findViewById<View>(R.id.bottom_navigation_small_item_icon) as ImageView
                val notification = view.findViewById<View>(R.id.bottom_navigation_notification) as TextView

                icon.isSelected = true

                if (titleState != TitleState.ALWAYS_HIDE) {
                    ANHelper.updateTopMargin(icon, inactiveMargin, activeMarginTop)
                    ANHelper.updateLeftMargin(notification, notificationInactiveMarginLeft, notificationActiveMarginLeft)
                    ANHelper.updateTopMargin(notification, notificationInactiveMarginTop, notificationActiveMarginTop)
                    ANHelper.updateTextColor(title, itemInactiveColor, itemActiveColor)
                    ANHelper.updateWidth(container, notSelectedItemWidth, selectedItemWidth)
                }

                ANHelper.updateAlpha(title, 0f, 1f)
                ANHelper.updateDrawableColor(items[itemIndex].getDrawable(context)!!, icon, itemInactiveColor,
                        itemActiveColor, forceTint)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && colored) {
                    val finalRadius = Math.max(width, height)
                    val cx = views[itemIndex].x.toInt() + views[itemIndex].width / 2
                    val cy = views[itemIndex].height / 2

                    if (circleRevealAnim != null && circleRevealAnim!!.isRunning) {
                        circleRevealAnim!!.cancel()
                        setBackgroundColor(items[itemIndex].getColor(context))
                        backgroundColorView!!.setBackgroundColor(Color.TRANSPARENT)
                    }

                    circleRevealAnim = ViewAnimationUtils.createCircularReveal(backgroundColorView, cx, cy, 0f, finalRadius.toFloat())
                    circleRevealAnim!!.startDelay = 5
                    circleRevealAnim!!.addListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {
                            backgroundColorView!!.setBackgroundColor(items[itemIndex].getColor(context))
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            setBackgroundColor(items[itemIndex].getColor(context))
                            backgroundColorView!!.setBackgroundColor(Color.TRANSPARENT)
                        }

                        override fun onAnimationCancel(animation: Animator) {}

                        override fun onAnimationRepeat(animation: Animator) {}
                    })
                    circleRevealAnim!!.start()
                } else if (colored) {
                    ANHelper.updateViewBackgroundColor(this, currentColor,
                            items[itemIndex].getColor(context))
                } else {
                    if (defaultBackgroundResource != 0) {
                        setBackgroundResource(defaultBackgroundResource)
                    } else {
                        setBackgroundColor(defaultBackgroundColor)
                    }
                    backgroundColorView!!.setBackgroundColor(Color.TRANSPARENT)
                }

            } else if (i == currentItem) {

                val container = view.findViewById<View>(R.id.bottom_navigation_small_container)
                val title = view.findViewById<View>(R.id.bottom_navigation_small_item_title) as TextView
                val icon = view.findViewById<View>(R.id.bottom_navigation_small_item_icon) as ImageView
                val notification = view.findViewById<View>(R.id.bottom_navigation_notification) as TextView

                icon.isSelected = false

                if (titleState != TitleState.ALWAYS_HIDE) {
                    ANHelper.updateTopMargin(icon, activeMarginTop, inactiveMargin)
                    ANHelper.updateLeftMargin(notification, notificationActiveMarginLeft, notificationInactiveMarginLeft)
                    ANHelper.updateTopMargin(notification, notificationActiveMarginTop, notificationInactiveMarginTop)
                    ANHelper.updateTextColor(title, itemActiveColor, itemInactiveColor)
                    ANHelper.updateWidth(container, selectedItemWidth, notSelectedItemWidth)
                }

                ANHelper.updateAlpha(title, 1f, 0f)
                ANHelper.updateDrawableColor(items[currentItem].getDrawable(context)!!, icon, itemActiveColor,
                        itemInactiveColor, forceTint)
            }
        }

        currentItem = itemIndex
        if (currentItem > 0 && currentItem < items.size) {
            currentColor = items[currentItem].getColor(context)
        } else if (currentItem == CURRENT_ITEM_NONE) {
            if (defaultBackgroundResource != 0) {
                setBackgroundResource(defaultBackgroundResource)
            } else {
                setBackgroundColor(defaultBackgroundColor)
            }
            backgroundColorView!!.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    /**
     * Update notifications
     */
    private fun updateNotifications(updateStyle: Boolean, itemPosition: Int) {

        for (i in views.indices) {

            if (i >= notifications!!.size) {
                break
            }

            if (itemPosition != UPDATE_ALL_NOTIFICATIONS && itemPosition != i) {
                continue
            }

            val notificationItem = notifications!![i]
            val currentTextColor = ANNotificationHelper.getTextColor(notificationItem, notificationTextColor)
            val currentBackgroundColor = ANNotificationHelper.getBackgroundColor(notificationItem, notificationBackgroundColor)

            val notification = views[i].findViewById<View>(R.id.bottom_navigation_notification) as TextView

            val currentValue = notification.text.toString()
            val animate = currentValue != notificationItem.text.toString()

            if (updateStyle) {
                notification.setTextColor(currentTextColor)
                if (notificationTypeface != null) {
                    notification.typeface = notificationTypeface
                } else {
                    notification.setTypeface(null, Typeface.BOLD)
                }

                if (notificationBackgroundDrawable != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        val drawable = notificationBackgroundDrawable!!.constantState!!.newDrawable()
                        notification.background = drawable
                    } else {
                        @Suppress("DEPRECATION")
                        notification.setBackgroundDrawable(notificationBackgroundDrawable)
                    }

                } else if (currentBackgroundColor != 0) {
                    val defautlDrawable = ContextCompat.getDrawable(context, R.drawable.notification_background)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        notification.background = ANHelper.getTintDrawable(defautlDrawable!!,
                                currentBackgroundColor, forceTint)
                    } else {
                        @Suppress("DEPRECATION")
                        notification.setBackgroundDrawable(ANHelper.getTintDrawable(defautlDrawable!!,
                                currentBackgroundColor, forceTint))
                    }
                }
            }

            if (notificationItem.isEmpty && notification.text.isNotEmpty()) {
                notification.text = ""
                if (animate) {
                    notification.animate()
                            .scaleX(0f)
                            .scaleY(0f)
                            .alpha(0f)
                            .setInterpolator(AccelerateInterpolator())
                            .setDuration(notificationAnimationDuration)
                            .start()
                }
            } else if (!notificationItem.isEmpty) {
                notification.text = notificationItem.text.toString()
                if (animate) {
                    notification.scaleX = 0f
                    notification.scaleY = 0f
                    notification.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setInterpolator(OvershootInterpolator())
                            .setDuration(notificationAnimationDuration)
                            .start()
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
    fun addItem(item: ANBottomNavigationItem) {
        if (this.items.size > MAX_ITEMS) {
            Log.w(TAG, "The items list should not have more than 5 items")
        }
        items.add(item)
        createItems()
    }

    /**
     * Add all items
     */
    fun addItems(items: List<ANBottomNavigationItem>) {
        if (items.size > MAX_ITEMS || this.items.size + items.size > MAX_ITEMS) {
            Log.w(TAG, "The items list should not have more than 5 items")
        }
        this.items.addAll(items)
        createItems()
    }

    /**
     * Remove an item at the given index
     */
    fun removeItemAtIndex(index: Int) {
        if (index < items.size) {
            this.items.removeAt(index)
            createItems()
        }
    }

    /**
     * Remove all items
     */
    fun removeAllItems() {
        this.items.clear()
        createItems()
    }

    /**
     * Refresh the AHBottomView
     */
    fun refresh() {
        createItems()
    }

    /**
     * Return the number of items
     *
     * @return int
     */
    fun getItemsCount(): Int {
        return items.size
    }

    /**
     * Return if the Bottom Navigation is colored
     */
    fun isColored(): Boolean {
        return colored
    }

    /**
     * Set if the Bottom Navigation is colored
     */
    fun setColored(colored: Boolean) {
        this.colored = colored
        this.itemActiveColor = if (colored) coloredTitleColorActive else titleColorActive
        this.itemInactiveColor = if (colored) coloredTitleColorInactive else titleColorInactive
        createItems()
    }

    /**
     * Return the bottom navigation background color
     *
     * @return The bottom navigation background color
     */
    fun getDefaultBackgroundColor(): Int {
        return defaultBackgroundColor
    }

    /**
     * Set the bottom navigation background color
     *
     * @param defaultBackgroundColor The bottom navigation background color
     */
    fun setDefaultBackgroundColor(@ColorInt defaultBackgroundColor: Int) {
        this.defaultBackgroundColor = defaultBackgroundColor
        createItems()
    }

    /**
     * Set the bottom navigation background resource
     *
     * @param defaultBackgroundResource The bottom navigation background resource
     */
    fun setDefaultBackgroundResource(@DrawableRes defaultBackgroundResource: Int) {
        this.defaultBackgroundResource = defaultBackgroundResource
        createItems()
    }

    /**
     * Get the accent color (used when the view contains 3 items)
     *
     * @return The default accent color
     */
    fun getAccentColor(): Int {
        return itemActiveColor
    }

    /**
     * Set the accent color (used when the view contains 3 items)
     *
     * @param accentColor The new accent color
     */
    fun setAccentColor(accentColor: Int) {
        this.titleColorActive = accentColor
        this.itemActiveColor = accentColor
        createItems()
    }

    /**
     * Get the inactive color (used when the view contains 3 items)
     *
     * @return The inactive color
     */
    fun getInactiveColor(): Int {
        return itemInactiveColor
    }

    /**
     * Set the inactive color (used when the view contains 3 items)
     *
     * @param inactiveColor The inactive color
     */
    fun setInactiveColor(inactiveColor: Int) {
        this.titleColorInactive = inactiveColor
        this.itemInactiveColor = inactiveColor
        createItems()
    }

    /**
     * Set the colors used when the bottom bar uses the colored mode
     *
     * @param colorActive   The active color
     * @param colorInactive The inactive color
     */
    fun setColoredModeColors(@ColorInt colorActive: Int, @ColorInt colorInactive: Int) {
        this.coloredTitleColorActive = colorActive
        this.coloredTitleColorInactive = colorInactive
        createItems()
    }

    /**
     * Set selected background visibility
     */
    fun setSelectedBackgroundVisible(visible: Boolean) {
        this.selectedBackgroundVisible = visible
        createItems()
    }

    /**
     * Set notification typeface
     *
     * @param typeface Typeface
     */
    fun setTitleTypeface(typeface: Typeface) {
        this.titleTypeface = typeface
        createItems()
    }

    /**
     * Set title text size in pixels
     *
     * @param activeSize
     * @param inactiveSize
     */
    fun setTitleTextSize(activeSize: Float, inactiveSize: Float) {
        this.titleActiveTextSize = activeSize
        this.titleInactiveTextSize = inactiveSize
        createItems()
    }

    /**
     * Set title text size in SP
     *
     * +	 * @param activeSize in sp
     * +	 * @param inactiveSize in sp
     */
    fun setTitleTextSizeInSp(activeSize: Float, inactiveSize: Float) {
        this.titleActiveTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, activeSize, resources.displayMetrics)
        this.titleInactiveTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, inactiveSize, resources.displayMetrics)
        createItems()
    }

    /**
     * Get item at the given index
     *
     * @param position int: item position
     * @return The item at the given position
     */
    fun getItem(position: Int): ANBottomNavigationItem? {
        if (position < 0 || position > items.size - 1) {
            Log.w(TAG, "The position is out of bounds of the items (" + items.size + " elements)")
            return null
        }
        return items[position]
    }

    /**
     * Get the current item
     *
     * @return The current item position
     */
    fun getCurrentItem(): Int {
        return currentItem
    }

    /**
     * Set the current item
     *
     * @param position int: position
     */
    fun setCurrentItem(position: Int) {
        setCurrentItem(position, true)
    }

    /**
     * Set the current item
     *
     * @param position    int: item position
     * @param useCallback boolean: use or not the callback
     */
    fun setCurrentItem(position: Int, useCallback: Boolean) {
        if (position >= items.size) {
            Log.w(TAG, "The position is out of bounds of the items (" + items.size + " elements)")
            return
        }

        if (titleState != TitleState.ALWAYS_HIDE && (items.size == MIN_ITEMS || titleState == TitleState.ALWAYS_SHOW)) {
            updateItems(position, useCallback)
        } else {
            updateSmallItems(position, useCallback)
        }
    }

    /**
     * Hide Bottom Navigation with animation
     */
    fun hideBottomNavigation() {
        hideBottomNavigation(true)
    }

    /**
     * Hide Bottom Navigation with or without animation
     *
     * @param withAnimation Boolean
     */
    fun hideBottomNavigation(withAnimation: Boolean) {
        if (bottomNavigationBehavior != null) {
            bottomNavigationBehavior!!.hideView(this, bottomNavigationHeight + insetBottom, withAnimation)
        } else if (parent is CoordinatorLayout) {
            needHideBottomNavigation = true
            hideBottomNavigationWithAnimation = withAnimation
        } else {
            // Hide bottom navigation
            ViewCompat.animate(this)
                    .translationY(bottomNavigationHeight.toFloat())
                    .setInterpolator(LinearOutSlowInInterpolator())
                    .setDuration((if (withAnimation) 300 else 0).toLong())
                    .start()
        }
    }

    /**
     * Restore Bottom Navigation with animation
     */
    fun restoreBottomNavigation() {
        restoreBottomNavigation(true)
    }

    /**
     * Restore Bottom Navigation with or without animation
     *
     * @param withAnimation Boolean
     */
    fun restoreBottomNavigation(withAnimation: Boolean) {
        if (bottomNavigationBehavior != null) {
            bottomNavigationBehavior!!.resetOffset(this, withAnimation)
        } else {
            // Show bottom navigation
            ViewCompat.animate(this)
                    .translationY(0f)
                    .setInterpolator(LinearOutSlowInInterpolator())
                    .setDuration((if (withAnimation) 300 else 0).toLong())
                    .start()
        }
    }

    /**
     * Return if the tint should be forced (with setColorFilter)
     *
     * @return Boolean
     */
    fun isForceTint(): Boolean {
        return forceTint
    }

    /**
     * Set the force tint value
     * If forceTint = true, the tint is made with drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
     *
     * @param forceTint Boolean
     */
    fun setForceTint(forceTint: Boolean) {
        this.forceTint = forceTint
        createItems()
    }

    /**
     * Return the title state for display
     *
     * @return TitleState
     */
    fun getTitleState(): TitleState {
        return titleState
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
    fun setTitleState(titleState: TitleState) {
        this.titleState = titleState
        createItems()
    }

    /**
     * Set AHOnTabSelectedListener
     */
    fun setOnTabSelectedListener(tabSelectedListener: OnTabSelectedListener) {
        this.tabSelectedListener = tabSelectedListener
    }

    /**
     * Remove AHOnTabSelectedListener
     */
    fun removeOnTabSelectedListener() {
        this.tabSelectedListener = null
    }

    /**
     * Set the notification number
     *
     * @param nbNotification int
     * @param itemPosition   int
     */
    @Deprecated("")
    fun setNotification(nbNotification: Int, itemPosition: Int) {
        if (itemPosition < 0 || itemPosition > items.size - 1) {
            throw IndexOutOfBoundsException(String.format(Locale.US, EXCEPTION_INDEX_OUT_OF_BOUNDS, itemPosition, items.size))
        }
        val title = if (nbNotification == 0) "" else nbNotification.toString()
        notifications!![itemPosition] = ANNotification.justText(title)
        updateNotifications(false, itemPosition)
    }

    /**
     * Set notification text
     *
     * @param title        String
     * @param itemPosition int
     */
    fun setNotification(title: String, itemPosition: Int) {
        if (itemPosition < 0 || itemPosition > items.size - 1) {
            throw IndexOutOfBoundsException(String.format(Locale.US, EXCEPTION_INDEX_OUT_OF_BOUNDS, itemPosition, items.size))
        }

        notifications!![itemPosition] = ANNotification.justText(title)
        updateNotifications(false, itemPosition)
    }

    /**
     * Set fully customized Notification
     *
     * @param inputNotification AHNotification
     * @param itemPosition Int
     */
    fun setNotification(inputNotification: ANNotification?, itemPosition: Int) {
        var notification = inputNotification
        if (itemPosition < 0 || itemPosition > items.size - 1) {
            throw IndexOutOfBoundsException(String.format(Locale.US, EXCEPTION_INDEX_OUT_OF_BOUNDS, itemPosition, items.size))
        }
        if (notification == null) {
            notification = ANNotification() // instead of null, use empty notification
        }
        notifications!![itemPosition] = notification
        updateNotifications(true, itemPosition)
    }

    /**
     * Set notification text color
     *
     * @param textColor int
     */
    fun setNotificationTextColor(@ColorInt textColor: Int) {
        this.notificationTextColor = textColor
        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS)
    }

    /**
     * Set notification text color
     *
     * @param textColor int
     */
    fun setNotificationTextColorResource(@ColorRes textColor: Int) {
        this.notificationTextColor = ContextCompat.getColor(context, textColor)
        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS)
    }

    /**
     * Set notification background resource
     *
     * @param drawable Drawable
     */
    fun setNotificationBackground(drawable: Drawable) {
        this.notificationBackgroundDrawable = drawable
        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS)
    }

    /**
     * Set notification background color
     *
     * @param color int
     */
    fun setNotificationBackgroundColor(@ColorInt color: Int) {
        this.notificationBackgroundColor = color
        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS)
    }

    /**
     * Set notification background color
     *
     * @param color int
     */
    fun setNotificationBackgroundColorResource(@ColorRes color: Int) {
        this.notificationBackgroundColor = ContextCompat.getColor(context, color)
        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS)
    }

    /**
     * Set notification typeface
     *
     * @param typeface Typeface
     */
    fun setNotificationTypeface(typeface: Typeface) {
        this.notificationTypeface = typeface
        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS)
    }

    fun setNotificationAnimationDuration(notificationAnimationDuration: Long) {
        this.notificationAnimationDuration = notificationAnimationDuration
        updateNotifications(true, UPDATE_ALL_NOTIFICATIONS)
    }

    /**
     * Set the notification margin left
     *
     * @param activeMargin
     * @param inactiveMargin
     */
    fun setNotificationMarginLeft(activeMargin: Int, inactiveMargin: Int) {
        this.notificationActiveMarginLeft = activeMargin
        this.notificationInactiveMarginLeft = inactiveMargin
        createItems()
    }

    /**
     * Activate or not the elevation
     *
     * @param useElevation boolean
     */
    fun setUseElevation(useElevation: Boolean) {
        ViewCompat.setElevation(this, if (useElevation)
            resources.getDimension(R.dimen.bottom_navigation_elevation)
        else
            0f)
        clipToPadding = false
    }

    /**
     * Activate or not the elevation, and set the value
     *
     * @param useElevation boolean
     * @param elevation    float
     */
    fun setUseElevation(useElevation: Boolean, elevation: Float) {
        ViewCompat.setElevation(this, if (useElevation) elevation else 0f)
        clipToPadding = false
    }

    /**
     * Return if the Bottom Navigation is hidden or not
     */
    fun isHidden(): Boolean {
        return if (bottomNavigationBehavior != null) {
            bottomNavigationBehavior!!.isHidden
        } else false
    }

    /**
     * Get the view at the given position
     * @param position int
     * @return The view at the position, or null
     */
    fun getViewAtPosition(position: Int): View? {
        return if (linearLayoutContainer != null && position >= 0
                && position < linearLayoutContainer!!.childCount) {
            linearLayoutContainer!!.getChildAt(position)
        } else null
    }

    /**
     * Enable the tab item at the given position
     * @param position int
     */
    fun enableItemAtPosition(position: Int) {
        if (position < 0 || position > items.size - 1) {
            Log.w(TAG, "The position is out of bounds of the items (" + items.size + " elements)")
            return
        }
        itemsEnabledStates[position] = true
        createItems()
    }

    /**
     * Disable the tab item at the given position
     * @param position int
     */
    fun disableItemAtPosition(position: Int) {
        if (position < 0 || position > items.size - 1) {
            Log.w(TAG, "The position is out of bounds of the items (" + items.size + " elements)")
            return
        }
        itemsEnabledStates[position] = false
        createItems()
    }

    /**
     * Set the item disable color
     * @param itemDisableColor int
     */
    fun setItemDisableColor(@ColorInt itemDisableColor: Int) {
        this.itemDisableColor = itemDisableColor
    }

    ////////////////
    // INTERFACES //
    ////////////////

    /**
     *
     */
    interface OnTabSelectedListener {
        /**
         * Called when a tab has been selected (clicked)
         *
         * @param position    int: Position of the selected tab
         * @param wasSelected boolean: true if the tab was already selected
         * @return boolean: true for updating the tab UI, false otherwise
         */
        fun onTabSelected(position: Int, wasSelected: Boolean): Boolean
    }
}