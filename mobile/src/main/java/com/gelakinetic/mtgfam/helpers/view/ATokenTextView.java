package com.gelakinetic.mtgfam.helpers.view;

import android.content.Context;
import android.graphics.Rect;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.KeyEvent;

import com.tokenautocomplete.TokenCompleteTextView;

public abstract class ATokenTextView extends TokenCompleteTextView<String> {
    private class TokenListener implements TokenCompleteTextView.TokenListener<String> {
        private TokenCompleteTextView.TokenListener<String> tokenListener;

        @Override
        public void onTokenAdded(String token) {
            if (this.tokenListener != null) {
                tokenListener.onTokenAdded(token);
            }
        }

        @Override
        public void onTokenRemoved(String token) {
            if (ATokenTextView.this.getObjects().isEmpty()) {
                String value = ATokenTextView.this.getText().toString();
                if (hasNonSplitChar(value)) {
                    ATokenTextView.this.clearText();
                }
            }
            if (this.tokenListener != null) {
                tokenListener.onTokenRemoved(token);
            }
        }

        private void setTokenListener(TokenCompleteTextView.TokenListener<String> tokenListener) {
            this.tokenListener = tokenListener;
        }
    }

    private ATokenTextView.TokenListener tokenListenerWrapper = new ATokenTextView.TokenListener();
    private boolean handleDismiss = false;

    public ATokenTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.performBestGuess(false);
        this.allowCollapse(true);
        this.setTokenClickStyle(TokenClickStyle.Delete);
        super.setTokenListener(tokenListenerWrapper);
    }

    @Override
    protected String defaultObject(String completionText) {
        return "";
    }

    @Override
    public boolean enoughToFilter() { return true; }

    @Override
    public void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        try {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            if (focused && this.getAdapter() != null) {
                performFiltering(getText(), 0);
            }
        } catch (IndexOutOfBoundsException e) {
            // Ignore it
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

    @Override
    public void setTokenListener(TokenCompleteTextView.TokenListener<String> l) {
        tokenListenerWrapper.setTokenListener(l);
    }

    public void clearTextAndTokens() {
        if (this.getObjects().isEmpty()) {
            this.clearText();
        } else {
            for (String token : this.getObjects()) {
                this.removeObject(token);
            }
        }
    }

    private boolean hasNonSplitChar(String s) {
        for(char c : s.toCharArray()){
            // These two chars are used for splitting tokens in TokenCompleteTextView
            // Unfortunately the array splitChar[] is private there.
            if(c != ',' && c != ';') {
                return true;
            }
        }
        return false;
    }

    private void clearText() {
        final Parcelable state = this.onSaveInstanceState();
        this.onRestoreInstanceState(state);
    }
}
