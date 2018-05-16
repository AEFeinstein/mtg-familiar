package com.gelakinetic.mtgfam;

import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.gelakinetic.mtgfam.helpers.DecklistHelpers.CompressedDecklistInfo;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.CompressedWishlistInfo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;

/**
 * Test the equality of MtgCards, CompressedWishlistInfos, and CompressedDecklistInfos
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class EqualityTests {

    @Rule
    public ActivityTestRule<FamiliarActivity> mFamiliarActivityRule =
            new ActivityTestRule<>(FamiliarActivity.class);

    private MtgCard lightningBolt;

    @Before
    public void setUp() {
        try {
            lightningBolt = new MtgCard(
                    mFamiliarActivityRule.getActivity(), "Lightning Bolt", null, false, 1);
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void cdiEqualscdi_ReturnsTrue() {
        CompressedDecklistInfo cdi1 = new CompressedDecklistInfo(lightningBolt, false);
        CompressedDecklistInfo cdi2 = new CompressedDecklistInfo(lightningBolt, false);

        assertEquals(true, cdi1.equals(cdi2));
    }

    @Test
    public void cdiEqualscdi_ReturnsFalse() {
        CompressedDecklistInfo cdi1 = new CompressedDecklistInfo(lightningBolt, false);
        CompressedDecklistInfo cdi2 = new CompressedDecklistInfo(lightningBolt, true);

        assertEquals(false, cdi1.equals(cdi2));
    }
    
    @Test
    public void cdiEqualsMtgCard_ReturnsTrue() {
        CompressedDecklistInfo cdi1 = new CompressedDecklistInfo(lightningBolt, false);

        assertEquals(true, cdi1.equals(lightningBolt));
    }

    @Test
    public void mtgCardEqualscdi_ReturnsTrue() {
        CompressedDecklistInfo cdi1 = new CompressedDecklistInfo(lightningBolt, false);

        assertEquals(true, lightningBolt.equals(cdi1));
    }

    @Test
    public void cwiEqualscwi_ReturnsTrue() {
        CompressedWishlistInfo cwi1 = new CompressedWishlistInfo(lightningBolt, 0);
        CompressedWishlistInfo cwi2 = new CompressedWishlistInfo(lightningBolt, 0);

        assertEquals(true, cwi1.equals(cwi2));
    }

    @Test
    public void cwiEqualsMtgCard_ReturnsTrue() {
        CompressedWishlistInfo cwi = new CompressedWishlistInfo(lightningBolt, 0);

        assertEquals(true, cwi.equals(lightningBolt));
    }

}
