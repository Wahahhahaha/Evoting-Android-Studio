<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';

$sql = "SELECT s.studentid, s.name, s.classid, c.classname, u.userid, u.username
        FROM student s
        INNER JOIN class c ON c.classid = s.classid
        INNER JOIN users u ON u.userid = s.userid
        ORDER BY s.studentid DESC";

$result = $conn->query($sql);
$data = [];

if ($result) {
    while ($row = $result->fetch_assoc()) {
        $data[] = $row;
    }
}

echo json_encode($data);
?>
