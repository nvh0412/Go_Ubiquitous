package com.example.android.sunshine.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;


public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
  private static final String LOG_TAG = MainActivity.class.getSimpleName();
  private TextView mTextView;
  private GoogleApiClient mGoogleApiClient;

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

    mGoogleApiClient = new GoogleApiClient.Builder(this)
      .addApi(Wearable.API)
      .addConnectionCallbacks(this)
      .addOnConnectionFailedListener(this)
      .build();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mGoogleApiClient.connect();
  }

  @Override
  protected void onPause() {
    if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {
      mGoogleApiClient.disconnect();
    }

    super.onPause();
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    Log.d(LOG_TAG, "onConnected(): Successfully connected to Google API client");
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.d(LOG_TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    Log.e(LOG_TAG, "onConnectionFailed(): Failed to connect, with result: " + connectionResult);
  }
}
