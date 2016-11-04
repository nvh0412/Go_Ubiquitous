package com.example.android.sunshine.app;

import android.graphics.Color;
import android.net.Uri;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by HoaNV on 10/30/16.
 */

public final class DigitalWatchFaceUtil {
  private static final String TAG = "DigitalWatchFaceUtil";

  /**
   * The {@link DataMap} key for {@link SunshineWatchFaceService} background color name.
   * The color name must be a {@link String} recognized by {@link Color#parseColor}.
   */
  public static final String KEY_BACKGROUND_COLOR = "BACKGROUND_COLOR";

  /**
   * The {@link DataMap} key for {@link SunshineWatchFaceService} hour digits color name.
   * The color name must be a {@link String} recognized by {@link Color#parseColor}.
   */
  public static final String KEY_HOURS_COLOR = "HOURS_COLOR";

  /**
   * The {@link DataMap} key for {@link SunshineWatchFaceService} minute digits color name.
   * The color name must be a {@link String} recognized by {@link Color#parseColor}.
   */
  public static final String KEY_MINUTES_COLOR = "MINUTES_COLOR";

  /**
   * The {@link DataMap} key for {@link SunshineWatchFaceService} second digits color name.
   * The color name must be a {@link String} recognized by {@link Color#parseColor}.
   */
  public static final String KEY_SECONDS_COLOR = "SECONDS_COLOR";

  /**
   * The path for the {@link DataItem} containing {@link SunshineWatchFaceService} configuration.
   */
  public static final String PATH_WITH_FEATURE = "/watch_face_config/Digital";

  /**
   * Name of the default interactive mode background color and the ambient mode background color.
   */
  public static final String COLOR_NAME_DEFAULT_AND_AMBIENT_BACKGROUND = "Black";
  public static final int COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND =
    parseColor(COLOR_NAME_DEFAULT_AND_AMBIENT_BACKGROUND);

  /**
   * Name of the default interactive mode hour digits color and the ambient mode hour digits
   * color.
   */
  public static final String COLOR_NAME_DEFAULT_AND_AMBIENT_HOUR_DIGITS = "White";
  public static final int COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS =
    parseColor(COLOR_NAME_DEFAULT_AND_AMBIENT_HOUR_DIGITS);

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
  public static final String COLOR_NAME_DEFAULT_AND_AMBIENT_SECOND_DIGITS = "Gray";
  public static final int COLOR_VALUE_DEFAULT_AND_AMBIENT_SECOND_DIGITS =
    parseColor(COLOR_NAME_DEFAULT_AND_AMBIENT_SECOND_DIGITS);

  /**
   * Callback interface to perform an action with the current config {@link DataMap} for
   * {@link SunshineWatchFaceService}.
   */
  public interface FetchConfigDataMapCallback {
    /**
     * Callback invoked with the current config {@link DataMap} for
     * {@link SunshineWatchFaceService}.
     */
    void onConfigDataMapFetched(DataMap config);
  }

  private static int parseColor(String colorName) {
    return Color.parseColor(colorName.toLowerCase());
  }

  /**
   * Asynchronously fetches the current config {@link DataMap} for {@link SunshineWatchFaceService}
   * and passes it to the given callback.
   * <p>
   * If the current config {@link DataItem} doesn't exist, it isn't created and the callback
   * receives an empty DataMap.
   */
  public static void fetchConfigDataMap(final GoogleApiClient client,
                                        final FetchConfigDataMapCallback callback) {
    Wearable.NodeApi.getLocalNode(client).setResultCallback(
      new ResultCallback<NodeApi.GetLocalNodeResult>() {
        @Override
        public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
          String localNode = getLocalNodeResult.getNode().getId();
          Uri uri = new Uri.Builder()
            .scheme("wear")
            .path(DigitalWatchFaceUtil.PATH_WITH_FEATURE)
            .authority(localNode)
            .build();
          Wearable.DataApi.getDataItem(client, uri)
            .setResultCallback(new DataItemResultCallback(callback));
        }
      }
    );
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

  private DigitalWatchFaceUtil() { }
}
