<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';

$sql = 'SELECT classid, classname FROM class ORDER BY classname ASC';
$result = $conn->query($sql);
$data = [];

if ($result) {
    while ($row = $result->fetch_assoc()) {
        $data[] = $row;
    }
}

echo json_encode($data);
?>
