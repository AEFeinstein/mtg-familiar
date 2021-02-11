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

import android.app.Activity;
import android.content.Context;
import android.os.Environment;

import com.gelakinetic.mtgfam.R;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static final String BACKUP_FILE_NAME = "MTGFamiliarBackup.zip";
    private static final String VERSION_PREFIX = "mtgf_backup_version_";

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

        assert activity.getFilesDir() != null;

        String sharedPrefsDir = activity.getFilesDir().getPath();
        sharedPrefsDir = sharedPrefsDir.substring(0, sharedPrefsDir.lastIndexOf("/")) + "/shared_prefs/";

        File zipOut = new File(activity.getApplicationContext().getExternalFilesDir(null), BACKUP_FILE_NAME);
        if (zipOut.exists()) {
            if (!zipOut.delete()) {
                return;
            }
        }

        ArrayList<File> files = findAllFiles(activity.getFilesDir(),
                new File(sharedPrefsDir));

        try {
            zipIt(zipOut, files, activity);
            SnackbarWrapper.makeAndShowText(activity, activity.getString(R.string.main_export_success) + " " + zipOut.getAbsolutePath(),
                    SnackbarWrapper.LENGTH_XLONG);
        } catch (ZipException e) {
            if (Objects.requireNonNull(e.getMessage()).equals("No entries")) {
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

        /* Try unzipping the file */
        try (ZipFile zipFile = new ZipFile(new File(activity.getApplicationContext().getExternalFilesDir(null), BACKUP_FILE_NAME))) {
            unZipIt(zipFile, activity);
            SnackbarWrapper.makeAndShowText(activity, R.string.main_import_success, SnackbarWrapper.LENGTH_SHORT);
        } catch (IOException e) {
            SnackbarWrapper.makeAndShowText(activity,
                    String.format(activity.getString(R.string.main_import_fail),
                            BACKUP_FILE_NAME,
                            activity.getFilesDir().getAbsolutePath()),
                    SnackbarWrapper.LENGTH_XLONG);
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
            File[] listFiles = dir.listFiles();
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
     * Unzip a file directly into getFilesDir(). This will check the version of the saved file
     *
     * @param zipFile The zip file to unzip
     * @param context The application context, for getting files and the like
     * @throws IOException Thrown if something goes wrong with unzipping and writing
     */
    private static void unZipIt(ZipFile zipFile, Context context) throws IOException {

        char zipVersion = '1';
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().startsWith(VERSION_PREFIX)) {
                zipVersion = entry.getName().charAt(VERSION_PREFIX.length());
                break;
            }
        }

        switch (zipVersion) {
            case '1': {
                unZipItV1(zipFile, context);
                break;
            }
            case '2': {
                unZipItV2(zipFile, context);
                break;
            }
            default: {
                throw new IOException("Unknown version");
            }
        }
    }

    /**
     * Unzip a file directly into getFilesDir() from a flat zip file with no directories
     *
     * @param zipFile The zip file to unzip
     * @param context The application context, for getting files and the like
     * @throws IOException Thrown if something goes wrong with unzipping and writing
     */
    private static void unZipItV1(ZipFile zipFile, Context context) throws IOException {
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
    }

    /**
     * Unzip a file directly into getFilesDir() from a zip file with directories
     *
     * @param zipFile The zip file to unzip
     * @param context The application context, for getting files and the like
     * @throws IOException Thrown if something goes wrong with unzipping and writing
     */
    private static void unZipItV2(ZipFile zipFile, Context context) throws IOException {
        // Get the folder for all the files
        String applicationPath = context.getFilesDir().getPath();
        applicationPath = applicationPath.substring(0, applicationPath.lastIndexOf("/") + 1);

        // For each entry in the zip file
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            // Don't unzip the version token
            if (entry.getName().contains(VERSION_PREFIX)) {
                continue;
            }

            // Create any folders on this zip entry's path
            String[] path = entry.getName().split("/");
            String pathCat = "";
            if (path.length > 1) {
                for (int i = 0; i < path.length - 1; i++) {
                    pathCat += path[i] + "/";
                    File tmp = new File(applicationPath, pathCat);
                    if (!tmp.exists() && !tmp.mkdir()) {
                        throw new IOException("Couldn't mkdir");
                    }
                }
            }

            // Unzip the zip entry to the disk
            try (InputStream in = zipFile.getInputStream(entry);
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(applicationPath, entry.getName())))) {
                IOUtils.copy(in, out);
            }
        }
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
        // Open a ZipOutputStream
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // For each file to compress
            for (File file : files) {
                // Add that file to the ZipOutputStream
                ZipEntry ze = new ZipEntry(file.getCanonicalPath().split(context.getPackageName() + '/')[1]);
                zos.putNextEntry(ze);
                try (FileInputStream fis = new FileInputStream(file)) {
                    IOUtils.copy(fis, zos);
                }
            }
            // Add a marker that this is a version 2 backup
            ZipEntry zipEntry = new ZipEntry(VERSION_PREFIX + '2');
            zos.putNextEntry(zipEntry);
        }
    }
}
