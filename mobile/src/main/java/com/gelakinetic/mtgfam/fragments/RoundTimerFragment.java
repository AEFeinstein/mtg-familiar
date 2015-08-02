package com.gelakinetic.mtgfam.fragments;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import com.alertdialogpro.AlertDialogPro;
import com.codetroopers.betterpickers.hmspicker.HmsPicker;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.RoundTimerBroadcastReceiver;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;

/**
 * This fragment starts and stops the round timer. When it is started, it commits the end time as a shared preference,
 * sets a series of PendingIntents with the AlarmManager, creates a notification, and tells the FamiliarActivity to
 * display the time in the ActionBar.
 * <p/>
 * Future activities and fragments will determine if the timer is running by checking the shared preference. If it is -1
 * the timer is no longer running. It will automatically be set to -1 when the final PendingIntent fires, or if it has
 * expired and then checked.
 * <p/>
 * This means that the round timer persists through literally anything, even getting automatically restarted in place
 * after a force close (if the app is opened again). The FamiliarActivity will take care of recreating the notification
 * and PendingIntents.
 */
public class RoundTimerFragment extends FamiliarFragment {

    /* Constants */
    public static final String ROUND_TIMER_INTENT = "rt_intent";
    public static final int TIMER_RING_ALARM = 1;
    public static final int TIMER_5_MIN_WARNING = 2;
    public static final int TIMER_10_MIN_WARNING = 3;
    public static final int TIMER_15_MIN_WARNING = 4;
    public static final int TIMER_EASTER_EGG = 5;
    public static final int TIMER_NOTIFICATION_ID = 53;
    private static final int DIALOG_SET_WARNINGS = 1;
    private static final int RINGTONE_REQUEST_CODE = 17;
    /* Variables */
    private Button mTimerButton;

    private HmsPicker mTimePicker;

    /**
     * This static method is used to either set or cancel PendingIntents with the AlarmManager. It is static so that
     * the FamiliarActivity can restart alarms without instantiating a fragment.
     *
     * @param context The application context to build the PendingIntents with
     * @param endTime The time the round should end
     * @param set     Whether to set the alarms after cancelling them
     */
    public static void setOrCancelAlarms(Context context, long endTime, boolean set) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent AlarmPendingIntent = PendingIntent.getBroadcast(context, TIMER_RING_ALARM, new Intent(context,
                        RoundTimerBroadcastReceiver.class).putExtra(ROUND_TIMER_INTENT, TIMER_RING_ALARM),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        PendingIntent fiveMinPI = PendingIntent.getBroadcast(context, TIMER_5_MIN_WARNING, new Intent(context,
                        RoundTimerBroadcastReceiver.class).putExtra(ROUND_TIMER_INTENT, TIMER_5_MIN_WARNING),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        PendingIntent tenMinPI = PendingIntent.getBroadcast(context, TIMER_10_MIN_WARNING, new Intent(context,
                        RoundTimerBroadcastReceiver.class).putExtra(ROUND_TIMER_INTENT, TIMER_10_MIN_WARNING),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        PendingIntent fifteenMinPI = PendingIntent.getBroadcast(context, TIMER_15_MIN_WARNING, new Intent(context,
                        RoundTimerBroadcastReceiver.class).putExtra(ROUND_TIMER_INTENT, TIMER_15_MIN_WARNING),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        PendingIntent easterEggPI = PendingIntent.getBroadcast(context, TIMER_EASTER_EGG, new Intent(context,
                        RoundTimerBroadcastReceiver.class).putExtra(ROUND_TIMER_INTENT, TIMER_EASTER_EGG),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

		/* Cancel any pending alarms */
        am.cancel(AlarmPendingIntent);
        am.cancel(fiveMinPI);
        am.cancel(tenMinPI);
        am.cancel(fifteenMinPI);
        am.cancel(easterEggPI);

        if (set) {
            if (Build.VERSION.SDK_INT >= 19) {
                /* Set all applicable alarms */
                am.setExact(AlarmManager.RTC_WAKEUP, endTime, AlarmPendingIntent);

                if (endTime - System.currentTimeMillis() > 5 * 60 * 1000) {
                    am.setExact(AlarmManager.RTC_WAKEUP, endTime - 5 * 60 * 1000, fiveMinPI);
                }
                if (endTime - System.currentTimeMillis() > 10 * 60 * 1000) {
                    am.setExact(AlarmManager.RTC_WAKEUP, endTime - 10 * 60 * 1000, tenMinPI);
                }
                if (endTime - System.currentTimeMillis() > 15 * 60 * 1000) {
                    am.setExact(AlarmManager.RTC_WAKEUP, endTime - 15 * 60 * 1000, fifteenMinPI);
                }
                if (endTime - System.currentTimeMillis() > 12 * 60 * 60 * 1000) {
                    am.setExact(AlarmManager.RTC_WAKEUP, endTime - 12 * 60 * 60 * 1000, easterEggPI);
                }
            } else {
                /* Set all applicable alarms */
                am.set(AlarmManager.RTC_WAKEUP, endTime, AlarmPendingIntent);

                if (endTime - System.currentTimeMillis() > 5 * 60 * 1000) {
                    am.set(AlarmManager.RTC_WAKEUP, endTime - 5 * 60 * 1000, fiveMinPI);
                }
                if (endTime - System.currentTimeMillis() > 10 * 60 * 1000) {
                    am.set(AlarmManager.RTC_WAKEUP, endTime - 10 * 60 * 1000, tenMinPI);
                }
                if (endTime - System.currentTimeMillis() > 15 * 60 * 1000) {
                    am.set(AlarmManager.RTC_WAKEUP, endTime - 15 * 60 * 1000, fifteenMinPI);
                }
                if (endTime - System.currentTimeMillis() > 12 * 60 * 60 * 1000) {
                    am.set(AlarmManager.RTC_WAKEUP, endTime - 12 * 60 * 60 * 1000, easterEggPI);
                }
            }
        }
    }

    /**
     * Create and show a notification in the status bar. It will say when the round ends. This method is static so that
     * FamiliarActivity can call it without instantiating a fragment
     *
     * @param context The application context to build the notification with
     * @param endTime The time the round will end, relative to System.currentTimeInMillis()
     */
    public static void showTimerRunningNotification(Context context, long endTime) {
        /* Format the String */
        Calendar then = Calendar.getInstance();
        then.add(Calendar.MILLISECOND, (int) (endTime - System.currentTimeMillis()));
        String messageText = String.format(context.getString(R.string.timer_notification_ongoing), then);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.notification_icon)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(context.getString(R.string.main_timer))
                .setContentText(messageText)
                .setContentIntent(PendingIntent.getActivity(context, 7, new Intent(context,
                        FamiliarActivity.class).setAction(FamiliarActivity.ACTION_ROUND_TIMER), 0))
                .setOngoing(true);

        /* Get an instance of the NotificationManager service */
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        /* Clear any existing notifications just in case there's still one there */
        notificationManager.cancel(TIMER_NOTIFICATION_ID);
        /* Then show the new one */
        notificationManager.notify(TIMER_NOTIFICATION_ID, builder.build());
    }

    /**
     * Inflate the View and pull out UI elements. Attach an action to the button to either start or stop the timer,
     * depending on if the timer is currently running or not.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return The inflated view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.round_timer_frag, container, false);

        assert v != null;

        mTimePicker = (HmsPicker) v.findViewById(R.id.rt_time_picker);
        mTimePicker.setTheme(getResourceIdFromAttr(R.attr.hms_picker_style));

        mTimerButton = ((Button) v.findViewById(R.id.rt_action_button));
        mTimerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getFamiliarActivity().mPreferenceAdapter.getRoundTimerEnd() != -1) {
                    /* Commit the endTime as -1 */
                    getFamiliarActivity().mPreferenceAdapter.setRoundTimerEnd(-1);
                    /* Cancel the alarms */
                    setOrCancelAlarms(getActivity(), 0, false);
                    /* Stop the ActionBar timer display*/
                    getFamiliarActivity().stopUpdatingDisplay();
                    /* Set button text to start again */
                    mTimerButton.setText(R.string.timer_start);
                    /* Cancel the notification */
                    NotificationManagerCompat.from(getActivity()).cancel(TIMER_NOTIFICATION_ID);
                } else {
                    /* Figure out the end time */
                    int hours = mTimePicker.getHours();
                    int minutes = mTimePicker.getMinutes();
                    int seconds = mTimePicker.getSeconds();

                    long timeInMillis = ((hours * 3600) + (minutes * 60) + seconds) * 1000;
                    if (timeInMillis == 0) {
                        return;
                    }
                    long endTime = System.currentTimeMillis() + timeInMillis;
                    /* Commit the end time */
                    getFamiliarActivity().mPreferenceAdapter.setRoundTimerEnd(endTime);

					/* Set the alarm, and any warning alarms if applicable */
                    setOrCancelAlarms(getActivity(), endTime, true);
                    /* Show the notification */
                    showTimerRunningNotification(getActivity(), endTime);
                    /* Start the ActionBar display Timer */
                    getFamiliarActivity().startUpdatingDisplay();
                    /* Set the button text to stop the timer */
                    mTimerButton.setText(R.string.timer_cancel);
                }
            }
        });

        if (getFamiliarActivity().mPreferenceAdapter.getRoundTimerEnd() != -1) {
            mTimerButton.setText(R.string.timer_cancel);
        }

        return v;
    }

    /**
     * Called from the handler in FamiliarActivity when the timer expires. The BroadcastReceiver doesn't have a
     * reference to this fragment
     */
    public void timerEnded() {
        mTimerButton.setText(R.string.timer_start);
    }

    /**
     * This will inflate the menu as usual. It also adds a SearchView to the ActionBar if the
     * Fragment does not override the search key, or a search button if it does override the key
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.timer_menu, menu);
    }

    /**
     * Handle a button press on the ActionBar. Either pull up settings for choosing a ringtone, or pull up settings for
     * 5/10/15 minute warnings
     *
     * @param item The MenuItem selected
     * @return true if the click was acted upon, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Handle item selection */
        switch (item.getItemId()) {
            case R.id.set_timer_ringtone:
                Uri soundFile = Uri.parse(getFamiliarActivity().mPreferenceAdapter.getTimerSound());

                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.timer_tone_dialog_title));
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, soundFile);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);

                startActivityForResult(intent, RINGTONE_REQUEST_CODE); /* This result is caught in the activity */
                return true;
            case R.id.set_timer_warnings:
                showDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Remove any showing dialogs, and show the requested one
     */
    private void showDialog() throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
		currently showing dialog, so make our own transaction and take care of that here. */

		/* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

		/* Create and show the dialog. */
        final FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {
            @NotNull
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                super.onCreateDialog(savedInstanceState);

				/* This will be set to false if we are returning a null dialog. It prevents a crash */
                setShowsDialog(true);

                switch (RoundTimerFragment.DIALOG_SET_WARNINGS) {
                    case DIALOG_SET_WARNINGS: {
                        final View v = View.inflate(this.getActivity(), R.layout.round_timer_warning_dialog, null);
                        final CheckBox chkFifteen = (CheckBox) v.findViewById(R.id.timer_pref_fifteen);
                        final CheckBox chkTen = (CheckBox) v.findViewById(R.id.timer_pref_ten);
                        final CheckBox chkFive = (CheckBox) v.findViewById(R.id.timer_pref_five);

                        boolean fifteen =
                                getFamiliarActivity().mPreferenceAdapter.getFifteenMinutePref();
                        boolean ten = getFamiliarActivity().mPreferenceAdapter.getTenMinutePref();
                        boolean five = getFamiliarActivity().mPreferenceAdapter.getFiveMinutePref();

                        chkFifteen.setChecked(fifteen);
                        chkTen.setChecked(ten);
                        chkFive.setChecked(five);

                        return new AlertDialogPro.Builder(getActivity())
                                .setView(v).setTitle(R.string.timer_warning_dialog_title)
                                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int which) {
                                        getFamiliarActivity().mPreferenceAdapter
                                                .setFifteenMinutePref(chkFifteen.isChecked());
                                        getFamiliarActivity().mPreferenceAdapter
                                                .setTenMinutePref(chkTen.isChecked());
                                        getFamiliarActivity().mPreferenceAdapter
                                                .setFiveMinutePref(chkFive.isChecked());
                                    }
                                })
                                .create();
                    }
                    default: {
                        return DontShowDialog();
                    }
                }
            }
        };
        newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
    }
}
