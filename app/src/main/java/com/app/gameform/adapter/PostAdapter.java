package com.app.gameform.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.domain.Post;
import com.app.gameform.utils.HtmlUtils;
import com.app.gameform.utils.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private Context context;
    private List<Post> postList;
    private OnPostClickListener onPostClickListener;
    private OnPostLikeListener onPostLikeListener;
    private SimpleDateFormat dateFormat;
    private boolean isUserPostList = false; // 新增：标识是否为用户发布页面

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
        this.dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
    }

    // 新增：设置是否为用户发布页面
    public void setUserPostList(boolean isUserPostList) {
        this.isUserPostList = isUserPostList;
    }

    // 设置点击监听器接口
    public interface OnPostClickListener {
        void onPostClick(Post post, int position);
        void onUserClick(Post post, int position);
        void onCommentClick(Post post, int position);
        void onViewClick(Post post, int position);  // 修改：分享改为浏览
        void onMoreClick(Post post, int position);
        void onDeleteClick(Post post, int position); // 新增：删除按钮点击
    }

    public interface OnPostLikeListener {
        void onLikeClick(Post post, int position);
    }

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.onPostClickListener = listener;
    }

    public void setOnPostLikeListener(OnPostLikeListener listener) {
        this.onPostLikeListener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        // 使用工具类加载用户头像
        ImageUtils.loadUserAvatar(context, holder.ivAvatar, post.getAvatar());
        // 设置用户昵称
        holder.tvUsername.setText(post.getNickName() != null ? post.getNickName() : "未知用户");
        // 设置发帖时间
        holder.tvTime.setText(formatTime(post.getCreateTime()));
        // 设置帖子内容 - 处理富文本标签
        holder.tvContent.setText(HtmlUtils.removeHtmlTags(post.getPostContent()));
        // 使用工具类加载帖子图片
        loadPostImage(holder.ivPostImage, holder.cvImage, post.getPhoto());
        // 设置互动数据
        holder.tvCommentCount.setText(String.format("%d",
                post.getCommentCount() != null ? post.getCommentCount() : 0));
        holder.tvLikeCount.setText(String.format("%d",
                post.getLikeCount() != null ? post.getLikeCount() : 0));
        // 修改：显示浏览量而不是分享
        holder.tvViewCount.setText(String.format("%s",
                formatViewCount(post.getViewCount() != null ? post.getViewCount() : 0)));

        // 根据页面类型设置更多按钮图标
        setupMoreButton(holder, post, position);

        // 设置点击监听器
        setupClickListeners(holder, post, position);
        // 设置置顶和热门标识
        setPostFlags(holder, post);
    }

    // 新增：设置更多按钮
    private void setupMoreButton(PostViewHolder holder, Post post, int position) {
        if (isUserPostList) {
            // 用户发布页面显示删除图标（X号）
            holder.ivMore.setImageResource(R.mipmap.ic_delete); // 需要添加删除图标
            holder.ivMore.setOnClickListener(v -> {
                if (onPostClickListener != null) {
                    onPostClickListener.onDeleteClick(post, position);
                }
            });
        } else {
            // 普通页面显示更多图标
            holder.ivMore.setImageResource(R.mipmap.ft);
            holder.ivMore.setOnClickListener(v -> {
                if (onPostClickListener != null) {
                    onPostClickListener.onMoreClick(post, position);
                }
            });
        }
    }

    private void updateLikeIcon(ImageView ivLike, Boolean hasLiked) {
        if (hasLiked != null && hasLiked) {
            ivLike.setImageResource(R.mipmap.ydz); // 已点赞图标
        } else {
            ivLike.setImageResource(R.mipmap.dz);  // 未点赞图标
        }
    }

    /**
     * 格式化浏览量显示
     * @param viewCount 浏览量
     * @return 格式化后的字符串
     */
    private String formatViewCount(int viewCount) {
        if (viewCount < 1000) {
            return String.valueOf(viewCount);
        } else if (viewCount < 10000) {
            return String.format("%.1fk", viewCount / 1000.0);
        } else if (viewCount < 100000) {
            return String.format("%.1fw", viewCount / 10000.0);
        } else {
            return String.format("%.0fw", viewCount / 10000.0);
        }
    }

    private void loadPostImage(ImageView imageView, CardView cardView, String photoUrl) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            cardView.setVisibility(View.VISIBLE);
            // 使用工具类加载图片
            ImageUtils.loadPostImage(context, imageView, photoUrl);
        } else {
            cardView.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners(PostViewHolder holder, Post post, int position) {
        // 帖子内容点击
        holder.itemView.setOnClickListener(v -> {
            if (onPostClickListener != null) {
                onPostClickListener.onPostClick(post, position);
            }
        });
        // 用户头像和昵称点击
        View.OnClickListener userClickListener = v -> {
            if (onPostClickListener != null) {
                onPostClickListener.onUserClick(post, position);
            }
        };
        holder.ivAvatar.setOnClickListener(userClickListener);
        holder.tvUsername.setOnClickListener(userClickListener);

        // 评论按钮点击
        holder.llComment.setOnClickListener(v -> {
            if (onPostClickListener != null) {
                onPostClickListener.onCommentClick(post, position);
            }
        });
        // 点赞按钮点击
        holder.llLike.setOnClickListener(v -> {
            if (onPostLikeListener != null) {
                onPostLikeListener.onLikeClick(post, position);
            }
        });
        // 修改：浏览按钮点击（原来的分享按钮）
        holder.llShare.setOnClickListener(v -> {
            if (onPostClickListener != null) {
                onPostClickListener.onViewClick(post, position);
            }
        });

        // 更多按钮的点击事件在 setupMoreButton 中已经设置
    }

    private void setPostFlags(PostViewHolder holder, Post post) {
        // 设置置顶标识
        if ("1".equals(post.getTopFlag())) {
            holder.tvUsername.setTextColor(Color.parseColor("#FF6B35"));
            holder.tvUsername.setTypeface(null, Typeface.BOLD);
        }

        // 设置热门标识
        if ("1".equals(post.getHotFlag())) {
            // 可以在这里添加热门标识的显示逻辑
        }
    }

    private String formatTime(Date createTime) {
        if (createTime == null) {
            return "未知时间";
        }

        long currentTime = System.currentTimeMillis();
        long postTime = createTime.getTime();
        long diff = currentTime - postTime;

        // 小于1分钟
        if (diff < 60 * 1000) {
            return "刚刚";
        }
        // 小于1小时
        else if (diff < 60 * 60 * 1000) {
            int minutes = (int) (diff / (60 * 1000));
            return minutes + "分钟前";
        }
        // 小于24小时
        else if (diff < 24 * 60 * 60 * 1000) {
            int hours = (int) (diff / (60 * 60 * 1000));
            return hours + "小时前";
        }
        // 小于7天
        else if (diff < 7 * 24 * 60 * 60 * 1000) {
            int days = (int) (diff / (24 * 60 * 60 * 1000));
            return days + "天前";
        }
        // 超过7天显示具体日期
        else {
            return dateFormat.format(createTime);
        }
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    // 更新点赞状态
    public void updateLikeStatus(int position, boolean isLiked, int likeCount) {
        if (position >= 0 && position < postList.size()) {
            Post post = postList.get(position);
            post.setLikeCount(likeCount);
            notifyItemChanged(position);
        }
    }

    // 更新评论数量
    public void updateCommentCount(int position, int commentCount) {
        if (position >= 0 && position < postList.size()) {
            Post post = postList.get(position);
            post.setCommentCount(commentCount);
            notifyItemChanged(position);
        }
    }

    // 新增：更新浏览量
    public void updateViewCount(int position, int viewCount) {
        if (position >= 0 && position < postList.size()) {
            Post post = postList.get(position);
            post.setViewCount(viewCount);
            notifyItemChanged(position);
        }
    }

    // 移除指定位置的帖子
    public void removePost(int position) {
        if (position >= 0 && position < postList.size()) {
            postList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, postList.size());
        }
    }

    // ViewHolder类
    static class PostViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvUsername;
        TextView tvTime;
        TextView tvContent;
        CardView cvImage;
        ImageView ivPostImage;
        View llComment;
        View llLike;
        View llShare;  // 注意：这里保持原来的命名，但实际用作浏览按钮
        TextView tvCommentCount;
        TextView tvLikeCount;
        TextView tvViewCount;  // 修改：原来的tvShare改为tvViewCount
        ImageView ivMore;
        ImageView ivLike;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvContent = itemView.findViewById(R.id.tv_content);
            cvImage = itemView.findViewById(R.id.cv_image);
            ivPostImage = itemView.findViewById(R.id.iv_post_image);
            llComment = itemView.findViewById(R.id.ll_comment);
            llLike = itemView.findViewById(R.id.ll_like);
            llShare = itemView.findViewById(R.id.ll_share);
            tvCommentCount = itemView.findViewById(R.id.tv_comment_count);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            tvViewCount = itemView.findViewById(R.id.tv_view_count);
            ivMore = itemView.findViewById(R.id.iv_more);
            ivLike = itemView.findViewById(R.id.iv_like);
        }
    }
}