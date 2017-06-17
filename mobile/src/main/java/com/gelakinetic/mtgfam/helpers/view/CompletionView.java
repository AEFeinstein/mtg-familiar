package com.gelakinetic.mtgfam.helpers.view;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;

public class CompletionView extends ATokenTextView {

    public CompletionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.allowDuplicates(false);
    }

    @Override
    protected View getViewForObject(String set) {
        LayoutInflater l = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        TextView view = (TextView) l.inflate(R.layout.set_token, (ViewGroup) getParent(), false);
        view.setText(set);

        return view;
    }
}
