package com.gelakinetic.mtgfam.helpers.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.tokenautocomplete.FilteredArrayAdapter;

import java.util.LinkedHashMap;

public class ManaCostTextView extends ATokenTextView {;
    private final ManaSymbolAdapter manaSymbolAdapter = new ManaSymbolAdapter();
    private static final LinkedHashMap<String, Integer> MANA_SYMBOLS = new LinkedHashMap<>();
    static {
        MANA_SYMBOLS.put("0", R.drawable.glyph_0);
        MANA_SYMBOLS.put("1", R.drawable.glyph_1);
        MANA_SYMBOLS.put("2", R.drawable.glyph_2);
        MANA_SYMBOLS.put("3", R.drawable.glyph_3);
        MANA_SYMBOLS.put("4", R.drawable.glyph_4);
        MANA_SYMBOLS.put("5", R.drawable.glyph_5);
        MANA_SYMBOLS.put("6", R.drawable.glyph_6);
        MANA_SYMBOLS.put("7", R.drawable.glyph_7);
        MANA_SYMBOLS.put("8", R.drawable.glyph_8);
        MANA_SYMBOLS.put("9", R.drawable.glyph_9);
        MANA_SYMBOLS.put("10", R.drawable.glyph_10);
        MANA_SYMBOLS.put("11", R.drawable.glyph_11);
        MANA_SYMBOLS.put("12", R.drawable.glyph_12);
        MANA_SYMBOLS.put("13", R.drawable.glyph_13);
        MANA_SYMBOLS.put("14", R.drawable.glyph_14);
        MANA_SYMBOLS.put("15", R.drawable.glyph_15);
        MANA_SYMBOLS.put("16", R.drawable.glyph_16);
        MANA_SYMBOLS.put("17", R.drawable.glyph_17);
        MANA_SYMBOLS.put("18", R.drawable.glyph_18);
        MANA_SYMBOLS.put("19", R.drawable.glyph_19);
        MANA_SYMBOLS.put("20", R.drawable.glyph_20);
        MANA_SYMBOLS.put("100", R.drawable.glyph_100);
        MANA_SYMBOLS.put("1000000", R.drawable.glyph_1000000);
        MANA_SYMBOLS.put("X", R.drawable.glyph_x);
        MANA_SYMBOLS.put("Y", R.drawable.glyph_y);
        MANA_SYMBOLS.put("Z", R.drawable.glyph_z);
        MANA_SYMBOLS.put("C", R.drawable.glyph_c);
        MANA_SYMBOLS.put("W", R.drawable.glyph_w);
        MANA_SYMBOLS.put("U", R.drawable.glyph_u);
        MANA_SYMBOLS.put("B", R.drawable.glyph_b);
        MANA_SYMBOLS.put("R", R.drawable.glyph_r);
        MANA_SYMBOLS.put("G", R.drawable.glyph_g);
        MANA_SYMBOLS.put("W2", R.drawable.glyph_w2);
        MANA_SYMBOLS.put("U2", R.drawable.glyph_u2);
        MANA_SYMBOLS.put("B2", R.drawable.glyph_b2);
        MANA_SYMBOLS.put("R2", R.drawable.glyph_r2);
        MANA_SYMBOLS.put("G2", R.drawable.glyph_g2);
        MANA_SYMBOLS.put("GW", R.drawable.glyph_gw);
        MANA_SYMBOLS.put("WU", R.drawable.glyph_wu);
        MANA_SYMBOLS.put("UB", R.drawable.glyph_ub);
        MANA_SYMBOLS.put("BR", R.drawable.glyph_br);
        MANA_SYMBOLS.put("RG", R.drawable.glyph_rg);
        MANA_SYMBOLS.put("GU", R.drawable.glyph_gu);
        MANA_SYMBOLS.put("UR", R.drawable.glyph_ur);
        MANA_SYMBOLS.put("RW", R.drawable.glyph_rw);
        MANA_SYMBOLS.put("WB", R.drawable.glyph_wb);
        MANA_SYMBOLS.put("BG", R.drawable.glyph_bg);
        MANA_SYMBOLS.put("PW", R.drawable.glyph_pw);
        MANA_SYMBOLS.put("PU", R.drawable.glyph_pu);
        MANA_SYMBOLS.put("PB", R.drawable.glyph_pb);
        MANA_SYMBOLS.put("PR", R.drawable.glyph_pr);
        MANA_SYMBOLS.put("PG", R.drawable.glyph_pg);
    }

    public ManaCostTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.allowDuplicates(true);
        this.setAdapter(manaSymbolAdapter);
    }

    @Override
    protected View getViewForObject(String symbol) {
        LayoutInflater l = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        ImageView view = (ImageView) l.inflate(R.layout.mana_token, (ViewGroup) getParent(), false);
        int resId = MANA_SYMBOLS.get(symbol);
        final Drawable drawable = ContextCompat.getDrawable(this.getContext(), resId);
        view.setImageDrawable(drawable);
        return view;
    }

    private class ManaSymbolAdapter extends FilteredArrayAdapter<String> {

        ManaSymbolAdapter() {
            super(ManaCostTextView.this.getContext(), R.layout.list_item_1,
                    MANA_SYMBOLS.keySet().toArray(new String[0]));
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            String symbol = super.getItem(position);
            int resId = MANA_SYMBOLS.get(symbol);
            view.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0);
            view.setCompoundDrawablePadding(10);
            return view;
        }

        @Override
        protected boolean keepObject(String obj, String mask) {
            return obj.toUpperCase().contains(mask.toUpperCase());
        }
    }

    /**
     * Return the mana cost as a string.
     * @return the mana cost string.
     */
    @NonNull
    public String getStringFromObjects() {
        String value = "";
        for (String part : this.getObjects()) {
            value += "{" + part + "}";
        }
        return value;
    }

    public void setObjectsFromString(@Nullable String value) {
        if (value == null) {
            this.clear();
        } else {
            /* Get a list of the persisted mana symbols */
            for (String symbol : value.split("}")) {
                this.addObject(symbol + "}");
            }
        }
    }
}
