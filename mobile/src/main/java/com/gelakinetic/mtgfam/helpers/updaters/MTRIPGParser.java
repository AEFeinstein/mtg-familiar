package com.gelakinetic.mtgfam.helpers.updaters;

import android.content.Context;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Calendar;

/**
 * This class handles updating the HTML files for the infraction procedure guide, and magic tournament rules. The files
 * are hosted at the URLs hardcoded below, and the date stamp is the first line of the file.
 */
class MTRIPGParser {

    public static final int MODE_IPG = 0;
    public static final int MODE_MTR = 1;
    public static final int MODE_JAR = 2;
    private static final String MTR_SOURCE =
            "https://raw.githubusercontent.com/AEFeinstein/GathererScraper/master/rules/MagicTournamentRules-light.html";
    private static final String IPG_SOURCE =
            "https://raw.githubusercontent.com/AEFeinstein/GathererScraper/master/rules/InfractionProcedureGuide-light.html";
    private static final String JAR_SOURCE =
            "https://raw.githubusercontent.com/AEFeinstein/GathererScraper/master/rules/JudgingAtRegular-light.html";
    private static final String MTR_LOCAL_FILE = "MTR.html";
    private static final String IPG_LOCAL_FILE = "IPG.html";
    private static final String JAR_LOCAL_FILE = "JAR.html";
    private final Context mContext;
    String mPrettyDate;

    /**
     * Default constructor
     *
     * @param context     This context is used to get file handles to write the HTML files later
     */
    public MTRIPGParser(Context context) {
        this.mContext = context;
    }

    /**
     * This method gets a new document from the web, compares it's date stamp to the one given in the constructor, and
     * writes it to the device if it is newer
     *
     * @param mode Whether we are updating the IPG or MTR
     * @return True if the document was updated, false otherwise
     */
    public boolean performMtrIpgUpdateIfNeeded(final int mode, PrintWriter logWriter) {
        boolean updated = false;

        /* First, inflate local files if they do not exist */
        File output = null;
        switch (mode) {
            case MODE_IPG:
                output = new File(mContext.getFilesDir(), IPG_LOCAL_FILE);
                break;
            case MODE_MTR:
                output = new File(mContext.getFilesDir(), MTR_LOCAL_FILE);
                break;
            case MODE_JAR:
                output = new File(mContext.getFilesDir(), JAR_LOCAL_FILE);
        }
        try {
            if (output != null && !output.exists()) {
                switch (mode) {
                    case MODE_IPG:
                        parseDocument(mode, mContext.getResources().openRawResource(R.raw.ipg));
                        break;
                    case MODE_MTR:
                        parseDocument(mode, mContext.getResources().openRawResource(R.raw.mtr));
                        break;
                    case MODE_JAR:
                        parseDocument(mode, mContext.getResources().openRawResource(R.raw.jar));
                        break;
                }
            }
        } catch (IOException e) {
            if (logWriter != null) {
                e.printStackTrace(logWriter);
            }
        }

        /* Then update via the internet */
        try {
            String urlString;
            switch (mode) {
                case MODE_IPG:
                    urlString = IPG_SOURCE;
                    break;
                case MODE_MTR:
                    urlString = MTR_SOURCE;
                    break;
                case MODE_JAR:
                    urlString = JAR_SOURCE;
                    break;
                default:
                    throw new FileNotFoundException("Invalid switch"); /* handled below */
            }
            InputStream stream = FamiliarActivity.getHttpInputStream(urlString, logWriter);
            if (stream != null) {
                updated = parseDocument(mode, stream);
            }
        } catch (IOException e) {
            if (logWriter != null) {
                e.printStackTrace(logWriter);
            }
        }

        return updated;
    }

    private boolean parseDocument(int mode, InputStream is) throws IOException {
        boolean updated = false;
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line = reader.readLine();
        String[] parts = line.split("-");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        long documentDate = c.getTimeInMillis();

        mPrettyDate = DateFormat.getDateInstance().format(c.getTime());

        boolean shouldUpdate;
        switch (mode) {
            case MODE_IPG: {
                shouldUpdate = documentDate != PreferenceAdapter.getLastIPGUpdate(mContext);
                break;
            }
            case MODE_MTR: {
                shouldUpdate = documentDate != PreferenceAdapter.getLastMTRUpdate(mContext);
                break;
            }
            case MODE_JAR: {
                shouldUpdate = documentDate != PreferenceAdapter.getLastJARUpdate(mContext);
                break;
            }
            default: {
                shouldUpdate = false;
            }
        }

        if (shouldUpdate) {
            StringBuilder sb = new StringBuilder();
            line = reader.readLine();
            while (line != null) {
                sb.append(line.trim());
                line = reader.readLine();
            }

            File output;
            switch (mode) {
                case MODE_IPG:
                    output = new File(mContext.getFilesDir(), IPG_LOCAL_FILE);
                    break;
                case MODE_MTR:
                    output = new File(mContext.getFilesDir(), MTR_LOCAL_FILE);
                    break;
                case MODE_JAR:
                    output = new File(mContext.getFilesDir(), JAR_LOCAL_FILE);
                    break;
                default:
                    throw new FileNotFoundException("Invalid switch"); /* handled below */
            }
            FileOutputStream fos = new FileOutputStream(output);
            fos.write(sb.toString().getBytes());
            fos.flush();
            fos.close();
            updated = true;

            switch (mode) {
                case MODE_IPG:
                    PreferenceAdapter.setLastIPGUpdate(mContext, documentDate);
                    break;
                case MODE_MTR:
                    PreferenceAdapter.setLastMTRUpdate(mContext, documentDate);
                    break;
                case MODE_JAR:
                    PreferenceAdapter.setLastJARUpdate(mContext, documentDate);
                    break;
                default:
                    throw new FileNotFoundException("Invalid switch"); /* handled below */
            }
        }

        is.close();
        reader.close();

        return updated;
    }
}
