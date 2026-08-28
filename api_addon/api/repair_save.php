<?php
/**
 * POST /api/repair_save.php   (multipart/form-data)
 * Same contract as pm_save.php, with these differences to match
 * Repair_forms / Repair_units / Repair_photos / Repair_personnel:
 *   - units have "location" and "serial_number" instead of a checklist
 *   - afi/recommendation/action_taken/ali are stored as single combined
 *     TEXT columns (checked options + "other", newline-joined) instead of
 *     JSON + separate "other" column
 *   - no next_pm_date
 * Deliberately out of scope: COA (Certification of Accomplishment) and
 * the free-form "particulars" sub-fields — finish those on the web if a
 * specific job needs them.
 */
require_once __DIR__ . '/bootstrap.php';
$user = require_auth($pdo);
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_fail('POST only.', 405);

$data = json_decode($_POST['data'] ?? '', true);
if (!is_array($data)) json_fail('Missing or invalid "data" field.');

$clientUuid = trim($data['client_uuid'] ?? '');
if ($clientUuid === '') json_fail('client_uuid is required.');

$existing = $pdo->prepare("SELECT id, service_report_no, work_order_no FROM Repair_forms WHERE client_uuid = ?");
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

function repair_combine_options($checked, $other) {
    $items = is_array($checked) ? array_values(array_filter(array_map('trim', $checked), fn($v) => $v !== '')) : [];
    $other = trim((string)$other);
    if ($other !== '') $items[] = $other;
    return implode("\n", $items);
}

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
    if ($companyName === '') json_fail('Company is required.');

    $serviceReportNo = next_service_report_no($pdo);

    $afi = repair_combine_options($data['afi'] ?? [], $data['afi_other'] ?? '');
    $rec = repair_combine_options($data['recommendation'] ?? [], $data['recommendation_other'] ?? '');
    $act = repair_combine_options($data['action_taken'] ?? [], $data['action_taken_other'] ?? '');
    $ali = repair_combine_options($data['ali'] ?? [], $data['ali_other'] ?? '');

    $findings = trim(implode("\n\n", array_filter([
        $afi !== '' ? "A.F.I.:\n{$afi}" : '',
        $rec !== '' ? "RECOMMENDATION:\n{$rec}" : '',
        $act !== '' ? "ACTION TAKEN:\n{$act}" : '',
        $ali !== '' ? "A.L.I.:\n{$ali}" : '',
    ])));
    if ($findings === '') $findings = trim($data['findings'] ?? '');

    $signatureDataUrl = trim($data['customer_signature'] ?? '');

    $stmt = $pdo->prepare("INSERT INTO Repair_forms
        (client_uuid, service_report_no, work_order_no, company_id, company_name, address, form_date,
         personnel1, findings, afi, recommendation, action_taken, ali,
         customer_name, customer_position, customer_signature_date, customer_signature,
         coa_type, coa_date, coa_generic_text,
         created_by, status)
        VALUES (?,?,?,?,?,?,?, ?,?,?,?,?,?, ?,?,?,?, 'none',NULL,NULL, ?, 'draft')");
    $stmt->execute([
        $clientUuid, $serviceReportNo,
        trim($data['work_order_no'] ?? '') ?: null,
        $companyId, $companyName,
        trim($data['address'] ?? ''),
        $data['form_date'] ?? date('Y-m-d'),
        $user['full_name'],
        $findings, $afi, $rec, $act, $ali,
        trim($data['customer_name'] ?? ''),
        trim($data['customer_position'] ?? '') ?: null,
        $num($data['customer_signature_date'] ?? null),
        $signatureDataUrl ?: null,
        $user['id'],
    ]);
    $repairFormId = (int)$pdo->lastInsertId();

    $personnel = array_slice(array_values(array_filter(array_map('trim', $data['personnel'] ?? []), fn($n) => $n !== '')), 0, 9);
    if ($personnel) {
        $pStmt = $pdo->prepare("INSERT INTO Repair_personnel (Repair_form_id, name, sort_order) VALUES (?,?,?)");
        foreach ($personnel as $i => $name) $pStmt->execute([$repairFormId, $name, $i]);
    }

    $readingFields = ['voltage_before','voltage_after','current_before','current_after',
        'suction_pressure_before','suction_pressure_after','discharge_pressure_before','discharge_pressure_after',
        'temp_supply_before','temp_supply_after','temp_return_before','temp_return_after'];

    $unitStmt = $pdo->prepare("INSERT INTO Repair_units
        (Repair_form_id, location, brand, serial_number, model, ac_type, capacity,
         voltage_before, voltage_after, current_before, current_after,
         suction_pressure_before, suction_pressure_after,
         discharge_pressure_before, discharge_pressure_after,
         temp_supply_before, temp_supply_after, temp_return_before, temp_return_after,
         particulars_item, particulars_temp_before, particulars_temp_after, particulars_status)
        VALUES (?,?,?,?,?,?,?, ?,?,?,?, ?,?, ?,?, ?,?,?,?, NULL,NULL,NULL,NULL)");
    $photoStmt = $pdo->prepare("INSERT INTO Repair_photos (Repair_form_id, Repair_unit_id, photo, label, sort_order) VALUES (?,?,?,?,?)");

    $units = is_array($data['units'] ?? null) ? $data['units'] : [];
    $allowedExt = ['jpg','jpeg','png','gif','webp','jfif','heic','heif'];

    foreach ($units as $ui => $u) {
        $location = trim($u['location'] ?? '');
        $brand = trim($u['brand'] ?? '');
        $serial = trim($u['serial_number'] ?? '');
        $model = trim($u['model'] ?? '');
        $acType = trim($u['ac_type'] ?? '');
        $capacity = trim($u['capacity'] ?? '');
        if ($location === '' && $brand === '' && $serial === '' && $model === '' && $acType === '' && $capacity === '') continue;

        $readingValues = array_map(fn($f) => $num($u[$f] ?? null), $readingFields);
        $unitStmt->execute(array_merge([$repairFormId, $num($location), $brand, $num($serial), $model, $acType, $capacity], $readingValues));
        $repairUnitId = (int)$pdo->lastInsertId();

        foreach (['before', 'after'] as $label) {
            $fieldName = "photo_{$label}_{$ui}";
            if (empty($_FILES[$fieldName])) continue;
            $files = $_FILES[$fieldName];
            $count = is_array($files['error']) ? count($files['error']) : 0;
            for ($i = 0; $i < min($count, 2); $i++) {
                if ($files['error'][$i] !== UPLOAD_ERR_OK) continue;
                $ext = strtolower(pathinfo($files['name'][$i], PATHINFO_EXTENSION));
                if (!in_array($ext, $allowedExt, true) || !is_uploaded_file($files['tmp_name'][$i])) continue;
                $safeName = "repair_{$repairFormId}_unit{$repairUnitId}_{$label}_" . uniqid() . ".{$ext}";
                if (move_uploaded_file($files['tmp_name'][$i], UNIT_PHOTO_DIR . $safeName)) {
                    $photoStmt->execute([$repairFormId, $repairUnitId, $safeName, $label, $i]);
                }
            }
        }
    }

    $pdo->commit();

    json_out([
        'success' => true,
        'server_id' => $repairFormId,
        'service_report_no' => $serviceReportNo,
        'work_order_no' => trim($data['work_order_no'] ?? '') ?: null,
    ]);
} catch (Throwable $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    error_log('[api/repair_save.php] ' . $e->getMessage());
    json_fail('Server error while saving the Repair form. Nothing was saved — safe to retry.', 500);
}
