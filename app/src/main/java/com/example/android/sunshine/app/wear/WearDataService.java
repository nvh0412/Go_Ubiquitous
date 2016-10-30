package com.example.android.sunshine.app.wear;

import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

public class WearDataService extends WearableListenerService {
  private static final String LOG_TAG = WearDataService.class.getSimpleName();

  @Override
  public void onDataChanged(DataEventBuffer dataEventBuffer) {
    for (DataEvent dataEvent : dataEventBuffer) {
      if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
        DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
        String path = dataEvent.getDataItem().getUri().getPath();
        if (path.equals("/step-counter")) {
          int steps = dataMap.getInt("step-count");
          long time = dataMap.getLong("timestamp");
          Log.d(LOG_TAG, "Steps: " + steps + "| Time: " + time);
          break;
        }
      }
    }
  }

}
