SELECT
    COUNT(DISTINCT rfid) AS cnt
FROM
    nfcpatrol.t_nfcpatrol_record
WHERE
    '2026-01-03 12:00:00' <= (dotime::DATE)::TIMESTAMP
    AND (dotime::DATE)::TIMESTAMP <= '2026-01-03 12:52:00';


SELECT
    COUNT(DISTINCT rfid) AS cnt
FROM
    nfcpatrol.t_nfcpatrol_record
WHERE
    (
        rfid IN (SELECT UNNEST(pointids) FROM nfcpatrol.t_nfcpatrol_line WHERE id = 1)
    )
        AND '2026-01-03 12:00:00' <= (dotime::DATE)::TIMESTAMP
        AND (dotime::DATE)::TIMESTAMP <= '2026-01-03 12:52:00';