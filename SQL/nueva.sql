
ALTER TABLE notificaciones
  ADD COLUMN IF NOT EXISTS id_bodega_destino INT NULL,
  ADD COLUMN IF NOT EXISTS id_bodega_origen INT NULL,
  ADD COLUMN IF NOT EXISTS evento VARCHAR(30) NULL,
  ADD COLUMN IF NOT EXISTS tipo_referencia VARCHAR(30) NULL,
  ADD COLUMN IF NOT EXISTS id_referencia INT NULL,
  ADD COLUMN IF NOT EXISTS leida TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS activa TINYINT(1) NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS fecha_lectura DATETIME NULL,
  ADD COLUMN IF NOT EXISTS id_usuario_destinatario INT NULL,
  ADD COLUMN IF NOT EXISTS para_todos TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NULL,
  ADD COLUMN IF NOT EXISTS categoria VARCHAR(30) NULL;

DROP INDEX IF EXISTS idx_notif_tr_ref ON notificaciones;
CREATE INDEX idx_notif_tr_ref ON notificaciones (tipo_referencia, id_referencia, evento, activa);
DROP INDEX IF EXISTS idx_notif_bodegas_lectura ON notificaciones;
CREATE INDEX idx_notif_bodegas_lectura ON notificaciones (id_bodega_origen, id_bodega_destino, leida, activa, evento);
DROP INDEX IF EXISTS idx_notif_user_lectura ON notificaciones;
CREATE INDEX idx_notif_user_lectura ON notificaciones (id_usuario_destinatario, leida, activa);

DROP PROCEDURE IF EXISTS sp_notificar_traspaso_evento;
DELIMITER $$
CREATE PROCEDURE sp_notificar_traspaso_evento(
  IN p_id_traspaso INT,
  IN p_evento VARCHAR(30),
  IN p_titulo VARCHAR(80),
  IN p_mensaje VARCHAR(255)
)
BEGIN
  DECLARE v_bod_origen INT;
  DECLARE v_bod_dest INT;
  DECLARE v_target INT;

  SELECT id_bodega_origen, id_bodega_destino INTO v_bod_origen, v_bod_dest
  FROM traspasos WHERE id_traspaso = p_id_traspaso;

  UPDATE notificaciones
    SET leida = 1, activa = 0, fecha_lectura = NOW()
  WHERE tipo_referencia = 'traspasos'
    AND id_referencia = p_id_traspaso
    AND evento = p_evento
    AND activa = 1;

  SET v_target = CASE
    WHEN p_evento IN ('solicitud') THEN v_bod_origen
    WHEN p_evento IN ('autorizado','enviado','en_transito') THEN v_bod_dest
    WHEN p_evento IN ('recibido','rechazado','cancelado') THEN v_bod_origen
    ELSE v_bod_dest
  END;

  INSERT INTO notificaciones (
    titulo, mensaje, tipo, categoria,
    id_usuario_destinatario, para_todos,
    id_referencia, tipo_referencia,
    id_bodega_origen, id_bodega_destino, evento, activa, leida
  )
  SELECT
    p_titulo, p_mensaje, 'warning', 'inventario',
    u.id_usuario, 0,
    p_id_traspaso, 'traspasos',
    v_bod_origen, v_bod_dest, p_evento, 1, 0
  FROM usuarios u
  WHERE u.activo = 1 AND u.id_bodega = v_target;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_sync_notif_traspasos;
DELIMITER $$
CREATE PROCEDURE sp_sync_notif_traspasos()
BEGIN
  UPDATE notificaciones n
  JOIN traspasos t ON n.id_referencia = t.id_traspaso AND n.tipo_referencia='traspasos'
  SET n.leida=1, n.activa=0, n.fecha_lectura=NOW()
  WHERE n.activa=1 AND (
    (n.evento='solicitud'   AND t.estado <> 'pendiente') OR
    (n.evento='autorizado'  AND t.estado NOT IN ('autorizado','en_transito')) OR
    (n.evento IN ('enviado','en_transito') AND t.estado NOT IN ('en_transito','enviado')) OR
    (t.estado IN ('recibido','cancelado','rechazado'))
  );
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_traspasos_ai;
DELIMITER $$
CREATE TRIGGER trg_traspasos_ai
AFTER INSERT ON traspasos
FOR EACH ROW
BEGIN
  CALL sp_notificar_traspaso_evento(
    NEW.id_traspaso,
    'solicitud',
    CONCAT('Solicitud de traspaso #', NEW.id_traspaso),
    'Se requiere autorización y envío'
  );
END $$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_traspasos_au_estado;
DELIMITER $$
CREATE TRIGGER trg_traspasos_au_estado
AFTER UPDATE ON traspasos
FOR EACH ROW
BEGIN
  IF NEW.estado <> OLD.estado THEN
    IF NEW.estado = 'autorizado' THEN
      CALL sp_notificar_traspaso_evento(NEW.id_traspaso, 'autorizado', CONCAT('Traspaso #', NEW.id_traspaso, ' autorizado'), 'Autorizado por bodega origen');
    ELSEIF NEW.estado IN ('enviado','en_transito') THEN
      CALL sp_notificar_traspaso_evento(NEW.id_traspaso, 'enviado', CONCAT('Traspaso #', NEW.id_traspaso, ' enviado'), 'En tránsito hacia bodega destino');
    ELSEIF NEW.estado = 'recibido' THEN
      CALL sp_notificar_traspaso_evento(NEW.id_traspaso, 'recibido', CONCAT('Traspaso #', NEW.id_traspaso, ' recibido'), 'Recepción registrada en bodega destino');
    ELSEIF NEW.estado IN ('rechazado','cancelado') THEN
      CALL sp_notificar_traspaso_evento(NEW.id_traspaso, NEW.estado, CONCAT('Traspaso #', NEW.id_traspaso, ' ', NEW.estado), 'Actualización de estado');
    END IF;
    CALL sp_sync_notif_traspasos();
  END IF;
END $$
DELIMITER ;
