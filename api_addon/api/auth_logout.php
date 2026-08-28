<?php
require_once __DIR__ . '/bootstrap.php';
$user = require_auth($pdo);

$header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
preg_match('/Bearer\s+(.+)/i', $header, $m);
$token = trim($m[1] ?? '');

if ($token) {
    $pdo->prepare("UPDATE api_tokens SET revoked_at = NOW() WHERE token = ?")->execute([$token]);
}

json_out(['success' => true]);
