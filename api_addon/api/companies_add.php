<?php
require_once __DIR__ . '/bootstrap.php';
$user = require_auth($pdo);
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_fail('POST only.', 405);

$body = read_json_body();
$name = trim($body['name'] ?? '');
$address = trim($body['address'] ?? '');
$type = ($body['type'] ?? 'regular') === 'sm_store' ? 'sm_store' : 'regular';

if ($name === '') json_fail('Company name is required.');

// If a company with this name already exists (e.g. two staff added it
// offline at the same time), just return the existing one instead of
// failing — keeps sync idempotent.
$stmt = $pdo->prepare("SELECT id, name, address, type FROM companies WHERE name = ?");
$stmt->execute([$name]);
$existing = $stmt->fetch();
if ($existing) {
    json_out(['success' => true, 'company' => $existing, 'already_existed' => true]);
}

$pdo->prepare("INSERT INTO companies (name, address, type) VALUES (?, ?, ?)")
    ->execute([$name, $address ?: null, $type]);
$id = (int)$pdo->lastInsertId();

json_out(['success' => true, 'company' => ['id' => $id, 'name' => $name, 'address' => $address, 'type' => $type]]);
