<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';

$response = ['success' => false, 'message' => 'ID siswa tidak valid'];
$studentid = (int)($_POST['studentid'] ?? 0);

if ($studentid > 0) {
    $getUser = $conn->prepare('SELECT userid FROM student WHERE studentid = ? LIMIT 1');
    $getUser->bind_param('i', $studentid);
    $getUser->execute();
    $result = $getUser->get_result();
    $student = $result ? $result->fetch_assoc() : null;

    if ($student) {
        $userid = (int)$student['userid'];
        $conn->begin_transaction();

        try {
            $deleteStudent = $conn->prepare('DELETE FROM student WHERE studentid = ?');
            $deleteStudent->bind_param('i', $studentid);
            $deleteStudent->execute();

            $deleteUser = $conn->prepare('DELETE FROM users WHERE userid = ?');
            $deleteUser->bind_param('i', $userid);
            $deleteUser->execute();

            $conn->commit();
            $response = ['success' => true, 'status' => 1, 'message' => 'Data siswa berhasil dihapus'];
        } catch (Throwable $e) {
            $conn->rollback();
            $response['message'] = 'Gagal menghapus data: ' . $e->getMessage();
        }
    } else {
        $response['message'] = 'Data siswa tidak ditemukan';
    }
}

echo json_encode($response);
?>
