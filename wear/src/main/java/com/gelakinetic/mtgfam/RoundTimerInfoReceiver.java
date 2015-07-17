package com.gelakinetic.mtgfam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RoundTimerInfoReceiver extends BroadcastReceiver {

    private final FamiliarDisplayActivity mActivity;

    /**
     * Necessary default constructor
     */
    public RoundTimerInfoReceiver() {
        super();
        mActivity = null;
    }
    /**
     * TODO
     * @param activity asd
     */
    public RoundTimerInfoReceiver(FamiliarDisplayActivity activity) {
        super();
        mActivity = activity;
    }

    /**
     * TODO
     *
     * @param arg0 asd
     * @param arg1 asd
     */
    @Override
    public void onReceive(Context arg0, Intent arg1) {
        if(mActivity == null) {
            return;
        }
        Log.v("MTG", "received something");
        if(arg1.hasExtra(FamiliarDisplayActivity.EXTRA_END_TIME)) {
            mActivity.mEndTime = arg1.getLongExtra(FamiliarDisplayActivity.EXTRA_END_TIME, 0);
            if(mActivity.mEndTime == 0) {
                Log.v("MTG", "Received kill signal");
                mActivity.mStopHandler = true;
            }
        }
        if(arg1.hasExtra(FamiliarDisplayActivity.EXTRA_FIVE_MINUTE_WARNING)) {
            mActivity.mFiveMinuteWarning = arg1.getBooleanExtra(FamiliarDisplayActivity.EXTRA_FIVE_MINUTE_WARNING, false);
            mActivity.mTenMinuteWarning = arg1.getBooleanExtra(FamiliarDisplayActivity.EXTRA_TEN_MINUTE_WARNING, false);
            mActivity.mFifteenMinuteWarning = arg1.getBooleanExtra(FamiliarDisplayActivity.EXTRA_FIFTEEN_MINUTE_WARNING, false);
            Log.v("MTG", "onReceive" + mActivity.mFiveMinuteWarning + " " + mActivity.mTenMinuteWarning + " " + mActivity.mFifteenMinuteWarning);
        }
    }
}