package com.app.gameform.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.app.gameform.R;
import com.app.gameform.domain.Section;

import java.util.List;

public class SectionAdapter extends RecyclerView.Adapter<SectionAdapter.SectionViewHolder> {
    private List<Section> sectionList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Section section);
    }

    public SectionAdapter(List<Section> sectionList) {
        this.sectionList = sectionList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public SectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section, parent, false);
        return new SectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SectionViewHolder holder, int position) {
        Section section = sectionList.get(position);
        holder.bind(section);
    }

    @Override
    public int getItemCount() {
        return sectionList.size();
    }

    class SectionViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSectionTitle;
        private TextView tvSectionDescription;
        private TextView tvPostCount;

        public SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSectionTitle = itemView.findViewById(R.id.tvSectionTitle);
            tvSectionDescription = itemView.findViewById(R.id.tvSectionDescription);
            tvPostCount = itemView.findViewById(R.id.tvPostCount);

            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener.onItemClick(sectionList.get(position));
                    }
                }
            });
        }

        public void bind(Section section) {
            tvSectionTitle.setText(section.getSectionName());
            tvSectionDescription.setText(section.getSectionDescription());

            // 只显示帖子数量，移除成员数量
            tvPostCount.setText("帖子: --");

            // 如果你需要显示实际的帖子统计数据，建议在Section类中添加这个字段：
            // tvPostCount.setText("帖子: " + section.getPostCount());
        }
    }
}