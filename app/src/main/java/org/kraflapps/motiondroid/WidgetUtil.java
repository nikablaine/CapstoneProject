package org.kraflapps.motiondroid;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

import static org.kraflapps.motiondroid.Util.isServiceRunning;

/**
 * @author Veronika Rodionova nika.blaine@gmail.com
 */

public class WidgetUtil {

    static void updateViews(Context context, RemoteViews views) {

        views.setImageViewResource(R.id.widgetImage,
                isServiceRunning(context, MotionService.class) ?
                        android.R.drawable.ic_media_pause :
                        android.R.drawable.ic_media_play
        );
        views.setTextViewText(R.id.widgetText,
                isServiceRunning(context, MotionService.class) ?
                        context.getString(R.string.widget_running) :
                        context.getString(R.string.widget_start));

        ComponentName thisWidget = new ComponentName(context, MotionWidgetProvider.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(thisWidget, views);
    }
}
