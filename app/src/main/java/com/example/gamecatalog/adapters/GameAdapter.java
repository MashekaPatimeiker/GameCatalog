package com.example.gamecatalog.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamecatalog.R;
import com.example.gamecatalog.data.database.entities.GameEntity;
import com.example.gamecatalog.utils.ImageLoader;
import com.example.gamecatalog.utils.SocialShareHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.ViewHolder> {

    private List<GameEntity> games = new ArrayList<>();
    private OnItemClickListener listener;
    private ImageLoader imageLoader;
    private Context context;
    private Set<Integer> favoriteIds = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(GameEntity game);
        void onItemLongClick(GameEntity game);
        void onFavoriteClick(GameEntity game, boolean isFavorite);
        void onShareClick(GameEntity game);
    }

    public GameAdapter(OnItemClickListener listener, Context context) {
        this.listener = listener;
        this.imageLoader = ImageLoader.getInstance();
        this.context = context;
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

        // Загрузка изображения
        String imagePath = game.getImagePath();
        if (imagePath != null && !imagePath.isEmpty() && imagePath.startsWith("http")) {
            imageLoader.loadImage(imagePath, holder.ivThumbnail, R.drawable.ic_game_placeholder);
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.ic_game_placeholder);
        }

        // Устанавливаем иконку избранного
        boolean isFavorite = favoriteIds.contains(game.getId());
        holder.btnFavorite.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);

        // Обработчики кликов
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

        holder.btnFavorite.setOnClickListener(v -> {
            boolean newFavoriteState = !favoriteIds.contains(game.getId());
            if (newFavoriteState) {
                favoriteIds.add(game.getId());
            } else {
                favoriteIds.remove(game.getId());
            }
            holder.btnFavorite.setImageResource(newFavoriteState ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);

            if (listener != null) {
                listener.onFavoriteClick(game, newFavoriteState);
            }
        });

        holder.btnShare.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShareClick(game);
            } else {
                SocialShareHelper.shareGame(context, game, game.getImagePath());
            }
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

    public void setFavoriteIds(Set<Integer> ids) {
        this.favoriteIds.clear();
        this.favoriteIds.addAll(ids);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, genreText, dateText;
        ImageView ivThumbnail;
        ImageButton btnFavorite, btnShare;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.gameTitle);
            genreText = itemView.findViewById(R.id.gameGenre);
            dateText = itemView.findViewById(R.id.gameDate);
            ivThumbnail = itemView.findViewById(R.id.ivGameThumbnail);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnShare = itemView.findViewById(R.id.btnShare);
        }
    }
}