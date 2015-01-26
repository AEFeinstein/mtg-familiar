package com.gelakinetic.mtgfam.helpers;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

/**
 * Created by XVicarious on 1/26/2015.
 */
public class BackupHelper extends BackupAgentHelper {
    // The name of the preferences file (and then the wishlist)
    static final String PREFS = "com.gelakinetic.mtgfam_preferences.xml";
    static final String WISHLIST = "card.wishlist";  // add other files here such as IPG.html, MTR.html if you choose

    // A key for the preferences (and then the wishlist, and other files)
    static final String PREFS_BACKUP_KEY = "prefs";
    static final String WISHLIST_BACKUP_KEY = "files";

    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper backupPrefs = new SharedPreferencesBackupHelper(this, PREFS);
        SharedPreferencesBackupHelper backupWishlist = new SharedPreferencesBackupHelper(this, WISHLIST); // Add additional arguments as filenames to backup to this
        addHelper(PREFS_BACKUP_KEY, backupPrefs);
        addHelper(WISHLIST_BACKUP_KEY, backupWishlist);
    }
}
