package com.example.gamecatalog.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamecatalog.R;
import com.example.gamecatalog.data.database.entities.GameEntity;
import com.example.gamecatalog.utils.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.ViewHolder> {

    private List<GameEntity> games = new ArrayList<>();
    private OnItemClickListener listener;
    private ImageLoader imageLoader;

    public interface OnItemClickListener {
        void onItemClick(GameEntity game);
        void onItemLongClick(GameEntity game);
    }

    public GameAdapter(OnItemClickListener listener) {
        this.listener = listener;
        this.imageLoader = ImageLoader.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_game, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GameEntity game = games.get(position);
        holder.titleText.setText(game.getTitle());
        holder.genreText.setText(game.getGenre());
        holder.dateText.setText(game.getReleaseDate());

        String imagePath = game.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            if (imagePath.startsWith("http")) {
                imageLoader.loadImage(imagePath, holder.ivThumbnail, R.drawable.ic_game_placeholder);
            } else {
                holder.ivThumbnail.setImageResource(R.drawable.ic_game_placeholder);
            }
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.ic_game_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(game);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(game);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    public void updateGames(List<GameEntity> newGames) {
        if (newGames == null) {
            games.clear();
        } else {
            games.clear();
            games.addAll(newGames);
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, genreText, dateText;
        ImageView ivThumbnail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.gameTitle);
            genreText = itemView.findViewById(R.id.gameGenre);
            dateText = itemView.findViewById(R.id.gameDate);
            ivThumbnail = itemView.findViewById(R.id.ivGameThumbnail);
        }
    }
}