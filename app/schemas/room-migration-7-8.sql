-- Documentação da migração efetiva codificada em BatteryDatabase.MIGRATION_7_8.
-- Não substitui o schema JSON gerado pelo KSP.
ALTER TABLE `lab_tests` ADD COLUMN `initialTemperatureC` REAL;
ALTER TABLE `lab_tests` ADD COLUMN `finalTemperatureC` REAL;
ALTER TABLE `lab_tests` ADD COLUMN `maxTemperatureC` REAL;
ALTER TABLE `lab_tests` ADD COLUMN `screenCondition` TEXT NOT NULL DEFAULT 'NAO_INFORMADA';
ALTER TABLE `lab_tests` ADD COLUMN `cameraReported` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `lab_tests` ADD COLUMN `wifiReported` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `lab_tests` ADD COLUMN `recordingReported` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `lab_tests` ADD COLUMN `notes` TEXT NOT NULL DEFAULT '';
