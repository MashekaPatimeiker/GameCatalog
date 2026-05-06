<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-User-Id');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Database configuration
$host = getenv('DB_HOST') ?: 'postgres';
$port = getenv('DB_PORT') ?: '5432';
$dbname = getenv('DB_NAME') ?: 'game_catalog';
$user = getenv('DB_USER') ?: 'gameuser';
$password = getenv('DB_PASS') ?: 'game123';

try {
    $pdo = new PDO("pgsql:host=$host;port=$port;dbname=$dbname", $user, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
    exit();
}

$method = $_SERVER['REQUEST_METHOD'];
$input = json_decode(file_get_contents('php://input'), true);
$action = isset($_GET['action']) ? $_GET['action'] : '';

// Get user ID from headers (simplified)
function getUserId($pdo) {
    $headers = getallheaders();
    if (isset($headers['X-User-Id'])) {
        $firebaseUid = $headers['X-User-Id'];
        $stmt = $pdo->prepare("SELECT id FROM users WHERE firebase_uid = ?");
        $stmt->execute([$firebaseUid]);
        $user = $stmt->fetch();
        if ($user) {
            return $user['id'];
        }
    }
    return 1; // Default user ID for demo
}

$userId = getUserId($pdo);

switch ($action) {
    case 'ping':
        echo json_encode(['status' => 'ok', 'timestamp' => time()]);
        break;
case 'register':
    if ($method !== 'POST') {
        http_response_code(405);
        break;
    }

    $email = $input['email'] ?? '';
    $password = $input['password'] ?? '';
    $username = $input['username'] ?? '';

    error_log("Register attempt: email=$email, username=$username");

    if (empty($email) || empty($password) || empty($username)) {
        echo json_encode(['success' => false, 'error' => 'All fields required']);
        break;
    }

    try {
        $passwordHash = password_hash($password, PASSWORD_DEFAULT);
        $stmt = $pdo->prepare("INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?) RETURNING id");
        $stmt->execute([$username, $email, $passwordHash]);
        $user = $stmt->fetch();

        if (!$user) {
            echo json_encode(['success' => false, 'error' => 'Failed to create user']);
            break;
        }

        // Create token
        $token = bin2hex(random_bytes(32));
        $stmt = $pdo->prepare("UPDATE users SET token = ? WHERE id = ?");
        $stmt->execute([$token, $user['id']]);

        echo json_encode([
            'success' => true,
            'user_id' => $user['id'],
            'username' => $username,
            'token' => $token
        ]);
    } catch (PDOException $e) {
        error_log("Register error: " . $e->getMessage());
        echo json_encode(['success' => false, 'error' => 'Email already exists']);
    }
    break;

case 'login':
    if ($method !== 'POST') {
        http_response_code(405);
        break;
    }

    $email = $input['email'] ?? '';
    $password = $input['password'] ?? '';

    error_log("Login attempt: email=$email");

    if (empty($email) || empty($password)) {
        echo json_encode(['success' => false, 'error' => 'Email and password required']);
        break;
    }

    $stmt = $pdo->prepare("SELECT id, username, email, password_hash FROM users WHERE email = ?");
    $stmt->execute([$email]);
    $user = $stmt->fetch();

    if ($user && password_verify($password, $user['password_hash'])) {
        // Update token
        $token = bin2hex(random_bytes(32));
        $stmt = $pdo->prepare("UPDATE users SET token = ? WHERE id = ?");
        $stmt->execute([$token, $user['id']]);

        echo json_encode([
            'success' => true,
            'user_id' => $user['id'],
            'username' => $user['username'],
            'email' => $user['email'],
            'token' => $token
        ]);
    } else {
        echo json_encode(['success' => false, 'error' => 'Invalid email or password']);
    }
    break;

case 'games':
    if ($method === 'GET') {
        // Проверяем авторизацию
        $userId = getUserId($pdo);
        if (!$userId) {
            http_response_code(401);
            echo json_encode(['error' => 'Unauthorized']);
            break;
        }

        $search = isset($_GET['search']) ? $_GET['search'] : '';
        $genre = isset($_GET['genre']) ? $_GET['genre'] : '';
        $sortBy = isset($_GET['sortBy']) ? $_GET['sortBy'] : 'title';
        $sortOrder = isset($_GET['sortOrder']) ? $_GET['sortOrder'] : 'ASC';

        // ВАЖНО: Добавляем фильтр по user_id
        $sql = "SELECT id, title, genre, release_date, description, image_path, is_favorite
                FROM games
                WHERE user_id = :user_id";
        $params = [':user_id' => $userId];

        if (!empty($search)) {
            $sql .= " AND title ILIKE :search";
            $params[':search'] = "%$search%";
        }

        if (!empty($genre) && $genre !== 'All') {
            $sql .= " AND genre ILIKE :genre";
            $params[':genre'] = "%$genre%";
        }

        $allowedSort = ['title', 'genre', 'release_date'];
        $sortBy = in_array($sortBy, $allowedSort) ? $sortBy : 'title';
        $sortOrder = strtoupper($sortOrder) === 'DESC' ? 'DESC' : 'ASC';
        $sql .= " ORDER BY $sortBy $sortOrder";

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $games = $stmt->fetchAll(PDO::FETCH_ASSOC);

        echo json_encode($games);

    } elseif ($method === 'POST') {
        // Создание игры - привязываем к текущему пользователю
        $userId = getUserId($pdo);
        if (!$userId) {
            http_response_code(401);
            echo json_encode(['error' => 'Unauthorized']);
            break;
        }

        if (!$input) {
            http_response_code(400);
            echo json_encode(['error' => 'Invalid input']);
            break;
        }

        $stmt = $pdo->prepare("INSERT INTO games (title, genre, release_date, description, image_path, user_id, is_synced)
                               VALUES (?, ?, ?, ?, ?, ?, true) RETURNING id");
        $stmt->execute([
            $input['title'] ?? '',
            $input['genre'] ?? '',
            $input['release_date'] ?? '',
            $input['description'] ?? '',
            $input['image_path'] ?? '',
            $userId
        ]);
        $result = $stmt->fetch();
        echo json_encode(['success' => true, 'id' => $result['id']]);

    } elseif ($method === 'PUT') {
        // Обновление игры - проверяем, что игра принадлежит пользователю
        $userId = getUserId($pdo);
        if (!$userId) {
            http_response_code(401);
            echo json_encode(['error' => 'Unauthorized']);
            break;
        }

        $gameId = isset($_GET['id']) ? (int)$_GET['id'] : null;
        if (!$gameId) {
            echo json_encode(['error' => 'Game ID required']);
            break;
        }

        // Проверяем, что игра принадлежит пользователю
        $stmt = $pdo->prepare("SELECT id FROM games WHERE id = ? AND user_id = ?");
        $stmt->execute([$gameId, $userId]);
        if (!$stmt->fetch()) {
            echo json_encode(['error' => 'Game not found or access denied']);
            break;
        }

        $stmt = $pdo->prepare("UPDATE games SET title = ?, genre = ?, release_date = ?, description = ?, image_path = ?, updated_at = CURRENT_TIMESTAMP
                               WHERE id = ? AND user_id = ? RETURNING id");
        $stmt->execute([
            $input['title'] ?? '',
            $input['genre'] ?? '',
            $input['release_date'] ?? '',
            $input['description'] ?? '',
            $input['image_path'] ?? '',
            $gameId,
            $userId
        ]);

        if ($stmt->fetch()) {
            echo json_encode(['success' => true]);
        } else {
            echo json_encode(['error' => 'Game not found']);
        }

    } elseif ($method === 'DELETE') {
        // Удаление игры - проверяем, что игра принадлежит пользователю
        $userId = getUserId($pdo);
        if (!$userId) {
            http_response_code(401);
            echo json_encode(['error' => 'Unauthorized']);
            break;
        }

        $gameId = isset($_GET['id']) ? (int)$_GET['id'] : null;
        if (!$gameId) {
            echo json_encode(['error' => 'Game ID required']);
            break;
        }

        $stmt = $pdo->prepare("DELETE FROM games WHERE id = ? AND user_id = ?");
        $stmt->execute([$gameId, $userId]);
        echo json_encode(['success' => true, 'deleted' => $stmt->rowCount()]);
    }
    break;
case 'favorites':
    $userId = getUserId($pdo);
    if (!$userId) {
        http_response_code(401);
        echo json_encode(['error' => 'Unauthorized']);
        break;
    }

    if ($method === 'GET') {
        // Получить избранные игры пользователя
        $stmt = $pdo->prepare("SELECT g.* FROM games g
                               JOIN favorites f ON g.id = f.game_id
                               WHERE f.user_id = ?");
        $stmt->execute([$userId]);
        echo json_encode($stmt->fetchAll(PDO::FETCH_ASSOC));

    } elseif ($method === 'POST') {
        // Добавить в избранное
        $gameId = $input['game_id'] ?? null;
        if (!$gameId) {
            echo json_encode(['error' => 'Game ID required']);
            break;
        }

        try {
            $stmt = $pdo->prepare("INSERT INTO favorites (user_id, game_id) VALUES (?, ?)");
            $stmt->execute([$userId, $gameId]);
            // Также обновляем is_favorite в таблице games
            $stmt = $pdo->prepare("UPDATE games SET is_favorite = true WHERE id = ?");
            $stmt->execute([$gameId]);
            echo json_encode(['success' => true]);
        } catch (PDOException $e) {
            echo json_encode(['success' => false, 'error' => 'Already in favorites']);
        }

    } elseif ($method === 'DELETE') {
        // Удалить из избранного
        $gameId = isset($_GET['game_id']) ? (int)$_GET['game_id'] : null;
        if (!$gameId) {
            echo json_encode(['error' => 'Game ID required']);
            break;
        }

        $stmt = $pdo->prepare("DELETE FROM favorites WHERE user_id = ? AND game_id = ?");
        $stmt->execute([$userId, $gameId]);
        // Обновляем is_favorite в таблице games
        $stmt = $pdo->prepare("UPDATE games SET is_favorite = false WHERE id = ?");
        $stmt->execute([$gameId]);
        echo json_encode(['success' => true, 'deleted' => $stmt->rowCount()]);
    }
    break;
    case 'subscribe':
        header('Content-Type: text/event-stream');
        header('Cache-Control: no-cache');
        header('Access-Control-Allow-Origin: *');
        header('X-Accel-Buffering: no'); // Отключаем буферизацию nginx

        // Отключаем буферизацию вывода
        ob_end_clean();
        ini_set('output_buffering', 'off');

        // Подписываемся на уведомления PostgreSQL
        $pdo->exec("LISTEN game_changes;");

        echo "retry: 1000\n\n";

        while (true) {
            // Ждём уведомления (1 секунда таймаут)
            $result = $pdo->pgsqlGetNotify(PDO::FETCH_ASSOC, 1000);

            if ($result) {
                // Отправляем событие клиенту
                echo "event: game_change\n";
                echo "data: {$result['payload']}\n\n";
            } else {
                // Heartbeat каждые 5 секунд
                if (time() % 5 == 0) {
                    echo ": heartbeat\n\n";
                }
            }

            ob_flush();
            flush();

            // Проверяем, не закрыл ли клиент соединение
            if (connection_aborted()) {
                break;
            }
        }
        break;
    default:
        // Default response for root endpoint
        echo json_encode([
            'message' => 'Game Catalog API',
            'version' => '1.0',
            'endpoints' => [
                'GET ?action=ping' => 'Health check',
                'POST ?action=register' => 'Register user',
                'POST ?action=login' => 'Login user',
                'GET ?action=games' => 'Get all games',
                'POST ?action=games' => 'Create game',
                'PUT ?action=games&id={id}' => 'Update game',
                'DELETE ?action=games&id={id}' => 'Delete game',
                'GET ?action=favorites' => 'Get favorites',
                'POST ?action=favorites' => 'Add to favorites',
                'DELETE ?action=favorites&game_id={id}' => 'Remove from favorites'
            ]
        ]);
}
$pdo = null;
?>