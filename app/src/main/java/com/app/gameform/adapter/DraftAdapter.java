package com.app.gameform.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.domain.Draft;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DraftAdapter extends RecyclerView.Adapter<DraftAdapter.DraftViewHolder> {
    private Context context;
    private List<Draft> draftList;
    private OnDraftClickListener onDraftClickListener;
    private SimpleDateFormat dateFormat;

    public DraftAdapter(Context context, List<Draft> draftList) {
        this.context = context;
        this.draftList = draftList;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }

    // 设置点击监听器接口
    public interface OnDraftClickListener {
        void onDraftClick(Draft draft, int position);
        void onMoreClick(Draft draft, int position);
    }

    public void setOnDraftClickListener(OnDraftClickListener listener) {
        this.onDraftClickListener = listener;
    }

    @NonNull
    @Override
    public DraftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_draft, parent, false);
        return new DraftViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DraftViewHolder holder, int position) {
        Draft draft = draftList.get(position);

        // 设置草稿标题
        holder.tvDraftTitle.setText(draft.getDisplayTitle());

        // 设置创建时间
        if (draft.getUpdateTime() != null) {
            holder.tvDraftTime.setText(dateFormat.format(draft.getUpdateTime()));
        } else if (draft.getCreateTime() != null) {
            holder.tvDraftTime.setText(dateFormat.format(draft.getCreateTime()));
        } else {
            holder.tvDraftTime.setText("未知时间");
        }

        // 设置点击监听器
        setupClickListeners(holder, draft, position);
    }

    private void setupClickListeners(DraftViewHolder holder, Draft draft, int position) {
        // 草稿项点击
        holder.itemView.setOnClickListener(v -> {
            if (onDraftClickListener != null) {
                onDraftClickListener.onDraftClick(draft, position);
            }
        });

        // 更多操作按钮点击
        holder.ivMore.setOnClickListener(v -> {
            if (onDraftClickListener != null) {
                onDraftClickListener.onMoreClick(draft, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return draftList != null ? draftList.size() : 0;
    }

    // 移除指定位置的草稿
    public void removeDraft(int position) {
        if (position >= 0 && position < draftList.size()) {
            draftList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, draftList.size());
        }
    }

    // 添加草稿
    public void addDraft(Draft draft) {
        if (draftList != null) {
            draftList.add(0, draft);
            notifyItemInserted(0);
        }
    }

    // 更新草稿
    public void updateDraft(int position, Draft draft) {
        if (position >= 0 && position < draftList.size()) {
            draftList.set(position, draft);
            notifyItemChanged(position);
        }
    }

    // ViewHolder类
    static class DraftViewHolder extends RecyclerView.ViewHolder {
        TextView tvDraftTitle;
        TextView tvDraftTime;
        ImageView ivMore;

        public DraftViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDraftTitle = itemView.findViewById(R.id.tv_draft_title);
            tvDraftTime = itemView.findViewById(R.id.tv_draft_time);
            ivMore = itemView.findViewById(R.id.iv_more);
        }
    }
}