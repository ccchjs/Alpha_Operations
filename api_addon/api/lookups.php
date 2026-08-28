<?php
/**
 * GET /api/lookups.php
 * Returns everything the app needs to cache LOCALLY so staff can open a
 * brand-new PM/Repair/Installation form with zero signal: companies list,
 * AC brand/type/capacity option lists, both checklists, and the repair
 * finding-option pick-lists (A.F.I. / Recommendation / Action Taken / A.L.I.).
 *
 * The app should call this once on login (and pull-to-refresh occasionally
 * while online) and store the result in Room. Nothing here changes often.
 */
require_once __DIR__ . '/bootstrap.php';
$user = require_auth($pdo);

$companies = $pdo->query("SELECT id, name, address, type FROM companies ORDER BY name ASC")->fetchAll();

$findingOptions = ['afi' => [], 'recommendation' => [], 'action_taken' => [], 'ali' => []];
try {
    $rows = $pdo->query("SELECT field_key, option_text FROM repair_finding_options ORDER BY field_key, sort_order, id")->fetchAll();
    foreach ($rows as $r) {
        if (isset($findingOptions[$r['field_key']])) {
            $findingOptions[$r['field_key']][] = $r['option_text'];
        }
    }
} catch (Exception $e) {
    // Table may not exist yet on older installs — app just falls back to free text.
}

json_out([
    'success' => true,
    'companies' => $companies,
    'ac_brands' => AC_BRANDS,
    'ac_types' => AC_TYPES,
    'ac_capacities' => AC_CAPACITIES,
    'reading_suggestions' => READING_SUGGESTIONS,
    'pm_checklist_items' => PM_CHECKLIST_ITEMS,
    'repair_checklist_items' => REPAIR_CHECKLIST_ITEMS,
    'repair_finding_fields' => REPAIR_FINDING_FIELDS,
    'repair_finding_options' => $findingOptions,
]);
