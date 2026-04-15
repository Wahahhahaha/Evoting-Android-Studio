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
        return [false, '', 'Upload foto gagal'];
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
        return [false, '', 'Gagal menyimpan foto'];
    }

    if ($oldPath !== '' && file_exists(__DIR__ . '/' . $oldPath)) {
        @unlink(__DIR__ . '/' . $oldPath);
    }

    return [true, 'uploads/' . $fileName, ''];
}

$response = ['success' => false, 'message' => 'Request tidak valid'];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $candidateid = (int)($_POST['candidateid'] ?? 0);
    $periodid = (int)($_POST['periodid'] ?? 0);
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

    if ($candidateid <= 0 || $periodid <= 0 || $presidentId <= 0 || $viceId <= 0 || $vision === '' || $mission === '' || $title === '' || $startdate === '' || $enddate === '') {
        $response['message'] = 'Data tidak lengkap';
        echo json_encode($response);
        exit;
    }

    if ($presidentId === $viceId) {
        $response['message'] = 'Ketua dan wakil harus berbeda';
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
        $response['message'] = 'Kandidat tidak ditemukan';
        echo json_encode($response);
        exit;
    }

    $checkUsed = $conn->prepare(
        'SELECT candidateid FROM candidate
         WHERE candidateid <> ?
           AND (
                COALESCE(president_studentid, studentid) IN (?, ?)
                OR COALESCE(vice_studentid, studentid) IN (?, ?)
           )
         LIMIT 1'
    );
    $checkUsed->bind_param('iiiii', $candidateid, $presidentId, $viceId, $presidentId, $viceId);
    $checkUsed->execute();
    $checkUsed->store_result();
    if ($checkUsed->num_rows > 0) {
        $response['message'] = 'Ketua atau wakil sudah dipakai pasangan lain';
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

    $conn->begin_transaction();

    try {
        $targetPeriodId = $periodid;

        $currentPeriodStmt = $conn->prepare('SELECT title, startdate, enddate FROM voting_period WHERE periodid = ? LIMIT 1');
        $currentPeriodStmt->bind_param('i', $periodid);
        $currentPeriodStmt->execute();
        $currentPeriod = $currentPeriodStmt->get_result()->fetch_assoc();
        if (!$currentPeriod) {
            throw new RuntimeException('Periode vote tidak ditemukan');
        }

        $isPeriodChanged = $currentPeriod['title'] !== $title
            || $currentPeriod['startdate'] !== $startdate
            || $currentPeriod['enddate'] !== $enddate;

        if ($isPeriodChanged) {
            $periodCandidateCountStmt = $conn->prepare('SELECT COUNT(*) AS total FROM candidate WHERE periodid = ?');
            $periodCandidateCountStmt->bind_param('i', $periodid);
            $periodCandidateCountStmt->execute();
            $periodCandidateCount = (int)($periodCandidateCountStmt->get_result()->fetch_assoc()['total'] ?? 0);

            if ($periodCandidateCount <= 1) {
                $updatePeriod = $conn->prepare('UPDATE voting_period SET title = ?, startdate = ?, enddate = ? WHERE periodid = ?');
                $updatePeriod->bind_param('sssi', $title, $startdate, $enddate, $periodid);
                $updatePeriod->execute();
            } else {
                $findPeriod = $conn->prepare('SELECT periodid FROM voting_period WHERE title = ? AND startdate = ? AND enddate = ? LIMIT 1');
                $findPeriod->bind_param('sss', $title, $startdate, $enddate);
                $findPeriod->execute();
                $foundPeriod = $findPeriod->get_result()->fetch_assoc();
                if ($foundPeriod) {
                    $targetPeriodId = (int)$foundPeriod['periodid'];
                } else {
                    $insertPeriod = $conn->prepare('INSERT INTO voting_period (title, startdate, enddate) VALUES (?, ?, ?)');
                    $insertPeriod->bind_param('sss', $title, $startdate, $enddate);
                    $insertPeriod->execute();
                    $targetPeriodId = (int)$conn->insert_id;
                }
            }
        }

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
            $targetPeriodId,
            $candidateid
        );
        $updateCandidate->execute();

        if ($targetPeriodId !== $periodid) {
            $oldCountStmt = $conn->prepare('SELECT COUNT(*) AS total FROM candidate WHERE periodid = ?');
            $oldCountStmt->bind_param('i', $periodid);
            $oldCountStmt->execute();
            $oldCount = (int)($oldCountStmt->get_result()->fetch_assoc()['total'] ?? 0);
            if ($oldCount === 0) {
                $deleteOldPeriod = $conn->prepare('DELETE FROM voting_period WHERE periodid = ?');
                $deleteOldPeriod->bind_param('i', $periodid);
                $deleteOldPeriod->execute();
            }
        }

        $conn->commit();
        $response = ['success' => true, 'message' => 'Pasangan kandidat berhasil diperbarui'];
    } catch (Throwable $e) {
        $conn->rollback();
        $response['message'] = 'Gagal memperbarui kandidat: ' . $e->getMessage();
    }
}

echo json_encode($response);

