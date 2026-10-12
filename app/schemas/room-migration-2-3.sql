-- Migração Room 2→3: só acrescenta colunas opcionais, sem apagar sessões/pontos.
ALTER TABLE `session_samples` ADD COLUMN `currentMa` REAL;
ALTER TABLE `session_samples` ADD COLUMN `temperatureC` REAL;
ALTER TABLE `session_samples` ADD COLUMN `currentSource` TEXT;
