<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';

$response = ['success' => false, 'message' => 'Request tidak valid'];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $name = trim($_POST['name'] ?? '');
    $username = trim($_POST['username'] ?? '');
    $password = $_POST['password'] ?? '';
    $classid = (int)($_POST['classid'] ?? 0);

    if ($name === '' || $username === '' || $password === '' || $classid <= 0) {
        $response['message'] = 'Nama, username, password, dan kelas wajib diisi';
        echo json_encode($response);
        exit;
    }

    $checkUser = $conn->prepare('SELECT userid FROM users WHERE username = ? LIMIT 1');
    $checkUser->bind_param('s', $username);
    $checkUser->execute();
    $checkUser->store_result();

    if ($checkUser->num_rows > 0) {
        $response['message'] = 'Username sudah dipakai';
        echo json_encode($response);
        exit;
    }

    $checkClass = $conn->prepare('SELECT classid FROM class WHERE classid = ? LIMIT 1');
    $checkClass->bind_param('i', $classid);
    $checkClass->execute();
    $checkClass->store_result();

    if ($checkClass->num_rows === 0) {
        $response['message'] = 'Kelas tidak ditemukan';
        echo json_encode($response);
        exit;
    }

    $conn->begin_transaction();

    try {
        $passwordHash = password_hash($password, PASSWORD_BCRYPT);
        $levelid = 2;

        $insertUser = $conn->prepare('INSERT INTO users (username, password, levelid) VALUES (?, ?, ?)');
        $insertUser->bind_param('ssi', $username, $passwordHash, $levelid);
        $insertUser->execute();
        $userid = $conn->insert_id;

        $insertStudent = $conn->prepare('INSERT INTO student (name, classid, userid) VALUES (?, ?, ?)');
        $insertStudent->bind_param('sii', $name, $classid, $userid);
        $insertStudent->execute();

        $conn->commit();
        $response = ['success' => true, 'message' => 'Data siswa berhasil ditambahkan'];
    } catch (Throwable $e) {
        $conn->rollback();
        $response['message'] = 'Gagal menyimpan data: ' . $e->getMessage();
    }
}

echo json_encode($response);
?>
