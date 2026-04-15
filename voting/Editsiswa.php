<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';

$response = ['success' => false, 'message' => 'Request tidak valid'];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $studentid = (int)($_POST['studentid'] ?? 0);
    $userid = (int)($_POST['userid'] ?? 0);
    $name = trim($_POST['name'] ?? '');
    $username = trim($_POST['username'] ?? '');
    $password = $_POST['password'] ?? '';
    $classid = (int)($_POST['classid'] ?? 0);

    if ($studentid <= 0 || $userid <= 0 || $name === '' || $username === '' || $classid <= 0) {
        $response['message'] = 'Data tidak lengkap';
        echo json_encode($response);
        exit;
    }

    $checkUser = $conn->prepare('SELECT userid FROM users WHERE username = ? AND userid <> ? LIMIT 1');
    $checkUser->bind_param('si', $username, $userid);
    $checkUser->execute();
    $checkUser->store_result();

    if ($checkUser->num_rows > 0) {
        $response['message'] = 'Username sudah dipakai';
        echo json_encode($response);
        exit;
    }

    $conn->begin_transaction();

    try {
        if ($password !== '') {
            $passwordHash = password_hash($password, PASSWORD_BCRYPT);
            $updateUser = $conn->prepare('UPDATE users SET username = ?, password = ? WHERE userid = ?');
            $updateUser->bind_param('ssi', $username, $passwordHash, $userid);
        } else {
            $updateUser = $conn->prepare('UPDATE users SET username = ? WHERE userid = ?');
            $updateUser->bind_param('si', $username, $userid);
        }
        $updateUser->execute();

        $updateStudent = $conn->prepare('UPDATE student SET name = ?, classid = ? WHERE studentid = ? AND userid = ?');
        $updateStudent->bind_param('siii', $name, $classid, $studentid, $userid);
        $updateStudent->execute();

        $conn->commit();
        $response = ['success' => true, 'message' => 'Data siswa berhasil diperbarui'];
    } catch (Throwable $e) {
        $conn->rollback();
        $response['message'] = 'Gagal memperbarui data: ' . $e->getMessage();
    }
}

echo json_encode($response);
?>
