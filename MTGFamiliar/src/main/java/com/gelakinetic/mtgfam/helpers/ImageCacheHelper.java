/**
 Copyright 2014 Devin Collins

 This file is part of MTG Familiar.

 MTG Familiar is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 MTG Familiar is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.gelakinetic.mtgfam.FamiliarActivity;

/**
 * This class handles the image cache so we can call it from anywhere
 */
public class ImageCacheHelper {

	private LruCache<String, Bitmap> mMemoryCache;

	/**
	 * Creates a new instance of the RAM cache and sizes it according to
	 * the user preference.
	 */
	public ImageCacheHelper(FamiliarActivity activity) {
		int maxMemory = 100 / activity.mPreferenceAdapter.getRAMCache();

		final int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024) / maxMemory;

		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getByteCount() / 1024;
			}
		};
	}

	/**
	 * Adds a bitmap to the cache based on a string key
	 */
	public void addBitmapToCache(String key, Bitmap bitmap) {
		if (getBitmapFromCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	/**
	 * Retrieves a bitmap from the cache based on a string key
	 *
	 * @param key The unique key used to retrieve the bitmap
	 */
	public Bitmap getBitmapFromCache(String key) {
		return mMemoryCache.get(key);
	}

}
