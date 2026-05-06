<?php
// websocket_server.php
require_once 'vendor/autoload.php'; // Если есть Composer

use Ratchet\MessageComponentInterface;
use Ratchet\ConnectionInterface;
use Ratchet\WebSocket\WsServer;
use Ratchet\Http\HttpServer;
use Ratchet\Server\IoServer;

// Подключение к PostgreSQL
$host = getenv('DB_HOST') ?: 'postgres';
$port = getenv('DB_PORT') ?: '5432';
$dbname = getenv('DB_NAME') ?: 'game_catalog';
$user = getenv('DB_USER') ?: 'gameuser';
$password = getenv('DB_PASS') ?: 'game123';

class GameNotifier implements MessageComponentInterface {
    protected $connections;
    protected $pdo;

    public function __construct($pdo) {
        $this->connections = new \SplObjectStorage;
        $this->pdo = $pdo;

        // Подписываемся на уведомления PostgreSQL
        $this->pdo->exec("LISTEN game_changes;");
    }

    public function onOpen(ConnectionInterface $conn) {
        $this->connections->attach($conn);
        echo "Новое подключение! ({$conn->resourceId})\n";

        // Отправляем приветственное сообщение
        $conn->send(json_encode([
            'type' => 'connected',
            'message' => 'Connected to game notifier'
        ]));
    }

    public function onMessage(ConnectionInterface $from, $msg) {
        echo "Получено сообщение: $msg\n";

        // Проверяем уведомления из БД
        while ($notification = $this->pdo->pgsqlGetNotify(PDO::FETCH_ASSOC, 0)) {
            echo "Уведомление из БД: " . $notification['payload'] . "\n";

            // Отправляем всем подключённым клиентам
            foreach ($this->connections as $client) {
                $client->send(json_encode([
                    'type' => 'db_change',
                    'data' => json_decode($notification['payload'], true)
                ]));
            }
        }
    }

    public function onClose(ConnectionInterface $conn) {
        $this->connections->detach($conn);
        echo "Подключение закрыто ({$conn->resourceId})\n";
    }

    public function onError(ConnectionInterface $conn, \Exception $e) {
        echo "Ошибка: {$e->getMessage()}\n";
        $conn->close();
    }
}

// Запускаем сервер
$server = IoServer::factory(
    new HttpServer(
        new WsServer(
            new GameNotifier($pdo)
        )
    ),
    8081  // Порт для WebSocket
);

echo "WebSocket сервер запущен на порту 8081\n";
$server->run();
?>