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

if ($classId <= 0) {
    jsonResponse(false, 'Invalid class id.');
}

try {
    $checkClass = $conn->prepare('SELECT classid FROM class WHERE classid = ? LIMIT 1');
    $checkClass->bind_param('i', $classId);
    $checkClass->execute();
    $classResult = $checkClass->get_result();
    if ($classResult->num_rows === 0) {
        $checkClass->close();
        jsonResponse(false, 'Class not found.');
    }
    $checkClass->close();

    $countStudent = $conn->prepare('SELECT COUNT(*) AS total FROM student WHERE classid = ?');
    $countStudent->bind_param('i', $classId);
    $countStudent->execute();
    $countResult = $countStudent->get_result();
    $totalStudents = 0;
    if ($row = $countResult->fetch_assoc()) {
        $totalStudents = (int)$row['total'];
    }
    $countStudent->close();

    if ($totalStudents > 0) {
        jsonResponse(false, 'Cannot delete class because students are still assigned to it.');
    }

    $delete = $conn->prepare('DELETE FROM class WHERE classid = ?');
    $delete->bind_param('i', $classId);
    if (!$delete->execute()) {
        $delete->close();
        jsonResponse(false, 'Failed to delete class.');
    }
    $delete->close();

    jsonResponse(true, 'Class deleted successfully.');
} catch (Throwable $e) {
    jsonResponse(false, $e->getMessage());
}
?>
