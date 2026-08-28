<?php
/**
 * GET /api/pm_list.php?limit=50
 * Staff see only their own records; admin sees everyone's — same rule as
 * the web dashboard. Returns lightweight rows (no nested units/photos) for
 * a fast "My Records" list in the app; full detail stays on the web view
 * since the app doesn't support editing synced records.
 */
require_once __DIR__ . '/bootstrap.php';
$user = require_auth($pdo);

$limit = max(1, min(200, (int)($_GET['limit'] ?? 50)));

if ($user['role'] === 'admin') {
    $stmt = $pdo->prepare("SELECT id, service_report_no, work_order_no, company_name, form_date, status, created_at
                            FROM pm_forms WHERE deleted_at IS NULL ORDER BY id DESC LIMIT ?");
    $stmt->bindValue(1, $limit, PDO::PARAM_INT);
} else {
    $stmt = $pdo->prepare("SELECT id, service_report_no, work_order_no, company_name, form_date, status, created_at
                            FROM pm_forms WHERE created_by = ? AND deleted_at IS NULL ORDER BY id DESC LIMIT ?");
    $stmt->bindValue(1, $user['id'], PDO::PARAM_INT);
    $stmt->bindValue(2, $limit, PDO::PARAM_INT);
}
$stmt->execute();

json_out(['success' => true, 'records' => $stmt->fetchAll()]);
