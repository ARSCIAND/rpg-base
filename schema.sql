-- Base de datos del registro de heroes (ejercicio grupal)
CREATE DATABASE IF NOT EXISTS rpg_grupal_db;
USE rpg_grupal_db;

DROP TABLE IF EXISTS heroes;

CREATE TABLE heroes (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(30) NOT NULL UNIQUE,
    tipo        VARCHAR(10) NOT NULL,
    vida_maxima INT NOT NULL,
    nivel       INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_tipo  CHECK (tipo IN ('guerrero', 'mago')),
    CONSTRAINT chk_vida  CHECK (vida_maxima > 0),
    CONSTRAINT chk_nivel CHECK (nivel >= 1)
);

-- unos heroes de prueba
INSERT INTO heroes (nombre, tipo, vida_maxima, nivel) VALUES
    ('Thor', 'guerrero', 150, 3),
    ('Merlin', 'mago', 90, 5),
    ('Xena', 'guerrero', 130, 2);
