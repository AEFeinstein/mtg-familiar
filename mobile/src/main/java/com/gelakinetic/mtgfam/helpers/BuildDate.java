/**
 * Copyright 2012 Michael Shick
 * <p/>
 * This file is part of MTG Familiar.
 * <p/>
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A class to extract the date the APK was built
 */
class BuildDate {
    /**
     * http://stackoverflow.com/questions/7607165/how-to-write-build-time-stamp-into-apk
     *
     * @param context the application context
     * @return a Date object with the time the APK was built
     */
    public static Date get(Context context) {
        try {
            assert context.getPackageManager() != null;
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            ZipFile zf = new ZipFile(ai.sourceDir);
            ZipEntry ze = zf.getEntry("classes.dex");
            long time = ze.getTime();
            return new Date(time);
        } catch (PackageManager.NameNotFoundException e) {
            return new GregorianCalendar(1990, 2, 13).getTime();
        } catch (IOException e) {
            return new GregorianCalendar(1990, 2, 13).getTime();
        }
    }
}