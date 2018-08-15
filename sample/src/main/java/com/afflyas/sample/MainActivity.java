package com.afflyas.sample;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;

import com.afflyas.afflyasnavigation.ANBottomNavigation;
import com.afflyas.afflyasnavigation.ANTopBar;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
         * Fix crashes caused by using vector drawables on Android 4.*
         */
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        /*
         * Always show notch
         */
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        final ANBottomNavigation bottomNavigation = findViewById(R.id.botNav);


        bottomNavigation.setOnTabSelectedListener(new ANBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
                switch (position){
                    case 1:
                        bottomNavigation.setNotification("", 1);
                        bottomNavigation.setNotification(" ", 2);
                        break;
                    case 2:
                        bottomNavigation.setNotification("22", 1);
                        bottomNavigation.setNotification("", 2);
                        break;
                }
                return true;
            }
        });

        bottomNavigation.setNotification("!", 1);

        findViewById(R.id.floating_action_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(findViewById(R.id.coordinator), "Hello", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Ok", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                
                            }
                        })
                        .show();
            }
        });

        ((Switch)findViewById(R.id.coloredSwitch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                bottomNavigation.setColored(isChecked);
            }
        });

        ((Switch)findViewById(R.id.enableTopBarScrollSwitch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                ((ANTopBar)findViewById(R.id.app_bar)).enableBehaviorTranslation(isChecked);
            }
        });

        ((Switch)findViewById(R.id.enableBotNavScrollSwitch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                bottomNavigation.enableBehaviorTranslation(isChecked);
            }
        });

        ((RadioButton)findViewById(R.id.defaultTitle)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) bottomNavigation.setTitleState(ANBottomNavigation.TitleState.SHOW_WHEN_ACTIVE);
            }
        });

        ((RadioButton)findViewById(R.id.showTitle)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) bottomNavigation.setTitleState(ANBottomNavigation.TitleState.ALWAYS_SHOW);
            }
        });

        ((RadioButton)findViewById(R.id.noTitle)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) bottomNavigation.setTitleState(ANBottomNavigation.TitleState.ALWAYS_HIDE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 123, 0, R.string.app_name)
                .setIcon(android.R.drawable.ic_menu_delete)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case 123:
                Snackbar.make(findViewById(R.id.coordinator), "Done", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Undo", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                            }
                        })
                        .show();
                break;
            case android.R.id.home:
                Snackbar.make(findViewById(R.id.coordinator), "Back", Snackbar.LENGTH_SHORT)
                        .setAction("Ok", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                            }
                        })
                        .show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
