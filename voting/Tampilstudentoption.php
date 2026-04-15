<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';
include 'CandidateSchema.php';

ensureCandidateSchema($conn);

$candidateid = (int)($_GET['candidateid'] ?? 0);

if ($candidateid > 0) {
    $sql = "SELECT s.studentid, s.name
            FROM student s
            WHERE s.studentid NOT IN (
                SELECT COALESCE(c2.president_studentid, c2.studentid) FROM candidate c2
                WHERE c2.candidateid <> ? AND COALESCE(c2.president_studentid, c2.studentid) IS NOT NULL
                UNION
                SELECT COALESCE(c3.vice_studentid, c3.studentid) FROM candidate c3
                WHERE c3.candidateid <> ? AND COALESCE(c3.vice_studentid, c3.studentid) IS NOT NULL
            )
            ORDER BY s.name ASC";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param('ii', $candidateid, $candidateid);
    $stmt->execute();
    $result = $stmt->get_result();
} else {
    $sql = "SELECT s.studentid, s.name
            FROM student s
            WHERE s.studentid NOT IN (
                SELECT COALESCE(c.president_studentid, c.studentid) FROM candidate c
                WHERE COALESCE(c.president_studentid, c.studentid) IS NOT NULL
                UNION
                SELECT COALESCE(c.vice_studentid, c.studentid) FROM candidate c
                WHERE COALESCE(c.vice_studentid, c.studentid) IS NOT NULL
            )
            ORDER BY s.name ASC";
    $result = $conn->query($sql);
}

$data = [];

if ($result) {
    while ($row = $result->fetch_assoc()) {
        $data[] = $row;
    }
}

echo json_encode($data);
?>
