<?php
header('Content-Type: application/json; charset=utf-8');
include 'Koneksi.php';

function tableExists(mysqli $conn, string $tableName): bool {
    $escaped = $conn->real_escape_string($tableName);
    $query = "SHOW TABLES LIKE '{$escaped}'";
    $result = $conn->query($query);
    return $result instanceof mysqli_result && $result->num_rows > 0;
}

function columnExists(mysqli $conn, string $tableName, string $columnName): bool {
    $escapedTable = str_replace('`', '``', $tableName);
    $escapedColumn = $conn->real_escape_string($columnName);
    $query = "SHOW COLUMNS FROM `{$escapedTable}` LIKE '{$escapedColumn}'";
    $result = $conn->query($query);
    return $result instanceof mysqli_result && $result->num_rows > 0;
}

function firstExistingColumn(mysqli $conn, string $tableName, array $candidates): string {
    foreach ($candidates as $candidate) {
        if (columnExists($conn, $tableName, $candidate)) {
            return $candidate;
        }
    }
    return '';
}

$hasClassTable = tableExists($conn, 'class');
$hasStudentTable = tableExists($conn, 'student');
$hasJurusanTable = tableExists($conn, 'jurusan');
$hasAngkatanTable = tableExists($conn, 'angkatan');

$classJurusanFk = firstExistingColumn($conn, 'class', ['jurusanid', 'id_jurusan']);
$classAngkatanFk = firstExistingColumn($conn, 'class', ['angkatanid', 'id_angkatan']);
$jurusanPk = firstExistingColumn($conn, 'jurusan', ['jurusanid', 'id_jurusan', 'id']);
$angkatanPk = firstExistingColumn($conn, 'angkatan', ['angkatanid', 'id_angkatan', 'id']);
$jurusanNameCol = firstExistingColumn($conn, 'jurusan', ['jurusanname', 'nama_jurusan', 'namajurusan', 'nama', 'name']);
$angkatanNameCol = firstExistingColumn($conn, 'angkatan', ['angkatanname', 'nama_angkatan', 'tahun_angkatan', 'nama', 'batchyear', 'name']);

if (
    $hasClassTable &&
    $hasStudentTable &&
    $hasJurusanTable &&
    $hasAngkatanTable &&
    $classJurusanFk !== '' &&
    $classAngkatanFk !== '' &&
    $jurusanPk !== '' &&
    $angkatanPk !== '' &&
    $jurusanNameCol !== '' &&
    $angkatanNameCol !== ''
) {
    $sql = "SELECT
                c.classid,
                c.classname,
                j.`{$jurusanNameCol}` AS jurusan,
                a.`{$angkatanNameCol}` AS angkatan,
                COUNT(s.studentid) AS studentcount
            FROM class c
            LEFT JOIN jurusan j ON j.`{$jurusanPk}` = c.`{$classJurusanFk}`
            LEFT JOIN angkatan a ON a.`{$angkatanPk}` = c.`{$classAngkatanFk}`
            LEFT JOIN student s ON s.classid = c.classid
            GROUP BY c.classid, c.classname, j.`{$jurusanNameCol}`, a.`{$angkatanNameCol}`
            ORDER BY c.classname ASC";
} else {
    $sql = "SELECT
                c.classid,
                c.classname,
                '' AS jurusan,
                '' AS angkatan,
                COUNT(s.studentid) AS studentcount
            FROM class c
            LEFT JOIN student s ON s.classid = c.classid
            GROUP BY c.classid, c.classname
            ORDER BY c.classname ASC";
}

$result = $conn->query($sql);
$data = [];

if ($result) {
    while ($row = $result->fetch_assoc()) {
        $data[] = $row;
    }
}

echo json_encode($data);
?>
