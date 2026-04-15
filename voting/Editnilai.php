<?php
include 'Koneksi.php';
header('Content-Type: application/json');

$response = ["success" => false, "message" => "Data tidak lengkap"];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $nilaiid = $_POST['nilaiid'] ?? '';
    $siswaid = $_POST['siswaid'] ?? '';
    $mtk = $_POST['matematika'] ?? '';
    $indo = $_POST['bahasa_indonesia'] ?? '';
    $inggris = $_POST['bahasa_inggris'] ?? '';

    if ($nilaiid && $siswaid && $mtk !== '' && $indo !== '' && $inggris !== '') {
        $cek = mysqli_query($conn, "SELECT nilaiid FROM nilai_tes WHERE siswaid='$siswaid' AND nilaiid != '$nilaiid' LIMIT 1");
        if ($cek && mysqli_num_rows($cek) > 0) {
            echo json_encode(["success" => false, "message" => "Siswa ini sudah memiliki data nilai lain"]);
            exit;
        }

        $total = (float)$mtk + (float)$indo + (float)$inggris;
        $ratarata = round($total / 3, 2);

        $sql = "UPDATE nilai_tes SET
                    siswaid='$siswaid',
                    nilai_matematika='$mtk',
                    nilai_bahasa_indonesia='$indo',
                    nilai_bahasa_inggris='$inggris',
                    nilai_total='$total',
                    ratarata='$ratarata',
                    tanggal_input=NOW()
                WHERE nilaiid='$nilaiid'";

        if (mysqli_query($conn, $sql)) {
            $response = ["success" => true, "message" => "Data nilai berhasil diperbarui"];
        } else {
            $response = ["success" => false, "message" => mysqli_error($conn)];
        }
    }
}

echo json_encode($response);
?>
