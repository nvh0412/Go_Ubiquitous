package com.vagabond.goubiquitous;

import android.util.Log;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by HoaNV on 10/31/16.
 */

public class DataLayerListenerService extends WearableListenerService {

  private static final String LOG_TAG = DataLayerListenerService.class.getSimpleName();

  @Override
  public void onCreate() {
    Log.d(LOG_TAG, "onCreate");
    super.onCreate();
  }

  @Override
  public void onDataChanged(DataEventBuffer dataEventBuffer) {
    Log.d(LOG_TAG, "onDataChanged");
    super.onDataChanged(dataEventBuffer);
  }
}
