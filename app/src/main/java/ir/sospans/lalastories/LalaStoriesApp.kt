package ir.sospans.lalastories

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.adtrace.sdk.AdTrace
import io.adtrace.sdk.AdTraceConfig

class LalaStoriesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = AdTraceConfig(this, "elox4ftqomz3", AdTraceConfig.ENVIRONMENT_PRODUCTION)
        AdTrace.onCreate(config)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = AdTrace.onResume()
            override fun onActivityPaused(activity: Activity) = AdTrace.onPause()
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }
}
