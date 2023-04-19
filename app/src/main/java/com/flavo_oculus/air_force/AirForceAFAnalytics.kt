package com.flavo_oculus.air_force

import android.content.Context
import android.util.Log
import com.appsflyer.AppsFlyerLib
import com.appsflyer.attribution.AppsFlyerRequestListener

class AirForceAFAnalytics(private val context: Context) {

    private var appsFlyerInstance: AppsFlyerLib = AppsFlyerLib.getInstance()

    fun logEvent(eventName: String, eventValues: Map<String, Any> = emptyMap()) {
        appsFlyerInstance.logEvent(context, eventName, eventValues, object : AppsFlyerRequestListener {
            override fun onSuccess() {
                Log.d("AppsFlyerAnalytics", "Success logging event")
            }
            override fun onError(p0: Int, p1: String) {
                Log.d("AppsFlyerAnalytics", "Failure logging event: statusCode = $p0, failure message: $p1")
            }
        })
    }

    companion object {
        const val FIRST_LAUNCH_EVENT = "first_launch"
    }
}