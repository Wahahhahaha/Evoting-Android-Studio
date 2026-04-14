package com.example.e_voting

object ApiConfig {
    // 10.0.2.2 dipakai saat aplikasi dijalankan dari Android Emulator.
    // Jika memakai HP fisik, ganti dengan IP laptop/PC yang menjalankan XAMPP.
    const val BASE_URL = "http://192.168.0.18/voting/"
    const val LOGIN_URL = "${BASE_URL}Login.php"
    const val STUDENT_LIST_URL = "${BASE_URL}Tampilsiswa.php"
    const val STUDENT_ADD_URL = "${BASE_URL}Tambahsiswa.php"
    const val STUDENT_EDIT_URL = "${BASE_URL}Editsiswa.php"
    const val STUDENT_DELETE_URL = "${BASE_URL}Hapussiswa.php"
    const val CLASS_LIST_URL = "${BASE_URL}Tampilclass.php"
    const val CANDIDATE_LIST_URL = "${BASE_URL}Tampilkandidat.php"
    const val ACTIVE_CANDIDATE_LIST_URL = "${BASE_URL}Tampilkandidataktif.php"
    const val CANDIDATE_ADD_URL = "${BASE_URL}Tambahkandidat.php"
    const val CANDIDATE_EDIT_URL = "${BASE_URL}Editkandidat.php"
    const val CANDIDATE_DELETE_URL = "${BASE_URL}Hapuskandidat.php"
    const val CANDIDATE_VOTE_URL = "${BASE_URL}Votekandidat.php"
    const val STUDENT_OPTIONS_URL = "${BASE_URL}Tampilstudentoption.php"
}
