package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

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
	 * @param context The application context, for getting files and the like
	 */
	public static void exportData(Context context) {
		assert context.getFilesDir() != null;
		ArrayList<File> files = findAllFiles(context.getFilesDir());

		File sdCard = Environment.getExternalStorageDirectory();
		File zipOut = new File(sdCard, BACKUP_FILE_NAME);
		if (zipOut.exists()) {
			if (!zipOut.delete()) {
				return;
			}
		}
		try {
			zipIt(zipOut, files, context);
			Toast.makeText(context, context.getString(R.string.main_export_success) + " " + zipOut.getAbsolutePath(),
					Toast.LENGTH_SHORT).show();
		} catch (ZipException e) {
			if (e.getMessage().equals("No entries")) {
				Toast.makeText(context, context.getString(R.string.main_export_no_data), Toast.LENGTH_SHORT).show();
			}
			else {
				Toast.makeText(context, context.getString(R.string.main_export_fail), Toast.LENGTH_SHORT).show();
			}
		}
		catch (IOException e) {
			Toast.makeText(context, context.getString(R.string.main_export_fail), Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * This method imports all data in a zip file on external storage into this application's getFilesDir()
	 *
	 * @param context The application context, for getting files and the like
	 */
	public static void importData(Context context) {
		File sdCard = Environment.getExternalStorageDirectory();
		File zipIn = new File(sdCard, BACKUP_FILE_NAME);
		try {
			unZipIt(new ZipFile(zipIn), context);
			Toast.makeText(context, context.getString(R.string.main_import_success), Toast.LENGTH_SHORT).show();
		} catch (ZipException e) {
			Toast.makeText(context, context.getString(R.string.main_import_fail), Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			Toast.makeText(context, context.getString(R.string.main_import_fail), Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Recursively crawl through a directory and build a list of all files
	 *
	 * @param dir The root directory
	 * @return An ArrayList of all the files in the root directory
	 */
	private static ArrayList<File> findAllFiles(File dir) {
		ArrayList<File> files = new ArrayList<File>();
		File listFiles[] = dir.listFiles();
		assert listFiles != null;
		for (File file : listFiles) {
			if (file.isDirectory()) {
				files.addAll(findAllFiles(file));
			}
			else {
				files.add(file);
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
			OutputStream out = new BufferedOutputStream(new FileOutputStream(
					new File(context.getFilesDir(), entry.getName())));
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
		int fileDirLen = context.getFilesDir().getAbsolutePath().length() + 1;
		for (File file : files) {
			ZipEntry ze = new ZipEntry(file.getAbsolutePath().substring(fileDirLen));
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
