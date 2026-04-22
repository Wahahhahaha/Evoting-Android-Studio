<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';

$response = ['success' => false, 'message' => 'Invalid request'];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $studentid = (int)($_POST['studentid'] ?? 0);
    $userid = (int)($_POST['userid'] ?? 0);
    $name = trim($_POST['name'] ?? '');
    $username = trim($_POST['username'] ?? '');
    $email = trim($_POST['email'] ?? '');
    $phonenumber = trim($_POST['phonenumber'] ?? '');
    $classid = (int)($_POST['classid'] ?? 0);

    if (
        $studentid <= 0 ||
        $userid <= 0 ||
        $name === '' ||
        $username === '' ||
        $email === '' ||
        $phonenumber === '' ||
        $classid <= 0
    ) {
        $response['message'] = 'Incomplete data';
        echo json_encode($response);
        exit;
    }

    $checkUser = $conn->prepare('SELECT userid FROM users WHERE username = ? AND userid <> ? LIMIT 1');
    $checkUser->bind_param('si', $username, $userid);
    $checkUser->execute();
    $checkUser->store_result();

    if ($checkUser->num_rows > 0) {
        $response['message'] = 'Username is already taken';
        echo json_encode($response);
        exit;
    }

    $conn->begin_transaction();

    try {
        $updateUser = $conn->prepare('UPDATE users SET username = ? WHERE userid = ?');
        $updateUser->bind_param('si', $username, $userid);
        $updateUser->execute();

        $updateStudent = $conn->prepare('UPDATE student SET name = ?, email = ?, phonenumber = ?, classid = ? WHERE studentid = ? AND userid = ?');
        $updateStudent->bind_param('sssiii', $name, $email, $phonenumber, $classid, $studentid, $userid);
        $updateStudent->execute();

        $conn->commit();
        $response = ['success' => true, 'message' => 'Student data updated successfully'];
    } catch (Throwable $e) {
        $conn->rollback();
        $response['message'] = 'Failed to update data: ' . $e->getMessage();
    }
}

echo json_encode($response);
?>
