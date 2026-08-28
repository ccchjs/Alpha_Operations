<?php
/**
 * POST /api/pm_save.php   (multipart/form-data)
 *
 * Fields:
 *   data          = JSON string, see shape below
 *   photo_before_<unitIndex>[]  = image file(s), up to 2 per unit
 *   photo_after_<unitIndex>[]   = image file(s), up to 2 per unit
 *
 * This endpoint only CREATES new PM records (the app is for field data
 * entry, not editing old ones — that stays on the web dashboard). It is
 * idempotent: if the same client_uuid is submitted twice (e.g. WorkManager
 * retried after a timeout that actually succeeded server-side), the second
 * call just returns the original record instead of inserting a duplicate.
 *
 * data = {
 *   "client_uuid": "…",
 *   "company_id": 12 | null,
 *   "company_name": "…",
 *   "address": "…",
 *   "form_date": "YYYY-MM-DD",
 *   "work_order_no": "…" | null,
 *   "personnel": ["name2", "name3", …]   // personnel1 is forced server-side
 *   "units": [{
 *       "brand": "…", "model": "…", "ac_type": "…", "capacity": "…",
 *       "voltage_before": 220, "voltage_after": 220, … (12 reading fields),
 *       "checklist": [{"item_name": "…", "status": "check|x|", "remarks": "…"}]
 *   }],
 *   "findings": "…",
 *   "afi": ["…"], "afi_other": "…",
 *   "recommendation": ["…"], "recommendation_other": "…",
 *   "action_taken": ["…"], "action_taken_other": "…",
 *   "ali": ["…"], "ali_other": "…",
 *   "next_pm_date": "YYYY-MM-DD" | null,
 *   "customer_name": "…", "customer_position": "…",
 *   "customer_signature_date": "YYYY-MM-DD" | null,
 *   "customer_signature": "data:image/png;base64,…" | null
 * }
 *
 * NOTE — deliberately out of scope for the app (finish these on the web
 * dashboard if a specific job needs them): SM Store checklist variant,
 * Certification of Accomplishment (COA), and free-form "particulars".
 */
require_once __DIR__ . '/bootstrap.php';
$user = require_auth($pdo);
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_fail('POST only.', 405);

$data = json_decode($_POST['data'] ?? '', true);
if (!is_array($data)) json_fail('Missing or invalid "data" field.');

$clientUuid = trim($data['client_uuid'] ?? '');
if ($clientUuid === '') json_fail('client_uuid is required.');

// --- Idempotency check ---
$existing = $pdo->prepare("SELECT id, service_report_no, work_order_no FROM pm_forms WHERE client_uuid = ?");
$existing->execute([$clientUuid]);
if ($row = $existing->fetch()) {
    json_out([
        'success' => true,
        'already_existed' => true,
        'server_id' => (int)$row['id'],
        'service_report_no' => $row['service_report_no'],
        'work_order_no' => $row['work_order_no'],
    ]);
}

$num = fn($v) => ($v === '' || $v === null) ? null : $v;

function pm_pack_field($arr, $other) {
    $arr = is_array($arr) ? array_values(array_filter(array_map('trim', $arr), fn($v) => $v !== '')) : [];
    $other = trim((string)$other);
    return ['json' => $arr ? json_encode($arr, JSON_UNESCAPED_UNICODE) : null, 'other' => $other !== '' ? $other : null];
}

try {
    $pdo->beginTransaction();

    // --- Resolve / auto-create company ---
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
        // Staff picked/typed a company that hasn't synced yet — reuse by
        // name if it now exists, otherwise create it.
        $c = $pdo->prepare("SELECT id FROM companies WHERE name = ?");
        $c->execute([$companyName]);
        if ($row = $c->fetch()) {
            $companyId = (int)$row['id'];
        } else {
            $pdo->prepare("INSERT INTO companies (name, address) VALUES (?, ?)")
                ->execute([$companyName, $data['address'] ?? null]);
            $companyId = (int)$pdo->lastInsertId();
        }
    }
    if ($companyName === '') json_fail('Company is required.');

    $serviceReportNo = next_service_report_no($pdo);

    $afi = pm_pack_field($data['afi'] ?? [], $data['afi_other'] ?? '');
    $rec = pm_pack_field($data['recommendation'] ?? [], $data['recommendation_other'] ?? '');
    $act = pm_pack_field($data['action_taken'] ?? [], $data['action_taken_other'] ?? '');
    $ali = pm_pack_field($data['ali'] ?? [], $data['ali_other'] ?? '');

    $signatureDataUrl = trim($data['customer_signature'] ?? '');

    $stmt = $pdo->prepare("INSERT INTO pm_forms
        (client_uuid, service_report_no, work_order_no, company_id, company_name, address, form_date,
         personnel1, findings,
         afi, afi_other, recommendation, recommendation_other, action_taken, action_taken_other, ali, ali_other,
         pm_statement, next_pm_date, customer_name, customer_position, customer_signature_date, customer_signature,
         coa_type, coa_month_year, coa_date, coa_generic_text,
         created_by, status)
        VALUES (?,?,?,?,?,?,?, ?,?, ?,?,?,?,?,?,?,?, NULL,?,?,?,?,?, 'none',NULL,NULL,NULL, ?, 'draft')");
    $stmt->execute([
        $clientUuid,
        $serviceReportNo,
        trim($data['work_order_no'] ?? '') ?: null,
        $companyId,
        $companyName,
        trim($data['address'] ?? ''),
        $data['form_date'] ?? date('Y-m-d'),
        $user['full_name'],
        trim($data['findings'] ?? ''),
        $afi['json'], $afi['other'], $rec['json'], $rec['other'], $act['json'], $act['other'], $ali['json'], $ali['other'],
        $num($data['next_pm_date'] ?? null),
        trim($data['customer_name'] ?? ''),
        trim($data['customer_position'] ?? '') ?: null,
        $num($data['customer_signature_date'] ?? null),
        $signatureDataUrl ?: null,
        $user['id'],
    ]);
    $pmFormId = (int)$pdo->lastInsertId();

    // --- Personnel (2..10); personnel1 is always the logged-in staff ---
    $personnel = array_slice(array_values(array_filter(array_map('trim', $data['personnel'] ?? []), fn($n) => $n !== '')), 0, 9);
    if ($personnel) {
        $pStmt = $pdo->prepare("INSERT INTO pm_personnel (pm_form_id, name, sort_order) VALUES (?,?,?)");
        foreach ($personnel as $i => $name) $pStmt->execute([$pmFormId, $name, $i]);
    }

    // --- Units + checklist + photos ---
    $readingFields = ['voltage_before','voltage_after','current_before','current_after',
        'suction_pressure_before','suction_pressure_after','discharge_pressure_before','discharge_pressure_after',
        'temp_supply_before','temp_supply_after','temp_return_before','temp_return_after'];

    $unitStmt = $pdo->prepare("INSERT INTO pm_units
        (pm_form_id, client_uuid, brand, model, ac_type, capacity,
         voltage_before, voltage_after, current_before, current_after,
         suction_pressure_before, suction_pressure_after,
         discharge_pressure_before, discharge_pressure_after,
         temp_supply_before, temp_supply_after, temp_return_before, temp_return_after)
        VALUES (?,?,?,?,?,?, ?,?,?,?, ?,?, ?,?, ?,?,?,?)");
    $checklistStmt = $pdo->prepare("INSERT INTO pm_checklist_items (pm_form_id, pm_unit_id, item_name, status, remarks) VALUES (?,?,?,?,?)");
    $photoStmt = $pdo->prepare("INSERT INTO pm_photos (pm_form_id, pm_unit_id, client_uuid, photo, label, sort_order) VALUES (?,?,?,?,?,?)");

    $units = is_array($data['units'] ?? null) ? $data['units'] : [];
    $allowedExt = ['jpg','jpeg','png','gif','webp','jfif','heic','heif'];

    foreach ($units as $ui => $u) {
        $brand = trim($u['brand'] ?? '');
        $model = trim($u['model'] ?? '');
        $acType = trim($u['ac_type'] ?? '');
        $capacity = trim($u['capacity'] ?? '');
        if ($brand === '' && $model === '' && $acType === '' && $capacity === '') continue;

        $readingValues = array_map(fn($f) => $num($u[$f] ?? null), $readingFields);
        $unitStmt->execute(array_merge([$pmFormId, $u['client_unit_uuid'] ?? null, $brand, $model, $acType, $capacity], $readingValues));
        $pmUnitId = (int)$pdo->lastInsertId();

        foreach (($u['checklist'] ?? []) as $item) {
            $itemName = trim($item['item_name'] ?? '');
            if ($itemName === '') continue;
            $checklistStmt->execute([$pmFormId, $pmUnitId, $itemName, $item['status'] ?? '', trim($item['remarks'] ?? '')]);
        }

        foreach (['before', 'after'] as $label) {
            $fieldName = "photo_{$label}_{$ui}";
            if (empty($_FILES[$fieldName])) continue;
            $files = $_FILES[$fieldName];
            $count = is_array($files['error']) ? count($files['error']) : 0;
            for ($i = 0; $i < min($count, 2); $i++) {
                if ($files['error'][$i] !== UPLOAD_ERR_OK) continue;
                $ext = strtolower(pathinfo($files['name'][$i], PATHINFO_EXTENSION));
                if (!in_array($ext, $allowedExt, true) || !is_uploaded_file($files['tmp_name'][$i])) continue;
                $safeName = "pm_{$pmFormId}_unit{$pmUnitId}_{$label}_" . uniqid() . ".{$ext}";
                if (move_uploaded_file($files['tmp_name'][$i], UNIT_PHOTO_DIR . $safeName)) {
                    $photoStmt->execute([$pmFormId, $pmUnitId, null, $safeName, $label, $i]);
                }
            }
        }
    }

    $pdo->commit();

    json_out([
        'success' => true,
        'server_id' => $pmFormId,
        'service_report_no' => $serviceReportNo,
        'work_order_no' => trim($data['work_order_no'] ?? '') ?: null,
    ]);
} catch (Throwable $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    error_log('[api/pm_save.php] ' . $e->getMessage());
    json_fail('Server error while saving the PM form. Nothing was saved — safe to retry.', 500);
}
