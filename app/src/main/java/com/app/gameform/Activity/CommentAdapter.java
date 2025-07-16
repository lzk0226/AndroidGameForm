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
        bindMainCommentData(holder, comment);
        bindReplyComments(holder, comment);
    }

    private void bindMainCommentData(ViewHolder holder, Comment comment) {
        setUserName(holder.tvUserName, comment.getNickName());
        setCommentContent(holder.tvCommentContent, comment.getCommentContent());
        setLikeCount(holder.tvLikeCount, comment.getLikeCount());
        loadUserAvatar(holder.itemView.getContext(), holder.ivUserAvatar, comment.getUserAvatar());
        updateLikeButtonStatus(holder.ivLike, comment.getHasLiked());
        setupLikeClickListener(holder.ivLike, comment);
        setupReplyClickListener(holder.tvReply, comment);
    }

    private void bindReplyComments(ViewHolder holder, Comment comment) {
        clearReplyCommentsContainer(holder.llRepliesContainer);
        List<Comment> replies = comment.getChildren();
        logCommentDetails(comment, replies);

        if (replies != null && !replies.isEmpty()) {
            showReplyCommentsContainer(holder.llRepliesContainer);
            displayInitialReplies(holder.llRepliesContainer, replies);

            Log.d("CommentAdapter", "准备设置展开按钮，当前评论ID: " + comment.getCommentId()
                    + ", 子评论数: " + replies.size());

            setupExpandRepliesButton(holder, replies);

            Log.d("CommentAdapter", "展开按钮状态: " +
                    (replies.size() > 3 ? "可见" : "隐藏"));
        } else {
            hideReplyCommentsContainer(holder.llRepliesContainer);
            hideExpandRepliesButton(holder.tvExpandReplies);
        }
    }

    private View createReplyView(ViewGroup parent, Comment reply) {
        try {
            View replyView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reply_comment, parent, false);

            CircleImageView ivReplyAvatar = replyView.findViewById(R.id.iv_reply_user_avatar);
            TextView tvReplyUserName = replyView.findViewById(R.id.tv_reply_user_name);
            TextView tvReplyContent = replyView.findViewById(R.id.tv_reply_content);
            TextView tvReplyTime = replyView.findViewById(R.id.tv_reply_time);
            TextView tvReplyLikeCount = replyView.findViewById(R.id.tv_reply_like_count);
            TextView tvReplyExpand = replyView.findViewById(R.id.tv_reply_expand);
            TextView tvReplyButton = replyView.findViewById(R.id.tv_reply_button);

            setUserName(tvReplyUserName, reply.getNickName());
            setupReplyContent(tvReplyContent, tvReplyExpand, reply.getCommentContent());
            setReplyTime(tvReplyTime, reply.getCreateTime());
            setReplyLikeCount(tvReplyLikeCount, reply.getLikeCount());
            loadUserAvatar(parent.getContext(), ivReplyAvatar, reply.getUserAvatar());
            setupReplyButtonClickListener(tvReplyButton, reply);

            return replyView;
        } catch (Exception e) {
            Log.e("CommentAdapter", "Error creating reply view", e);
            return null;
        }
    }

    private void setupTextCollapse(TextView textView, TextView expandButton, String content) {
        if (content.length() > 30) {
            String shortText = content.substring(0, 30) + "...";
            textView.setText(shortText);
            expandButton.setVisibility(View.VISIBLE);

            final boolean[] isExpanded = {false};
            expandButton.setOnClickListener(v -> {
                if (!isExpanded[0]) {
                    textView.setText(content);
                    textView.setMaxLines(Integer.MAX_VALUE);
                    expandButton.setText("收起");
                    isExpanded[0] = true;
                } else {
                    textView.setText(shortText);
                    textView.setMaxLines(2);
                    expandButton.setText("展开");
                    isExpanded[0] = false;
                }
            });
        } else {
            textView.setText(content);
            expandButton.setVisibility(View.GONE);
        }
    }

    private void updateLikeButtonStatus(ImageView likeButton, Boolean hasLiked) {
        if (hasLiked != null && hasLiked) {
            likeButton.setImageResource(R.mipmap.ydz);
        } else {
            likeButton.setImageResource(R.mipmap.dz);
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public void updateCommentList(List<Comment> newCommentList) {
        this.commentList = newCommentList;
        notifyDataSetChanged();
    }

    public void updateCommentLikeStatus(int commentId, boolean hasLiked, int likeCount) {
        for (int i = 0; i < commentList.size(); i++) {
            Comment comment = commentList.get(i);
            if (comment.getCommentId() == commentId) {
                comment.setHasLiked(hasLiked);
                comment.setLikeCount(likeCount);
                notifyItemChanged(i);
                return;
            }

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

    // 辅助方法
    private void setUserName(TextView textView, String name) {
        textView.setText(name);
    }

    private void setCommentContent(TextView textView, String content) {
        textView.setText(HtmlUtils.removeHtmlTags(content));
    }

    private void setLikeCount(TextView textView, int count) {
        textView.setText(String.valueOf(count));
    }

    private void loadUserAvatar(android.content.Context context, CircleImageView imageView, String avatarUrl) {
        ImageUtils.loadUserAvatar(context, imageView, avatarUrl);
    }

    private void setupLikeClickListener(ImageView likeButton, Comment comment) {
        likeButton.setOnClickListener(v -> {
            if (likeClickListener != null) {
                likeClickListener.onCommentLikeClick(comment);
            }
        });
    }

    private void setupReplyClickListener(TextView replyButton, Comment comment) {
        replyButton.setOnClickListener(v -> {
            if (replyClickListener != null) {
                replyClickListener.onCommentReplyClick(comment);
            }
        });
    }

    private void clearReplyCommentsContainer(LinearLayout container) {
        container.removeAllViews();
    }

    private void logCommentDetails(Comment comment, List<Comment> replies) {
        Log.d("评论调试", "主评论ID=" + comment.getCommentId() + "，子评论数=" +
                (replies == null ? "null" : replies.size()));
        Log.d("CommentAdapter", "Comment ID: " + comment.getCommentId());
        Log.d("CommentAdapter", "Replies count: " + (replies != null ? replies.size() : 0));
    }

    private void showReplyCommentsContainer(LinearLayout container) {
        container.setVisibility(View.VISIBLE);
    }

    private void displayInitialReplies(LinearLayout container, List<Comment> replies) {
        int displayCount = Math.min(3, replies.size());
        for (int i = 0; i < displayCount; i++) {
            View replyView = createReplyView(container, replies.get(i));
            if (replyView != null) {
                container.addView(replyView);
            }
        }
    }

    private void setupExpandRepliesButton(ViewHolder holder, List<Comment> replies) {
        if (replies.size() > 3) {
            holder.tvExpandReplies.setVisibility(View.VISIBLE);
            holder.tvExpandReplies.setText("展开更多回复(" + (replies.size() - 3) + ")");
            holder.tvExpandReplies.setOnClickListener(v -> {
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
    }

    private void hideReplyCommentsContainer(LinearLayout container) {
        container.setVisibility(View.GONE);
    }

    private void hideExpandRepliesButton(TextView button) {
        button.setVisibility(View.GONE);
    }

    private void setupReplyContent(TextView contentView, TextView expandButton, String content) {
        String cleanContent = HtmlUtils.removeHtmlTags(content);
        setupTextCollapse(contentView, expandButton, cleanContent);
    }

    private void setReplyTime(TextView textView, java.util.Date createTime) {
        if (createTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/d", Locale.getDefault());
            textView.setText(sdf.format(createTime));
        }
    }

    private void setReplyLikeCount(TextView textView, int count) {
        textView.setText("点赞" + count);
    }

    private void setupReplyButtonClickListener(TextView replyButton, Comment comment) {
        replyButton.setOnClickListener(v -> {
            if (replyClickListener != null) {
                replyClickListener.onCommentReplyClick(comment);
            }
        });
    }
}