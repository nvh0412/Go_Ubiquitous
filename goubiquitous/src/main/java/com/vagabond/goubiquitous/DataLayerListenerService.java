package com.vagabond.goubiquitous;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

/**
 * Created by HoaNV on 10/31/16.
 */

public class DataLayerListenerService extends WearableListenerService implements GoogleApiClient.OnConnectionFailedListener {

  private static final String LOG_TAG = DataLayerListenerService.class.getSimpleName();
  private GoogleApiClient mGoogleApiClient;
  @Override
  public void onCreate() {
    Log.d(LOG_TAG, "onCreate");
    super.onCreate();
    mGoogleApiClient = new GoogleApiClient.Builder(this)
      .addApi(Wearable.API)
      .addOnConnectionFailedListener(this)
      .build();
    mGoogleApiClient.connect();
  }

  @Override
  public void onDataChanged(DataEventBuffer dataEventBuffer) {
    LOGD(LOG_TAG, "onDataChanged: " + dataEventBuffer);
    if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
      ConnectionResult connectionResult = mGoogleApiClient
        .blockingConnect(30, TimeUnit.SECONDS);
      if (!connectionResult.isSuccess()) {
        Log.e(LOG_TAG, "DataLayerListenerService failed to connect to GoogleApiClient, "
          + "error code: " + connectionResult.getErrorCode());
        return;
      }
    }

    // Loop through the events and send a message back to the node that created the data item.
    for (DataEvent event : dataEventBuffer) {
      Uri uri = event.getDataItem().getUri();
      String path = uri.getPath();
      if ("/sunshine".equals(path)) {
        // Get the node id of the node that created the data item from the host portion of
        // the uri.
        Log.d(LOG_TAG, "DataItem:" + event.getDataItem().toString());
      }
    }
  }

  public static void LOGD(final String tag, String message) {
    if (Log.isLoggable(tag, Log.DEBUG)) {
      Log.d(tag, message);
    }
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    Log.e(LOG_TAG, "onConnectionFailed(): Failed to connect, with result: " + connectionResult);
  }
}
