import 'dart:async';
import 'dart:collection';
import 'package:flutter/foundation.dart';
import 'package:mysql_client/mysql_client.dart';
import 'package:inventory_app/models/conteo_model.dart';
import 'package:inventory_app/models/detalle_conteo_model.dart';
import 'package:inventory_app/models/bodega_model.dart';
import 'package:inventory_app/models/traspaso_model.dart';
import 'package:inventory_app/models/traspaso_detalle_model.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:dbcrypt/dbcrypt.dart';

class DatabaseService {
  static final DatabaseService _instance = DatabaseService._internal();
  factory DatabaseService() => _instance;
  DatabaseService._internal();

  MySQLConnectionPool? _pool;

  // LRU Cache: Max 50 items
  static const int _maxCacheSize = 50;
  static final LinkedHashMap<int, Uint8List?> _imageCache = LinkedHashMap();

  // Concurrency Control: Max 4 simultaneous image requests
  static final _ImageSemaphore _semaphore = _ImageSemaphore(4);

  // Decrypted Credentials
  final String _host = '154.12.250.222';
  final String _database = 'soft_xtreme';
  final String _user = 'siro_admin';
  final String _password = 'Siro2025\$Prueba';
  final int _port = 3306;

  Future<void> _ensureConnection() async {
    _pool ??= MySQLConnectionPool(
      host: _host,
      port: _port,
      userName: _user,
      password: _password,
      databaseName: _database,
      maxConnections: 10,
      secure: false,
    );
  }

  Future<IResultSet> _query(String sql, [Map<String, dynamic>? params]) async {
    int retries = 2;
    while (retries > 0) {
      try {
        await _ensureConnection();
        return await _pool!.execute(sql, params);
      } catch (e) {
        String err = e.toString().toLowerCase();
        if (err.contains('connection closed') ||
            err.contains('socketexception') ||
            err.contains('mysqlclientexception') ||
            err.contains('broken pipe')) {
          try {
            await _pool?.close();
          } catch (_) {}
          _pool = null;
          retries--;
          // Avoid tight loop
          if (retries > 0) {
            await Future.delayed(const Duration(milliseconds: 1000));
          }
        } else {
          rethrow;
        }
      }
    }
    throw Exception(
      'Error de base de datos: Servicio no disponible momentáneamente',
    );
  }

  Future<Map<String, dynamic>> login(String username, String password) async {
    await _ensureConnection();
    final result = await _query(
      'SELECT id_usuario, username, password, nombre, rol, id_rol, ubicacion, id_bodega FROM usuarios WHERE username = :username AND activo = 1',
      {'username': username},
    );

    if (result.rows.isNotEmpty) {
      final user = result.rows.first.assoc();
      final dbPassword = user['password'] ?? '';
      bool passwordMatch = false;

      // Logic matching Java ServiceUser.authenticate:
      // 1. Check if it's a bcrypt hash
      if (dbPassword.length == 60 &&
          (dbPassword.startsWith('\$2a\$') ||
              dbPassword.startsWith('\$2b\$') ||
              dbPassword.startsWith('\$2y\$'))) {
        passwordMatch = DBCrypt().checkpw(password, dbPassword);
      } else {
        // 2. Legacy plain text check (matching Java password.equals(dbPassword))
        passwordMatch = (password == dbPassword);
      }

      if (passwordMatch) {
        SharedPreferences prefs = await SharedPreferences.getInstance();
        final int idUser = int.parse(user['id_usuario']!);
        final int idBodega = int.parse(user['id_bodega'] ?? '0');
        final String rol = user['rol'] ?? '';

        await prefs.setInt('userId', idUser);
        await prefs.setString('userName', user['nombre']!);
        await prefs.setInt('id_bodega', idBodega);
        await prefs.setString('rol', rol);

        // Fetch and store permissions
        final permissions = await getPermissions(idUser);
        await prefs.setStringList('permissions', permissions);

        user.remove('password');
        Map<String, dynamic> userData = Map<String, dynamic>.from(user);
        userData['id_usuario'] = idUser;
        userData['permissions'] = permissions;

        return {'success': true, 'user': userData};
      }
    }
    return {'success': false, 'message': 'Credenciales inválidas'};
  }

  Future<List<Conteo>> getConteos() async {
    final prefs = await SharedPreferences.getInstance();
    final int? userBodegaId = prefs.getInt('id_bodega');
    await _ensureConnection();

    String sql =
        "SELECT c.id_conteo, c.nombre, c.fecha_programada, c.tipo, c.estado FROM conteos_inventario c WHERE c.estado != 'completado' ";
    Map<String, dynamic> params = {};

    // Filtrar siempre por bodega si el usuario tiene una asignada,
    // a menos que sea un superadmin (pero el usuario pidió restringirlo)
    if (userBodegaId != null && userBodegaId > 0) {
      sql += " AND c.id_bodega = :idBodega ";
      params['idBodega'] = userBodegaId;
    }

    sql += " ORDER BY c.fecha_programada DESC";

    final result = await _query(sql, params);

    return result.rows.map((row) {
      final data = row.assoc();
      return Conteo(
        id: int.parse(data['id_conteo']!),
        nombre: data['nombre']!,
        fechaProgramada: data['fecha_programada']!,
        tipo: data['tipo']!,
        estado: data['estado']!,
      );
    }).toList();
  }

  Future<List<DetalleConteo>> getDetalles(int idConteo) async {
    await _ensureConnection();
    const sql = '''
      SELECT 
          d.id_detalle_conteo, 
          d.id_conteo,
          d.stock_contado,
          d.estado,
          d.id_variante,
          p.nombre as nombre_producto,
          pv.ean as codigo,
          c.nombre as color,
          t.numero as talla,
          p.id_producto
      FROM detalles_conteo_inventario d
      JOIN productos p ON d.id_producto = p.id_producto
      LEFT JOIN producto_variantes pv ON d.id_variante = pv.id_variante
      LEFT JOIN colores c ON pv.id_color = c.id_color
      LEFT JOIN tallas t ON pv.id_talla = t.id_talla
      WHERE d.id_conteo = :id
    ''';

    final result = await _query(sql, {'id': idConteo});

    return result.rows.map((row) {
      final data = row.assoc();
      return DetalleConteo(
        id: int.parse(data['id_detalle_conteo']!),
        idConteo: int.parse(data['id_conteo']!),
        stockContado: int.parse(data['stock_contado'] ?? '0'),
        idVariante: data['id_variante'] != null
            ? int.parse(data['id_variante']!)
            : null,
        estado: data['estado']!,
        nombreProducto: data['nombre_producto']!,
        codigo: data['codigo'] ?? '',
        color: data['color'] ?? '',
        talla: data['talla'] ?? '',
        idProducto: data['id_producto'] != null
            ? int.parse(data['id_producto']!)
            : null,
      );
    }).toList();
  }

  Future<bool> registrarConteo(
    int idDetalle,
    int cantidad,
    int idUsuario,
  ) async {
    await _ensureConnection();
    try {
      const sql = '''
        UPDATE detalles_conteo_inventario 
        SET 
            stock_contado = :cantidad,
            diferencia = :cantidad - stock_sistema,
            id_usuario_contador = :id_usuario,
            estado = 'contado',
            fecha_conteo = NOW()
        WHERE id_detalle_conteo = :id_detalle
      ''';

      final result = await _query(sql, {
        'cantidad': cantidad,
        'id_usuario': idUsuario,
        'id_detalle': idDetalle,
      });

      return result.affectedRows.toInt() > 0;
    } catch (e) {
      print('Error al registrar conteo: $e');
      return false;
    }
  }

  Future<DetalleConteo?> buscarProducto(int idConteo, String codigo) async {
    await _ensureConnection();
    try {
      // Clean code if scanner adds characters
      final cleanCode = codigo.trim();
      final searchTerm = '%$cleanCode%';

      const sql = '''
        SELECT 
            d.id_detalle_conteo, 
            d.id_conteo,
            d.stock_contado,
            d.estado,
            d.id_variante,
            p.nombre as nombre_producto,
            pv.ean as codigo,
            c.nombre as color,
            t.numero as talla,
            p.id_producto
        FROM detalles_conteo_inventario d
        JOIN productos p ON d.id_producto = p.id_producto
        LEFT JOIN producto_variantes pv ON d.id_variante = pv.id_variante
        LEFT JOIN colores c ON pv.id_color = c.id_color
        LEFT JOIN tallas t ON pv.id_talla = t.id_talla
        WHERE d.id_conteo = :id_conteo 
        AND (pv.ean = :codigo OR pv.sku = :codigo OR p.nombre LIKE :search_term OR p.codigo_modelo = :codigo)
        LIMIT 1
      ''';

      final result = await _query(sql, {
        'id_conteo': idConteo,
        'codigo': cleanCode,
        'search_term': searchTerm,
      });

      if (result.rows.isNotEmpty) {
        final data = result.rows.first.assoc();
        return DetalleConteo(
          id: int.parse(data['id_detalle_conteo']!),
          idConteo: int.parse(data['id_conteo']!),
          stockContado: int.parse(data['stock_contado'] ?? '0'),
          idVariante: data['id_variante'] != null
              ? int.parse(data['id_variante']!)
              : null,
          estado: data['estado']!,
          nombreProducto: data['nombre_producto']!,
          codigo: data['codigo'] ?? '',
          color: data['color'] ?? '',
          talla: data['talla'] ?? '',
          idProducto: data['id_producto'] != null
              ? int.parse(data['id_producto']!)
              : null,
        );
      }
      return null;
    } catch (e) {
      print('Error al buscar producto: $e');
      return null;
    }
  }

  Future<List<String>> getPermissions(int userId) async {
    try {
      // 1. Check customized permissions (PrivilegioUsuario)
      final customResult = await _query(
        'SELECT p.modulo FROM privilegio_usuario pu JOIN permisos p ON pu.id_permiso = p.id_permiso WHERE pu.id_usuario = :id AND (pu.puede_ver = 1 OR pu.puede_crear = 1 OR pu.puede_editar = 1 OR pu.puede_eliminar = 1)',
        {'id': userId},
      );

      if (customResult.rows.isNotEmpty) {
        return customResult.rows.map((row) => row.assoc()['modulo']!).toList();
      }

      // 2. Fallback to Role permissions
      final roleResult = await _query(
        'SELECT p.modulo FROM roles_privilegios rp JOIN permisos p ON rp.id_permiso = p.id_permiso JOIN usuarios u ON u.id_rol = rp.id_rol WHERE u.id_usuario = :id',
        {'id': userId},
      );

      return roleResult.rows.map((row) => row.assoc()['modulo']!).toList();
    } catch (e) {
      print('Error loading permissions: $e');
      return [];
    }
  }

  Future<Map<int, String>> getMarcas() async {
    await _ensureConnection();
    final result = await _query(
      'SELECT id_marca, nombre FROM marcas WHERE activo = 1 ORDER BY nombre',
    );
    return {
      for (var row in result.rows)
        int.parse(row.assoc()['id_marca']!): row.assoc()['nombre']!,
    };
  }

  Future<Map<int, String>> getTallas() async {
    await _ensureConnection();
    final result = await _query(
      "SELECT id_talla, CONCAT(numero, ' ', COALESCE(sistema,''), CASE genero WHEN 'HOMBRE' THEN ' (H)' WHEN 'MUJER' THEN ' (M)' WHEN 'NIÑO' THEN ' (N)' ELSE '' END) AS talla FROM tallas WHERE activo = 1 ORDER BY numero, genero",
    );
    return {
      for (var row in result.rows)
        int.parse(row.assoc()['id_talla']!): row.assoc()['talla']!,
    };
  }

  Future<Map<int, String>> getColores() async {
    await _ensureConnection();
    final result = await _query(
      'SELECT id_color, nombre FROM colores WHERE activo = 1 ORDER BY nombre',
    );
    return {
      for (var row in result.rows)
        int.parse(row.assoc()['id_color']!): row.assoc()['nombre']!,
    };
  }

  Future<List<TraspasoDetalle>> buscarProductosOptimizado({
    String? texto,
    int? idMarca,
    int? idTalla,
    int? idColor,
    required int idBodega,
  }) async {
    await _ensureConnection();

    // Base SQL following Java BuscadorProductoDialog logic
    String sql = '''
      SELECT pv.id_variante, pv.id_producto, p.nombre as nombre_producto, p.codigo_modelo, 
             COALESCE(pv.ean, '') AS ean, COALESCE(pv.sku, '') AS sku, 
             CONCAT(COALESCE(t.numero, ''), ' ', COALESCE(t.sistema,'')) AS talla, 
             COALESCE(c.nombre, '') AS color, 
             COALESCE(ib.Stock_par, 0) AS Stock_par, 
             COALESCE(ib.Stock_caja, 0) AS Stock_caja, 
             COALESCE(m.nombre, '') AS marca
      FROM producto_variantes pv 
      INNER JOIN productos p ON pv.id_producto = p.id_producto AND p.activo = 1 
      LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.id_bodega = :idBodega AND ib.activo = 1 
      LEFT JOIN tallas t ON pv.id_talla = t.id_talla 
      LEFT JOIN colores c ON pv.id_color = c.id_color 
      LEFT JOIN marcas m ON p.id_marca = m.id_marca 
      WHERE 1=1 
    ''';

    Map<String, dynamic> params = {'idBodega': idBodega};

    if (texto != null && texto.isNotEmpty) {
      final cleanText = texto.trim();
      sql +=
          ' AND (p.nombre LIKE :text OR p.codigo_modelo LIKE :text OR pv.ean = :exactText OR pv.sku = :exactText) ';
      params['text'] = '%$cleanText%';
      params['exactText'] = cleanText;
    }

    if (idMarca != null && idMarca > 0) {
      sql += ' AND p.id_marca = :idMarca ';
      params['idMarca'] = idMarca;
    }

    if (idTalla != null && idTalla > 0) {
      sql += ' AND pv.id_talla = :idTalla ';
      params['idTalla'] = idTalla;
    }

    if (idColor != null && idColor > 0) {
      sql += ' AND pv.id_color = :idColor ';
      params['idColor'] = idColor;
    }

    // Filter: Only greater than 0
    sql +=
        ' AND (COALESCE(ib.Stock_par, 0) > 0 OR COALESCE(ib.Stock_caja, 0) > 0) ';

    sql += ' ORDER BY p.nombre, t.numero LIMIT 100';

    final result = await _query(sql, params);
    return result.rows
        .map((row) => TraspasoDetalle.fromMap(row.typedAssoc()))
        .toList();
  }

  Future<Uint8List?> getImagenVariante(int idVariante) async {
    if (idVariante <= 0) return null;

    // Check Cache (LRU)
    if (_imageCache.containsKey(idVariante)) {
      // Re-insert to mark as recently used
      final data = _imageCache.remove(idVariante);
      _imageCache[idVariante] = data;
      return data;
    }

    return _semaphore.run(() async {
      try {
        print('DEBUG: [getImagenVariante] Starting loading v:$idVariante');

        // Fetch as HEX to avoid UTF-8 decoding issues
        // We use _query which has retry logic
        final result = await _query(
          'SELECT HEX(imagen) as imagen_hex FROM producto_variantes WHERE id_variante = :id',
          {'id': idVariante},
        );

        Uint8List? blob;
        if (result.rows.isNotEmpty) {
          final hexStr = result.rows.first.assoc()['imagen_hex'];
          if (hexStr != null && hexStr.isNotEmpty) {
            // Offload heavy decoding to background isolate
            blob = await compute(decodeHex, hexStr);
          }
        }

        // Fallback to product image
        if (blob == null) {
          // We do a quick check for product ID
          final prodIdResult = await _query(
            'SELECT id_producto FROM producto_variantes WHERE id_variante = :id',
            {'id': idVariante},
          );
          if (prodIdResult.rows.isNotEmpty) {
            final prodId = int.tryParse(
              prodIdResult.rows.first.assoc()['id_producto'] ?? '',
            );
            if (prodId != null) {
              print('DEBUG: [getImagenVariante] Fallback to p:$prodId');
              blob = await getImagenProducto(prodId);
            }
          }
        }

        // Update Cache (LRU Logic)
        if (blob != null) {
          if (_imageCache.length >= _maxCacheSize) {
            _imageCache.remove(_imageCache.keys.first); // Remove oldest
          }
          _imageCache[idVariante] = blob;
        } else {
          // Cache nulls too to avoid repeated failed queries?
          // For now, avoiding caching nulls might be safer for retry,
          // but caching 'no image' is good for performance.
          // Let's cache nulls but maybe with a separate logic?
          // Simpler: Just cache it.
          if (_imageCache.length >= _maxCacheSize) {
            _imageCache.remove(_imageCache.keys.first);
          }
          _imageCache[idVariante] = null;
        }

        return blob;
      } catch (e) {
        print('DEBUG: [getImagenVariante] Error v:$idVariante > $e');
        return null;
      }
    });
  }

  Future<Uint8List?> getImagenProducto(int id) async {
    // Note: getImagenProducto is usually called by getImagenVariante
    // If called directly, it should also use semaphore?
    // Since getImagenVariante calls this, we might block if we wrap this in semaphore too
    // IF the lock is re-entrant.
    // Our simple semaphore is NOT re-entrant.
    // HOWEVER, getImagenVariante calls this inside the lock.
    // So we should NOT wrap this in _semaphore.run if called from there.
    // BUT we might call it directly from UI.
    // To be safe and simple: We will execute the query directly here.
    // Since getImagenVariante already holds the lock when it calls this,
    // the DB connection usage is "accounted for".
    // If called independently, it bypasses the limit?
    // That's acceptable for fallback, or we create a private _getImagenProductoInternal.

    try {
      String sql = '''
          SELECT HEX(imagen) as imagen_hex FROM producto_variantes 
          WHERE id_producto = :id 
          AND imagen IS NOT NULL AND imagen != "" 
          LIMIT 1
        ''';

      final result = await _query(sql, {'id': id});

      if (result.rows.isNotEmpty) {
        final hexStr = result.rows.first.assoc()['imagen_hex'];
        if (hexStr != null && hexStr.isNotEmpty) {
          return await compute(decodeHex, hexStr);
        }
      }
    } catch (e) {
      print('DEBUG: [getImagenProducto] Error p:$id > $e');
    }
    return null;
  }

  // --- MÉTODOS DE TRASPASOS ---

  Future<List<Bodega>> getBodegas() async {
    await _ensureConnection();
    final result = await _query(
      'SELECT id_bodega, codigo, nombre, direccion, telefono, responsable, tipo, activa FROM bodegas WHERE activa = 1 ORDER BY nombre',
    );
    return result.rows.map((row) => Bodega.fromMap(row.assoc())).toList();
  }

  Future<String> generarNumeroTraspaso() async {
    await _ensureConnection();
    final result = await _query(
      "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_traspaso, 3) AS UNSIGNED)), 0) + 1 as next_number FROM traspasos WHERE numero_traspaso LIKE 'TR%'",
    );
    if (result.rows.isNotEmpty) {
      final nextNumber = int.parse(result.rows.first.assoc()['next_number']!);
      return 'TR${nextNumber.toString().padLeft(6, '0')}';
    }
    return 'TR000001';
  }

  Future<bool> crearTraspaso(Traspaso traspaso) async {
    await _ensureConnection();
    try {
      // Usar transacción manual si el pool lo permite o ejecutar secuencialmente
      // En mysql_client, el pool maneja conexiones, implementamos lógica secuencial

      const sqlTraspaso = '''
        INSERT INTO traspasos (numero_traspaso, id_bodega_origen, id_bodega_destino, 
        id_usuario_solicita, fecha_solicitud, estado, motivo, observaciones, total_productos) 
        VALUES (:numeroString, :idOrigen, :idDestino, :idUsuario, NOW(), 'pendiente', :motivo, :observaciones, :totalProductos)
      ''';

      final result = await _query(sqlTraspaso, {
        'numeroString': traspaso.numeroTraspaso,
        'idOrigen': traspaso.idBodegaOrigen,
        'idDestino': traspaso.idBodegaDestino,
        'idUsuario': traspaso.idUsuarioSolicita,
        'motivo': traspaso.motivo ?? '',
        'observaciones': traspaso.observaciones ?? '',
        'totalProductos': traspaso.productos.length,
      });

      if (result.affectedRows.toInt() > 0) {
        final idTraspaso = result.lastInsertID.toInt();

        for (var detalle in traspaso.productos) {
          const sqlDetalle = '''
            INSERT INTO traspaso_detalles (id_traspaso, id_producto, id_variante, 
            cantidad_solicitada, Tipo, observaciones, estado_detalle) 
            VALUES (:idTraspaso, :idProducto, :idVariante, :cantidad, :tipo, :obs, 'pendiente')
          ''';
          await _query(sqlDetalle, {
            'idTraspaso': idTraspaso,
            'idProducto': detalle.idProducto,
            'idVariante': detalle.idVariante,
            'cantidad': detalle.cantidadSolicitada,
            'tipo': detalle.tipo,
            'obs': detalle.observaciones ?? '',
          });
        }
        return true;
      }
      return false;
    } catch (e) {
      print('Error al crear traspaso: $e');
      return false;
    }
  }

  Future<List<Traspaso>> getTraspasos({String? estado}) async {
    final prefs = await SharedPreferences.getInstance();
    final int? userBodegaId = prefs.getInt('id_bodega');
    final String? rol = prefs.getString('rol')?.toLowerCase();
    final bool isAdmin =
        rol == 'admin' ||
        (prefs.getStringList('permissions')?.contains('admin_traspasos') ??
            false);

    String sql = '''
      SELECT t.*, bo.nombre as bodega_origen, bd.nombre as bodega_destino 
      FROM traspasos t 
      INNER JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega 
      INNER JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega 
    ''';

    Map<String, dynamic> params = {};
    List<String> conditions = [];

    if (estado != null && estado != 'Todos') {
      conditions.add('t.estado = :estado');
      params['estado'] = estado;
    }

    // Restriction: Only show transfers involving my warehouse, unless admin
    if (!isAdmin && userBodegaId != null) {
      conditions.add(
        '(t.id_bodega_origen = :userBodega OR t.id_bodega_destino = :userBodega)',
      );
      params['userBodega'] = userBodegaId;
    }

    if (conditions.isNotEmpty) {
      sql += ' WHERE ${conditions.join(' AND ')}';
    }

    sql += ' ORDER BY t.fecha_solicitud DESC LIMIT 100';

    final result = await _query(sql, params);
    return result.rows.map((row) => Traspaso.fromMap(row.assoc())).toList();
  }

  Future<List<TraspasoDetalle>> getTraspasoDetalles(int idTraspaso) async {
    await _ensureConnection();
    const sql = '''
      SELECT td.*, p.nombre as nombre_producto, pv.ean, c.nombre as color, t.numero as talla, p.id_producto
      FROM traspaso_detalles td
      JOIN productos p ON td.id_producto = p.id_producto
      LEFT JOIN producto_variantes pv ON td.id_variante = pv.id_variante
      LEFT JOIN colores c ON pv.id_color = c.id_color
      LEFT JOIN tallas t ON pv.id_talla = t.id_talla
      WHERE td.id_traspaso = :id
    ''';
    final result = await _query(sql, {'id': idTraspaso});
    return result.rows
        .map((row) => TraspasoDetalle.fromMap(row.assoc()))
        .toList();
  }

  Future<bool> actualizarEstadoTraspaso(
    String numeroTraspaso,
    String nuevoEstado,
    int idUsuario,
  ) async {
    await _ensureConnection();
    try {
      String sql = 'UPDATE traspasos SET estado = :estado';
      Map<String, dynamic> params = {
        'estado': nuevoEstado,
        'numero': numeroTraspaso,
      };

      if (nuevoEstado == 'autorizado') {
        sql += ', fecha_autorizacion = NOW(), id_usuario_autoriza = :idUsuario';
        params['idUsuario'] = idUsuario;
      } else if (nuevoEstado == 'enviado') {
        sql += ', fecha_envio = NOW(), id_usuario_envia = :idUsuario';
        params['idUsuario'] = idUsuario;
      } else if (nuevoEstado == 'recibido') {
        sql += ', fecha_recepcion = NOW(), id_usuario_recibe = :idUsuario';
        params['idUsuario'] = idUsuario;
      }

      sql += ' WHERE numero_traspaso = :numero';

      final result = await _query(sql, params);
      return result.affectedRows.toInt() > 0;
    } catch (e) {
      print('Error al actualizar estado de traspaso: $e');
      return false;
    }
  }

  Future<TraspasoDetalle?> buscarProductoParaTraspaso(
    String codigo, {
    int? idBodega,
  }) async {
    await _ensureConnection();
    try {
      final cleanCode = codigo.trim();
      const sql = '''
        SELECT p.id_producto, p.nombre as nombre_producto, pv.id_variante, pv.ean, 
               c.nombre as color, t.numero as talla, 
               COALESCE(ib.Stock_par, 0) as Stock_par, COALESCE(ib.Stock_caja, 0) as Stock_caja,
               m.nombre as marca
        FROM productos p
        LEFT JOIN producto_variantes pv ON p.id_producto = pv.id_producto
        LEFT JOIN colores c ON pv.id_color = c.id_color
        LEFT JOIN tallas t ON pv.id_talla = t.id_talla
        LEFT JOIN marcas m ON p.id_marca = m.id_marca
        LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante AND ib.id_bodega = :idBodega AND ib.activo = 1
        WHERE pv.ean = :codigo OR pv.sku = :codigo OR p.codigo_modelo = :codigo OR p.nombre LIKE :search
        LIMIT 1
      ''';
      final result = await _query(sql, {
        'codigo': cleanCode,
        'search': '%$cleanCode%',
        'idBodega': idBodega ?? 0,
      });

      print(
        'DEBUG: buscarProductoParaTraspaso result count: ${result.rows.length}',
      );
      if (result.rows.isNotEmpty) {
        final rowMap = result.rows.first.assoc();
        print('DEBUG: buscarProductoParaTraspaso row: $rowMap');
        return TraspasoDetalle.fromMap(rowMap);
      }
      return null;
    } catch (e) {
      print('Error al buscar producto para traspaso: $e');
      return null;
    }
  }
}

// Top-level function for compute
Uint8List decodeHex(String hex) {
  if (hex.length % 2 != 0) {
    throw Exception('Invalid hex string length: ${hex.length}');
  }
  var bytes = Uint8List(hex.length ~/ 2);
  for (var i = 0; i < hex.length; i += 2) {
    var byte = int.parse(hex.substring(i, i + 2), radix: 16);
    bytes[i ~/ 2] = byte;
  }
  return bytes;
}

// Simple Semaphore for concurrency control
class _ImageSemaphore {
  final int maxConcurrent;
  int _current = 0;
  final List<Completer<void>> _queue = [];

  _ImageSemaphore(this.maxConcurrent);

  Future<T> run<T>(Future<T> Function() task) async {
    if (_current >= maxConcurrent) {
      final completer = Completer<void>();
      _queue.add(completer);
      await completer.future;
    }
    _current++;
    try {
      return await task();
    } finally {
      _current--;
      if (_queue.isNotEmpty) {
        _queue.removeAt(0).complete();
      }
    }
  }
}
