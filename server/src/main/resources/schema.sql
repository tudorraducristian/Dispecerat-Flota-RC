CREATE TABLE IF NOT EXISTS zone (
  id            VARCHAR(20)  NOT NULL PRIMARY KEY,
  nume          VARCHAR(100) NOT NULL,
  poligon       JSON         NOT NULL,
  viteza_maxima INT          NOT NULL
);

CREATE TABLE IF NOT EXISTS masini (
  id            VARCHAR(20) NOT NULL PRIMARY KEY,
  zona_id       VARCHAR(20) NOT NULL,
  stare         VARCHAR(10) NOT NULL DEFAULT 'OFFLINE',
  ultima_vedere DATETIME(3) NULL,
  ultimul_seq   BIGINT      NULL,
  recuperate    BIGINT      NOT NULL DEFAULT 0,
  pierdute      BIGINT      NOT NULL DEFAULT 0,
  CONSTRAINT fk_masini_zona FOREIGN KEY (zona_id) REFERENCES zone (id)
);

CREATE TABLE IF NOT EXISTS pozitii (
  id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  masina_id   VARCHAR(20) NOT NULL,
  seq         BIGINT      NOT NULL,
  ora_masinii DATETIME(3) NOT NULL,
  ora_sosirii DATETIME(3) NOT NULL,
  lat         DOUBLE      NOT NULL,
  lon         DOUBLE      NOT NULL,
  viteza      DOUBLE      NOT NULL,
  temperatura DOUBLE      NOT NULL,
  baterie     INT         NOT NULL,
  din_buffer  BOOLEAN     NOT NULL,
  INDEX idx_pozitii_masina_ora (masina_id, ora_masinii),
  CONSTRAINT fk_pozitii_masina FOREIGN KEY (masina_id) REFERENCES masini (id)
);

CREATE TABLE IF NOT EXISTS alarme (
  id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  masina_id   VARCHAR(20)  NOT NULL,
  tip         VARCHAR(30)  NOT NULL,
  sursa       VARCHAR(10)  NOT NULL,
  ora         DATETIME(3)  NOT NULL,
  ora_sosirii DATETIME(3)  NOT NULL,
  detalii     VARCHAR(255) NULL,
  vazuta      BOOLEAN      NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_alarme_masina FOREIGN KEY (masina_id) REFERENCES masini (id)
);

CREATE TABLE IF NOT EXISTS comenzi (
  id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  masina_id       VARCHAR(20)  NOT NULL,
  tip             VARCHAR(30)  NOT NULL,
  parametri       JSON         NOT NULL,
  ora_trimiterii  DATETIME(3)  NOT NULL,
  ora_confirmarii DATETIME(3)  NULL,
  stare           VARCHAR(12)  NOT NULL,
  motiv           VARCHAR(255) NULL,
  CONSTRAINT fk_comenzi_masina FOREIGN KEY (masina_id) REFERENCES masini (id)
);

CREATE TABLE IF NOT EXISTS evenimente_retea (
  id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tinta       VARCHAR(20)  NOT NULL,
  linie       VARCHAR(200) NOT NULL,
  raspuns     VARCHAR(200) NOT NULL,
  ora_start   DATETIME(3)  NOT NULL,
  ora_sfarsit DATETIME(3)  NULL
);
