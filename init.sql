CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    token VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS games (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    release_date VARCHAR(50),
    description TEXT,
    image_path TEXT,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    is_synced BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS favorites (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    game_id INTEGER REFERENCES games(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, game_id)
);
-- Вместо picsum.photos используй другие источники
INSERT INTO games (title, genre, release_date, description, image_path, user_id) VALUES
('The Legend of Zelda: Breath of the Wild', 'Action-Adventure', '2017-03-03',
 'Open world action-adventure game set in Hyrule',
 'https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.jpg', 1),

('Super Mario Odyssey', 'Platformer', '2017-10-27',
 '3D platformer featuring Mario on a globe-trotting adventure',
 'https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyz.jpg', 1),

('Elden Ring', 'RPG', '2022-02-25',
 'Action RPG set in a vast fantasy world',
 'https://images.igdb.com/igdb/image/upload/t_cover_big/co4jni.jpg', 1);
-- Создаем тестового пользователя (пароль: test123)
INSERT INTO users (username, email, password_hash) VALUES
('testuser', 'test@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi')
ON CONFLICT (email) DO NOTHING;