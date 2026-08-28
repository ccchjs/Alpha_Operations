<?php
require_once __DIR__ . '/bootstrap.php';
$user = require_auth($pdo);

$limit = max(1, min(200, (int)($_GET['limit'] ?? 50)));

if ($user['role'] === 'admin') {
    $stmt = $pdo->prepare("SELECT id, service_report_no, work_order_no, company_name, form_date, status, created_at
                            FROM Installation_forms WHERE deleted_at IS NULL ORDER BY id DESC LIMIT ?");
    $stmt->bindValue(1, $limit, PDO::PARAM_INT);
} else {
    $stmt = $pdo->prepare("SELECT id, service_report_no, work_order_no, company_name, form_date, status, created_at
                            FROM Installation_forms WHERE created_by = ? AND deleted_at IS NULL ORDER BY id DESC LIMIT ?");
    $stmt->bindValue(1, $user['id'], PDO::PARAM_INT);
    $stmt->bindValue(2, $limit, PDO::PARAM_INT);
}
$stmt->execute();

json_out(['success' => true, 'records' => $stmt->fetchAll()]);
