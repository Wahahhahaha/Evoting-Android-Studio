<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';
include 'CandidateSchema.php';

ensureCandidateSchema($conn);

function updateCandidatePhoto(array $file, string $oldPath, string $prefix): array
{
    if (!isset($file['tmp_name']) || (int)($file['error'] ?? UPLOAD_ERR_NO_FILE) === UPLOAD_ERR_NO_FILE) {
        return [true, $oldPath, ''];
    }

    if ((int)($file['error'] ?? UPLOAD_ERR_OK) !== UPLOAD_ERR_OK) {
        return [false, '', 'Photo upload failed'];
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
        return [false, '', 'Failed to save photo'];
    }

    if ($oldPath !== '' && file_exists(__DIR__ . '/' . $oldPath)) {
        @unlink(__DIR__ . '/' . $oldPath);
    }

    return [true, 'uploads/' . $fileName, ''];
}

$response = ['success' => false, 'message' => 'Invalid request'];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $candidateid = (int)($_POST['candidateid'] ?? 0);
    $periodid = (int)($_POST['periodid'] ?? 0);
    $presidentId = (int)($_POST['presidentid'] ?? $_POST['studentid'] ?? 0);
    $viceId = (int)($_POST['viceid'] ?? 0);
    $vision = trim($_POST['vision'] ?? '');
    $mission = trim($_POST['mission'] ?? '');

    if ($viceId <= 0) {
        $viceId = $presidentId;
    }

    if ($candidateid <= 0 || $periodid <= 0 || $presidentId <= 0 || $viceId <= 0 || $vision === '' || $mission === '') {
        $response['message'] = 'Incomplete data';
        echo json_encode($response);
        exit;
    }

    if ($presidentId === $viceId) {
        $response['message'] = 'President and vice president must be different';
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

    $checkCandidate = $conn->prepare(
        'SELECT candidateid, picture, COALESCE(vice_picture, "") AS vice_picture
         FROM candidate
         WHERE candidateid = ?
         LIMIT 1'
    );
    $checkCandidate->bind_param('i', $candidateid);
    $checkCandidate->execute();
    $existing = $checkCandidate->get_result()->fetch_assoc();
    if (!$existing) {
        $response['message'] = 'Candidate not found';
        echo json_encode($response);
        exit;
    }

    $checkUsed = $conn->prepare(
        'SELECT candidateid FROM candidate
         WHERE candidateid <> ?
           AND periodid = ?
           AND (
                COALESCE(NULLIF(president_studentid, 0), studentid) IN (?, ?)
                OR COALESCE(NULLIF(vice_studentid, 0), studentid) IN (?, ?)
           )
         LIMIT 1'
    );
    $checkUsed->bind_param('iiiiii', $candidateid, $periodid, $presidentId, $viceId, $presidentId, $viceId);
    $checkUsed->execute();
    $checkUsed->store_result();
    if ($checkUsed->num_rows > 0) {
        $response['message'] = 'President or vice is already used in this period';
        echo json_encode($response);
        exit;
    }

    $presidentFile = $_FILES['photo_president'] ?? null;
    $viceFile = $_FILES['photo_vice'] ?? null;

    [$okPresident, $presidentPicture, $presidentError] = updateCandidatePhoto(
        $presidentFile ?? [],
        $existing['picture'] ?? '',
        'president'
    );
    if (!$okPresident) {
        $response['message'] = $presidentError;
        echo json_encode($response);
        exit;
    }

    [$okVice, $vicePicture, $viceError] = updateCandidatePhoto(
        $viceFile ?? [],
        $existing['vice_picture'] ?? '',
        'vice'
    );
    if (!$okVice) {
        $response['message'] = $viceError;
        echo json_encode($response);
        exit;
    }

    try {
        $updateCandidate = $conn->prepare(
            'UPDATE candidate
             SET studentid = ?, president_studentid = ?, vice_studentid = ?, picture = ?, vice_picture = ?, vision = ?, mission = ?, periodid = ?
             WHERE candidateid = ?'
        );
        $updateCandidate->bind_param(
            'iiissssii',
            $presidentId,
            $presidentId,
            $viceId,
            $presidentPicture,
            $vicePicture,
            $vision,
            $mission,
            $periodid,
            $candidateid
        );
        $updateCandidate->execute();

        $response = ['success' => true, 'message' => 'Candidate pair updated successfully'];
    } catch (Throwable $e) {
        $response['message'] = 'Failed to update candidate: ' . $e->getMessage();
    }
}

echo json_encode($response);
?>