<?php
/**
 * POST /api/installation_save.php  (multipart/form-data, photos optional)
 * Matches Installation_forms / Installation_personnel / Installation_units.
 * Deliberately out of scope: COA and "Start-Up Certificates" sub-form
 * (28 technical fields per AC unit, admin/engineer-filled after startup —
 * finish those on the web dashboard).
 */
require_once __DIR__ . '/bootstrap.php';
$user = require_auth($pdo);
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_fail('POST only.', 405);

$data = json_decode($_POST['data'] ?? '', true);
if (!is_array($data)) json_fail('Missing or invalid "data" field.');

$clientUuid = trim($data['client_uuid'] ?? '');
if ($clientUuid === '') json_fail('client_uuid is required.');

$existing = $pdo->prepare("SELECT id, service_report_no, work_order_no FROM Installation_forms WHERE client_uuid = ?");
$existing->execute([$clientUuid]);
if ($row = $existing->fetch()) {
    json_out([
        'success' => true, 'already_existed' => true,
        'server_id' => (int)$row['id'],
        'service_report_no' => $row['service_report_no'],
        'work_order_no' => $row['work_order_no'],
    ]);
}

$num = fn($v) => ($v === '' || $v === null) ? null : $v;

try {
    $pdo->beginTransaction();

    $companyId = $num($data['company_id'] ?? null);
    $companyName = trim($data['company_name'] ?? '');
    if ($companyId) {
        $c = $pdo->prepare("SELECT id, name FROM companies WHERE id = ?");
        $c->execute([$companyId]);
        $companyRow = $c->fetch();
        if ($companyRow) $companyName = $companyRow['name'];
        else $companyId = null;
    }
    if (!$companyId && $companyName !== '') {
        $c = $pdo->prepare("SELECT id FROM companies WHERE name = ?");
        $c->execute([$companyName]);
        if ($row = $c->fetch()) {
            $companyId = (int)$row['id'];
        } else {
            $pdo->prepare("INSERT INTO companies (name, address) VALUES (?, ?)")->execute([$companyName, $data['address'] ?? null]);
            $companyId = (int)$pdo->lastInsertId();
        }
    }
    if (!$companyId || $companyName === '') json_fail('Company is required.');
    if (empty($data['form_date'])) json_fail('Date is required.');

    // Shared plain sequence, same series as PM/Repair/Checkup.
    $serviceReportNo = next_service_report_no($pdo);

    $signatureDataUrl = trim($data['customer_signature'] ?? '');

    $stmt = $pdo->prepare("INSERT INTO Installation_forms
        (client_uuid, service_report_no, work_order_no, company_id, company_name, address, form_date,
         personnel1, pm_activity,
         coa_type, coa_date, coa_generic_text,
         customer_name, customer_position, customer_signature_date, customer_signature,
         status, created_by, created_at)
        VALUES (?,?,?,?,?,?,?, ?,?, 'none',NULL,NULL, ?,?,?,?, 'draft', ?, NOW())");
    $stmt->execute([
        $clientUuid, $serviceReportNo,
        trim($data['work_order_no'] ?? '') ?: null,
        $companyId, $companyName,
        trim($data['address'] ?? '') ?: null,
        $data['form_date'],
        $user['full_name'],
        trim($data['pm_activity'] ?? '') ?: null,
        trim($data['customer_name'] ?? '') ?: null,
        trim($data['customer_position'] ?? '') ?: null,
        $num($data['customer_signature_date'] ?? null),
        $signatureDataUrl ?: null,
        $user['id'],
    ]);
    $installationId = (int)$pdo->lastInsertId();

    $personnel = array_slice(array_values(array_filter(array_map('trim', $data['personnel'] ?? []), fn($n) => $n !== '')), 0, 9);
    if ($personnel) {
        $pStmt = $pdo->prepare("INSERT INTO Installation_personnel (Installation_form_id, name, sort_order) VALUES (?,?,?)");
        foreach ($personnel as $i => $name) $pStmt->execute([$installationId, $name, $i]);
    }

    $units = is_array($data['units'] ?? null) ? $data['units'] : [];
    if ($units) {
        $unitStmt = $pdo->prepare("INSERT INTO Installation_units (Installation_form_id, quantity, ac_type, brand, model, capacity) VALUES (?,?,?,?,?,?)");
        foreach ($units as $u) {
            $qty = isset($u['quantity']) && $u['quantity'] !== '' ? (int)$u['quantity'] : 1;
            $acType = trim($u['ac_type'] ?? '');
            $brand = trim($u['brand'] ?? '');
            $model = trim($u['model'] ?? '');
            $capacity = trim($u['capacity'] ?? '');
            if ($acType === '' && $brand === '' && $model === '' && $capacity === '') continue;
            $unitStmt->execute([$installationId, $qty, $acType ?: null, $brand ?: null, $model ?: null, $capacity ?: null]);
        }
    }

    // Optional site photos (not tied to a specific unit — installation
    // photos are usually "before site" / "after install" shots).
    $allowedExt = ['jpg','jpeg','png','gif','webp','jfif','heic','heif'];
    foreach (['before', 'after'] as $label) {
        $fieldName = "photo_{$label}_0";
        if (empty($_FILES[$fieldName])) continue;
        // Installation_units has no dedicated photos table in the current
        // schema — photos are stored the same way as PM/Repair via a
        // shared uploads folder; if you later add an Installation_photos
        // table, insert its row here instead of only moving the file.
        $files = $_FILES[$fieldName];
        $count = is_array($files['error']) ? count($files['error']) : 0;
        for ($i = 0; $i < min($count, 4); $i++) {
            if ($files['error'][$i] !== UPLOAD_ERR_OK) continue;
            $ext = strtolower(pathinfo($files['name'][$i], PATHINFO_EXTENSION));
            if (!in_array($ext, $allowedExt, true) || !is_uploaded_file($files['tmp_name'][$i])) continue;
            $safeName = "install_{$installationId}_{$label}_" . uniqid() . ".{$ext}";
            move_uploaded_file($files['tmp_name'][$i], UNIT_PHOTO_DIR . $safeName);
        }
    }

    $pdo->commit();

    json_out([
        'success' => true,
        'server_id' => $installationId,
        'service_report_no' => $serviceReportNo,
        'work_order_no' => trim($data['work_order_no'] ?? '') ?: null,
    ]);
} catch (Throwable $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    error_log('[api/installation_save.php] ' . $e->getMessage());
    json_fail('Server error while saving the Installation record. Nothing was saved — safe to retry.', 500);
}
