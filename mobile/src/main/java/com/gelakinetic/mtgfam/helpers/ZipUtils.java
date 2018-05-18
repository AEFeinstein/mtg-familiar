/*
 * Copyright 2017 Adam Feinstein
 *
 * This file is part of MTG Familiar.
 *
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static final String BACKUP_FILE_NAME = "MTGFamiliarBackup.zip";

    /**
     * This method exports any data in this application's getFilesDir() into a zip file on external storage
     *
     * @param activity The application activity, for getting files and the like
     */
    public static void exportData(Activity activity) {

        /* Make sure external storage exists */
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            SnackbarWrapper.makeAndShowText(activity, R.string.card_view_no_external_storage, SnackbarWrapper.LENGTH_LONG);
            return;
        }

        /* Check if permission is granted */
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            /* Request the permission */
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    FamiliarActivity.REQUEST_WRITE_EXTERNAL_STORAGE_BACKUP);
            return;
        }

        assert activity.getFilesDir() != null;

        String sharedPrefsDir = activity.getFilesDir().getPath();
        sharedPrefsDir = sharedPrefsDir.substring(0, sharedPrefsDir.lastIndexOf("/")) + "/shared_prefs/";

        ArrayList<File> files = findAllFiles(activity.getFilesDir(),
                new File(sharedPrefsDir));

        File sdCard = Environment.getExternalStorageDirectory();
        File zipOut = new File(sdCard, BACKUP_FILE_NAME);
        if (zipOut.exists()) {
            if (!zipOut.delete()) {
                return;
            }
        }
        try {
            zipIt(zipOut, files, activity);
            SnackbarWrapper.makeAndShowText(activity, activity.getString(R.string.main_export_success) + " " + zipOut.getAbsolutePath(),
                    SnackbarWrapper.LENGTH_SHORT);
        } catch (ZipException e) {
            if (e.getMessage().equals("No entries")) {
                SnackbarWrapper.makeAndShowText(activity, R.string.main_export_no_data, SnackbarWrapper.LENGTH_SHORT);
            } else {
                SnackbarWrapper.makeAndShowText(activity, R.string.main_export_fail, SnackbarWrapper.LENGTH_SHORT);
            }
        } catch (IOException e) {
            SnackbarWrapper.makeAndShowText(activity, R.string.main_export_fail, SnackbarWrapper.LENGTH_SHORT);
        }
    }

    /**
     * This method imports all data in a zip file on external storage into this application's getFilesDir()
     *
     * @param activity The application activity, for getting files and the like
     */
    public static void importData(Activity activity) {

        /* Make sure external storage exists */
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            SnackbarWrapper.makeAndShowText(activity, R.string.card_view_no_external_storage, SnackbarWrapper.LENGTH_LONG);
            return;
        }

        /* Only check permissions after Jelly Bean */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            /* Check if permission is granted */
            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                /* Request the permission */
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        FamiliarActivity.REQUEST_READ_EXTERNAL_STORAGE_BACKUP);
                return;
            }
        }

        File sdCard = Environment.getExternalStorageDirectory();
        File zipIn = new File(sdCard, BACKUP_FILE_NAME);
        try {
            unZipIt(new ZipFile(zipIn), activity);
            SnackbarWrapper.makeAndShowText(activity, R.string.main_import_success, SnackbarWrapper.LENGTH_SHORT);
        } catch (IOException e) {
            SnackbarWrapper.makeAndShowText(activity, R.string.main_import_fail, SnackbarWrapper.LENGTH_SHORT);
        }
    }

    /**
     * Recursively crawl through a directory and build a list of all files
     *
     * @param dirs The root directories
     * @return An ArrayList of all the files in the root directory
     */
    private static ArrayList<File> findAllFiles(File... dirs) {
        ArrayList<File> files = new ArrayList<>();
        for (File dir : dirs) {
            File listFiles[] = dir.listFiles();
            assert listFiles != null;
            for (File file : listFiles) {
                if (file.isDirectory()) {
                    files.addAll(findAllFiles(file));
                } else {
                    files.add(file);
                }
            }
        }
        return files;
    }

    /**
     * Unzip a file directly into getFilesDir()
     *
     * @param zipFile The zip file to unzip
     * @param context The application context, for getting files and the like
     * @throws IOException Thrown if something goes wrong with unzipping and writing
     */
    private static void unZipIt(ZipFile zipFile, Context context) throws IOException {
        Enumeration<? extends ZipEntry> entries;

        entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (entry.isDirectory()) {
                /* Assume directories are stored parents first then children.
                 * This is not robust, just for demonstration purposes. */
                if (!(new File(entry.getName())).mkdir()) {
                    return;
                }
                continue;
            }
            String[] path = entry.getName().split("/");
            String pathCat = "";
            if (path.length > 1) {
                for (int i = 0; i < path.length - 1; i++) {
                    pathCat += path[i] + "/";
                    File tmp = new File(context.getFilesDir(), pathCat);
                    if (!tmp.exists()) {
                        if (!tmp.mkdir()) {
                            return;
                        }
                    }
                }
            }

            InputStream in = zipFile.getInputStream(entry);
            OutputStream out;
            if (entry.getName().contains("_preferences.xml")) {
                String sharedPrefsDir = context.getFilesDir().getPath();
                sharedPrefsDir = sharedPrefsDir.substring(0, sharedPrefsDir.lastIndexOf("/")) + "/shared_prefs/";

                out = new BufferedOutputStream(new FileOutputStream(
                        new File(sharedPrefsDir, entry.getName())));
            } else {
                out = new BufferedOutputStream(new FileOutputStream(
                        new File(context.getFilesDir(), entry.getName())));
            }
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) >= 0) {
                out.write(buffer, 0, len);
            }

            in.close();
            out.close();
        }

        zipFile.close();
    }

    /**
     * Zips all files specified in an ArrayList into a given file
     *
     * @param zipFile The file to zip files into
     * @param files   The files to be zipped
     * @param context The application context, for getting files and the like
     * @throws IOException IOException Thrown if something goes wrong with zipping and reading
     */
    private static void zipIt(File zipFile, ArrayList<File> files, Context context) throws IOException {

        byte[] buffer = new byte[1024];

        FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos);
        assert context.getFilesDir() != null;
        for (File file : files) {
            ZipEntry ze = new ZipEntry(file.getName());
            zos.putNextEntry(ze);

            FileInputStream in = new FileInputStream(file);

            int len;
            while ((len = in.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            in.close();
        }

        zos.closeEntry();
        zos.close();
    }

}
