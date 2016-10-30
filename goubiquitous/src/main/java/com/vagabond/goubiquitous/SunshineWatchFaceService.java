package com.vagabond.goubiquitous;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

  private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    static final String COLON_STRING = ":";

    /** Alpha value for drawing time when in mute mode. */
    static final int MUTE_ALPHA = 100;

    /** Alpha value for drawing time when not in mute mode. */
    static final int NORMAL_ALPHA = 255;

    static final int MSG_UPDATE_TIME = 0;

    /** How often {@link #mUpdateTimeHandler} ticks in milliseconds. */
    long mInteractiveUpdateRateMs = NORMAL_UPDATE_RATE_MS;

    /** Handler to update the time periodically in interactive mode. */
    final Handler mUpdateTimeHandler = new Handler() {
      @Override
      public void handleMessage(Message message) {
        switch (message.what) {
          case MSG_UPDATE_TIME:
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
              Log.v(TAG, "updating time");
            }
            invalidate();
            if (shouldTimerBeRunning()) {
              long timeMs = System.currentTimeMillis();
              long delayMs =
                mInteractiveUpdateRateMs - (timeMs % mInteractiveUpdateRateMs);
              mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
            break;
        }
      }
    };

    GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFaceService.this)
      .addConnectionCallbacks(this)
      .addOnConnectionFailedListener(this)
      .addApi(Wearable.API)
      .build();

    /**
     * Handles time zone and locale changes.
     */
    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        mCalendar.setTimeZone(TimeZone.getDefault());
        initFormats();
        invalidate();
      }
    };

    /**
     * Unregistering an unregistered receiver throws an exception. Keep track of the
     * registration state to prevent that.
     */
    boolean mRegisteredReceiver = false;

    Paint mBackgroundPaint;
    Paint mDatePaint;
    Paint mHourPaint;
    Paint mMinutePaint;
    Paint mColonPaint;
    Paint linePaint;
    float mColonWidth;
    boolean mMute;

    Calendar mCalendar;
    Date mDate;
    java.text.DateFormat mDateFormat;

    boolean mShouldDrawColons;
    float mXOffset;
    float mYOffset;
    float mLineHeight;
    int mInteractiveBackgroundColor =
      R.color.primary;
    int mInteractiveHourDigitsColor =
      DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS;
    int mInteractiveMinuteDigitsColor =
      DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS;

    /**
     * Whether the display supports fewer bits for each color in ambient mode. When true, we
     * disable anti-aliasing in ambient mode.
     */
    boolean mLowBitAmbient;

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
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onVisibilityChanged: " + visible);
      }
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
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));
      }
      super.onApplyWindowInsets(insets);

      // Load resources that have alternate values for round watches.
      Resources resources = SunshineWatchFaceService.this.getResources();
      boolean isRound = insets.isRound();
      mXOffset = resources.getDimension(isRound
        ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
      float textSize = resources.getDimension(isRound
        ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

      mDatePaint.setTextSize(resources.getDimension(R.dimen.digital_date_text_size));
      mHourPaint.setTextSize(textSize);
      mMinutePaint.setTextSize(textSize);
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
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
      }
      invalidate();
    }

    @Override
    public void onAmbientModeChanged(boolean inAmbientMode) {
      super.onAmbientModeChanged(inAmbientMode);
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
      }
      adjustPaintColorToCurrentMode(mBackgroundPaint, mInteractiveBackgroundColor,
        DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND);
      adjustPaintColorToCurrentMode(mHourPaint, mInteractiveHourDigitsColor,
        DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS);
      adjustPaintColorToCurrentMode(mMinutePaint, mInteractiveMinuteDigitsColor,
        DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS);

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

    private void adjustPaintColorToCurrentMode(Paint paint, int interactiveColor,
                                               int ambientColor) {
      paint.setColor(isInAmbientMode() ? ambientColor : interactiveColor);
    }

    @Override
    public void onInterruptionFilterChanged(int interruptionFilter) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onInterruptionFilterChanged: " + interruptionFilter);
      }
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

    private void updatePaintIfInteractive(Paint paint, int interactiveColor) {
      if (!isInAmbientMode() && paint != null) {
        paint.setColor(interactiveColor);
      }
    }

    private void setInteractiveBackgroundColor(int color) {
      mInteractiveBackgroundColor = color;
      updatePaintIfInteractive(mBackgroundPaint, color);
    }

    private void setInteractiveHourDigitsColor(int color) {
      mInteractiveHourDigitsColor = color;
      updatePaintIfInteractive(mHourPaint, color);
    }

    private void setInteractiveMinuteDigitsColor(int color) {
      mInteractiveMinuteDigitsColor = color;
      updatePaintIfInteractive(mMinutePaint, color);
    }

    private String formatTwoDigitNumber(int hour) {
      return String.format("%02d", hour);
    }

    @Override
    public void onDraw(Canvas canvas, Rect bounds) {
      long now = System.currentTimeMillis();
      mCalendar.setTimeInMillis(now);
      mDate.setTime(now);

      // Show colons for the first half of each second so the colons blink on when the time
      // updates.
      mShouldDrawColons = (System.currentTimeMillis() % 1000) < 500;

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

      if (getPeekCardPosition().isEmpty()) {
        canvas.drawText(mDateFormat.format(mDate), mXOffset, mYOffset + mLineHeight, mDatePaint);
      }

      // Draw the horizontal line
      x = bounds.width() / 2 - 30;

      canvas.drawLine(x,  mYOffset + mLineHeight * 2, x + 60,  mYOffset + mLineHeight * 2, linePaint);
    }

    /**
     * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
     * or stops it if it shouldn't be running but currently is.
     */
    private void updateTimer() {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "updateTimer");
      }
      mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
      if (shouldTimerBeRunning()) {
        mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
      }
    }

    /**
     * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
     * only run when we're visible and in interactive mode.
     */
    private boolean shouldTimerBeRunning() {
      return isVisible() && !isInAmbientMode();
    }

    private void updateConfigDataItemAndUiOnStartup() {
      DigitalWatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
        new DigitalWatchFaceUtil.FetchConfigDataMapCallback() {
          @Override
          public void onConfigDataMapFetched(DataMap startupConfig) {
            // If the DataItem hasn't been created yet or some keys are missing,
            // use the default values.
            setDefaultValuesForMissingConfigKeys(startupConfig);
            DigitalWatchFaceUtil.putConfigDataItem(mGoogleApiClient, startupConfig);

            updateUiForConfigDataMap(startupConfig);
          }
        }
      );
    }

    private void setDefaultValuesForMissingConfigKeys(DataMap config) {
      addIntKeyIfMissing(config, DigitalWatchFaceUtil.KEY_BACKGROUND_COLOR,
        DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND);
      addIntKeyIfMissing(config, DigitalWatchFaceUtil.KEY_HOURS_COLOR,
        DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS);
      addIntKeyIfMissing(config, DigitalWatchFaceUtil.KEY_MINUTES_COLOR,
        DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS);
      addIntKeyIfMissing(config, DigitalWatchFaceUtil.KEY_SECONDS_COLOR,
        DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_SECOND_DIGITS);
    }

    private void addIntKeyIfMissing(DataMap config, String key, int color) {
      if (!config.containsKey(key)) {
        config.putInt(key, color);
      }
    }

    @Override // DataApi.DataListener
    public void onDataChanged(DataEventBuffer dataEvents) {
      for (DataEvent dataEvent : dataEvents) {
        if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
          continue;
        }

        DataItem dataItem = dataEvent.getDataItem();
        if (!dataItem.getUri().getPath().equals(
          DigitalWatchFaceUtil.PATH_WITH_FEATURE)) {
          continue;
        }

        DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
        DataMap config = dataMapItem.getDataMap();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Config DataItem updated:" + config);
        }
        updateUiForConfigDataMap(config);
      }
    }

    private void updateUiForConfigDataMap(final DataMap config) {
      boolean uiUpdated = false;
      for (String configKey : config.keySet()) {
        if (!config.containsKey(configKey)) {
          continue;
        }
        int color = config.getInt(configKey);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Found watch face config key: " + configKey + " -> "
            + Integer.toHexString(color));
        }
        if (updateUiForKey(configKey, color)) {
          uiUpdated = true;
        }
      }
      if (uiUpdated) {
        invalidate();
      }
    }

    /**
     * Updates the color of a UI item according to the given {@code configKey}. Does nothing if
     * {@code configKey} isn't recognized.
     *
     * @return whether UI has been updated
     */
    private boolean updateUiForKey(String configKey, int color) {
      if (configKey.equals(DigitalWatchFaceUtil.KEY_BACKGROUND_COLOR)) {
        setInteractiveBackgroundColor(color);
      } else if (configKey.equals(DigitalWatchFaceUtil.KEY_HOURS_COLOR)) {
        setInteractiveHourDigitsColor(color);
      } else if (configKey.equals(DigitalWatchFaceUtil.KEY_MINUTES_COLOR)) {
        setInteractiveMinuteDigitsColor(color);
      } else {
        Log.w(TAG, "Ignoring unknown config key: " + configKey);
        return false;
      }
      return true;
    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onConnected: " + connectionHint);
      }
      Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
      updateConfigDataItemAndUiOnStartup();
    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
      }
    }

    @Override  // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "onConnectionFailed: " + result);
      }
    }
  }
}

