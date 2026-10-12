-- Baza și utilizatorul pentru flota-rc. Se rulează o singură dată, ca root.
-- Parola e „flota”: baza ascultă doar pe 127.0.0.1, iar repo-ul de pe GitHub e privat.
CREATE DATABASE IF NOT EXISTS flota CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'flota'@'localhost' IDENTIFIED BY 'flota';
CREATE USER IF NOT EXISTS 'flota'@'127.0.0.1' IDENTIFIED BY 'flota';

GRANT ALL PRIVILEGES ON flota.* TO 'flota'@'localhost';
GRANT ALL PRIVILEGES ON flota.* TO 'flota'@'127.0.0.1';
