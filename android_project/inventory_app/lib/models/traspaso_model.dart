import 'traspaso_detalle_model.dart';

class Traspaso {
  final int? idTraspaso;
  final String numeroTraspaso;
  final DateTime? fechaSolicitud;
  final int idBodegaOrigen;
  final String? nombreBodegaOrigen;
  final int idBodegaDestino;
  final String? nombreBodegaDestino;
  final String
  estado; // 'pendiente', 'autorizado', 'en_transito', 'recibido', 'cancelado'
  final String? motivo;
  final String? observaciones;
  final int? idUsuarioSolicita;
  final List<TraspasoDetalle> productos;

  Traspaso({
    this.idTraspaso,
    required this.numeroTraspaso,
    this.fechaSolicitud,
    required this.idBodegaOrigen,
    this.nombreBodegaOrigen,
    required this.idBodegaDestino,
    this.nombreBodegaDestino,
    this.estado = 'pendiente',
    this.motivo,
    this.observaciones,
    this.idUsuarioSolicita,
    this.productos = const [],
  });

  factory Traspaso.fromMap(
    Map<String, dynamic> map, {
    List<TraspasoDetalle> productos = const [],
  }) {
    return Traspaso(
      idTraspaso: map['id_traspaso'] != null
          ? int.parse(map['id_traspaso'].toString())
          : null,
      numeroTraspaso: map['numero_traspaso'] ?? '',
      fechaSolicitud: map['fecha_solicitud'] != null
          ? DateTime.tryParse(map['fecha_solicitud'].toString())
          : null,
      idBodegaOrigen: int.parse(map['id_bodega_origen']?.toString() ?? '0'),
      nombreBodegaOrigen: map['bodega_origen'],
      idBodegaDestino: int.parse(map['id_bodega_destino']?.toString() ?? '0'),
      nombreBodegaDestino: map['bodega_destino'],
      estado: map['estado'] ?? 'pendiente',
      motivo: map['motivo'],
      observaciones: map['observaciones'],
      idUsuarioSolicita: map['id_usuario_solicita'] != null
          ? int.parse(map['id_usuario_solicita'].toString())
          : null,
      productos: productos,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id_traspaso': idTraspaso,
      'numero_traspaso': numeroTraspaso,
      'id_bodega_origen': idBodegaOrigen,
      'id_bodega_destino': idBodegaDestino,
      'id_usuario_solicita': idUsuarioSolicita,
      'estado': estado,
      'motivo': motivo,
      'observaciones': observaciones,
      'total_productos': productos.length,
    };
  }
}
