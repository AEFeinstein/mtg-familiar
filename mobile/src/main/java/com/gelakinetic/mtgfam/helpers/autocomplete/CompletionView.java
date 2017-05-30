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

public class CompletionView extends CustomTokenCompleteView<String> {
    public CompletionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setTokenClickStyle(TokenClickStyle.Delete);
    }

    @Override
    protected View getViewForObject(String set) {
        LayoutInflater l = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        TextView view = (TextView) l.inflate(R.layout.set_token, (ViewGroup) getParent(), false);
        view.setText(set);

        return view;
    }

    @Override
    protected String defaultObject(String completionText) {
        return "";
    }
}
