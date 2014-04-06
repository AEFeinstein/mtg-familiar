package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

/**
 * This class extends AutoCompleteTextView, but catches potential exceptions in onFilterComplete and showDropDown
 */
public class SafeAutoCompleteTextView extends AutoCompleteTextView {

	/**
	 * Constructor, necessary to inflate this class in XML
	 *
	 * @param context A Context to pass to super
	 */
	public SafeAutoCompleteTextView(Context context) {
		super(context);
	}

	/**
	 * Constructor, necessary to inflate this class in XML
	 *
	 * @param context A Context to pass to super
	 * @param attrs   An AttributeSet to pass to super
	 */
	public SafeAutoCompleteTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * Constructor, necessary to inflate this class in XML
	 *
	 * @param context  A Context to pass to super
	 * @param attrs    An AttributeSet to pass to super
	 * @param defStyle An int to pass to super
	 */
	public SafeAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * Notifies the end of a filtering operation, with a catch for IllegalStateException
	 *
	 * @param count the number of values computed by the filter
	 */
	@Override
	public void onFilterComplete(int count) {
		try {
			super.onFilterComplete(count);
		} catch (IllegalStateException e) {
			/* Ignore */
		}
	}

	/**
	 * Displays the drop down on screen, with a catch for IllegalStateException
	 */
	@Override
	public void showDropDown() {
		try {
			super.showDropDown();
		} catch (IllegalStateException e) {
			/* Ignore */
		}
	}
}