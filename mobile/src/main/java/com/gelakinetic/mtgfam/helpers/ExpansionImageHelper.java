package com.gelakinetic.mtgfam.helpers;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.gelakinetic.mtgfam.R;

import java.util.LinkedHashSet;

public class ExpansionImageHelper {

    public enum ExpansionImageSize {
        SMALL,
        LARGE
    }

    public static class ExpansionImageData {
        private final String mExpansionName;
        private final String mExpansionCode;
        private final char mRarity;
        private final long mDbId;

        public ExpansionImageData(String name, String code, char rarity, long dbID) {
            mExpansionName = name;
            mExpansionCode = code;
            mRarity = rarity;
            mDbId = dbID;
        }

        public String getSetCode() {
            return mExpansionCode;
        }

        public long getDbId() {
            return mDbId;
        }
    }

    private class ChangeSetListViewHolder extends RecyclerView.ViewHolder {

        private final TextView setName;
        private final ImageView setImage;
        private ExpansionImageData data;

        ChangeSetListViewHolder(@NonNull ViewGroup view, ChangeSetListAdapter changeSetListAdapter) {
            // Inflates to itemView
            super(LayoutInflater.from(view.getContext()).inflate(R.layout.trader_change_set, view, false));
            setName = itemView.findViewById(R.id.changeSetName);
            setImage = itemView.findViewById(R.id.changeSetImage);
            itemView.findViewById(R.id.changeSetCombo).setOnClickListener(v -> {
                if (null != data) {
                    changeSetListAdapter.onClickDismiss(data);
                }
            });
        }

        ImageView getImageView() {
            return setImage;
        }

        public void setData(ExpansionImageData d) {
            data = d;
        }
    }

    public abstract class ChangeSetListAdapter extends RecyclerView.Adapter<ChangeSetListViewHolder> {

        private final Context mContext;
        private final ExpansionImageData[] mExpansions;
        private Dialog dialog;
        private final ExpansionImageSize mImageSize;

        protected ChangeSetListAdapter(Context context, LinkedHashSet<ExpansionImageData> expansions, ExpansionImageSize size) {
            mContext = context;
            mExpansions = expansions.toArray(new ExpansionImageData[0]);
            mImageSize = size;
        }

        @NonNull
        @Override
        public ChangeSetListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ChangeSetListViewHolder(viewGroup, this);
        }

        @Override
        public void onBindViewHolder(@NonNull ChangeSetListViewHolder changeSetListViewHolder, int i) {
            changeSetListViewHolder.setData(mExpansions[i]);
            changeSetListViewHolder.setName.setText(mExpansions[i].mExpansionName);
            ExpansionImageHelper.loadExpansionImage(mContext, mExpansions[i].mExpansionCode, mExpansions[i].mRarity, changeSetListViewHolder.getImageView(), null, mImageSize);
        }

        @Override
        public int getItemCount() {
            return mExpansions.length;
        }

        public void setDialogReference(@NonNull Dialog d) {
            dialog = d;
        }

        void onClickDismiss(ExpansionImageData data) {
            onClick(data);
            if (null != dialog) {
                dialog.dismiss();
            }
        }

        protected abstract void onClick(ExpansionImageData data);
    }

    public static void loadExpansionImage(Context context, String set, char rarity, ImageView imageView, @Nullable TextView textView, ExpansionImageSize size) {
        if (context != null) {

            if (null != textView) {
                textView.setVisibility(View.VISIBLE);
            }
            imageView.setVisibility(View.GONE);

            int width, height;
            switch (size) {
                case SMALL:
                    width = context.getResources().getDimensionPixelSize(R.dimen.ExpansionImageWidthSmall);
                    height = context.getResources().getDimensionPixelSize(R.dimen.ExpansionImageHeightSmall);
                    break;
                default:
                case LARGE:
                    width = context.getResources().getDimensionPixelSize(R.dimen.ExpansionImageWidthLarge);
                    height = context.getResources().getDimensionPixelSize(R.dimen.ExpansionImageHeightLarge);
                    break;
            }

            // Then load the image
            Glide.with(context)
                    .load("https://github.com/AEFeinstein/Mtgjson2Familiar/raw/main/symbols/" + set + "_" + rarity + ".png")
                    .dontAnimate()
                    .fitCenter()
                    .override(width, height)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            if (null != textView) {
                                textView.setVisibility(View.GONE);
                            }
                            imageView.setVisibility(View.VISIBLE);
                            return false;
                        }
                    })
                    .into(imageView);
        }
    }
}
