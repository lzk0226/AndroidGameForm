package com.app.gameform.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.Activity.UserListActivity;
import com.app.gameform.Activity.UserProfileActivity;
import com.app.gameform.R;
import com.app.gameform.domain.UserFollow;
import com.app.gameform.utils.ImageUtils;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {

    private Context context;
    private List<UserFollow> userList;
    private int listType;
    private OnActionClickListener listener;

    public interface OnActionClickListener {
        void onFollowClick(UserFollow user, int position);
        void onUnfollowClick(UserFollow user, int position);
    }

    public UserListAdapter(Context context, List<UserFollow> userList, int listType) {
        this.context = context;
        this.userList = userList;
        this.listType = listType;
    }

    public void setOnActionClickListener(OnActionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    /**
     * 加载用户头像
     */
    private void loadAvatar(CircleImageView imageView, String avatarUrl) {
        if (!TextUtils.isEmpty(avatarUrl)) {
            ImageUtils.loadUserAvatar(context, imageView, avatarUrl);
        } else {
            imageView.setImageResource(R.drawable.ic_default_avatar);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserFollow user = userList.get(position);

        // 根据列表类型显示不同的用户信息
        if (listType == UserListActivity.TYPE_FOLLOWING) {
            // 关注列表:显示被关注用户的信息
            holder.tvNickname.setText(user.getFollowingNickName());
            loadAvatar(holder.ivUserAvatar, user.getFollowingAvatar());

            // 关注列表显示"取消关注"按钮 - 灰色
            holder.btnAction.setText("取消关注");
            holder.btnActionCard.setCardBackgroundColor(0xFFE0E0E0); // 灰色背景
            holder.btnAction.setTextColor(0xFF666666); // 灰色文字
            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUnfollowClick(user, position);
                }
            });

            // ✅ 添加整个列表项的点击事件 - 跳转到被关注用户的详情页
            holder.itemView.setOnClickListener(v -> {
                UserProfileActivity.start(context, user.getFollowingId());
            });

        } else {
            // 粉丝列表:显示粉丝的信息
            holder.tvNickname.setText(user.getFollowerNickName());
            loadAvatar(holder.ivUserAvatar, user.getFollowerAvatar());

            // 粉丝列表显示"关注"按钮 - 蓝色
            holder.btnAction.setText("关注");
            holder.btnActionCard.setCardBackgroundColor(0xFF007AFF); // 蓝色背景
            holder.btnAction.setTextColor(0xFFFFFFFF); // 白色文字
            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFollowClick(user, position);
                }
            });

            // ✅ 添加整个列表项的点击事件 - 跳转到粉丝的详情页
            holder.itemView.setOnClickListener(v -> {
                UserProfileActivity.start(context, user.getFollowerId());
            });
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivUserAvatar;
        TextView tvNickname;
        CardView btnActionCard;
        TextView btnAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvNickname = itemView.findViewById(R.id.tvNickname);
            btnActionCard = itemView.findViewById(R.id.btnActionCard);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}