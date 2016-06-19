package org.kraflapps.motiondroid;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * @author Veronika Rodionova nika.blaine@gmail.com
 */

public class AnalyticsApplication extends Application {
    private Tracker mTracker;

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            analytics.setLocalDispatchPeriod(1800);
            mTracker = analytics.newTracker("UA-79593861-1");
            mTracker.enableExceptionReporting(true);
            mTracker.enableAdvertisingIdCollection(true);
            mTracker.enableAutoActivityTracking(true);
        }
        return mTracker;
    }
}
