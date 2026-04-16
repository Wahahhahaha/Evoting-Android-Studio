-- Schema Paslon untuk DB voting
-- Tabel baru agar tidak bentrok dengan tabel candidate lama

CREATE TABLE IF NOT EXISTS candidate_paslon (
    paslon_id INT NOT NULL AUTO_INCREMENT,
    periodid INT NOT NULL,
    no_urut TINYINT UNSIGNED NOT NULL,

    president_studentid INT NOT NULL,
    vice_studentid INT NOT NULL,

    president_picture VARCHAR(255) NOT NULL,
    vice_picture VARCHAR(255) NOT NULL,

    vision TEXT NOT NULL,
    mission TEXT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (paslon_id),

    CONSTRAINT chk_paslon_not_same CHECK (president_studentid <> vice_studentid),

    CONSTRAINT fk_paslon_period
        FOREIGN KEY (periodid) REFERENCES voting_period(periodid)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT fk_paslon_president
        FOREIGN KEY (president_studentid) REFERENCES student(studentid)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT fk_paslon_vice
        FOREIGN KEY (vice_studentid) REFERENCES student(studentid)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    UNIQUE KEY uq_paslon_period_no_urut (periodid, no_urut),
    UNIQUE KEY uq_paslon_period_president (periodid, president_studentid),
    UNIQUE KEY uq_paslon_period_vice (periodid, vice_studentid),
    UNIQUE KEY uq_paslon_period_pair (periodid, president_studentid, vice_studentid),

    KEY idx_paslon_period (periodid),
    KEY idx_paslon_period_order (periodid, no_urut),
    KEY idx_paslon_president (president_studentid),
    KEY idx_paslon_vice (vice_studentid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;