package org.kraflapps.motiondroid;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.Calendar;

import static org.kraflapps.motiondroid.Util.isServiceRunning;

/**
 * @author Veronika Rodionova nika.blaine@gmail.com
 */

public class MotionWidgetProvider extends AppWidgetProvider {

    private static final long UPDATE_INTERVAL = 15 * 1000L;
    private static final String TOGGLE_MOTION = "Motion";
    private PendingIntent service = null;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        final Calendar TIME = Calendar.getInstance();
        TIME.set(Calendar.MINUTE, 0);
        TIME.set(Calendar.SECOND, 0);
        TIME.set(Calendar.MILLISECOND, 0);

        final Intent intent = new Intent(context, MotionWidgetUpdaterService.class);
        if (service == null) {
            service = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
        alarmManager.setRepeating(AlarmManager.RTC, TIME.getTime().getTime(), UPDATE_INTERVAL, service);

        // start/stop service on widget click
        for (int appWidgetId : appWidgetIds) {
            Intent newIntent = new Intent(context, MotionWidgetProvider.class);
            newIntent.setAction(TOGGLE_MOTION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, newIntent, 0);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            views.setOnClickPendingIntent(R.id.widgetImage, pendingIntent);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TOGGLE_MOTION)) {

            Intent serviceIntent = new Intent(context, MotionService.class);
            if (isServiceRunning(context, MotionService.class)) {
                context.stopService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            WidgetUtil.updateViews(context, views);
        }

        super.onReceive(context, intent);
    }

    @Override
    public void onDisabled(Context context) {
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(service);
    }
}
