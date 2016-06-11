package org.kraflapps.motiondroid;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private boolean mRunning;

    private View.OnClickListener onClickStartListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Snackbar.make(view, "Starting the motion service ..", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();

            CameraFragment fragment = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
            fragment.closeCamera();

            Intent serviceIntent = new Intent(getApplicationContext(), MotionService.class);
            /*AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, serviceIntent, 0);

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                    10000, alarmIntent);*/

            startService(serviceIntent);
            setViews();
        }
    };

    private View.OnClickListener onClickEndListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Snackbar.make(view, "Stopping the motion service ..", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();

            Intent serviceIntent = new Intent(getApplicationContext(), MotionService.class);
            stopService(serviceIntent);
            setViews();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isServiceRunning(MotionService.class)) {
            addFragment(R.id.fragment,
                    new PreviewFragment(),
                    PreviewFragment.FRAGMENT_TAG);
        } else {
            addFragment(R.id.fragment,
                    new CameraFragment(),
                    CameraFragment.FRAGMENT_TAG);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setViews();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        setViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean isServiceRunning(Class<?> serviceClass) {
        return Util.isServiceRunning(this, serviceClass);
    }

    private void setViews() {
        setFab();
        setFragments();
    }

    private void setFab() {
        mRunning = isServiceRunning(MotionService.class);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(mRunning ? Color.RED : Color.GREEN));
            fab.setOnClickListener(mRunning ? onClickEndListener : onClickStartListener);
        }
    }

    private void setFragments() {
        replaceFragment(R.id.fragment,
                mRunning ? new PreviewFragment() : new CameraFragment(),
                mRunning ? PreviewFragment.FRAGMENT_TAG : CameraFragment.FRAGMENT_TAG,
                null
        );
    }

    protected void addFragment(@IdRes int containerViewId,
                               @NonNull android.support.v4.app.Fragment fragment,
                               @NonNull String fragmentTag) {
        getSupportFragmentManager()
                .beginTransaction()
                .add(containerViewId, fragment, fragmentTag)
                .disallowAddToBackStack()
                .commit();
    }

    protected void replaceFragment(@IdRes int containerViewId,
                                   @NonNull android.support.v4.app.Fragment fragment,
                                   @NonNull String fragmentTag,
                                   @Nullable String backStackStateName) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(containerViewId, fragment, fragmentTag)
                .addToBackStack(backStackStateName)
                .commit();
    }
}
