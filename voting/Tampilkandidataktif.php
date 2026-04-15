<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';
include 'CandidateSchema.php';

ensureCandidateSchema($conn);

$studentid = (int)($_GET['studentid'] ?? 0);
$data = [];

$activePeriods = [];
$activePeriodResult = $conn->query('SELECT periodid FROM voting_period WHERE CURDATE() BETWEEN startdate AND enddate ORDER BY startdate DESC');
if ($activePeriodResult) {
    while ($period = $activePeriodResult->fetch_assoc()) {
        $activePeriods[] = (int)$period['periodid'];
    }
}

if (!empty($activePeriods)) {
    $periodIdsSql = implode(',', $activePeriods);
    if ($studentid > 0) {
        $sql = "SELECT c.candidateid, c.picture, COALESCE(c.vice_picture, c.picture) AS vice_picture,
                       COALESCE(c.president_studentid, c.studentid) AS studentid,
                       COALESCE(c.president_studentid, c.studentid) AS president_studentid,
                       COALESCE(c.vice_studentid, c.studentid) AS vice_studentid,
                       c.periodid, p.name AS president_name, v.name AS vice_name, c.vision, c.mission,
                       vp.title AS period_title, vp.startdate, vp.enddate,
                       CASE WHEN uv.voteid IS NULL THEN 0 ELSE 1 END AS has_voted,
                       CASE WHEN uv.candidateid = c.candidateid THEN 1 ELSE 0 END AS is_voted_candidate,
                       0 AS vote_count, 0 AS is_winner, 'voting' AS display_mode
                FROM candidate c
                INNER JOIN student p ON p.studentid = COALESCE(c.president_studentid, c.studentid)
                INNER JOIN student v ON v.studentid = COALESCE(c.vice_studentid, c.studentid)
                INNER JOIN voting_period vp ON vp.periodid = c.periodid
                LEFT JOIN vote uv ON uv.periodid = c.periodid AND uv.studentid = ?
                WHERE c.periodid IN ($periodIdsSql)
                ORDER BY vp.startdate DESC, c.candidateid DESC";

        $stmt = $conn->prepare($sql);
        $stmt->bind_param('i', $studentid);
        $stmt->execute();
        $result = $stmt->get_result();
    } else {
        $sql = "SELECT c.candidateid, c.picture, COALESCE(c.vice_picture, c.picture) AS vice_picture,
                       COALESCE(c.president_studentid, c.studentid) AS studentid,
                       COALESCE(c.president_studentid, c.studentid) AS president_studentid,
                       COALESCE(c.vice_studentid, c.studentid) AS vice_studentid,
                       c.periodid, p.name AS president_name, v.name AS vice_name, c.vision, c.mission,
                       vp.title AS period_title, vp.startdate, vp.enddate,
                       0 AS has_voted, 0 AS is_voted_candidate,
                       0 AS vote_count, 0 AS is_winner, 'voting' AS display_mode
                FROM candidate c
                INNER JOIN student p ON p.studentid = COALESCE(c.president_studentid, c.studentid)
                INNER JOIN student v ON v.studentid = COALESCE(c.vice_studentid, c.studentid)
                INNER JOIN voting_period vp ON vp.periodid = c.periodid
                WHERE c.periodid IN ($periodIdsSql)
                ORDER BY vp.startdate DESC, c.candidateid DESC";
        $result = $conn->query($sql);
    }
} else {
    $periodSql = 'SELECT periodid FROM voting_period WHERE DATE_ADD(enddate, INTERVAL 1 DAY) <= CURDATE() ORDER BY enddate DESC LIMIT 1';
    $periodResult = $conn->query($periodSql);
    $periodRow = $periodResult ? $periodResult->fetch_assoc() : null;

    if ($periodRow) {
        $periodid = (int)$periodRow['periodid'];
        $sql = "SELECT c.candidateid, c.picture, COALESCE(c.vice_picture, c.picture) AS vice_picture,
                       COALESCE(c.president_studentid, c.studentid) AS studentid,
                       COALESCE(c.president_studentid, c.studentid) AS president_studentid,
                       COALESCE(c.vice_studentid, c.studentid) AS vice_studentid,
                       c.periodid, p.name AS president_name, v.name AS vice_name, c.vision, c.mission,
                       vp.title AS period_title, vp.startdate, vp.enddate,
                       1 AS has_voted, 0 AS is_voted_candidate,
                       COUNT(vt.voteid) AS vote_count,
                       0 AS is_winner,
                       'result' AS display_mode
                FROM candidate c
                INNER JOIN student p ON p.studentid = COALESCE(c.president_studentid, c.studentid)
                INNER JOIN student v ON v.studentid = COALESCE(c.vice_studentid, c.studentid)
                INNER JOIN voting_period vp ON vp.periodid = c.periodid
                LEFT JOIN vote vt ON vt.candidateid = c.candidateid
                WHERE c.periodid = ?
                GROUP BY c.candidateid
                ORDER BY vote_count DESC, c.candidateid ASC";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param('i', $periodid);
        $stmt->execute();
        $result = $stmt->get_result();
    } else {
        $result = null;
    }
}

if ($result) {
    $rows = [];
    $topVote = -1;
    while ($row = $result->fetch_assoc()) {
        $rows[] = $row;
        if (($row['display_mode'] ?? '') === 'result') {
            $topVote = max($topVote, (int)$row['vote_count']);
        }
    }

    foreach ($rows as $row) {
        if (($row['display_mode'] ?? '') === 'result') {
            $row['is_winner'] = ((int)$row['vote_count'] === $topVote && $topVote >= 0) ? 1 : 0;
        }
        $data[] = $row;
    }
}

echo json_encode($data);

