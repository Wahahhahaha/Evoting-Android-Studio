package com.example.e_voting

object ApiConfig {
    // 10.0.2.2 dipakai saat aplikasi dijalankan dari Android Emulator.
    // Jika memakai HP fisik, ganti dengan IP laptop/PC yang menjalankan XAMPP.
//    const val BASE_URL = "http://192.168.0.18/voting/"
    const val BASE_URL = "http://10.34.38.242/voting/"
    const val LOGIN_URL = "${BASE_URL}Login.php"
    const val STUDENT_LIST_URL = "${BASE_URL}Tampilsiswa.php"
    const val STUDENT_ADD_URL = "${BASE_URL}Tambahsiswa.php"
    const val STUDENT_EDIT_URL = "${BASE_URL}Editsiswa.php"
    const val STUDENT_DELETE_URL = "${BASE_URL}Hapussiswa.php"
    const val CLASS_LIST_URL = "${BASE_URL}Tampilclass.php"
    const val CLASS_ADD_URL = "${BASE_URL}Tambahclass.php"
    const val CLASS_EDIT_URL = "${BASE_URL}Editclass.php"
    const val CLASS_DELETE_URL = "${BASE_URL}Hapusclass.php"
    const val CANDIDATE_LIST_URL = "${BASE_URL}Tampilkandidat.php"
    const val ACTIVE_CANDIDATE_LIST_URL = "${BASE_URL}Tampilkandidataktif.php"
    const val CANDIDATE_ADD_URL = "${BASE_URL}Tambahkandidat.php"
    const val CANDIDATE_EDIT_URL = "${BASE_URL}Editkandidat.php"
    const val CANDIDATE_DELETE_URL = "${BASE_URL}Hapuskandidat.php"
    const val CANDIDATE_VOTE_URL = "${BASE_URL}Votekandidat.php"
    const val VOTING_RESULT_URL = "${BASE_URL}Tampilkandidataktif.php?result=1"
    const val STUDENT_OPTIONS_URL = "${BASE_URL}Tampilstudentoption.php"
    const val PERIOD_LIST_URL = "${BASE_URL}Tampilperiode.php"
    const val PERIOD_ADD_URL = "${BASE_URL}Tambahperiode.php"
    const val PERIOD_EDIT_URL = "${BASE_URL}Editperiode.php"
    const val PERIOD_DELETE_URL = "${BASE_URL}Hapusperiode.php"
}
