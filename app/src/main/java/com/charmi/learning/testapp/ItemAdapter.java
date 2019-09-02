package com.charmi.learning.testapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.charmi.learning.testapp.POJO.Result;

import java.util.List;

import javax.sql.DataSource;

class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<Result> mItems;
    private ItemListener mListener;
    private Context context;

    ItemAdapter(Context ctx ,  ItemListener listener , List<Result> items) {
        context = ctx;
        mItems = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bottom_sheet_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {
        try{
            viewHolder.tvPlace.setText(mItems.get(position).getName());
            viewHolder.tvVicinity.setText(mItems.get(position).getVicinity());

            if (mItems.get(position).getRating() != null){
                viewHolder.tvRating.setText(mItems.get(position).getRating().toString());
                viewHolder.ratingBar.setRating(Float.parseFloat(mItems.get(position).getRating().toString()));
            } else {
                viewHolder.tvRating.setVisibility(View.GONE);
                viewHolder.ratingBar.setVisibility(View.GONE);
            }

            if(mItems.get(position).getPhotoUrl() != null){

                String photo_uri = mItems.get(position).getPhotoUrl();

                Glide.with(context)
                        .load(photo_uri)
                        .centerCrop()
                        .placeholder(R.mipmap.ic_not_found)
                        .crossFade()
                        .into(viewHolder.ivPlace);

            }

        }catch (Exception e){
            e.printStackTrace();
        }


    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView tvPlace, tvVicinity,tvRating;
        RatingBar ratingBar;
        ImageView ivPlace;
        String item;

        ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            tvPlace = (TextView) itemView.findViewById(R.id.tv_place);
            tvVicinity = (TextView) itemView.findViewById(R.id.tv_vicinity);
            tvRating = (TextView) itemView.findViewById(R.id.tv_rating);
            ratingBar =  itemView.findViewById(R.id.rating_bar);
            ivPlace =  itemView.findViewById(R.id.iv_place);
        }

        void setData(String item) {
            this.item = item;
            tvPlace.setText(item);
            tvVicinity.setText(item);
            tvRating.setText(item);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onItemClick(item);
            }
        }
    }

    interface ItemListener {
        void onItemClick(String item);
    }
}
