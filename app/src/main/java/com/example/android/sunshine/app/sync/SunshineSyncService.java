package com.example.android.sunshine.app.sync;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

public class SunshineSyncService extends Service implements
  GoogleApiClient.ConnectionCallbacks,
  GoogleApiClient.OnConnectionFailedListener {
  private static final String LOG_TAG = SunshineSyncService.class.getSimpleName();
  private static final Object sSyncAdapterLock = new Object();
  private static SunshineSyncAdapter sSunshineSyncAdapter = null;
  private GoogleApiClient mGoogleApiClient;

  @Override
  public void onCreate() {
    Log.d("SunshineSyncService", "onCreate - SunshineSyncService");
    synchronized (sSyncAdapterLock) {
      if (sSunshineSyncAdapter == null) {
        mGoogleApiClient = new GoogleApiClient.Builder(getBaseContext())
          .addApi(Wearable.API)
          .addConnectionCallbacks(this)
          .addOnConnectionFailedListener(this)
          .build();

        mGoogleApiClient.connect();

        sSunshineSyncAdapter = new SunshineSyncAdapter(getApplicationContext(), true, mGoogleApiClient);
      }
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return sSunshineSyncAdapter.getSyncAdapterBinder();
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    Log.d(LOG_TAG, "onConnected");
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.d(LOG_TAG,"onConnectionSuspended");
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    Log.d(LOG_TAG,"onConnectionFailed");
  }
}