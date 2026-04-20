<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';
include 'CandidateSchema.php';

ensureCandidateSchema($conn);

$response = ['success' => false, 'message' => 'Invalid request'];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $studentid = (int)($_POST['studentid'] ?? 0);
    $userid = (int)($_POST['userid'] ?? 0);
    $candidateid = (int)($_POST['candidateid'] ?? 0);

    if ($studentid <= 0 && $userid > 0) {
        $findStudent = $conn->prepare('SELECT studentid FROM student WHERE userid = ? LIMIT 1');
        $findStudent->bind_param('i', $userid);
        $findStudent->execute();
        $studentResult = $findStudent->get_result();
        $student = $studentResult ? $studentResult->fetch_assoc() : null;
        if ($student) {
            $studentid = (int)$student['studentid'];
        }
    }

    if ($studentid <= 0 || $candidateid <= 0) {
        $response['message'] = 'Student and candidate are required';
        echo json_encode($response);
        exit;
    }

    $checkStudent = $conn->prepare('SELECT studentid FROM student WHERE studentid = ? LIMIT 1');
    $checkStudent->bind_param('i', $studentid);
    $checkStudent->execute();
    $checkStudent->store_result();

    if ($checkStudent->num_rows === 0) {
        $response['message'] = 'Student not found';
        echo json_encode($response);
        exit;
    }

    $candidateQuery = $conn->prepare(
        'SELECT c.periodid, p.name AS president_name, v.name AS vice_name, vp.title
         FROM candidate c
         INNER JOIN voting_period vp ON vp.periodid = c.periodid
         INNER JOIN student p ON p.studentid = COALESCE(c.president_studentid, c.studentid)
         INNER JOIN student v ON v.studentid = COALESCE(c.vice_studentid, c.studentid)
         WHERE c.candidateid = ? AND CURDATE() BETWEEN vp.startdate AND vp.enddate
         LIMIT 1'
    );
    $candidateQuery->bind_param('i', $candidateid);
    $candidateQuery->execute();
    $candidateResult = $candidateQuery->get_result();
    $candidate = $candidateResult ? $candidateResult->fetch_assoc() : null;

    if (!$candidate) {
        $response['message'] = 'Candidate is not active or not found';
        echo json_encode($response);
        exit;
    }

    $periodid = (int)$candidate['periodid'];

    $checkVote = $conn->prepare('SELECT voteid FROM vote WHERE studentid = ? AND periodid = ? LIMIT 1');
    $checkVote->bind_param('ii', $studentid, $periodid);
    $checkVote->execute();
    $checkVote->store_result();

    if ($checkVote->num_rows > 0) {
        $response['message'] = 'You already voted in this period';
        $response['periodid'] = $periodid;
        echo json_encode($response);
        exit;
    }

    $insertVote = $conn->prepare('INSERT INTO vote (studentid, candidateid, periodid) VALUES (?, ?, ?)');
    $insertVote->bind_param('iii', $studentid, $candidateid, $periodid);

    if ($insertVote->execute()) {
        $response = [
            'success' => true,
            'message' => 'Vote for pair ' . $candidate['president_name'] . ' & ' . $candidate['vice_name'] . ' has been saved successfully',
            'periodid' => $periodid
        ];
    } else {
        $response['message'] = 'Failed to save vote';
    }
}

echo json_encode($response);
?>