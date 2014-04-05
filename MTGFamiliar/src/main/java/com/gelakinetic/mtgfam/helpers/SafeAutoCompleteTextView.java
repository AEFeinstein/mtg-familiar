package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

/**
 * Created by Adam Feinstein on 4/4/2014.
 */
public class SafeAutoCompleteTextView extends AutoCompleteTextView {

	public SafeAutoCompleteTextView(Context context) {
		super(context);
	}

	public SafeAutoCompleteTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SafeAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void onFilterComplete (int count) {
		try {
			super.onFilterComplete(count);
		} catch (IllegalStateException e) {
			/* Ignore */
		}
	}

	@Override
	public void showDropDown() {
		try {
			super.showDropDown();
		} catch (IllegalStateException e) {
			/* Ignore */
		}
	}
}