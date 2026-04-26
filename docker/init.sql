-- Создание таблиц
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    firebase_uid VARCHAR(255),
    token VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE TABLE IF NOT EXISTS games (
    id SERIAL PRIMARY KEY,
    firebase_id VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    release_date VARCHAR(50),
    description TEXT,
    image_path TEXT,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    is_synced BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS favorites (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    game_id INTEGER REFERENCES games(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, game_id)
);

CREATE TABLE IF NOT EXISTS game_reviews (
    id SERIAL PRIMARY KEY,
    game_id INTEGER REFERENCES games(id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индексы
CREATE INDEX idx_games_user_id ON games(user_id);
CREATE INDEX idx_games_title ON games(title);
CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_game_id ON favorites(game_id);
CREATE INDEX idx_game_reviews_game_id ON game_reviews(game_id);

-- Триггер для updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_games_updated_at BEFORE UPDATE ON games
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Тестовые данные
INSERT INTO users (username, email, password_hash, firebase_uid) VALUES
('demouser', 'demo@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'firebase-demo-uid-1')
ON CONFLICT (email) DO NOTHING;

INSERT INTO games (title, genre, release_date, description, image_path, user_id) VALUES
('The Legend of Zelda: Breath of the Wild', 'Action-Adventure', '2017-03-03', 'Open world action-adventure game set in Hyrule', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.jpg', 1),
('Super Mario Odyssey', 'Platformer', '2017-10-27', '3D platformer featuring Mario on a globe-trotting adventure', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyz.jpg', 1),
('Elden Ring', 'RPG', '2022-02-25', 'Action RPG set in a vast fantasy world', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co4jni.jpg', 1),
('God of War Ragnarök', 'Action-Adventure', '2022-11-09', 'Epic sequel following Kratos and Atreus', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co4k0g.jpg', 1),
('Cyberpunk 2077', 'RPG', '2020-12-10', 'Open-world RPG set in a dystopian future', 'https://images.igdb.com/igdb/image/upload/t_cover_big/co49nu.jpg', 1)
ON CONFLICT (id) DO NOTHING;