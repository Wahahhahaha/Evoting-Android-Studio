<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);
$conn->set_charset('utf8mb4');

function respond(int $statusCode, array $payload): void
{
    http_response_code($statusCode);
    echo json_encode($payload, JSON_UNESCAPED_UNICODE);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    respond(405, [
        'success' => false,
        'message' => 'Method harus GET'
    ]);
}

$periodId = isset($_GET['periodid']) ? (int)$_GET['periodid'] : null;
$activeOnly = isset($_GET['active']) ? (int)$_GET['active'] : 0;

try {
    $params = [];
    $types = '';
    $where = [];

    if ($periodId !== null && $periodId > 0) {
        $where[] = 'cp.periodid = ?';
        $types .= 'i';
        $params[] = $periodId;
    }

    if ($activeOnly === 1) {
        $where[] = 'CURDATE() BETWEEN vp.startdate AND vp.enddate';
    }

    $whereSql = '';
    if (!empty($where)) {
        $whereSql = 'WHERE ' . implode(' AND ', $where);
    }

    $sql = "SELECT
                cp.paslon_id,
                cp.periodid,
                vp.title AS period_title,
                vp.startdate,
                vp.enddate,
                cp.no_urut,
                cp.president_studentid,
                p.name AS president_name,
                cp.president_picture,
                cp.vice_studentid,
                v.name AS vice_name,
                cp.vice_picture,
                cp.vision,
                cp.mission,
                cp.created_at,
                cp.updated_at
            FROM candidate_paslon cp
            INNER JOIN student p ON p.studentid = cp.president_studentid
            INNER JOIN student v ON v.studentid = cp.vice_studentid
            INNER JOIN voting_period vp ON vp.periodid = cp.periodid
            {$whereSql}
            ORDER BY cp.periodid DESC, cp.no_urut ASC, cp.paslon_id ASC";

    $stmt = $conn->prepare($sql);

    if (!empty($params)) {
        $stmt->bind_param($types, ...$params);
    }

    $stmt->execute();
    $result = $stmt->get_result();

    $rows = [];
    while ($row = $result->fetch_assoc()) {
        $rows[] = $row;
    }

    respond(200, [
        'success' => true,
        'count' => count($rows),
        'data' => $rows
    ]);
} catch (mysqli_sql_exception $e) {
    respond(500, [
        'success' => false,
        'message' => 'Gagal mengambil data paslon',
        'error' => $e->getMessage()
    ]);
}
?>