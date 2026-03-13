ALTER TABLE notas_credito MODIFY COLUMN estado ENUM('emitida', 'aplicada', 'anulada', 'vencida', 'generada', 'consumida') DEFAULT 'emitida';
