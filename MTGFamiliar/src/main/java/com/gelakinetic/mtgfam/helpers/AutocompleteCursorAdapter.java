/**
 Copyright 2011 Adam Feinstein

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

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;

/**
 * This cursor adapter provides suggestions for card names directly from the database
 */
public class AutocompleteCursorAdapter extends CursorAdapter {

	private CardDbAdapter mDbHelper;

	/**
	 * Default constructor. Open a database, then close it. It will be opened/closed on queries later
	 *
	 * @param context The context
	 */
	public AutocompleteCursorAdapter(Context context) {
		super(context, null, 0);
		try {
			mDbHelper = new CardDbAdapter(context);
			mDbHelper.close(); // close the immediately opened db
		} catch (FamiliarDbException e) {
			// something went wrong
		}
	}

	/**
	 * Makes a new view to hold the data pointed to by cursor.
	 *
	 * @param context Interface to application's global information
	 * @param cursor  The cursor from which to get the data. The cursor is already moved to the correct position.
	 * @param parent  The parent to which the new view is attached to
	 * @return the newly created view.
	 */
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return inflater.inflate(R.layout.list_item_1, null);

	}

	/**
	 * Bind an existing view to the data pointed to by cursor
	 *
	 * @param view    Existing view, returned earlier by newView
	 * @param context Interface to application's global information
	 * @param cursor  The cursor from which to get the data. The cursor is already moved to the correct position.
	 */
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		String keyword = cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_NAME));
		TextView tv = (TextView) view.findViewById(R.id.text1);
		tv.setText(keyword);
	}

	/**
	 * Converts the cursor into a CharSequence.
	 *
	 * @param cursor the cursor to convert to a CharSequence
	 * @return a CharSequence representing the value
	 */
	@Override
	public CharSequence convertToString(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_NAME));
	}

	/**
	 * Runs a query with the specified constraint. This query is requested by the filter attached to this adapter. The
	 * query is provided by a FilterQueryProvider. If no provider is specified, the current cursor is not filtered and
	 * returned. After this method returns the resulting cursor is passed to changeCursor(Cursor) and the previous
	 * cursor is closed. This method is always executed on a background thread, not on the application's main thread
	 * (or UI thread.) Contract: when constraint is null or empty, the original results, prior to any filtering, must
	 * be returned.
	 *
	 * @param constraint the constraint with which the query must be filtered
	 * @return a Cursor representing the results of the new query
	 */
	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
		String filter;
		if (constraint != null) {
			filter = constraint.toString();
		}
		else {
			return null;
		}

		Cursor cursor = null;
		try {
			if (mDbHelper != null) {
				mDbHelper.openReadable();
				cursor = mDbHelper.autoComplete(filter);
				mDbHelper.close();
			}
		} catch (FamiliarDbException e) {
			return null;
		}
		return cursor;
	}
}
