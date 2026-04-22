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

$className = trim($_POST['classname'] ?? '');
$majorName = trim($_POST['jurusan'] ?? '');
$batchName = trim($_POST['angkatan'] ?? '');

if ($className === '' || $majorName === '' || $batchName === '') {
    jsonResponse(false, 'Incomplete class data.');
}

$conn->begin_transaction();

try {
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

    $duplicateCheck = $conn->prepare('SELECT classid FROM class WHERE classname = ? AND jurusanid = ? AND angkatanid = ? LIMIT 1');
    $duplicateCheck->bind_param('sii', $className, $majorId, $batchId);
    $duplicateCheck->execute();
    $duplicateResult = $duplicateCheck->get_result();
    if ($duplicateResult->num_rows > 0) {
        $duplicateCheck->close();
        throw new RuntimeException('Class already exists.');
    }
    $duplicateCheck->close();

    $insertClass = $conn->prepare('INSERT INTO class (classname, jurusanid, angkatanid) VALUES (?, ?, ?)');
    $insertClass->bind_param('sii', $className, $majorId, $batchId);
    if (!$insertClass->execute()) {
        throw new RuntimeException('Failed to add class.');
    }
    $insertClass->close();

    $conn->commit();
    jsonResponse(true, 'Class added successfully.');
} catch (Throwable $e) {
    $conn->rollback();
    jsonResponse(false, $e->getMessage());
}
?>
