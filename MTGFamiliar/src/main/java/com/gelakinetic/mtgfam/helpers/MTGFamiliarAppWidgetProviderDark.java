package com.gelakinetic.mtgfam.helpers;

import com.gelakinetic.mtgfam.R;

/**
 * Super simple extended widget with the dark theme
 */
public class MTGFamiliarAppWidgetProviderDark extends MTGFamiliarAppWidgetProvider {
    @Override
    public void setLayout() {
        mLayout = R.layout.mtgfamiliar_appwidget_dark;
    }
}
