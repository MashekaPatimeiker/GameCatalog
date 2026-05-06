# В контейнере php (где ты запустил сервер)
cat > sse.php << 'EOF'
<?php
header('Content-Type: text/event-stream');
header('Cache-Control: no-cache');
header('Access-Control-Allow-Origin: *');

// Отключаем буферизацию
ob_end_clean();
ini_set('output_buffering', 'off');

// Подключение к БД
$host = getenv('DB_HOST') ?: 'postgres';
$port = getenv('DB_PORT') ?: '5432';
$dbname = getenv('DB_NAME') ?: 'game_catalog';
$user = getenv('DB_USER') ?: 'gameuser';
$password = getenv('DB_PASS') ?: 'game123';

try {
    $pdo = new PDO("pgsql:host=$host;port=$port;dbname=$dbname", $user, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Подписываемся на уведомления
    $pdo->exec("LISTEN game_changes;");
    
    echo "retry: 1000\n\n";
    
    while (true) {
        $result = $pdo->pgsqlGetNotify(PDO::FETCH_ASSOC, 1000);
        
        if ($result) {
            echo "event: game_change\n";
            echo "data: {$result['payload']}\n\n";
            ob_flush();
            flush();
        } else {
            // Heartbeat каждые 5 секунд
            if (time() % 5 == 0) {
                echo ": heartbeat\n\n";
                ob_flush();
                flush();
            }
        }
        
        if (connection_aborted()) {
            break;
        }
    }
} catch (PDOException $e) {
    echo "event: error\n";
    echo "data: " . json_encode(['error' => $e->getMessage()]) . "\n\n";
}
?>
EOF