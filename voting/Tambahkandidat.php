<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';
include 'CandidateSchema.php';

ensureCandidateSchema($conn);

function saveCandidatePhoto(array $file, string $prefix): array
{
    if (!isset($file['tmp_name']) || (int)($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK) {
        return [false, '', 'Candidate photo is required'];
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
        return [false, '', 'Photo format must be JPG, PNG, or WEBP'];
    }

    $uploadDir = __DIR__ . '/uploads/';
    if (!is_dir($uploadDir) && !mkdir($uploadDir, 0777, true) && !is_dir($uploadDir)) {
        return [false, '', 'Upload folder is unavailable'];
    }

    $fileName = $prefix . '_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $allowedMime[$mimeType];
    $targetPath = $uploadDir . $fileName;
    if (!move_uploaded_file($tmpPath, $targetPath)) {
        return [false, '', 'Failed to save photo file'];
    }

    return [true, 'uploads/' . $fileName, ''];
}

$response = ['success' => false, 'message' => 'Invalid request'];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $presidentId = (int)($_POST['presidentid'] ?? $_POST['studentid'] ?? 0);
    $viceId = (int)($_POST['viceid'] ?? 0);
    $periodid = (int)($_POST['periodid'] ?? 0);
    $vision = trim($_POST['vision'] ?? '');
    $mission = trim($_POST['mission'] ?? '');

    if ($viceId <= 0) {
        $viceId = $presidentId;
    }

    if ($presidentId <= 0 || $viceId <= 0 || $periodid <= 0 || $vision === '' || $mission === '') {
        $response['message'] = 'All fields are required';
        echo json_encode($response);
        exit;
    }

    if ($presidentId === $viceId) {
        $response['message'] = 'President and vice president must be different students';
        echo json_encode($response);
        exit;
    }

    $periodCheck = $conn->prepare('SELECT periodid FROM voting_period WHERE periodid = ? LIMIT 1');
    $periodCheck->bind_param('i', $periodid);
    $periodCheck->execute();
    $periodCheck->store_result();
    if ($periodCheck->num_rows === 0) {
        $response['message'] = 'Voting period not found';
        echo json_encode($response);
        exit;
    }

    $studentCheck = $conn->prepare('SELECT COUNT(*) AS total FROM student WHERE studentid IN (?, ?)');
    $studentCheck->bind_param('ii', $presidentId, $viceId);
    $studentCheck->execute();
    $studentTotal = (int)($studentCheck->get_result()->fetch_assoc()['total'] ?? 0);
    if ($studentTotal < 2) {
        $response['message'] = 'President/vice data not found';
        echo json_encode($response);
        exit;
    }

    $checkUsed = $conn->prepare(
        'SELECT candidateid FROM candidate
         WHERE periodid = ?
           AND (
                COALESCE(NULLIF(president_studentid, 0), studentid) IN (?, ?)
                OR COALESCE(NULLIF(vice_studentid, 0), studentid) IN (?, ?)
           )
         LIMIT 1'
    );
    $checkUsed->bind_param('iiiii', $periodid, $presidentId, $viceId, $presidentId, $viceId);
    $checkUsed->execute();
    $checkUsed->store_result();
    if ($checkUsed->num_rows > 0) {
        $response['message'] = 'President or vice is already used in this period';
        echo json_encode($response);
        exit;
    }

    $presidentFile = $_FILES['photo_president'] ?? $_FILES['photo'] ?? null;
    $viceFile = $_FILES['photo_vice'] ?? $_FILES['photo'] ?? null;
    if (!$presidentFile || !$viceFile) {
        $response['message'] = 'President and vice photos are required';
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

    try {
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

        $response = ['success' => true, 'message' => 'Candidate pair added successfully'];
    } catch (Throwable $e) {
        if ($presidentPicture !== '' && file_exists(__DIR__ . '/' . $presidentPicture)) {
            @unlink(__DIR__ . '/' . $presidentPicture);
        }
        if ($vicePicture !== '' && file_exists(__DIR__ . '/' . $vicePicture)) {
            @unlink(__DIR__ . '/' . $vicePicture);
        }
        $response['message'] = 'Failed to save candidate: ' . $e->getMessage();
    }
}

echo json_encode($response);
?>