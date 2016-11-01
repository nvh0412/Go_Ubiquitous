package com.vagabond.goubiquitous;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class SunshineWatchFaceService extends CanvasWatchFaceService {

  private static final String TAG = "SunshineFace";

  private static final Typeface BOLD_TYPEFACE =
    Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
  private static final Typeface NORMAL_TYPEFACE =
    Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

  /**
   * Update rate in milliseconds for normal (not ambient and not mute) mode. We update twice
   * a second to blink the colons.
   */
  private static final long NORMAL_UPDATE_RATE_MS = 500;

  /**
   * Update rate in milliseconds for mute mode. We update every minute, like in ambient mode.
   */
  private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

  @Override
  public Engine onCreateEngine() {
    return new Engine();
  }

  private class Engine extends CanvasWatchFaceService.Engine implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private static final String COLON_STRING = ":";
    private static final int MUTE_ALPHA = 100;
    private static final int NORMAL_ALPHA = 255;
    private static final int MSG_UPDATE_TIME = 0;
    private static final String KEY_PATH = "/weather";
    private static final String KEY_WEATHER_ID = "KEY_WEATHER_ID";
    private static final String KEY_MAX_TEMP = "KEY_MAX_TEMP";
    private static final String KEY_MIN_TEMP = "KEY_MIN_TEMP";
    private long mInteractiveUpdateRateMs = NORMAL_UPDATE_RATE_MS;
    private boolean mRegisteredReceiver = false;

    private Paint mBackgroundPaint;
    private Paint mDatePaint;
    private Paint mHourPaint;
    private Paint mMinutePaint;
    private Paint mColonPaint;
    private Paint linePaint;
    private float mColonWidth;
    private boolean mMute;

    private Calendar mCalendar;
    private Date mDate;
    private java.text.DateFormat mDateFormat;

    private float mXOffset;
    private float mYOffset;
    private float mLineHeight;
    private int mInteractiveBackgroundColor = R.color.primary;
    private int mInteractiveHourDigitsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS;
    private int mInteractiveMinuteDigitsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS;
    private boolean mLowBitAmbient;

    final Handler mUpdateTimeHandler = new Handler() {
      @Override
      public void handleMessage(Message message) {
        switch (message.what) {
          case MSG_UPDATE_TIME:
            if (shouldTimerBeRunning()) {
              long timeMs = System.currentTimeMillis();
              long delayMs = mInteractiveUpdateRateMs - (timeMs % mInteractiveUpdateRateMs);
              mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
            invalidate();
            break;
        }
      }
    };

    GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFaceService.this)
      .addConnectionCallbacks(this)
      .addOnConnectionFailedListener(this)
      .addApi(Wearable.API)
      .build();


    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        mCalendar.setTimeZone(TimeZone.getDefault());
        initFormats();
        invalidate();
      }
    };
    private Paint mMaxTempPaint;
    private Paint mMinTempPaint;
    private String mWeatherMaxTemp;
    private String mWeatherMinTemp;
    private Bitmap mWeatherImage;

    @Override
    public void onCreate(SurfaceHolder holder) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onCreate");
      }
      super.onCreate(holder);

      setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFaceService.this)
        .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
        .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
        .setShowSystemUiTime(false)
        .build());
      Resources resources = SunshineWatchFaceService.this.getResources();
      mYOffset = resources.getDimension(R.dimen.digital_y_offset);
      mLineHeight = resources.getDimension(R.dimen.digital_line_height);

      mBackgroundPaint = new Paint();
      mBackgroundPaint.setColor(resources.getColor(mInteractiveBackgroundColor));
      mDatePaint = createTextPaint(resources.getColor(R.color.digital_date));
      mHourPaint = createTextPaint(mInteractiveHourDigitsColor, BOLD_TYPEFACE);
      mMinutePaint = createTextPaint(mInteractiveMinuteDigitsColor);
      mMaxTempPaint = createTextPaint(resources.getColor(R.color.digital_date), BOLD_TYPEFACE);
      mMinTempPaint = createTextPaint(resources.getColor(R.color.digital_date));
      mColonPaint = createTextPaint(resources.getColor(R.color.digital_colons));

      linePaint = new Paint();
      linePaint.setColor(resources.getColor(R.color.digital_date));

      mCalendar = Calendar.getInstance();
      mDate = new Date();
      initFormats();
    }

    @Override
    public void onDestroy() {
      mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
      super.onDestroy();
    }

    private Paint createTextPaint(int defaultInteractiveColor) {
      return createTextPaint(defaultInteractiveColor, NORMAL_TYPEFACE);
    }

    private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
      Paint paint = new Paint();
      paint.setColor(defaultInteractiveColor);
      paint.setTypeface(typeface);
      paint.setAntiAlias(true);
      return paint;
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
      Log.d(TAG, "onVisibilityChanged: " + visible);
      super.onVisibilityChanged(visible);

      if (visible) {
        mGoogleApiClient.connect();

        registerReceiver();

        // Update time zone and date formats, in case they changed while we weren't visible.
        mCalendar.setTimeZone(TimeZone.getDefault());
        initFormats();
      } else {
        unregisterReceiver();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
          Wearable.DataApi.removeListener(mGoogleApiClient, this);
          mGoogleApiClient.disconnect();
        }
      }

      // Whether the timer should be running depends on whether we're visible (as well as
      // whether we're in ambient mode), so we may need to start or stop the timer.
      updateTimer();
    }

    private void initFormats() {
      mDateFormat = new SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault());
      mDateFormat.setCalendar(mCalendar);
    }

    private void registerReceiver() {
      if (mRegisteredReceiver) {
        return;
      }
      mRegisteredReceiver = true;
      IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
      filter.addAction(Intent.ACTION_LOCALE_CHANGED);
      SunshineWatchFaceService.this.registerReceiver(mReceiver, filter);
    }

    private void unregisterReceiver() {
      if (!mRegisteredReceiver) {
        return;
      }
      mRegisteredReceiver = false;
      SunshineWatchFaceService.this.unregisterReceiver(mReceiver);
    }

    @Override
    public void onApplyWindowInsets(WindowInsets insets) {
      Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));
      super.onApplyWindowInsets(insets);

      // Load resources that have alternate values for round watches.
      Resources resources = SunshineWatchFaceService.this.getResources();
      boolean isRound = insets.isRound();
      mXOffset = resources.getDimension(isRound
        ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
      float textSize = resources.getDimension(isRound
        ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

      float tempSize = resources.getDimension(isRound
        ? R.dimen.digital_temp_size_round : R.dimen.digital_temp_size);

      mDatePaint.setTextSize(resources.getDimension(R.dimen.digital_date_text_size));
      mHourPaint.setTextSize(textSize);
      mMinutePaint.setTextSize(textSize);
      mMaxTempPaint.setTextSize(tempSize);
      mMinTempPaint.setTextSize(tempSize);
      mColonPaint.setTextSize(textSize);

      mColonWidth = mColonPaint.measureText(COLON_STRING);
    }

    @Override
    public void onPropertiesChanged(Bundle properties) {
      super.onPropertiesChanged(properties);

      boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
      mHourPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);

      mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onPropertiesChanged: burn-in protection = " + burnInProtection
          + ", low-bit ambient = " + mLowBitAmbient);
      }
    }

    @Override
    public void onTimeTick() {
      super.onTimeTick();
      Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
      invalidate();
    }

    @Override
    public void onAmbientModeChanged(boolean inAmbientMode) {
      super.onAmbientModeChanged(inAmbientMode);
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
      }

      if (mLowBitAmbient) {
        boolean antiAlias = !inAmbientMode;
        mDatePaint.setAntiAlias(antiAlias);
        mHourPaint.setAntiAlias(antiAlias);
        mMinutePaint.setAntiAlias(antiAlias);
        mColonPaint.setAntiAlias(antiAlias);
      }
      invalidate();

      // Whether the timer should be running depends on whether we're in ambient mode (as well
      // as whether we're visible), so we may need to start or stop the timer.
      updateTimer();
    }

    @Override
    public void onInterruptionFilterChanged(int interruptionFilter) {
      Log.d(TAG, "onInterruptionFilterChanged: " + interruptionFilter);
      super.onInterruptionFilterChanged(interruptionFilter);

      boolean inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE;
      // We only need to update once a minute in mute mode.
      setInteractiveUpdateRateMs(inMuteMode ? MUTE_UPDATE_RATE_MS : NORMAL_UPDATE_RATE_MS);

      if (mMute != inMuteMode) {
        mMute = inMuteMode;
        int alpha = inMuteMode ? MUTE_ALPHA : NORMAL_ALPHA;
        mDatePaint.setAlpha(alpha);
        mHourPaint.setAlpha(alpha);
        mMinutePaint.setAlpha(alpha);
        mColonPaint.setAlpha(alpha);
        invalidate();
      }
    }

    public void setInteractiveUpdateRateMs(long updateRateMs) {
      if (updateRateMs == mInteractiveUpdateRateMs) {
        return;
      }
      mInteractiveUpdateRateMs = updateRateMs;

      // Stop and restart the timer so the new update rate takes effect immediately.
      if (shouldTimerBeRunning()) {
        updateTimer();
      }
    }

    private String formatTwoDigitNumber(int hour) {
      return String.format("%02d", hour);
    }

    @Override
    public void onDraw(Canvas canvas, Rect bounds) {
      long now = System.currentTimeMillis();
      mCalendar.setTimeInMillis(now);
      mDate.setTime(now);

      // Draw the background.
      canvas.drawColor(SunshineWatchFaceService.this.getResources().getColor(R.color.primary));
      canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

      // Draw the hours.
      String hourString = formatTwoDigitNumber(mCalendar.get(Calendar.HOUR_OF_DAY));
      float x = bounds.width() / 2 - mHourPaint.measureText(hourString);

      canvas.drawText(hourString, x, mYOffset, mHourPaint);
      x += mHourPaint.measureText(hourString);

      canvas.drawText(COLON_STRING, x, mYOffset, mColonPaint);
      x += mColonWidth;

      // Draw the minutes.
      String minuteString = formatTwoDigitNumber(mCalendar.get(Calendar.MINUTE));
      canvas.drawText(minuteString, x, mYOffset, mMinutePaint);

      String mDateStr = mDateFormat.format(mDate);
      float mDatePosition = (bounds.width() - mDatePaint.measureText(mDateStr)) / 2;
      if (getPeekCardPosition().isEmpty()) {
        canvas.drawText(mDateStr, mDatePosition, mYOffset + mLineHeight, mDatePaint);
      }

      // Draw the horizontal line
      x = bounds.width() / 2 - 30;

      canvas.drawLine(x, mYOffset + mLineHeight * 2, x + 60, mYOffset + mLineHeight * 2, linePaint);

      // Weather
      String maxTempString = mWeatherMaxTemp + "\u00b0";
      String minTempString = mWeatherMinTemp + "\u00b0";
      float maxTempPosition = bounds.width() / 2;
      float minTempPosition = bounds.width() / 2;
      // MaxTemp
      canvas.drawText(maxTempString, maxTempPosition,
        mYOffset + mLineHeight * 3, mMaxTempPaint);
      // MinTemp
      canvas.drawText(minTempString, minTempPosition,
        mYOffset + mLineHeight * 4, mMinTempPaint);
    }

    private void updateTimer() {
      Log.d(TAG, "updateTimer");
      mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
      if (shouldTimerBeRunning()) {
        mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
      }
    }

    private boolean shouldTimerBeRunning() {
      return isVisible() && !isInAmbientMode();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
      Log.d(TAG, "onConnected: " + bundle);
      Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
      updateWeatherDataItemAndUiOnStartup();
    }

    private void updateWeatherDataItemAndUiOnStartup() {
      WatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
        new WatchFaceUtil.FetchConfigDataMapCallback() {
          @Override
          public void onConfigDataMapFetched(DataMap startupData) {
            WatchFaceUtil.putConfigDataItem(mGoogleApiClient, startupData);
            invalidate();
          }
        }
      );
    }

    @Override
    public void onConnectionSuspended(int i) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onConnectionSuspended: " + i);
      }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
      Log.e(TAG, "onConnectionFailed: " + result);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
      Log.d(TAG, "onDataChanged");
      for (DataEvent dataEvent : dataEventBuffer) {
        Log.d(TAG, "onDataChanged: DataEvent: " + dataEvent.getDataItem().toString());
        DataItem dataItem = dataEvent.getDataItem();
        if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
          DataItem item = dataEvent.getDataItem();
          if (dataItem.getUri().getPath().equals(KEY_PATH)) {
            processDataFor(item);
          }
        }
      }
    }

    private void processDataFor(DataItem dataItem) {
      Log.d(TAG, "processConfigurationFor");
      DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
      int weatherId = dataMap.getInt(KEY_WEATHER_ID);
      if (weatherId != 0) {
        mWeatherImage = BitmapFactory.decodeResource(getResources(),
          WatchFaceUtil.getArtResourceForWeatherCondition(weatherId));
      }
      if (mWeatherImage == null) {
        mWeatherImage = BitmapFactory.decodeResource(getResources(),
          R.mipmap.ic_launcher);
      }
      mWeatherMaxTemp = dataMap.getString(KEY_MAX_TEMP);
      mWeatherMinTemp = dataMap.getString(KEY_MIN_TEMP);
    }
  }
}

