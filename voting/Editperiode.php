<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';

$response = ['success' => false, 'message' => 'Invalid request'];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $periodid = (int)($_POST['periodid'] ?? 0);
    $title = trim($_POST['title'] ?? '');
    $startdate = trim($_POST['startdate'] ?? '');
    $enddate = trim($_POST['enddate'] ?? '');

    if ($periodid <= 0 || $title === '' || $startdate === '' || $enddate === '') {
        $response['message'] = 'Incomplete period data';
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

    $checkExisting = $conn->prepare('SELECT periodid FROM voting_period WHERE periodid = ? LIMIT 1');
    $checkExisting->bind_param('i', $periodid);
    $checkExisting->execute();
    $checkExisting->store_result();

    if ($checkExisting->num_rows === 0) {
        $response['message'] = 'Period not found';
        echo json_encode($response);
        exit;
    }

    $checkDuplicate = $conn->prepare('SELECT periodid FROM voting_period WHERE title = ? AND startdate = ? AND enddate = ? AND periodid <> ? LIMIT 1');
    $checkDuplicate->bind_param('sssi', $title, $startdate, $enddate, $periodid);
    $checkDuplicate->execute();
    $checkDuplicate->store_result();

    if ($checkDuplicate->num_rows > 0) {
        $response['message'] = 'The same period already exists';
        echo json_encode($response);
        exit;
    }

    $updatePeriod = $conn->prepare('UPDATE voting_period SET title = ?, startdate = ?, enddate = ? WHERE periodid = ?');
    $updatePeriod->bind_param('sssi', $title, $startdate, $enddate, $periodid);

    if ($updatePeriod->execute()) {
        $response = ['success' => true, 'message' => 'Period updated successfully'];
    } else {
        $response['message'] = 'Failed to update period';
    }
}

echo json_encode($response);
?>