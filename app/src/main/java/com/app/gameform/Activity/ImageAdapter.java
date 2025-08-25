package com.app.gameform.Activity;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;

import java.util.List;

/**
 * 已选图片的适配器
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<Uri> imageUris;
    private OnImageDeleteListener onImageDeleteListener;

    // 添加删除监听器接口
    public interface OnImageDeleteListener {
        void onImageDeleted(int position);
    }

    public ImageAdapter(List<Uri> imageUris) {
        this.imageUris = imageUris;
    }

    // 设置删除监听器
    public void setOnImageDeleteListener(OnImageDeleteListener listener) {
        this.onImageDeleteListener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post_image, parent, false); // 注意：这里应该是 item_post_image
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri uri = imageUris.get(position);
        holder.imageView.setImageURI(uri);

        // 设置删除按钮的点击事件
        holder.btnDelete.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                // 从列表中删除图片
                imageUris.remove(adapterPosition);

                // 通知适配器数据变化
                notifyItemRemoved(adapterPosition);
                notifyItemRangeChanged(adapterPosition, imageUris.size());

                // 如果设置了删除监听器，调用回调
                if (onImageDeleteListener != null) {
                    onImageDeleteListener.onImageDeleted(adapterPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton btnDelete; // 添加删除按钮引用

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_image);
            btnDelete = itemView.findViewById(R.id.btn_delete); // 绑定删除按钮
        }
    }
}