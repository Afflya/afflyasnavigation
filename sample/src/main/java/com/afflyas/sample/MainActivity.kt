package com.afflyas.sample

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import com.afflyas.afflyasnavigation.ANBottomNavigation
import com.afflyas.afflyasnavigation.ANBottomNavigationItem
import com.google.android.material.snackbar.Snackbar
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

        /**
         * Always show notch
         */
        if (Build.VERSION.SDK_INT >= 28) {
            (window.attributes as WindowManager.LayoutParams).layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val bottomNavigation = botNav

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

        bottomNavigation.setNotification(title = "!", itemPosition = 1)

        floating_action_button.setOnClickListener {
            Snackbar.make(coordinator, "Hello", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Ok") { }
                    .show()
        }

        coloredSwitch.setOnCheckedChangeListener { _, isChecked ->
            bottomNavigation.setColored(isChecked)
        }

        enableTopBarScrollSwitch.setOnCheckedChangeListener { _, isChecked ->
            app_bar.enableBehaviorTranslation(isChecked)
        }

        enableBotNavScrollSwitch.setOnCheckedChangeListener { _, isChecked ->
            bottomNavigation.enableBehaviorTranslation(isChecked)
        }

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
        if (item!!.itemId == 123) {
            Snackbar.make(coordinator, "Done", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Undo") { }
                    .show()
        }

        if (item.itemId == android.R.id.home)
            Snackbar.make(coordinator, "Back", Snackbar.LENGTH_SHORT)
                    .setAction("Ok") { }
                    .show()
        return super.onOptionsItemSelected(item)
    }

}
