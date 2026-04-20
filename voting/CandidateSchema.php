<?php

function candidateColumnExists(mysqli $conn, string $column): bool
{
    $safeColumn = $conn->real_escape_string($column);
    $safeDb = $conn->real_escape_string($conn->query('SELECT DATABASE() AS db')->fetch_assoc()['db'] ?? '');
    if ($safeDb === '') {
        return false;
    }

    $sql = "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = '{$safeDb}'
              AND TABLE_NAME = 'candidate'
              AND COLUMN_NAME = '{$safeColumn}'
            LIMIT 1";
    $result = $conn->query($sql);
    return $result && $result->num_rows > 0;
}

function ensureCandidateSchema(mysqli $conn): void
{
    $changes = [];

    if (!candidateColumnExists($conn, 'president_studentid')) {
        $changes[] = "ADD COLUMN president_studentid INT NULL AFTER picture";
    }
    if (!candidateColumnExists($conn, 'vice_studentid')) {
        $changes[] = "ADD COLUMN vice_studentid INT NULL AFTER president_studentid";
    }
    if (!candidateColumnExists($conn, 'vice_picture')) {
        $changes[] = "ADD COLUMN vice_picture VARCHAR(255) NULL AFTER picture";
    }

    if (!empty($changes)) {
        $sql = "ALTER TABLE candidate " . implode(', ', $changes);
        $conn->query($sql);
    }

    if (!candidateColumnExists($conn, 'president_studentid')) {
        return;
    }

    $conn->query('UPDATE candidate SET president_studentid = studentid WHERE (president_studentid IS NULL OR president_studentid = 0) AND studentid IS NOT NULL');
    $conn->query('UPDATE candidate SET vice_studentid = studentid WHERE (vice_studentid IS NULL OR vice_studentid = 0) AND studentid IS NOT NULL');
}

