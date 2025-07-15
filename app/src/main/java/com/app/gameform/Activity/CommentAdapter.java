package com.app.gameform.Activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.domain.Comment;
import com.app.gameform.utils.HtmlUtils;
import com.app.gameform.utils.ImageUtils;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private List<Comment> commentList;
    private OnCommentLikeClickListener likeClickListener;

    public CommentAdapter(List<Comment> commentList, OnCommentLikeClickListener likeClickListener) {
        this.commentList = commentList;
        this.likeClickListener = likeClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.tvUserName.setText(comment.getNickName());
        holder.tvCommentContent.setText(HtmlUtils.removeHtmlTags(comment.getCommentContent()));
        holder.tvLikeCount.setText(String.valueOf(comment.getLikeCount()));

        // 加载用户头像
        ImageUtils.loadUserAvatar(holder.itemView.getContext(),
                holder.ivUserAvatar, comment.getUserAvatar());

        // 设置点赞状态
        if (comment.getHasLiked() != null && comment.getHasLiked()) {
            holder.ivLike.setImageResource(R.mipmap.dz);
        } else {
            holder.ivLike.setImageResource(R.mipmap.dz);
        }

        // 点赞点击事件
        holder.ivLike.setOnClickListener(v -> {
            if (likeClickListener != null) {
                likeClickListener.onCommentLikeClick(comment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivUserAvatar;
        TextView tvUserName;
        TextView tvCommentContent;
        ImageView ivLike;
        TextView tvLikeCount;

        ViewHolder(View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvCommentContent = itemView.findViewById(R.id.tv_comment_content);
            ivLike = itemView.findViewById(R.id.iv_like);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
        }
    }

    public interface OnCommentLikeClickListener {
        void onCommentLikeClick(Comment comment);
    }
}