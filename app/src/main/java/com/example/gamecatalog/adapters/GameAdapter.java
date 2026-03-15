package com.example.gamecatalog.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gamecatalog.R;
import com.example.gamecatalog.models.Game;
import com.example.gamecatalog.utils.ImagePickerHelper;
import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.ViewHolder> {
    private List<Game> games;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Game game);
        void onItemLongClick(Game game);
    }

    public GameAdapter(List<Game> games, OnItemClickListener listener) {
        this.games = games;
        this.listener = listener;
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
        Game game = games.get(position);
        holder.titleText.setText(game.getTitle());
        holder.genreText.setText(game.getGenre());
        holder.dateText.setText(game.getReleaseDate());

        ImagePickerHelper.loadImageIntoView(game.getImagePath(), holder.ivThumbnail);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(game));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onItemLongClick(game);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return games.size();
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