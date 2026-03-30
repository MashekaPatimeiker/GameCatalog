package com.example.gamecatalog.data.api.models;

import java.util.ArrayList;
import java.util.List;

public class ApiGameList extends ArrayList<ApiGame> {

    public ApiGameList() {
        super();
    }

    public ApiGameList(List<ApiGame> games) {
        super(games);
    }
}