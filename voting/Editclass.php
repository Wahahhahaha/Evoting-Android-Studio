<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';

function jsonResponse(bool $success, string $message): void {
    echo json_encode([
        'success' => $success,
        'message' => $message
    ]);
    exit;
}

$classId = (int)($_POST['classid'] ?? 0);
$className = trim($_POST['classname'] ?? '');
$majorName = trim($_POST['jurusan'] ?? '');
$batchName = trim($_POST['angkatan'] ?? '');

if ($classId <= 0 || $className === '' || $majorName === '' || $batchName === '') {
    jsonResponse(false, 'Incomplete class data.');
}

$conn->begin_transaction();

try {
    $classCheck = $conn->prepare('SELECT classid FROM class WHERE classid = ? LIMIT 1');
    $classCheck->bind_param('i', $classId);
    $classCheck->execute();
    $classResult = $classCheck->get_result();
    if ($classResult->num_rows === 0) {
        throw new RuntimeException('Class not found.');
    }
    $classCheck->close();

    $majorId = 0;
    $majorSelect = $conn->prepare('SELECT jurusanid FROM jurusan WHERE nama = ? LIMIT 1');
    $majorSelect->bind_param('s', $majorName);
    $majorSelect->execute();
    $majorResult = $majorSelect->get_result();
    if ($row = $majorResult->fetch_assoc()) {
        $majorId = (int)$row['jurusanid'];
    }
    $majorSelect->close();

    if ($majorId <= 0) {
        $majorInsert = $conn->prepare('INSERT INTO jurusan (nama) VALUES (?)');
        $majorInsert->bind_param('s', $majorName);
        if (!$majorInsert->execute()) {
            throw new RuntimeException('Failed to save major.');
        }
        $majorId = (int)$majorInsert->insert_id;
        $majorInsert->close();
    }

    $batchId = 0;
    $batchSelect = $conn->prepare('SELECT angkatanid FROM angkatan WHERE nama = ? LIMIT 1');
    $batchSelect->bind_param('s', $batchName);
    $batchSelect->execute();
    $batchResult = $batchSelect->get_result();
    if ($row = $batchResult->fetch_assoc()) {
        $batchId = (int)$row['angkatanid'];
    }
    $batchSelect->close();

    if ($batchId <= 0) {
        $batchInsert = $conn->prepare('INSERT INTO angkatan (nama) VALUES (?)');
        $batchInsert->bind_param('s', $batchName);
        if (!$batchInsert->execute()) {
            throw new RuntimeException('Failed to save batch.');
        }
        $batchId = (int)$batchInsert->insert_id;
        $batchInsert->close();
    }

    $update = $conn->prepare('UPDATE class SET classname = ?, jurusanid = ?, angkatanid = ? WHERE classid = ?');
    $update->bind_param('siii', $className, $majorId, $batchId, $classId);
    if (!$update->execute()) {
        throw new RuntimeException('Failed to update class.');
    }
    $update->close();

    $conn->commit();
    jsonResponse(true, 'Class updated successfully.');
} catch (Throwable $e) {
    $conn->rollback();
    jsonResponse(false, $e->getMessage());
}
?>
