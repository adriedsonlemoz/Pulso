-- SQL de referência, idêntico à migração BatteryDatabase.MIGRATION_4_5.
-- Adiciona identificação opcional do tipo de fonte e nome manual à sessão.
-- Backups antigos não possuem esses campos: permanecerão NULL.
ALTER TABLE `sessions` ADD COLUMN `chargeSource` TEXT;
ALTER TABLE `sessions` ADD COLUMN `chargerLabel` TEXT;
