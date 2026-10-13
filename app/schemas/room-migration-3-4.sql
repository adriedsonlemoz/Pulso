-- Referência para testes: a migração oficial é BatteryDatabase.MIGRATION_3_4 em Store.kt.
CREATE TABLE IF NOT EXISTS `telemetry_samples` (
 `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
 `sessionId` INTEGER NOT NULL,
 `timestamp` INTEGER NOT NULL,
 `elapsedMs` INTEGER NOT NULL,
 `mode` TEXT NOT NULL,
 `percent` INTEGER NOT NULL,
 `currentMa` REAL,
 `currentSource` TEXT,
 `temperatureC` REAL,
 `counterUah` INTEGER,
 FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_telemetry_samples_sessionId` ON `telemetry_samples` (`sessionId`);
CREATE INDEX IF NOT EXISTS `index_telemetry_samples_timestamp` ON `telemetry_samples` (`timestamp`);
