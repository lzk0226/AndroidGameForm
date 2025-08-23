package com.app.gameform.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.app.gameform.R;
import com.app.gameform.domain.Game;
import com.app.gameform.utils.ImageUtils;
import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {
    private List<Game> gameList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Game game);
    }

    public GameAdapter(List<Game> gameList) {
        this.gameList = gameList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        Game game = gameList.get(position);
        holder.bind(game);
    }

    @Override
    public int getItemCount() {
        return gameList.size();
    }

    class GameViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivGameIcon;
        private TextView tvGameName;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGameIcon = itemView.findViewById(R.id.ivGameIcon);
            tvGameName = itemView.findViewById(R.id.tvGameName);

            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener.onItemClick(gameList.get(position));
                    }
                }
            });
        }

        public void bind(Game game) {
            // 设置游戏名称
            tvGameName.setText(game.getGameName());

            // 加载游戏图标 - 参考GameDetailActivity的加载方式
            String iconUrl = game.getGameIcon();
            if (iconUrl != null && !iconUrl.isEmpty() && !"null".equals(iconUrl)) {
                // 使用 ImageUtils 加载图片，和 GameDetailActivity 中的方式一致
                ImageUtils.loadImage(itemView.getContext(), ivGameIcon, iconUrl,
                        R.drawable.bg_button_primary, R.drawable.bg_button_primary);
            } else {
                // 设置默认占位图
                ivGameIcon.setImageResource(R.drawable.bg_button_primary);
            }
        }
    }
}