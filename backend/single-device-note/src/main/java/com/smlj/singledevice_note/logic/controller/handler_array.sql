UPDATE nfcpatrol.t_nfcpatrol_point
SET lineid = NULL
WHERE
    rfid IN (SELECT UNNEST(pointids) FROM nfcpatrol.t_nfcpatrol_line WHERE id = 1);
    
    
update nfcpatrol.t_nfcpatrol_line
SET deleted = TRUE,
    pointids = null
where id = 1;


SELECT UNNEST (pointids) FROM nfcpatrol.t_nfcpatrol_line WHERE id = 2

SELECT
    COUNT(DISTINCT rfid) as cnt
FROM
    nfcpatrol.t_nfcpatrol_record
WHERE
    rfid IN (SELECT UNNEST (pointids) FROM nfcpatrol.t_nfcpatrol_line WHERE id = 2)
    
    
select * from nfcpatrol.t_nfcpatrol_line where deptid = '1-1'

    
