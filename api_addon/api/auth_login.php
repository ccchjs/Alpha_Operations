<?php
require_once __DIR__ . '/bootstrap.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_fail('POST only.', 405);

$body = read_json_body();
$username = trim($body['username'] ?? '');
$password = (string)($body['password'] ?? '');
$deviceLabel = trim($body['device_label'] ?? '');

if ($username === '' || $password === '') {
    json_fail('Username and password are required.');
}

$stmt = $pdo->prepare("SELECT id, username, password, full_name, role FROM users WHERE username = ?");
$stmt->execute([$username]);
$user = $stmt->fetch();

if (!$user || !password_verify($password, $user['password'])) {
    json_fail('Invalid username or password.', 401);
}

$token = bin2hex(random_bytes(32)); // 64 hex chars, matches api_tokens.token CHAR(64)

$pdo->prepare("INSERT INTO api_tokens (user_id, token, device_label) VALUES (?, ?, ?)")
    ->execute([$user['id'], $token, $deviceLabel ?: null]);

json_out([
    'success' => true,
    'token' => $token,
    'user' => [
        'id' => (int)$user['id'],
        'username' => $user['username'],
        'full_name' => $user['full_name'],
        'role' => $user['role'],
    ],
]);
