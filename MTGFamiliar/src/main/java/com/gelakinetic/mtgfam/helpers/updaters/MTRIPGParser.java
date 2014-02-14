package com.gelakinetic.mtgfam.helpers.updaters;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

/**
 * This class handles updating the HTML files for the infraction procedure guide, and magic tournament rules. The files
 * are hosted at the URLs hardcoded below, and the date stamp is the first line of the file.
 */
public class MTRIPGParser {

	private static final String MTR_SOURCE =
			"https://sites.google.com/site/mtgfamiliar/rules/MagicTournamentRules.html";
	private static final String IPG_SOURCE =
			"https://sites.google.com/site/mtgfamiliar/rules/InfractionProcedureGuide.html";

	public static final String MTR_LOCAL_FILE = "MTR.html";
	public static final String IPG_LOCAL_FILE = "IPG.html";

	public enum MTR_IPG_MODE {MODE_IPG, MODE_MTR}

	private Date mLastUpdated;
	private Context mContext;

	/**
	 * Default constructor
	 *
	 * @param lastUpdated The last time the file was updated, used to check whether it should be updated
	 * @param context     This context is used to get file handles to write the HTML files later
	 */
	public MTRIPGParser(Date lastUpdated, Context context) {
		this.mLastUpdated = lastUpdated;
		this.mContext = context;
	}

	/**
	 * This method gets a new document from the web, compares it's date stamp to the one given in the constructor, and
	 * writes it to the device if it is newer
	 *
	 * @param mode Whether we are updating the IPG or MTR
	 * @return True if the document was updated, false otherwise
	 */
	public boolean performMtrIpgUpdateIfNeeded(final MTR_IPG_MODE mode) {
		boolean updated = false;
		InputStream is = null;
		BufferedReader reader = null;
		FileOutputStream fos = null;

		try {
			URL url;
			switch (mode) {
				case MODE_IPG:
					url = new URL(IPG_SOURCE);
					break;
				case MODE_MTR:
					url = new URL(MTR_SOURCE);
					break;
				default:
					throw new FileNotFoundException("Invalid switch"); /* handled below */
			}

			is = url.openStream();
			reader = new BufferedReader(new InputStreamReader(is));

			String line = reader.readLine();
			String[] parts = line.split("-");
			Calendar c = Calendar.getInstance();
			c.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));

			if (c.getTime().after(this.mLastUpdated)) {
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
					default:
						throw new FileNotFoundException("Invalid switch"); /* handled below */
				}
				fos = new FileOutputStream(output);
				fos.write(sb.toString().getBytes());
				fos.flush();
				updated = true;
			}
		} catch (MalformedURLException e) {
			/* eat it */
		} catch (FileNotFoundException e) {
			/* eat it */
		} catch (IOException e) {
			/* eat it */
		} finally {
			try {
				if (is != null) {
					is.close();
				}
				if (reader != null) {
					reader.close();
				}
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				/* eat it */
			}
		}
		return updated;
	}
}
