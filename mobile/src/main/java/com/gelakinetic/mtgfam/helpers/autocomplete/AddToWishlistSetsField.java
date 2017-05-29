package com.gelakinetic.mtgfam.helpers.autocomplete;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.tokenautocomplete.FilteredArrayAdapter;

import java.util.HashMap;

public class AddToWishlistSetsField extends CustomTokenCompleteView<MtgCard> {
    private final MtgCard defaultCard = new MtgCard();
    private final HashMap<String, TextView> textViews = new HashMap<>();
    private final HashMap<String, TextView> textViewsFoil = new HashMap<>();

    private class SetAdapter extends FilteredArrayAdapter<MtgCard> {
        SetAdapter(Activity activity, MtgCard[] cards) {
            super(activity, R.layout.wishlist_set_view, R.id.name, cards);
            //TODO cannot add foil and non-foil of one set, because of MtgCard equals.
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final MtgCard mtgCard = super.getItem(position);
            View view = super.getView(position, convertView, parent);
            if (!mtgCard.foil) {
                view.findViewById(R.id.wishlistDialogFoil).setVisibility(GONE);
            }
            TextView textView = (TextView) view.findViewById(R.id.name);
            textView.setText(mtgCard.setCode + " " + mtgCard.setName);
            return view;
        }

        @Override
        protected boolean keepObject(MtgCard obj, String mask) {
            //TODO does not seem to work...
            return (obj.setName.contains(mask) || obj.setCode.contains(mask));
        }
    }

    public AddToWishlistSetsField(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setTokenClickStyle(TokenClickStyle.None);
    }

    public void init(Activity activity, MtgCard[] cards) {
        final SetAdapter setAdapter = new SetAdapter(activity, cards);
        this.setAdapter(setAdapter);
        this.setTokenListener(new TokenListener<MtgCard>() {
            @Override
            public void onTokenAdded(MtgCard token) {
                token.numberOf++;
                TextView textView = null;
                if (token.foil) {
                    textView = textViewsFoil.get(token.setCode);
                } else {
                    textView = textViews.get(token.setCode);
                }
                textView.setText(token.numberOf + "x " + token.setCode);
                //TODO set rarity text color.
            }

            @Override
            public void onTokenRemoved(MtgCard token) {
                token.numberOf = 0;
            }
        });
        for (MtgCard mtgCard : cards) {
            if (mtgCard.numberOf > 0) {
                AddToWishlistSetsField.this.addObject(mtgCard);
            }
        }
    }

    @Override
    protected View getViewForObject(MtgCard card) {
        LayoutInflater l = (LayoutInflater) getContext().getSystemService(
                Activity.LAYOUT_INFLATER_SERVICE);
        LinearLayout layout = (LinearLayout) l.inflate(R.layout.wishlist_token,
                (ViewGroup) getParent(), false);
        TextView textView = (TextView) layout.findViewById(R.id.name);
        if (card.foil) {
            textViewsFoil.put(card.setCode, textView);
        } else {
            textViews.put(card.setCode, textView);
            layout.findViewById(R.id.wishlistDialogFoil).setVisibility(GONE);
        }
        textView.setText(card.numberOf + "x " + card.setCode);
        return layout;
    }

    @Override
    protected MtgCard defaultObject(String completionText) {
        return defaultCard;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        //TODO implement pop-up to change quantity.
        Toast toast = Toast.makeText(AddToWishlistSetsField.this.getContext(), "Clicked",
                Toast.LENGTH_SHORT);
        toast.show();
        return super.onTouchEvent(event);
    }
}
