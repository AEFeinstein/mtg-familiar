/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.v4.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

class PreferenceManagerCompat {

    private static final String TAG = PreferenceManagerCompat.class.getSimpleName();

    static PreferenceManager newInstance(Activity activity) {
        try {
            Constructor<PreferenceManager> c = PreferenceManager.class.getDeclaredConstructor(Activity.class, int.class);
            c.setAccessible(true);
            return c.newInstance(activity, PreferenceFragment.FIRST_REQUEST_CODE);
        } catch (Exception e) {
            Log.w(TAG, "Couldn't call constructor PreferenceManager by reflection", e);
        }
        return null;
    }

    /**
     * Sets the callback to be invoked when a {@link Preference} in the
     * hierarchy rooted at this {@link PreferenceManager} is clicked.
     *
     * @param listener The callback to be invoked.
     */
    static void setOnPreferenceTreeClickListener(PreferenceManager manager, final OnPreferenceTreeClickListener listener) {
        try {
            Field onPreferenceTreeClickListener = PreferenceManager.class.getDeclaredField("mOnPreferenceTreeClickListener");
            onPreferenceTreeClickListener.setAccessible(true);
            if (listener != null) {
                Object proxy = Proxy.newProxyInstance(
                        onPreferenceTreeClickListener.getType().getClassLoader(),
                        new Class[]{onPreferenceTreeClickListener.getType()},
                        new InvocationHandler() {
                            public Object invoke(Object proxy, Method method, Object[] args) {
                                if (method.getName().equals("onPreferenceTreeClick")) {
                                    return listener.onPreferenceTreeClick((Preference) args[1]);
                                } else {
                                    return null;
                                }
                            }
                        }
                );
                onPreferenceTreeClickListener.set(manager, proxy);
            } else {
                onPreferenceTreeClickListener.set(manager, null);
            }
        } catch (Exception e) {
            Log.w(TAG, "Couldn't set PreferenceManager.mOnPreferenceTreeClickListener by reflection", e);
        }
    }

    /**
     * Calls PreferenceManager.inflateFromResource by reflection
     * Inflates a preference hierarchy from XML. If a preference hierarchy is
     * given, the new preference hierarchies will be merged in.
     *
     * @param manager  The PreferenceManager to reflect
     * @param activity The context of the resource.
     * @param screen   Optional existing hierarchy to merge the new hierarchies into.
     * @return The root hierarchy (if one was not provided, the new hierarchy's root).
     */
    static PreferenceScreen inflateFromResource(PreferenceManager manager, Activity activity, PreferenceScreen screen) {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("inflateFromResource", Context.class, int.class,
                    PreferenceScreen.class);
            m.setAccessible(true);
            return (PreferenceScreen) m.invoke(manager, activity, com.gelakinetic.mtgfam.R.xml.preferences, screen);
        } catch (Exception e) {
            Log.w(TAG, "Couldn't call PreferenceManager.inflateFromResource by reflection", e);
        }
        return null;
    }

    /**
     * Returns the root of the preference hierarchy managed by this class.
     *
     * @return The {@link PreferenceScreen} object that is at the root of the hierarchy.
     */
    static PreferenceScreen getPreferenceScreen(PreferenceManager manager) {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("getPreferenceScreen");
            m.setAccessible(true);
            return (PreferenceScreen) m.invoke(manager);
        } catch (Exception e) {
            Log.w(TAG, "Couldn't call PreferenceManager.getPreferenceScreen by reflection", e);
        }
        return null;
    }

    /**
     * Called by the {@link PreferenceManager} to dispatch a sub-activity result.
     */
    static void dispatchActivityResult(PreferenceManager manager, int requestCode, int resultCode, Intent data) {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("dispatchActivityResult", int.class, int.class, Intent.class);
            m.setAccessible(true);
            m.invoke(manager, requestCode, resultCode, data);
        } catch (Exception e) {
            Log.w(TAG, "Couldn't call PreferenceManager.dispatchActivityResult by reflection", e);
        }
    }

    /**
     * Called by the {@link PreferenceManager} to dispatch the activity stop
     * event.
     */
    static void dispatchActivityStop(PreferenceManager manager) {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("dispatchActivityStop");
            m.setAccessible(true);
            m.invoke(manager);
        } catch (Exception e) {
            Log.w(TAG, "Couldn't call PreferenceManager.dispatchActivityStop by reflection", e);
        }
    }

    /**
     * Called by the {@link PreferenceManager} to dispatch the activity destroy
     * event.
     */
    static void dispatchActivityDestroy(PreferenceManager manager) {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("dispatchActivityDestroy");
            m.setAccessible(true);
            m.invoke(manager);
        } catch (Exception e) {
            Log.w(TAG, "Couldn't call PreferenceManager.dispatchActivityDestroy by reflection", e);
        }
    }

    /**
     * Calls PreferenceManager.setPreferences by reflection
     * Sets the root of the preference hierarchy.
     *
     * @param manager the PreferenceManager to reflect
     * @param screen  The root {@link PreferenceScreen} of the preference hierarchy.
     * @return Whether the {@link PreferenceScreen} given is different than the previous.
     */
    static boolean setPreferences(PreferenceManager manager, PreferenceScreen screen) {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("setPreferences", PreferenceScreen.class);
            m.setAccessible(true);
            return ((Boolean) m.invoke(manager, screen));
        } catch (Exception e) {
            Log.w(TAG, "Couldn't call PreferenceManager.setPreferences by reflection", e);
        }
        return false;
    }

    /**
     * Interface definition for a callback to be invoked when a
     * {@link Preference} in the hierarchy rooted at this {@link PreferenceScreen} is
     * clicked.
     */
    interface OnPreferenceTreeClickListener {
        /**
         * Called when a preference in the tree rooted at this
         * {@link PreferenceScreen} has been clicked.
         *
         * @param preference The preference that was clicked.
         * @return Whether the click was handled.
         */
        boolean onPreferenceTreeClick(Preference preference);
    }

}
