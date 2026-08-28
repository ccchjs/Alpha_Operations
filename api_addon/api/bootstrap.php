<?php
/**
 * api/bootstrap.php
 * Shared entrypoint for every api/*.php endpoint.
 * - Loads the existing config.php (same $pdo connection as the web app)
 * - Forces JSON responses
 * - Provides json_out(), json_fail(), require_auth(), read_json_body()
 *
 * This file does NOT start a PHP session and does NOT rely on cookies —
 * the Android app is stateless and authenticates every request with a
 * Bearer token (see auth_login.php).
 */

// The web app's config.php calls session_set_cookie_params() before
// session_start(); the API never calls session_start(), so that's harmless.
require_once __DIR__ . '/../config.php';

header('Content-Type: application/json; charset=utf-8');
// Allow the Android app (and any future client) to call this API from
// anywhere. Tighten this to your own domain if you want to lock it down.
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Authorization, Content-Type');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

function json_out(array $data, int $status = 200): void {
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function json_fail(string $message, int $status = 400): void {
    json_out(['success' => false, 'message' => $message], $status);
}

/** Reads either application/json body or regular POST (for multipart form-data endpoints). */
function read_json_body(): array {
    $raw = file_get_contents('php://input');
    if ($raw === '' || $raw === false) return $_POST;
    $decoded = json_decode($raw, true);
    return is_array($decoded) ? $decoded : $_POST;
}

/**
 * Validates the Bearer token from the Authorization header and returns
 * the authenticated user row (id, username, full_name, role). Ends the
 * request with 401 if missing/invalid/revoked.
 */
function require_auth(PDO $pdo): array {
    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? ($_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '');
    if (!$header && function_exists('apache_request_headers')) {
        $h = apache_request_headers();
        $header = $h['Authorization'] ?? $h['authorization'] ?? '';
    }
    if (!preg_match('/Bearer\s+(.+)/i', $header, $m)) {
        json_fail('Missing or malformed Authorization header.', 401);
    }
    $token = trim($m[1]);

    $stmt = $pdo->prepare(
        "SELECT u.id, u.username, u.full_name, u.role
         FROM api_tokens t
         JOIN users u ON u.id = t.user_id
         WHERE t.token = ? AND t.revoked_at IS NULL"
    );
    $stmt->execute([$token]);
    $user = $stmt->fetch();

    if (!$user) {
        json_fail('Invalid or expired token. Please log in again.', 401);
    }

    $pdo->prepare("UPDATE api_tokens SET last_used_at = NOW() WHERE token = ?")->execute([$token]);
    return $user;
}

/** Small helper: uploads a base64 data-URL (signature pad) to UNIT_PHOTO_DIR-sibling folder. */
function save_base64_png(string $dataUrl, string $prefix): ?string {
    if (!$dataUrl || strpos($dataUrl, 'base64,') === false) return null;
    [, $b64] = explode('base64,', $dataUrl, 2);
    $bytes = base64_decode($b64);
    if ($bytes === false) return null;
    $dir = __DIR__ . '/../uploads/signatures/';
    if (!is_dir($dir)) @mkdir($dir, 0775, true);
    $filename = $prefix . '_' . bin2hex(random_bytes(8)) . '.png';
    file_put_contents($dir . $filename, $bytes);
    return $filename;
}
