package com.afflyas.sample

import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import com.afflyas.afflyasnavigation.ANBottomNavigation
import com.afflyas.afflyasnavigation.ANBottomNavigationItem
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar.setTitle(R.string.app_name)
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)

//        /**
//         * Always show notch
//         * TODO uncomment these lines if your target sdk version >= 28
//         */
//        if (Build.VERSION.SDK_INT >= 28) {
//            (/*params*/window.attributes as WindowManager.LayoutParams).layoutInDisplayCutoutMode =
//                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
//        }

        val bottomNavigation = botNav

        val item1 = ANBottomNavigationItem(R.string.home, R.drawable.ic_home_black_24dp, R.color.colorPrimary)
        val item2 = ANBottomNavigationItem(R.string.chat, R.drawable.ic_chat_black_24dp, R.color.colorAccent)
        val item3 = ANBottomNavigationItem(R.string.dashboard, R.drawable.ic_dashboard_black_24dp, R.color.colorPrimaryDark)
        val item4 = ANBottomNavigationItem(R.string.music, R.drawable.ic_audiotrack_black_24dp, android.R.color.holo_red_light)
        val item5 = ANBottomNavigationItem(R.string.settings, R.drawable.ic_settings_black_24dp, android.R.color.holo_green_light)

//        val items: List<ANBottomNavigationItem> = listOf(item1, item2, item3, item4, item5)
//        bottomNavigation.addItem(item1)
//        bottomNavigation.addItem(item2)
//        bottomNavigation.addItem(item3)
//        bottomNavigation.addItem(item4)
//        bottomNavigation.addItem(item5)

        bottomNavigation.addItems(listOf(item1, item2, item3, item4, item5))

        bottomNavigation.enableBehaviorTranslation(true)

        bottomNavigation.setTitleState(ANBottomNavigation.TitleState.ALWAYS_SHOW)

        bottomNavigation.setOnTabSelectedListener(object : ANBottomNavigation.OnTabSelectedListener {
            override fun onTabSelected(position: Int, wasSelected: Boolean): Boolean {
                when (position) {
                    1 -> {
                        bottomNavigation.setNotification("", 1)
                        bottomNavigation.setNotification(" ", 2)
                    }
                    2 -> {
                        bottomNavigation.setNotification("22", 1)
                        bottomNavigation.setNotification("", 2)
                    }
                }
                return true
            }
        })

        bottomNavigation.setNotification("!", 1)

        floating_action_button.setOnClickListener({
            Snackbar.make(coordinator, "Hello", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Ok", { })
                    .show();
        })

        bottomNavigation.setColored(true)
        coloredSwitch.setOnCheckedChangeListener({ _, isChecked ->
            bottomNavigation.setColored(isChecked)
        })

        enableTopBarScrollSwitch.setOnCheckedChangeListener({ _, isChecked ->
            app_bar.enableBehaviorTranslation(isChecked)
        })

        enableBotNavScrollSwitch.setOnCheckedChangeListener({ _, isChecked ->
            bottomNavigation.enableBehaviorTranslation(isChecked)
        })

        defaultTitle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) bottomNavigation.setTitleState(ANBottomNavigation.TitleState.SHOW_WHEN_ACTIVE)
        }

        showTitle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) bottomNavigation.setTitleState(ANBottomNavigation.TitleState.ALWAYS_SHOW)
        }

        noTitle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) bottomNavigation.setTitleState(ANBottomNavigation.TitleState.ALWAYS_HIDE)
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 123, 0, R.string.app_name)!!
                .setIcon(android.R.drawable.ic_menu_delete)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == 123)
            Snackbar.make(coordinator, "Done", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Undo", { })
                    .show()

        if (item.itemId == android.R.id.home)
            Snackbar.make(coordinator, "Back", Snackbar.LENGTH_SHORT)
                    .setAction("Ok", { })
                    .show()
        return super.onOptionsItemSelected(item)
    }

}
