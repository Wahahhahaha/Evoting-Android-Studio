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

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respond(405, [
        'success' => false,
        'message' => 'Method harus POST'
    ]);
}

$rawInput = file_get_contents('php://input');
$jsonInput = json_decode($rawInput, true);
$input = is_array($jsonInput) ? $jsonInput : $_POST;

$requiredFields = [
    'periodid',
    'no_urut',
    'president_studentid',
    'vice_studentid',
    'president_picture',
    'vice_picture',
    'vision',
    'mission'
];

foreach ($requiredFields as $field) {
    if (!isset($input[$field]) || $input[$field] === '') {
        respond(422, [
            'success' => false,
            'message' => "Field wajib kosong: {$field}"
        ]);
    }
}

$periodId = (int)$input['periodid'];
$noUrut = (int)$input['no_urut'];
$presidentStudentId = (int)$input['president_studentid'];
$viceStudentId = (int)$input['vice_studentid'];
$presidentPicture = trim((string)$input['president_picture']);
$vicePicture = trim((string)$input['vice_picture']);
$vision = trim((string)$input['vision']);
$mission = trim((string)$input['mission']);

if ($periodId <= 0 || $noUrut <= 0 || $presidentStudentId <= 0 || $viceStudentId <= 0) {
    respond(422, [
        'success' => false,
        'message' => 'Nilai ID/no_urut harus lebih dari 0'
    ]);
}

if ($presidentStudentId === $viceStudentId) {
    respond(422, [
        'success' => false,
        'message' => 'Presiden dan Wakil tidak boleh student yang sama'
    ]);
}

try {
    $sql = "INSERT INTO candidate_paslon (
                periodid,
                no_urut,
                president_studentid,
                vice_studentid,
                president_picture,
                vice_picture,
                vision,
                mission
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param(
        'iiiissss',
        $periodId,
        $noUrut,
        $presidentStudentId,
        $viceStudentId,
        $presidentPicture,
        $vicePicture,
        $vision,
        $mission
    );
    $stmt->execute();

    respond(201, [
        'success' => true,
        'message' => 'Paslon berhasil ditambahkan',
        'data' => [
            'paslon_id' => $stmt->insert_id,
            'periodid' => $periodId,
            'no_urut' => $noUrut,
            'president_studentid' => $presidentStudentId,
            'vice_studentid' => $viceStudentId,
            'president_picture' => $presidentPicture,
            'vice_picture' => $vicePicture,
            'vision' => $vision,
            'mission' => $mission
        ]
    ]);
} catch (mysqli_sql_exception $e) {
    if ($e->getCode() === 1062) {
        respond(409, [
            'success' => false,
            'message' => 'Data duplikat: no_urut atau pasangan sudah ada di periode ini'
        ]);
    }

    respond(500, [
        'success' => false,
        'message' => 'Gagal menyimpan data paslon',
        'error' => $e->getMessage()
    ]);
}
?>