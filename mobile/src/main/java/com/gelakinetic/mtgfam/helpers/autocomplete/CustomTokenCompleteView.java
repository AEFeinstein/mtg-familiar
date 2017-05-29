package com.gelakinetic.mtgfam.helpers.autocomplete;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.tokenautocomplete.TokenCompleteTextView;

public abstract class CustomTokenCompleteView<T>  extends TokenCompleteTextView<T> {
    private boolean handleDismiss = false;

    public CustomTokenCompleteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.allowDuplicates(false);
        this.performBestGuess(false);
        this.allowCollapse(true);
    }

    @Override
    public boolean enoughToFilter() { return true; }

    @Override
    public void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused && this.getAdapter() != null) {
            performFiltering(getText(), 0);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        handleDismiss = true;
        super.onWindowFocusChanged(hasWindowFocus);
        handleDismiss = false;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        handleDismiss = true;
        boolean result = super.onKeyPreIme(keyCode, event);
        handleDismiss = false;
        return result;
    }

    @Override
    protected void onDetachedFromWindow() {
        handleDismiss = true;
        super.onDetachedFromWindow();
        handleDismiss = false;
    }

    public void dismissDropDown() {
        if (handleDismiss) {
            super.dismissDropDown();
        }
    }
}
