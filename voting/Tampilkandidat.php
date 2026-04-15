<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';
include 'CandidateSchema.php';

ensureCandidateSchema($conn);

$sql = "SELECT c.candidateid, c.picture, COALESCE(c.vice_picture, c.picture) AS vice_picture,
               COALESCE(c.president_studentid, c.studentid) AS studentid,
               COALESCE(c.president_studentid, c.studentid) AS president_studentid,
               COALESCE(c.vice_studentid, c.studentid) AS vice_studentid,
               c.periodid, p.name AS president_name, v.name AS vice_name, c.vision, c.mission,
               vp.title AS period_title, vp.startdate, vp.enddate
        FROM candidate c
        INNER JOIN student p ON p.studentid = COALESCE(c.president_studentid, c.studentid)
        INNER JOIN student v ON v.studentid = COALESCE(c.vice_studentid, c.studentid)
        INNER JOIN voting_period vp ON vp.periodid = c.periodid
        ORDER BY c.candidateid DESC";

$result = $conn->query($sql);
$data = [];

if ($result) {
    while ($row = $result->fetch_assoc()) {
        $data[] = $row;
    }
}

echo json_encode($data);
?>
