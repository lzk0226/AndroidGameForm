package com.app.gameform.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.utils.ImageUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

/**
 * 游戏图片轮播适配器
 * @author SockLightDust
 */
public class GameImageAdapter extends RecyclerView.Adapter<GameImageAdapter.GameImageViewHolder> {

    private static final String TAG = "GameImageAdapter";

    private List<String> gameImages;
    private Context context;

    public GameImageAdapter(List<String> gameImages) {
        this.gameImages = gameImages;
    }

    @NonNull
    @Override
    public GameImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_game_image, parent, false);
        return new GameImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameImageViewHolder holder, int position) {
        if (gameImages == null || gameImages.isEmpty()) {
            // 设置默认图片
            holder.ivGameImage.setImageResource(R.color.light_gray);
            return;
        }

        String imageUrl = gameImages.get(position);
        Log.d(TAG, "Loading game image at position " + position + ": " + imageUrl);

        // 使用 ImageUtils 加载图片
        ImageUtils.loadImage(context, holder.ivGameImage, imageUrl,
                R.color.light_gray, R.color.light_gray);
    }

    @Override
    public int getItemCount() {
        return gameImages != null ? gameImages.size() : 0;
    }

    /**
     * 更新图片列表
     */
    public void updateImages(List<String> newImages) {
        this.gameImages = newImages;
        notifyDataSetChanged();
    }

    public static class GameImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGameImage;

        public GameImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGameImage = itemView.findViewById(R.id.iv_game_image);
        }
    }
}