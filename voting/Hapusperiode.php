<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';

$response = ['success' => false, 'message' => 'Invalid period ID'];
$periodid = (int)($_POST['periodid'] ?? 0);

if ($periodid > 0) {
    $checkCandidate = $conn->prepare('SELECT COUNT(*) AS total FROM candidate WHERE periodid = ?');
    $checkCandidate->bind_param('i', $periodid);
    $checkCandidate->execute();
    $candidateTotal = (int)($checkCandidate->get_result()->fetch_assoc()['total'] ?? 0);

    if ($candidateTotal > 0) {
        $response['message'] = 'This period cannot be deleted because it is used by candidates';
        echo json_encode($response);
        exit;
    }

    $checkVote = $conn->prepare('SELECT COUNT(*) AS total FROM vote WHERE periodid = ?');
    $checkVote->bind_param('i', $periodid);
    $checkVote->execute();
    $voteTotal = (int)($checkVote->get_result()->fetch_assoc()['total'] ?? 0);

    if ($voteTotal > 0) {
        $response['message'] = 'This period cannot be deleted because vote data already exists';
        echo json_encode($response);
        exit;
    }

    $deletePeriod = $conn->prepare('DELETE FROM voting_period WHERE periodid = ?');
    $deletePeriod->bind_param('i', $periodid);
    $deletePeriod->execute();

    if ($deletePeriod->affected_rows > 0) {
        $response = ['success' => true, 'status' => 1, 'message' => 'Period deleted successfully'];
    } else {
        $response['message'] = 'Period not found';
    }
}

echo json_encode($response);
?>