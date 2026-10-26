-- Pozițiile mașinii din experiment, în fereastra rulării, pentru articol (faza 6, Task 9).
-- Variabilele @masina, @de și @pana le pune masoara.ps1; orele sînt în UTC, ca în bază.
-- latenta_ms = ora_sosirii - ora_masinii; pentru rândurile din buffer include și cât a stat mesajul în buffer.
SELECT seq,
       CONCAT(REPLACE(ora_masinii, ' ', 'T'), 'Z') AS ora_masinii,
       CONCAT(REPLACE(ora_sosirii, ' ', 'T'), 'Z') AS ora_sosirii,
       din_buffer,
       ROUND(TIMESTAMPDIFF(MICROSECOND, ora_masinii, ora_sosirii) / 1000) AS latenta_ms
FROM pozitii
WHERE masina_id = @masina
  AND ora_masinii BETWEEN @de AND @pana
ORDER BY ora_masinii;
