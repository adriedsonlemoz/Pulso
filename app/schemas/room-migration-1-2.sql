-- Referência da migração Room 1 -> 2 implementada em Store.kt.
CREATE TABLE IF NOT EXISTS `session_samples` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `sessionId` INTEGER NOT NULL,
  `timestamp` INTEGER NOT NULL,
  `percent` INTEGER NOT NULL,
  FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_session_samples_sessionId` ON `session_samples` (`sessionId`);
