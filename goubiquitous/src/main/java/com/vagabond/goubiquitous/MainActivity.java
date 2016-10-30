package com.vagabond.goubiquitous;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Date;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
  private static final String LOG_TAG = MainActivity.class.getSimpleName();
  private TextView mTextView;
  private GoogleApiClient googleApiClient;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
    stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
      @Override
      public void onLayoutInflated(WatchViewStub stub) {
        mTextView = (TextView) stub.findViewById(R.id.text);
      }
    });

    googleApiClient = new GoogleApiClient.Builder(this)
      .addApi(Wearable.API)
      .addConnectionCallbacks(this)
      .addOnConnectionFailedListener(this)
      .build();
    googleApiClient.connect();
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    Log.d(LOG_TAG, "onConnected");
    sendStepCount(1, (new Date()).getTime());
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.d(LOG_TAG, "onConnectionSuspended");
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    Log.d(LOG_TAG, "onConnectionFailed");
  }

  public void sendStepCount(int steps, long timeStamp) {
    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/step-counter");

    putDataMapRequest.getDataMap().putInt("step-count", steps);
    putDataMapRequest.getDataMap().putLong("timestamp", timeStamp);

    PutDataRequest request = putDataMapRequest.asPutDataRequest();
    Wearable.DataApi.putDataItem(googleApiClient, request)
      .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
        @Override
        public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
          if (!dataItemResult.getStatus().isSuccess()) {
            Log.e(LOG_TAG, "Wearable connection fails");
          } else {
            Log.e(LOG_TAG, "Wearable connection succeeds");
          }
        }
      });
  }
}
