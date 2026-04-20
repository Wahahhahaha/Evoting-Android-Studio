<?php
// Jangan ada spasi atau baris kosong sebelum <?php
header('Content-Type: application/json; charset=utf-8');
error_reporting(E_ALL); // Aktifkan error sementara untuk debug
ini_set('display_errors', 0); // Tapi jangan tampilkan di output (agar JSON tidak rusak)

include 'Koneksi.php';

$username = $_POST['username'] ?? '';
$password = $_POST['password'] ?? '';

$response = array();

if (empty($username) || empty($password)) {
    $response['success'] = false;
    $response['message'] = "Username/Password kosong";
    echo json_encode($response);
    exit;
}

$username = mysqli_real_escape_string($conn, $username);
$sql = "SELECT * FROM users WHERE username = '$username'";
$result = mysqli_query($conn, $sql);

// ... (bagian atas kode sama)

if ($result && mysqli_num_rows($result) > 0) {
    $row = mysqli_fetch_assoc($result);
    $hash_di_db = $row['password'];

    // Gunakan password_verify untuk mengecek hash Bcrypt
    if (password_verify($password, $hash_di_db)) {
        $response['success'] = true;
        // Di SQL kamu, kolomnya adalah 'levelid'
        $response['level'] = (int)$row['levelid'];
        $response['userid'] = (int)$row['userid'];
        $response['message'] = "Login successful";

        if ((int)$row['levelid'] === 2) {
            $studentQuery = $conn->prepare('SELECT studentid, name FROM student WHERE userid = ? LIMIT 1');
            $studentQuery->bind_param('i', $row['userid']);
            $studentQuery->execute();
            $studentResult = $studentQuery->get_result();
            $student = $studentResult ? $studentResult->fetch_assoc() : null;

            if ($student) {
                $response['studentid'] = (int)$student['studentid'];
                $response['name'] = $student['name'];
            }
        }
    } else {
        $response['success'] = false;
        $response['message'] = "Password salah";
    }
} else {
    $response['success'] = false;
    $response['message'] = "Username not found";
}

echo json_encode($response);
?>
