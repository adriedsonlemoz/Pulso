-- Room 6 -> 7: preserva testes solares existentes.
ALTER TABLE `lab_tests` ADD COLUMN `kind` TEXT NOT NULL DEFAULT 'SOLAR';
ALTER TABLE `lab_tests` ADD COLUMN `externalSeen` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `lab_tests` ADD COLUMN `lastPlugged` INTEGER;
