package org.kraflapps.motiondroid;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;

/**
 * @author Veronika Rodionova nika.blaine@gmail.com
 */

public class MotionWidgetUpdaterService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        update();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void update() {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);
        WidgetUtil.updateViews(this, views);
    }
}
