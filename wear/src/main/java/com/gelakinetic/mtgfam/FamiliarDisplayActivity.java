package com.gelakinetic.mtgfam;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;


public class FamiliarDisplayActivity extends Activity {

    public static final String EXTRA_TITLE = "EXTRA_TITLE";
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        mTextView = (TextView) findViewById(R.id.text);

        Intent intent = getIntent();
        if (intent != null) {
            mTextView.setText(intent.getStringExtra(EXTRA_TITLE));
        }
    }
}