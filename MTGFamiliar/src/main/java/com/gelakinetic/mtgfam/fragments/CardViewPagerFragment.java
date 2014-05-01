package com.gelakinetic.mtgfam.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gelakinetic.mtgfam.R;

/**
 * This class will nest the CardViewFragments found by a search in a ViewPager
 */
public class CardViewPagerFragment extends FamiliarFragment {

	/* Bundle keys */
	public static final String CARD_ID_ARRAY = "card_id_array";
	public static final String STARTING_CARD_POSITION = "starting_card_id";

	/**
	 * Grab the array of card IDs and the current position, then create the view amd attach the pager adapter
	 *
	 * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
	 * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
	 *                           fragment should not add the view itself, but this can be used to generate the
	 *                           LayoutParams of the view.
	 * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
	 *                           here.
	 * @return The inflated view
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		Bundle args = getArguments();
		long cardIds[] = args.getLongArray(CARD_ID_ARRAY);
		int currentPosition = args.getInt(STARTING_CARD_POSITION);

		/* Instantiate a ViewPager and a PagerAdapter. */
		View v = inflater.inflate(R.layout.card_view_pager, container, false);
		assert v != null; /* Because Android Studio */
		ViewPager pager = (ViewPager) v.findViewById(R.id.pager);
		CardViewPagerAdapter pagerAdapter = new CardViewPagerAdapter(getChildFragmentManager(), cardIds);
		pager.setAdapter(pagerAdapter);
		pager.setCurrentItem(currentPosition);
		pager.setPageTransformer(true, new DepthPageTransformer());

		/* Retain the instance */
		if (getParentFragment() == null) {
			this.setRetainInstance(true);
		}
		return v;
	}

	/**
	 * A simple pager adapter that holds CardViewFragments
	 */
	private class CardViewPagerAdapter extends FragmentStatePagerAdapter {
		final long[] mCardIds;

		/**
		 * Default Constructor
		 *
		 * @param fm      The FragmentManager which handles the fragments
		 * @param cardIds The array of card IDs to make fragments with
		 */
		public CardViewPagerAdapter(FragmentManager fm, long[] cardIds) {
			super(fm);
			this.mCardIds = cardIds;
		}

		/**
		 * Make a fragment using the ID at this position in mCardIds, and return it
		 *
		 * @param position The index of the Fragment to make
		 * @return The Fragment at that index
		 */
		@Override
		public Fragment getItem(int position) {
			CardViewFragment cvf = new CardViewFragment();
			Bundle args = new Bundle();
			args.putLong(CardViewFragment.CARD_ID, mCardIds[position]);
			cvf.setArguments(args);
			return cvf;
		}

		/**
		 * Returns the count of all the items in the ViewPager
		 *
		 * @return the count of all the items in the ViewPager
		 */
		@Override
		public int getCount() {
			return mCardIds.length;
		}
	}

	/**
	 * Just to be fancy, lets spice up the transformation
	 * http://developer.android.com/training/animation/screen-slide.html
	 */
	public class DepthPageTransformer implements ViewPager.PageTransformer {
		private final float MIN_SCALE = 0.75f;

		/**
		 * A custom transformer to get a sweet page effect
		 *
		 * @param view     The view being transformed
		 * @param position Where the view currently is
		 */
		public void transformPage(View view, float position) {
			int pageWidth = view.getWidth();

			if (position < -1) { /* [-Infinity,-1)
				This page is way off-screen to the left. */
				view.setAlpha(0);
			}
			else if (position <= 0) { /* [-1,0]
				Use the default slide transition when moving to the left page */
				view.setTranslationX(0);
				view.setScaleX(1);
				view.setScaleY(1);
				view.setAlpha(1);
			}
			else if (position <= 1) { /* (0,1]
				Fade the page out. */
				view.setAlpha(1 - position);

				/* Counteract the default slide transition */
				view.setTranslationX(pageWidth * -position);

				/* Scale the page down (between MIN_SCALE and 1) */
				float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
				view.setScaleX(scaleFactor);
				view.setScaleY(scaleFactor);

			}
			else { /* (1,+Infinity] */
				/* This page is way off-screen to the right. */
				view.setAlpha(0);
			}
		}
	}
}
