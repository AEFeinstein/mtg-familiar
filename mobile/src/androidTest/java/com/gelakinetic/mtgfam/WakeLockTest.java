package com.gelakinetic.mtgfam;

import android.content.Context;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import androidx.fragment.app.Fragment;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.gelakinetic.mtgfam.fragments.DiceFragment;
import com.gelakinetic.mtgfam.fragments.LifeCounterFragment;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests to verify that FLAG_KEEP_SCREEN_ON is retained when removing dialog fragments
 * in LifeCounterFragment mode (if preference is enabled), and cleared otherwise (Issue #660).
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class WakeLockTest {

    @Rule
    public ActivityTestRule<FamiliarActivity> mFamiliarActivityRule =
            new ActivityTestRule<>(FamiliarActivity.class);

    /**
     * Test Case 1: LifeCounterFragment + Preference Enabled -> Wake Lock MUST be retained.
     */
    @Test
    @UiThreadTest
    public void testWakeLockRetainedInLifeCounterWhenEnabled() {
        FamiliarActivity activity = mFamiliarActivityRule.getActivity();

        // 1. Explicitly enable Wake Lock preference
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.key_wakelock), true)
                .commit();

        assertTrue("Wake lock preference should be enabled", PreferenceAdapter.getKeepScreenOn(activity));

        // 2. Load LifeCounterFragment & activate
        LifeCounterFragment fragment = new LifeCounterFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, FamiliarActivity.FRAGMENT_TAG)
                .commitNow();
        fragment.onResume();

        // 3. Remove dialog & verify wake lock is retained
        activity.removeDialogFragment(activity.getSupportFragmentManager());

        boolean hasWakeLock = (activity.getWindow().getAttributes().flags &
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0;
        assertTrue("Activity MUST retain FLAG_KEEP_SCREEN_ON in Life Counter when preference is enabled", hasWakeLock);

        // 4. Assert OS PowerManager state: Device screen must remain interactive (awake)
        PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        assertNotNull("PowerManager service should be available", pm);
        assertTrue("Device display screen MUST remain interactive (awake) when Wake Lock is active", pm.isInteractive());
    }

    /**
     * Test Case 2: LifeCounterFragment + Preference Disabled -> Wake Lock MUST be cleared.
     */
    @Test
    @UiThreadTest
    public void testWakeLockClearedInLifeCounterWhenDisabled() {
        FamiliarActivity activity = mFamiliarActivityRule.getActivity();

        // 1. Explicitly disable Wake Lock preference
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.key_wakelock), false)
                .commit();

        // 2. Load LifeCounterFragment & activate
        LifeCounterFragment fragment = new LifeCounterFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, FamiliarActivity.FRAGMENT_TAG)
                .commitNow();
        fragment.onResume();

        // 3. Set temporary FLAG_KEEP_SCREEN_ON
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 4. Remove dialog & verify wake lock is cleared
        activity.removeDialogFragment(activity.getSupportFragmentManager());

        boolean hasWakeLock = (activity.getWindow().getAttributes().flags &
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0;
        assertFalse("Activity MUST clear FLAG_KEEP_SCREEN_ON in Life Counter when preference is disabled", hasWakeLock);
    }

    /**
     * Test Case 3: Non-LifeCounter fragment + Preference Enabled -> Wake Lock MUST be cleared (original behavior preserved).
     */
    @Test
    @UiThreadTest
    public void testWakeLockClearedWhenNotLifeCounter() {
        FamiliarActivity activity = mFamiliarActivityRule.getActivity();

        // 1. Explicitly enable Wake Lock preference
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.key_wakelock), true)
                .commit();

        // 2. Load non-LifeCounter fragment (DiceFragment) & activate
        Fragment fragment = new DiceFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, FamiliarActivity.FRAGMENT_TAG)
                .commitNow();
        fragment.onResume();

        // 3. Set temporary FLAG_KEEP_SCREEN_ON (simulating dialog show)
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 4. Remove dialog & verify wake lock is cleared
        activity.removeDialogFragment(activity.getSupportFragmentManager());

        boolean hasWakeLock = (activity.getWindow().getAttributes().flags &
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0;
        assertFalse("Activity MUST clear FLAG_KEEP_SCREEN_ON when not in Life Counter screen", hasWakeLock);
    }
}
