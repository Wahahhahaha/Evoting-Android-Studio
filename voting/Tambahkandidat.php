<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';
include 'CandidateSchema.php';

ensureCandidateSchema($conn);

function saveCandidatePhoto(array $file, string $prefix): array
{
    if (!isset($file['tmp_name']) || (int)($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK) {
        return [false, '', 'Foto kandidat wajib dipilih'];
    }

    $tmpPath = $file['tmp_name'];
    $finfo = new finfo(FILEINFO_MIME_TYPE);
    $mimeType = $finfo->file($tmpPath);
    $allowedMime = [
        'image/jpeg' => 'jpg',
        'image/png' => 'png',
        'image/webp' => 'webp'
    ];

    if (!isset($allowedMime[$mimeType])) {
        return [false, '', 'Format foto harus JPG, PNG, atau WEBP'];
    }

    $uploadDir = __DIR__ . '/uploads/';
    if (!is_dir($uploadDir) && !mkdir($uploadDir, 0777, true) && !is_dir($uploadDir)) {
        return [false, '', 'Folder upload tidak tersedia'];
    }

    $fileName = $prefix . '_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $allowedMime[$mimeType];
    $targetPath = $uploadDir . $fileName;
    if (!move_uploaded_file($tmpPath, $targetPath)) {
        return [false, '', 'Gagal menyimpan file foto'];
    }

    return [true, 'uploads/' . $fileName, ''];
}

$response = ['success' => false, 'message' => 'Request tidak valid'];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $presidentId = (int)($_POST['presidentid'] ?? $_POST['studentid'] ?? 0);
    $viceId = (int)($_POST['viceid'] ?? 0);
    $vision = trim($_POST['vision'] ?? '');
    $mission = trim($_POST['mission'] ?? '');
    $title = trim($_POST['title'] ?? '');
    $startdate = trim($_POST['startdate'] ?? '');
    $enddate = trim($_POST['enddate'] ?? '');

    if ($viceId <= 0) {
        $viceId = $presidentId;
    }

    if ($presidentId <= 0 || $viceId <= 0 || $vision === '' || $mission === '' || $title === '' || $startdate === '' || $enddate === '') {
        $response['message'] = 'Semua field wajib diisi';
        echo json_encode($response);
        exit;
    }

    if ($presidentId === $viceId) {
        $response['message'] = 'Ketua dan wakil harus siswa yang berbeda';
        echo json_encode($response);
        exit;
    }

    $startTs = strtotime($startdate);
    $endTs = strtotime($enddate);
    if (!$startTs || !$endTs || $startTs > $endTs) {
        $response['message'] = 'Tanggal periode tidak valid';
        echo json_encode($response);
        exit;
    }

    $studentCheck = $conn->prepare('SELECT COUNT(*) AS total FROM student WHERE studentid IN (?, ?)');
    $studentCheck->bind_param('ii', $presidentId, $viceId);
    $studentCheck->execute();
    $studentTotal = (int)($studentCheck->get_result()->fetch_assoc()['total'] ?? 0);
    if ($studentTotal < 2) {
        $response['message'] = 'Data ketua/wakil tidak ditemukan';
        echo json_encode($response);
        exit;
    }

    $checkUsed = $conn->prepare(
        'SELECT candidateid FROM candidate
         WHERE COALESCE(president_studentid, studentid) IN (?, ?)
            OR COALESCE(vice_studentid, studentid) IN (?, ?)
         LIMIT 1'
    );
    $checkUsed->bind_param('iiii', $presidentId, $viceId, $presidentId, $viceId);
    $checkUsed->execute();
    $checkUsed->store_result();
    if ($checkUsed->num_rows > 0) {
        $response['message'] = 'Ketua atau wakil sudah terdaftar di pasangan lain';
        echo json_encode($response);
        exit;
    }

    $presidentFile = $_FILES['photo_president'] ?? $_FILES['photo'] ?? null;
    $viceFile = $_FILES['photo_vice'] ?? $_FILES['photo'] ?? null;
    if (!$presidentFile || !$viceFile) {
        $response['message'] = 'Foto ketua dan wakil wajib dipilih';
        echo json_encode($response);
        exit;
    }

    [$okPresident, $presidentPicture, $presidentError] = saveCandidatePhoto($presidentFile, 'president');
    if (!$okPresident) {
        $response['message'] = $presidentError;
        echo json_encode($response);
        exit;
    }

    [$okVice, $vicePicture, $viceError] = saveCandidatePhoto($viceFile, 'vice');
    if (!$okVice) {
        if ($presidentPicture !== '' && file_exists(__DIR__ . '/' . $presidentPicture)) {
            @unlink(__DIR__ . '/' . $presidentPicture);
        }
        $response['message'] = $viceError;
        echo json_encode($response);
        exit;
    }

    $conn->begin_transaction();

    try {
        $periodid = 0;
        $findPeriod = $conn->prepare('SELECT periodid FROM voting_period WHERE title = ? AND startdate = ? AND enddate = ? LIMIT 1');
        $findPeriod->bind_param('sss', $title, $startdate, $enddate);
        $findPeriod->execute();
        $period = $findPeriod->get_result()->fetch_assoc();
        if ($period) {
            $periodid = (int)$period['periodid'];
        } else {
            $insertPeriod = $conn->prepare('INSERT INTO voting_period (title, startdate, enddate) VALUES (?, ?, ?)');
            $insertPeriod->bind_param('sss', $title, $startdate, $enddate);
            $insertPeriod->execute();
            $periodid = (int)$conn->insert_id;
        }

        $insertCandidate = $conn->prepare(
            'INSERT INTO candidate (picture, vice_picture, studentid, president_studentid, vice_studentid, vision, mission, periodid)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $insertCandidate->bind_param(
            'ssiiissi',
            $presidentPicture,
            $vicePicture,
            $presidentId,
            $presidentId,
            $viceId,
            $vision,
            $mission,
            $periodid
        );
        $insertCandidate->execute();

        $conn->commit();
        $response = ['success' => true, 'message' => 'Pasangan kandidat berhasil ditambahkan'];
    } catch (Throwable $e) {
        $conn->rollback();
        if ($presidentPicture !== '' && file_exists(__DIR__ . '/' . $presidentPicture)) {
            @unlink(__DIR__ . '/' . $presidentPicture);
        }
        if ($vicePicture !== '' && file_exists(__DIR__ . '/' . $vicePicture)) {
            @unlink(__DIR__ . '/' . $vicePicture);
        }
        $response['message'] = 'Gagal menyimpan kandidat: ' . $e->getMessage();
    }
}

echo json_encode($response);

