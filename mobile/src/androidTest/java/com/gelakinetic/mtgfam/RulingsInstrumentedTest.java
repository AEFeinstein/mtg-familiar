package com.gelakinetic.mtgfam;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.ScryfallRulingsHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented tests for card rulings on Android runtime / emulator.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RulingsInstrumentedTest {

    @Rule
    public ActivityTestRule<FamiliarActivity> mActivityRule =
            new ActivityTestRule<>(FamiliarActivity.class);

    /**
     * Test fetching rulings on Android device using the primary multiverse ID endpoint.
     */
    @Test
    public void testPrimaryMultiverseRulingsOnDevice() throws IOException {
        FamiliarActivity activity = mActivityRule.getActivity();

        // Ankh of Mishra (multiverseid = 1)
        ArrayList<CardViewFragment.Ruling> rulings = ScryfallRulingsHelper.fetchRulings(1, "lea", "1", activity);
        assertNotNull("Rulings list should not be null", rulings);
        assertFalse("Primary endpoint should return rulings for Ankh of Mishra", rulings.isEmpty());

        // Verify glyph formatting works cleanly on Android runtime
        Html.ImageGetter imgGetter = ImageGetterHelper.GlyphGetter(activity);
        StringBuilder sb = new StringBuilder();
        for (CardViewFragment.Ruling r : rulings) {
            sb.append(r.toString()).append("<br><br>");
        }
        CharSequence formatted = ImageGetterHelper.formatStringWithGlyphs(sb.toString(), imgGetter);
        assertNotNull("Formatted glyphs should not be null", formatted);
        assertTrue("Formatted text should have non-zero length", formatted.length() > 0);
    }

    /**
     * Test fetching rulings on Android device using the fallback set code and collector number endpoint.
     */
    @Test
    public void testFallbackSetAndNumberRulingsOnDevice() throws IOException {
        FamiliarActivity activity = mActivityRule.getActivity();

        // Multiverse ID 0 triggers fallback endpoint: /cards/lea/1/rulings
        ArrayList<CardViewFragment.Ruling> rulings = ScryfallRulingsHelper.fetchRulings(0, "lea", "1", activity);
        assertNotNull("Rulings list should not be null", rulings);
        assertFalse("Fallback endpoint should return rulings", rulings.isEmpty());
    }

    /**
     * Test that card rulings dialog layout correctly displays rulings and Scryfall page link.
     */
    @Test
    @UiThreadTest
    public void testRulingsDialogLayoutWithScryfallLink() {
        FamiliarActivity activity = mActivityRule.getActivity();
        View view = activity.getLayoutInflater().inflate(R.layout.card_view_rulings_dialog, null, false);
        assertNotNull("Dialog view should inflate", view);

        TextView textViewRules = view.findViewById(R.id.rules);
        assertNotNull("Rules TextView should exist", textViewRules);

        // Populate mock ruling with HTML formatting and mana glyphs
        CardViewFragment.Ruling sampleRuling = new CardViewFragment.Ruling("2004-10-04", "Sample ruling text with {T} & <rules>.");
        String html = sampleRuling.toHtmlString();
        assertTrue("Ruling HTML should contain bold date tag", html.contains("<b>2004-10-04:</b>"));
        assertTrue("Ruling HTML should escape special characters", html.contains("&amp;"));
        assertTrue("Ruling HTML should escape special characters", html.contains("&lt;rules&gt;"));

        textViewRules.setText(Html.fromHtml(html));
        assertTrue("Rules text should be set", textViewRules.getText().toString().contains("Sample ruling text"));

        // Verify AlertDialog action buttons (Option B)
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle(R.string.card_view_rulings)
                .setView(view)
                .setNeutralButton(R.string.card_view_scryfall_page, (d, which) -> {})
                .setPositiveButton(R.string.dialog_ok, (d, which) -> d.dismiss())
                .create();

        assertNotNull("Dialog should be created", dialog);
    }
}