<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';
include 'CandidateSchema.php';

ensureCandidateSchema($conn);

$response = ['success' => false, 'message' => 'ID kandidat tidak valid'];
$candidateid = (int)($_POST['candidateid'] ?? 0);

if ($candidateid > 0) {
    $getPeriod = $conn->prepare('SELECT periodid FROM candidate WHERE candidateid = ? LIMIT 1');
    $getPeriod->bind_param('i', $candidateid);
    $getPeriod->execute();
    $result = $getPeriod->get_result();
    $candidate = $result ? $result->fetch_assoc() : null;

    if ($candidate) {
        $periodid = (int)$candidate['periodid'];
        $conn->begin_transaction();

        try {
            $deleteCandidate = $conn->prepare('DELETE FROM candidate WHERE candidateid = ?');
            $deleteCandidate->bind_param('i', $candidateid);
            $deleteCandidate->execute();

            $checkRemaining = $conn->prepare('SELECT COUNT(*) AS total FROM candidate WHERE periodid = ?');
            $checkRemaining->bind_param('i', $periodid);
            $checkRemaining->execute();
            $remaining = (int)($checkRemaining->get_result()->fetch_assoc()['total'] ?? 0);

            if ($remaining === 0) {
                $deletePeriod = $conn->prepare('DELETE FROM voting_period WHERE periodid = ?');
                $deletePeriod->bind_param('i', $periodid);
                $deletePeriod->execute();
            }

            $conn->commit();
            $response = ['success' => true, 'status' => 1, 'message' => 'Kandidat berhasil dihapus'];
        } catch (Throwable $e) {
            $conn->rollback();
            $response['message'] = 'Gagal menghapus kandidat: ' . $e->getMessage();
        }
    } else {
        $response['message'] = 'Data kandidat tidak ditemukan';
    }
}

echo json_encode($response);
?>
