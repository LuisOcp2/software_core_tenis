ALTER TABLE devoluciones MODIFY COLUMN estado ENUM('pendiente', 'procesando', 'aprobada', 'rechazada', 'finalizada', 'anulada') DEFAULT 'pendiente';
