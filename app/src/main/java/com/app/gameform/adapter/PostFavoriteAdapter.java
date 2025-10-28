package com.app.gameform.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.domain.PostFavorite;
import com.app.gameform.utils.ImageUtils;
import com.app.gameform.utils.TimeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 收藏列表适配器
 */
public class PostFavoriteAdapter extends RecyclerView.Adapter<PostFavoriteAdapter.ViewHolder> {

    private Context context;
    private List<PostFavorite> favoriteList;
    private OnItemClickListener listener;
    private boolean isEditMode = false;
    private Set<Integer> selectedPositions = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(PostFavorite favorite);
        void onCheckChanged(int position, boolean isChecked);
    }

    public PostFavoriteAdapter(Context context, List<PostFavorite> favoriteList, OnItemClickListener listener) {
        this.context = context;
        this.favoriteList = favoriteList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PostFavorite favorite = favoriteList.get(position);

        // 标题
        if (!TextUtils.isEmpty(favorite.getPostTitle())) {
            holder.tvTitle.setText(favorite.getPostTitle());
        } else {
            holder.tvTitle.setText("无标题");
        }

        // 作者 - 使用postAuthorName
        if (!TextUtils.isEmpty(favorite.getPostAuthorName())) {
            holder.tvAuthor.setText(favorite.getPostAuthorName());
        } else {
            holder.tvAuthor.setText("匿名用户");
        }

        // 时间 - 使用收藏时间
        if (favorite.getCreateTime() != null) {
            holder.tvTime.setText(TimeUtils.formatTimeAgo(favorite.getCreateTime()));
        } else {
            holder.tvTime.setText("");
        }

        // 版块名称（如果有）
        if (!TextUtils.isEmpty(favorite.getSectionName())) {
            holder.tvSection.setVisibility(View.VISIBLE);
            holder.tvSection.setText(favorite.getSectionName());
        } else {
            holder.tvSection.setVisibility(View.GONE);
        }

        // 封面图（使用photo字段）
        if (!TextUtils.isEmpty(favorite.getPhoto())) {
            holder.ivCover.setVisibility(View.VISIBLE);
            ImageUtils.loadPostImage(context, holder.ivCover, favorite.getPhoto());
        } else {
            holder.ivCover.setVisibility(View.GONE);
        }

        // 编辑模式
        if (isEditMode) {
            holder.cbSelect.setVisibility(View.VISIBLE);
            holder.cbSelect.setOnCheckedChangeListener(null);
            holder.cbSelect.setChecked(selectedPositions.contains(position));
            holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedPositions.add(position);
                } else {
                    selectedPositions.remove(position);
                }
                if (listener != null) {
                    listener.onCheckChanged(position, isChecked);
                }
            });
        } else {
            holder.cbSelect.setVisibility(View.GONE);
        }

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                if (isEditMode) {
                    // 编辑模式下，点击切换选中状态
                    holder.cbSelect.setChecked(!holder.cbSelect.isChecked());
                } else {
                    // 正常模式下，打开帖子详情
                    listener.onItemClick(favorite);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }

    /**
     * 设置编辑模式
     */
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (!editMode) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
    }

    /**
     * 全选/取消全选
     */
    public void selectAll(boolean selectAll) {
        selectedPositions.clear();
        if (selectAll) {
            for (int i = 0; i < favoriteList.size(); i++) {
                selectedPositions.add(i);
            }
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onCheckChanged(-1, selectAll);
        }
    }

    /**
     * 清除选中状态
     */
    public void clearSelection() {
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    /**
     * 获取选中数量
     */
    public int getSelectedCount() {
        return selectedPositions.size();
    }

    /**
     * 获取选中的帖子ID列表
     */
    public List<Integer> getSelectedPostIds() {
        List<Integer> postIds = new ArrayList<>();
        for (int position : selectedPositions) {
            if (position < favoriteList.size()) {
                postIds.add(favoriteList.get(position).getPostId());
            }
        }
        return postIds;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        selectedPositions.clear();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        ImageView ivCover;
        TextView tvTitle;
        TextView tvAuthor;
        TextView tvTime;
        TextView tvSection;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_select);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvSection = itemView.findViewById(R.id.tv_section);
        }
    }
}