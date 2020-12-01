package com.gelakinetic.mtgfam.helpers;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;

import androidx.core.app.ShareCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

public class FamiliarLogger {

    private static final String DB_LOG_FILE_NAME = "mtgf_sqlite_log.txt";
    private static boolean mLoggingEnabled = false;
    private static String mExternalFileDirPath = null;

    /**
     * TODO
     *
     * @param activity
     */
    public static void initLogger(FamiliarActivity activity) {
        mExternalFileDirPath = activity.getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
        mLoggingEnabled = PreferenceAdapter.getLoggingPref(activity);
    }

    /**
     * TODO
     *
     * @param sb
     * @param methodName
     */
    public static void appendToLogFile(StringBuilder sb, String methodName) {
        /* Try to open up a log */
        if (mLoggingEnabled && null != mExternalFileDirPath &&
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            /* If the log doesn't exist, start if off with system info */
            File logFile = new File(mExternalFileDirPath, DB_LOG_FILE_NAME);
            if (!logFile.exists()) {
                try {
                    FileUtils.touch(logFile);
                    logDeviceInfo();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            /* Open the log, append to it */
            try (FileWriter logWriter = new FileWriter(logFile, true)) {
                /* Datestamp it */
                logWriter.write("Date : " + (new Date()).toString() + '\n');
                logWriter.write("From : " + methodName + '\n');
                logWriter.write(sb.toString() + "\n\n");
            } catch (IOException e) {
                /* Couldn't open log, oh well */
            }
        }
    }

    /**
     * TODO
     */
    private static void logDeviceInfo() {
        // Log SDK, manufacturer, and model
        appendToLogFile(new StringBuilder("Android SDK: ").append(Build.VERSION.SDK_INT)
                .append(", Manufacturer: ").append(Build.MANUFACTURER)
                .append(", Model: ").append(Build.MODEL), "SYS Info");

        // Log CPU info
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ProcessBuilder("/system/bin/cat", "/proc/cpuinfo").start().getInputStream()))) {
            String line;
            StringBuilder sb = new StringBuilder("/proc/cpuinfo\n");
            while (null != (line = br.readLine())) {
                sb.append(line).append('\n');
            }
            appendToLogFile(sb, "CPU Info");
        } catch (IOException e) {
            // Eh
        }

        // Log memory info
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ProcessBuilder("/system/bin/cat", "/proc/meminfo").start().getInputStream()))) {
            String line;
            StringBuilder sb = new StringBuilder("/proc/meminfo\n");
            while (null != (line = br.readLine())) {
                sb.append(line).append('\n');
            }
            appendToLogFile(sb, "MEM Info");
        } catch (IOException e) {
            // Eh
        }
    }

    /**
     * TODO
     *
     * @param sql
     * @param args
     * @param methodName
     */
    public static void logRawQuery(String sql, String[] args, String methodName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Query: ").append(sql).append('\n');
        if (null != args) {
            for (String arg : args) {
                sb.append("Arg  :").append(arg).append('\n');
            }
        }
        appendToLogFile(sb, methodName);
    }

    /**
     * TODO
     *
     * @param distinct
     * @param table
     * @param columns
     * @param selection
     * @param selectionArgs
     * @param groupBy
     * @param having
     * @param orderBy
     * @param limit
     * @param methodName
     */
    public static void logQuery(boolean distinct, String table, String[] columns, String selection,
                                String[] selectionArgs, String groupBy, String having,
                                String orderBy, String limit, String methodName) {
        StringBuilder sb = new StringBuilder();
        sb.append("distinct: ").append(distinct).append('\n');
        sb.append("table   : ").append(table).append('\n');
        if (null != columns) {
            for (String arg : columns) {
                sb.append("columns :").append(arg).append('\n');
            }
        }
        sb.append("selection: ").append(selection).append('\n');
        if (null != selectionArgs) {
            for (String arg : selectionArgs) {
                sb.append("selArgs :").append(arg).append('\n');
            }
        }
        sb.append("groupBy : ").append(groupBy).append('\n');
        sb.append("having  : ").append(having).append('\n');
        sb.append("orderBy : ").append(orderBy).append('\n');
        sb.append("limit   : ").append(limit).append('\n');
        appendToLogFile(sb, methodName);
    }

    /**
     * TODO
     *
     * @param projectionIn
     * @param selection
     * @param selectionArgs
     * @param groupBy
     * @param having
     * @param sortOrder
     * @param methodName
     */
    public static void logBuiltQuery(String[] projectionIn, String selection, String[] selectionArgs,
                                     String groupBy, String having, String sortOrder, String methodName) {
        StringBuilder sb = new StringBuilder();
        if (null != projectionIn) {
            for (String arg : projectionIn) {
                sb.append("projIn :").append(arg).append('\n');
            }
        }
        sb.append("select : ").append(selection).append('\n');
        if (null != selectionArgs) {
            for (String arg : selectionArgs) {
                sb.append("selArgs:").append(arg).append('\n');
            }
        }
        sb.append("groupBy: ").append(groupBy).append('\n');
        sb.append("having : ").append(having).append('\n');
        sb.append("sortOrd: ").append(sortOrder).append('\n');
        appendToLogFile(sb, methodName);
    }

    /**
     * TODO
     *
     * @param activity
     */
    private static void shareLog(FamiliarActivity activity) {
        try (BufferedReader br = new BufferedReader(new FileReader(
                new File(activity.getExternalFilesDir(null), DB_LOG_FILE_NAME)))) {
            // Read the entire log file to a String
            StringBuilder sb = new StringBuilder();
            String line;
            while (null != (line = br.readLine())) {
                sb.append(line).append('\n');
            }

            // Send an email intent with the contents of the log
            ShareCompat.IntentBuilder.from(activity)
                    .setType("message/rfc822")
                    .addEmailTo("mtg.familiar@gmail.com")
                    .setSubject(activity.getString(R.string.logging_database_log))
                    .setText(sb.toString())
                    .setChooserTitle(activity.getString(R.string.logging_send_database_log))
                    .startChooser();
        } catch (IOException e) {
            // Eh
        }
    }

    /**
     * TODO
     *
     * @param familiarActivity
     */
    private static void exportDatabase(FamiliarActivity familiarActivity) {
        FamiliarDbHandle handle = new FamiliarDbHandle();
        try {
            SQLiteDatabase database = DatabaseManager.openDatabase(familiarActivity, false, handle);
            File srcFile = new File(database.getPath());
            DatabaseManager.closeDatabase(familiarActivity, handle);
            File dstFile = new File(mExternalFileDirPath, "mtgfam.sqlite");
            FileUtils.copyFile(srcFile, dstFile);
            SnackbarWrapper.makeAndShowText(familiarActivity, familiarActivity.getString(R.string.logging_database_copied_to) + dstFile,
                    SnackbarWrapper.LENGTH_XLONG);
        } catch (SQLiteException | IOException | FamiliarDbException e) {
            // Eh
        } finally {
            DatabaseManager.closeDatabase(familiarActivity, handle);
        }
    }

    /**
     * TODO
     *
     * @param familiarActivity
     * @param builder
     * @return
     */
    public static Dialog createDialog(FamiliarActivity familiarActivity, MaterialDialog.Builder builder) {
        builder.title(R.string.logging_title);
        builder.positiveText(R.string.dialog_ok);

        /* Set the custom view, with some images below the text */
        LayoutInflater inflater = familiarActivity.getLayoutInflater();
        @SuppressLint("InflateParams") View dialogLayout = inflater.inflate(R.layout.activity_dialog_logging, null, false);
        assert dialogLayout != null;
        ((Switch) dialogLayout.findViewById(R.id.logging_switch)).setChecked(PreferenceAdapter.getLoggingPref(familiarActivity));
        ((Switch) dialogLayout.findViewById(R.id.logging_switch)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            /* delete the log if logging is being disabled */
            if (!isChecked) {
                //noinspection ResultOfMethodCallIgnored
                new File(mExternalFileDirPath, DB_LOG_FILE_NAME).delete();
            }
            /* Save the preference */
            PreferenceAdapter.setLoggingPref(familiarActivity, isChecked);
            mLoggingEnabled = isChecked;
        });
        dialogLayout.findViewById(R.id.export_db).setOnClickListener(v -> {
            familiarActivity.removeDialogFragment(familiarActivity.getSupportFragmentManager());
            FamiliarLogger.exportDatabase(familiarActivity);
        });
        dialogLayout.findViewById(R.id.share_log).setOnClickListener(v -> {
            familiarActivity.removeDialogFragment(familiarActivity.getSupportFragmentManager());
            FamiliarLogger.shareLog(familiarActivity);
        });
        builder.customView(dialogLayout, false);
        return builder.build();
    }
}
