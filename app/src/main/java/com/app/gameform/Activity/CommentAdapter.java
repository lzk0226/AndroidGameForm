package com.app.gameform.Activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.domain.Comment;
import com.app.gameform.utils.HtmlUtils;
import com.app.gameform.utils.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private List<Comment> commentList;
    private OnCommentLikeClickListener likeClickListener;
    private OnCommentReplyClickListener replyClickListener;

    public CommentAdapter(List<Comment> commentList,
                          OnCommentLikeClickListener likeClickListener,
                          OnCommentReplyClickListener replyClickListener) {
        this.commentList = commentList;
        this.likeClickListener = likeClickListener;
        this.replyClickListener = replyClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        // 绑定主评论数据
        bindMainComment(holder, comment);

        // 绑定子评论数据
        bindReplies(holder, comment);
    }

    private void bindMainComment(ViewHolder holder, Comment comment) {
        holder.tvUserName.setText(comment.getNickName());
        holder.tvCommentContent.setText(HtmlUtils.removeHtmlTags(comment.getCommentContent()));
        holder.tvLikeCount.setText(String.valueOf(comment.getLikeCount()));

        // 加载用户头像
        ImageUtils.loadUserAvatar(holder.itemView.getContext(),
                holder.ivUserAvatar, comment.getUserAvatar());

        // 设置点赞状态
        updateLikeStatus(holder.ivLike, comment.getHasLiked());

        // 点赞点击事件
        holder.ivLike.setOnClickListener(v -> {
            if (likeClickListener != null) {
                likeClickListener.onCommentLikeClick(comment);
            }
        });

        // 回复点击事件
        holder.tvReply.setOnClickListener(v -> {
            if (replyClickListener != null) {
                replyClickListener.onCommentReplyClick(comment);
            }
        });
    }

    private void bindReplies(ViewHolder holder, Comment comment) {
        // 清空之前的子评论
        holder.llRepliesContainer.removeAllViews();

        List<Comment> replies = comment.getChildren();
        for (Comment c : commentList) {
            Log.d("评论调试", "主评论ID=" + c.getCommentId() + "，子评论数=" +
                    (c.getChildren() == null ? "null" : c.getChildren().size()));
        }


        // 添加调试日志
        Log.d("CommentAdapter", "Comment ID: " + comment.getCommentId());
        Log.d("CommentAdapter", "Replies count: " + (replies != null ? replies.size() : 0));

        if (replies != null && !replies.isEmpty()) {
            // 确保容器可见
            holder.llRepliesContainer.setVisibility(View.VISIBLE);

            // 显示前3个回复，如果有更多则显示"展开更多回复"按钮
            int displayCount = Math.min(3, replies.size());
            for (int i = 0; i < displayCount; i++) {
                View replyView = createReplyView(holder.llRepliesContainer, replies.get(i));
                if (replyView != null) {
                    holder.llRepliesContainer.addView(replyView);
                }
            }

            // 如果有更多回复，显示展开按钮
            if (replies.size() > 3) {
                holder.tvExpandReplies.setVisibility(View.VISIBLE);
                holder.tvExpandReplies.setText("展开更多回复(" + (replies.size() - 3) + ")");
                holder.tvExpandReplies.setOnClickListener(v -> {
                    // 显示所有回复
                    for (int i = 3; i < replies.size(); i++) {
                        View replyView = createReplyView(holder.llRepliesContainer, replies.get(i));
                        if (replyView != null) {
                            holder.llRepliesContainer.addView(replyView);
                        }
                    }
                    holder.tvExpandReplies.setVisibility(View.GONE);
                });
            } else {
                holder.tvExpandReplies.setVisibility(View.GONE);
            }
        } else {
            holder.llRepliesContainer.setVisibility(View.GONE);
            holder.tvExpandReplies.setVisibility(View.GONE);
        }
    }

    private View createReplyView(ViewGroup parent, Comment reply) {
        try {
            // 重要：传入正确的parent参数，确保布局参数正确
            View replyView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reply_comment, parent, false);

            CircleImageView ivReplyAvatar = replyView.findViewById(R.id.iv_reply_user_avatar);
            TextView tvReplyUserName = replyView.findViewById(R.id.tv_reply_user_name);
            TextView tvReplyContent = replyView.findViewById(R.id.tv_reply_content);
            TextView tvReplyTime = replyView.findViewById(R.id.tv_reply_time);
            TextView tvReplyLikeCount = replyView.findViewById(R.id.tv_reply_like_count);
            TextView tvReplyExpand = replyView.findViewById(R.id.tv_reply_expand);
            TextView tvReplyButton = replyView.findViewById(R.id.tv_reply_button);

            // 绑定回复数据
            tvReplyUserName.setText(reply.getNickName());

            // 处理回复内容和文本折叠
            String content = HtmlUtils.removeHtmlTags(reply.getCommentContent());
            setupTextCollapse(tvReplyContent, tvReplyExpand, content);

            // 设置时间
            if (reply.getCreateTime() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/d", Locale.getDefault());
                tvReplyTime.setText(sdf.format(reply.getCreateTime()));
            }

            // 设置点赞数
            tvReplyLikeCount.setText("点赞" + reply.getLikeCount());

            // 加载头像
            ImageUtils.loadUserAvatar(parent.getContext(), ivReplyAvatar, reply.getUserAvatar());

            // 回复点击事件
            tvReplyButton.setOnClickListener(v -> {
                if (replyClickListener != null) {
                    replyClickListener.onCommentReplyClick(reply);
                }
            });

            return replyView;
        } catch (Exception e) {
            Log.e("CommentAdapter", "Error creating reply view", e);
            return null;
        }
    }

    /**
     * 设置文本折叠功能
     */
    private void setupTextCollapse(TextView textView, TextView expandButton, String content) {
        if (content.length() > 30) {
            // 文本超过30字，需要折叠
            String shortText = content.substring(0, 30) + "...";
            textView.setText(shortText);
            expandButton.setVisibility(View.VISIBLE);

            final boolean[] isExpanded = {false};
            expandButton.setOnClickListener(v -> {
                if (!isExpanded[0]) {
                    // 展开
                    textView.setText(content);
                    textView.setMaxLines(Integer.MAX_VALUE);
                    expandButton.setText("收起");
                    isExpanded[0] = true;
                } else {
                    // 收起
                    textView.setText(shortText);
                    textView.setMaxLines(2);
                    expandButton.setText("展开");
                    isExpanded[0] = false;
                }
            });
        } else {
            // 文本不超过30字，直接显示
            textView.setText(content);
            expandButton.setVisibility(View.GONE);
        }
    }

    private void updateLikeStatus(ImageView likeButton, Boolean hasLiked) {
        if (hasLiked != null && hasLiked) {
            likeButton.setImageResource(R.mipmap.ydz); // 已点赞状态的图标
        } else {
            likeButton.setImageResource(R.mipmap.dz); // 未点赞状态的图标
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    // 更新评论列表
    public void updateCommentList(List<Comment> newCommentList) {
        this.commentList = newCommentList;
        notifyDataSetChanged();
    }

    // 更新单个评论的点赞状态
    public void updateCommentLikeStatus(int commentId, boolean hasLiked, int likeCount) {
        for (int i = 0; i < commentList.size(); i++) {
            Comment comment = commentList.get(i);
            if (comment.getCommentId() == commentId) {
                comment.setHasLiked(hasLiked);
                comment.setLikeCount(likeCount);
                notifyItemChanged(i);
                return;
            }

            // 检查子评论
            if (comment.getChildren() != null) {
                for (Comment reply : comment.getChildren()) {
                    if (reply.getCommentId() == commentId) {
                        reply.setHasLiked(hasLiked);
                        reply.setLikeCount(likeCount);
                        notifyItemChanged(i);
                        return;
                    }
                }
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivUserAvatar;
        TextView tvUserName;
        TextView tvCommentContent;
        ImageView ivLike;
        TextView tvLikeCount;
        TextView tvReply;
        LinearLayout llRepliesContainer;
        TextView tvExpandReplies;

        ViewHolder(View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvCommentContent = itemView.findViewById(R.id.tv_comment_content);
            ivLike = itemView.findViewById(R.id.iv_like);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            tvReply = itemView.findViewById(R.id.tv_reply);
            llRepliesContainer = itemView.findViewById(R.id.ll_replies_container);
            tvExpandReplies = itemView.findViewById(R.id.tv_expand_replies);
        }
    }

    public interface OnCommentLikeClickListener {
        void onCommentLikeClick(Comment comment);
    }

    public interface OnCommentReplyClickListener {
        void onCommentReplyClick(Comment comment);
    }
}