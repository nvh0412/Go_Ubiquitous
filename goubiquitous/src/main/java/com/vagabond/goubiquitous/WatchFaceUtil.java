package com.vagabond.goubiquitous;

import android.graphics.Color;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by HoaNV on 11/1/16.
 */
public class WatchFaceUtil {
  private static final String TAG = "WatchFaceUtil";

  public static final String PATH_WITH_FEATURE = "/watch_face_config/Digital";

  /**
   * Name of the default interactive mode background color and the ambient mode background color.
   */
  public static final String COLOR_NAME_DEFAULT_AND_AMBIENT_BACKGROUND = "Blue";

  /**
   * Name of the default interactive mode hour digits color and the ambient mode hour digits
   * color.
   */
  public static final String COLOR_NAME_DEFAULT_AND_AMBIENT_HOUR_DIGITS = "White";
  /**
   * Name of the default interactive mode minute digits color and the ambient mode minute digits
   * color.
   */
  public static final String COLOR_NAME_DEFAULT_AND_AMBIENT_MINUTE_DIGITS = "White";
  public static final int COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS =
    parseColor(COLOR_NAME_DEFAULT_AND_AMBIENT_MINUTE_DIGITS);

  /**
   * Name of the default interactive mode second digits color and the ambient mode second digits
   * color.
   */
  public static final String COLOR_NAME_DEFAULT_AND_AMBIENT_SECOND_DIGITS = "White";
  public static final int COLOR_VALUE_DEFAULT_AND_AMBIENT_SECOND_DIGITS =
    parseColor(COLOR_NAME_DEFAULT_AND_AMBIENT_SECOND_DIGITS);

  public interface FetchConfigDataMapCallback {
    void onConfigDataMapFetched(DataMap config);
  }

  private static int parseColor(String colorName) {
    return Color.parseColor(colorName.toLowerCase());
  }

  public static void fetchConfigDataMap(final GoogleApiClient client,
                                        final FetchConfigDataMapCallback callback) {
    Wearable.NodeApi.getLocalNode(client).setResultCallback(
      new ResultCallback<NodeApi.GetLocalNodeResult>() {
        @Override
        public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
          String localNode = getLocalNodeResult.getNode().getId();
          Uri uri = new Uri.Builder()
            .scheme("wear")
            .path(WatchFaceUtil.PATH_WITH_FEATURE)
            .authority(localNode)
            .build();
          Wearable.DataApi.getDataItem(client, uri)
            .setResultCallback(new DataItemResultCallback(callback));
        }
      }
    );
  }

  public static void putConfigDataItem(GoogleApiClient googleApiClient, DataMap newConfig) {
    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_WITH_FEATURE);
    DataMap configToPut = putDataMapRequest.getDataMap();
    configToPut.putAll(newConfig);
    Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest())
      .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
        @Override
        public void onResult(DataApi.DataItemResult dataItemResult) {
          if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "putDataItem result status: " + dataItemResult.getStatus());
          }
        }
      });
  }

  private static class DataItemResultCallback implements ResultCallback<DataApi.DataItemResult> {

    private final FetchConfigDataMapCallback mCallback;

    public DataItemResultCallback(FetchConfigDataMapCallback callback) {
      mCallback = callback;
    }

    @Override
    public void onResult(DataApi.DataItemResult dataItemResult) {
      if (dataItemResult.getStatus().isSuccess()) {
        if (dataItemResult.getDataItem() != null) {
          DataItem configDataItem = dataItemResult.getDataItem();
          DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
          DataMap config = dataMapItem.getDataMap();
          mCallback.onConfigDataMapFetched(config);
        } else {
          mCallback.onConfigDataMapFetched(new DataMap());
        }
      }
    }
  }

  /**
   * Helper method to provide the art resource id according to the weather condition id returned
   * by the OpenWeatherMap call.
   *
   * @param weatherId from OpenWeatherMap API response
   * @return resource id for the corresponding icon. -1 if no relation is found.
   */
  public static int getArtResourceForWeatherCondition(int weatherId) {
    // Based on weather code data found at:
    // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
    if (weatherId >= 200 && weatherId <= 232) {
      return R.drawable.art_storm;
    } else if (weatherId >= 300 && weatherId <= 321) {
      return R.drawable.art_light_rain;
    } else if (weatherId >= 500 && weatherId <= 504) {
      return R.drawable.art_rain;
    } else if (weatherId == 511) {
      return R.drawable.art_snow;
    } else if (weatherId >= 520 && weatherId <= 531) {
      return R.drawable.art_rain;
    } else if (weatherId >= 600 && weatherId <= 622) {
      return R.drawable.art_snow;
    } else if (weatherId >= 701 && weatherId <= 761) {
      return R.drawable.art_fog;
    } else if (weatherId == 761 || weatherId == 781) {
      return R.drawable.art_storm;
    } else if (weatherId == 800) {
      return R.drawable.art_clear;
    } else if (weatherId == 801) {
      return R.drawable.art_light_clouds;
    } else if (weatherId >= 802 && weatherId <= 804) {
      return R.drawable.art_clouds;
    }
    return -1;
  }
}
