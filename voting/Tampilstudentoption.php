<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';
include 'CandidateSchema.php';

ensureCandidateSchema($conn);

$periodid = (int)($_GET['periodid'] ?? 0);
$candidateid = (int)($_GET['candidateid'] ?? 0);
$data = [];

if ($periodid <= 0) {
    echo json_encode($data);
    exit;
}

if ($candidateid > 0) {
    $sql = "SELECT s.studentid, s.name
            FROM student s
            WHERE NOT EXISTS (
                SELECT 1 FROM candidate c
                WHERE c.periodid = ?
                  AND c.candidateid <> ?
                  AND (
                      COALESCE(NULLIF(c.president_studentid, 0), c.studentid) = s.studentid
                      OR COALESCE(NULLIF(c.vice_studentid, 0), c.studentid) = s.studentid
                  )
            )
            ORDER BY s.name ASC";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param('ii', $periodid, $candidateid);
    $stmt->execute();
    $result = $stmt->get_result();
} else {
    $sql = "SELECT s.studentid, s.name
            FROM student s
            WHERE NOT EXISTS (
                SELECT 1 FROM candidate c
                WHERE c.periodid = ?
                  AND (
                      COALESCE(NULLIF(c.president_studentid, 0), c.studentid) = s.studentid
                      OR COALESCE(NULLIF(c.vice_studentid, 0), c.studentid) = s.studentid
                  )
            )
            ORDER BY s.name ASC";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param('i', $periodid);
    $stmt->execute();
    $result = $stmt->get_result();
}

if ($result) {
    while ($row = $result->fetch_assoc()) {
        $data[] = $row;
    }
}

echo json_encode($data);
?>