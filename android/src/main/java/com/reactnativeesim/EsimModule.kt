package com.reactnativeesim

import android.os.Build
import android.content.Context.EUICC_SERVICE

import android.telephony.euicc.EuiccManager
import androidx.annotation.RequiresApi
import com.facebook.react.bridge.*
import android.app.PendingIntent
import android.R.attr.action
import android.content.Intent
import android.telephony.euicc.DownloadableSubscription
import android.content.IntentFilter
import android.content.Context
import android.content.BroadcastReceiver

class EsimModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

  private val ACTION_DOWNLOAD_SUBSCRIPTION = "download_subscription"
  private val mReactContext: ReactContext = reactContext

  @RequiresApi(Build.VERSION_CODES.P)
  private val mgr:EuiccManager? = mReactContext.getSystemService(EUICC_SERVICE) as EuiccManager?

  override fun getName(): String {
    return "RNESimManager"
  }

  @RequiresApi(Build.VERSION_CODES.P)
  @ReactMethod
  fun isEsimSupported(promise: Promise) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && mgr !== null ) {
      promise.resolve(mgr.isEnabled)
    } else {
      promise.resolve(false)
    }
    return
  }

  @RequiresApi(Build.VERSION_CODES.P)
  @ReactMethod
  fun setupEsim(config: ReadableMap, promise: Promise) {
    if (mgr == null){
      promise.reject("0", "Could not get react context", Exception("Could not get react context"))
      return
    }

    val receiver: BroadcastReceiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent) {
        if (!action.equals(intent.getAction())) {
          return
        }
        resultCode = getResultCode();
        if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK){
          promise.resolve(2)
        } else if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR){
          promise.reject("1", "Embedded Subscription Error", Exception("Embedded Subscription Error"))
        } else {
          promise.reject("0", "Unknown Error", Exception("Unknown Error"))
        }
      }
    }
    val LPA_DECLARED_PERMISSION = "com.your.company.lpa.permission.BROADCAST"

    mReactContext.registerReceiver(
      receiver,
      IntentFilter(ACTION_DOWNLOAD_SUBSCRIPTION),
      LPA_DECLARED_PERMISSION,
      null)

    val sub: DownloadableSubscription = DownloadableSubscription.forActivationCode(
      config.getString("confirmationCode") /* encodedActivationCode*/)

    val callbackIntent = PendingIntent.getBroadcast(
      mReactContext,
      0 /* requestCode */,
      Intent(ACTION_DOWNLOAD_SUBSCRIPTION),
      PendingIntent.FLAG_UPDATE_CURRENT)

    mgr.downloadSubscription(sub, false, callbackIntent)
  }

}
