package com.gelakinetic.mtgfam.helpers;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.RoundTimerFragment;

import java.util.HashMap;

/**
 * This class receives PendingIntents from the AlarmManager and acts upon them. If the device supports TTS, a service
 * will be created to do the speaking, since it can't be done in the BroadcastReceiver itself. Any failed TTS will
 * fallback to just a ringtone.
 */
public class RoundTimerBroadcastReceiver extends BroadcastReceiver {

    /* Key to pass extras into the TTS service */
    private static final String TEXT_TO_SPEAK = "text_to_speak";

    /**
     * If an N minute left PendingIntent is received, tell the TTS service to do it's thing. If the round is over,
     * play the notification sound and reset the notification in the status bar. The activity will handle the ActionBar
     * display and button text in the fragment, if it is showing
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(final Context context, Intent intent) {
        assert intent.getExtras() != null;
        int type = intent.getExtras().getInt(RoundTimerFragment.ROUND_TIMER_INTENT);
        PreferenceAdapter preferenceAdapter = new PreferenceAdapter(context);

        switch (type) {
            case RoundTimerFragment.TIMER_RING_ALARM:
                /* Play the notification sound */
                Uri ringURI = Uri.parse(preferenceAdapter.getTimerSound());
                Ringtone r = RingtoneManager.getRingtone(context, ringURI);
                r.play();

				/* Change the notification to show that the round ended */
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                Notification notification = builder
                        .setSmallIcon(R.drawable.notification_icon)
                        .setWhen(System.currentTimeMillis())
                        .setContentTitle(context.getString(R.string.main_timer))
                        .setContentText(context.getString(R.string.timer_ended))
                        .setContentIntent(PendingIntent.getActivity(context, 7, (new Intent(context,
                                FamiliarActivity.class).setAction(FamiliarActivity.ACTION_ROUND_TIMER)), 0))
                        .build();
                notification.flags |= Notification.FLAG_AUTO_CANCEL;

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(RoundTimerFragment.TIMER_NOTIFICATION_ID);
                notificationManager.notify(RoundTimerFragment.TIMER_NOTIFICATION_ID, notification);
                break;
            case RoundTimerFragment.TIMER_5_MIN_WARNING:
                if (preferenceAdapter.getFiveMinutePref()) {
                    context.startService(new Intent(context, TtsService.class)
                            .putExtra(TEXT_TO_SPEAK, R.string.timer_five_minutes_left));
                }
                break;
            case RoundTimerFragment.TIMER_10_MIN_WARNING:
                if (preferenceAdapter.getTenMinutePref()) {
                    context.startService(new Intent(context, TtsService.class)
                            .putExtra(TEXT_TO_SPEAK, R.string.timer_ten_minutes_left));
                }
                break;
            case RoundTimerFragment.TIMER_15_MIN_WARNING:
                if (preferenceAdapter.getFifteenMinutePref()) {
                    context.startService(new Intent(context, TtsService.class)
                            .putExtra(TEXT_TO_SPEAK, R.string.timer_fifteen_minutes_left));
                }
                break;
            case RoundTimerFragment.TIMER_EASTER_EGG:
                if (preferenceAdapter.getFifteenMinutePref()) {
                    context.startService(new Intent(context, TtsService.class)
                            .putExtra(TEXT_TO_SPEAK, R.string.timer_easter_egg));
                }
                break;
        }
    }

    /**
     * This nested service is responsible for initializing the TTS engine and speaking warnings
     */
    public static class TtsService extends Service implements TextToSpeech.OnInitListener,
            TextToSpeech.OnUtteranceCompletedListener, AudioManager.OnAudioFocusChangeListener {

        private static final String WARNING_SPEECH = "warning_speech";
        private TextToSpeech mTts;
        private String mTextToSpeak;
        private AudioManager mAudioManager;

        /**
         * Default empty constructor. Necessary
         */
        public TtsService() {
            super();
        }

        /**
         * Returns null, since the client does not bind to the service
         *
         * @param arg0 The intent that was used to bind to this service
         * @return null, since nothing binds to this service
         */
        @Override
        public IBinder onBind(Intent arg0) {
            return null;
        }

        /**
         * Make sure to clean up the tts engine when being destroyed
         */
        @Override
        public void onDestroy() {
            if (mTts != null) {
                mTts.stop();
                mTts.shutdown();
            }
            super.onDestroy();
        }

        /**
         * Called by the system every time a client explicitly starts the service by calling startService(Intent),
         * providing the arguments it supplied and a unique integer token representing the start request.
         * Do not call this method directly. Starts the tts engine and grabs the string to speak.
         *
         * @param intent  The Intent supplied to startService(Intent), as given.
         * @param flags   Additional data about this start request. Currently either 0, START_FLAG_REDELIVERY, or
         *                START_FLAG_RETRY.
         * @param startId A unique integer representing this specific request to start. Use with stopSelfResult(int).
         * @return START_NOT_STICKY since this service does a little work and then stops itself
         */
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            mTts = new TextToSpeech(this, this);
            mTts.setOnUtteranceCompletedListener(this);
            mTextToSpeak = getString(intent.getIntExtra(TEXT_TO_SPEAK, 0));
            assert getApplicationContext() != null;
            mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            return START_NOT_STICKY;
        }

        /**
         * When the TTS engine initializes, do some speaking. If it doesn't initialize, or the device does not support
         * the language, fall back to playing the chosen ringtone, then stop the service
         *
         * @param status TextToSpeech.SUCCESS or TextToSpeech.ERROR
         */
        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                /* Try to say what needs to be said */
                int result = mTts.setLanguage(getResources().getConfiguration().locale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    /* Fall back to ringtone */
                    Uri ringURI = Uri.parse(new PreferenceAdapter(getApplicationContext()).getTimerSound());
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), ringURI);
                    r.play();
                } else {
                    /* Request audio focus for playback on the alarm stream */
                    int res = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_ALARM,
                            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

                    if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                                            /* Do some speaking, on the alarm stream */
                        HashMap<String, String> ttsParams = new HashMap<>();
                        ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, WARNING_SPEECH);
                        ttsParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
                        mTts.speak(mTextToSpeak, TextToSpeech.QUEUE_FLUSH, ttsParams);
                        return; /* if we don't return, the service will stop before speaking */
                    } else {
                        /* Fall back to ringtone */
                        Uri ringURI = Uri.parse(new PreferenceAdapter(getApplicationContext()).getTimerSound());
                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), ringURI);
                        r.play();
                    }
                }
            } else {
				/* Fall back to ringtone */
                Uri ringURI = Uri.parse(new PreferenceAdapter(getApplicationContext()).getTimerSound());
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), ringURI);
                r.play();
            }
			/* The ringtone has played, so stop the service */
            stopSelf();
        }

        /**
         * When the TTS is done speaking, stop the service
         *
         * @param s The value in the HashMap, ignored since this service says one thing and then dies
         */
        @Override
        public void onUtteranceCompleted(String s) {
			/* The TTS is done, so release audio focus and stop the service */
            mAudioManager.abandonAudioFocus(this);
            stopSelf();
        }

        /**
         * Necessary to implement AudioManager.OnAudioFocusChangeListener, but it's never called.
         *
         * @param focusChange The new focus
         */
        @Override
        public void onAudioFocusChange(int focusChange) {
			/* don't really care */
        }
    }
}
