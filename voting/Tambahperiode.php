<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';

$response = ['success' => false, 'message' => 'Invalid request'];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $title = trim($_POST['title'] ?? '');
    $startdate = trim($_POST['startdate'] ?? '');
    $enddate = trim($_POST['enddate'] ?? '');

    if ($title === '' || $startdate === '' || $enddate === '') {
        $response['message'] = 'All fields are required';
        echo json_encode($response);
        exit;
    }

    $startTs = strtotime($startdate);
    $endTs = strtotime($enddate);
    if (!$startTs || !$endTs || $startTs > $endTs) {
        $response['message'] = 'Invalid period date range';
        echo json_encode($response);
        exit;
    }

    $checkPeriod = $conn->prepare('SELECT periodid FROM voting_period WHERE title = ? AND startdate = ? AND enddate = ? LIMIT 1');
    $checkPeriod->bind_param('sss', $title, $startdate, $enddate);
    $checkPeriod->execute();
    $checkPeriod->store_result();

    if ($checkPeriod->num_rows > 0) {
        $response['message'] = 'The same period already exists';
        echo json_encode($response);
        exit;
    }

    $insertPeriod = $conn->prepare('INSERT INTO voting_period (title, startdate, enddate) VALUES (?, ?, ?)');
    $insertPeriod->bind_param('sss', $title, $startdate, $enddate);

    if ($insertPeriod->execute()) {
        $response = ['success' => true, 'message' => 'Period added successfully'];
    } else {
        $response['message'] = 'Failed to add period';
    }
}

echo json_encode($response);
?>