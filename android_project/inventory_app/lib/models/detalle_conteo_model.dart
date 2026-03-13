class DetalleConteo {
  final int id;
  final int idConteo;
  final int stockContado;
  final String nombreProducto;
  final String codigo;
  final String color;
  final String talla;
  String estado;
  final int? idVariante;
  final int? idProducto;

  DetalleConteo({
    required this.id,
    required this.idConteo,
    required this.stockContado,
    required this.nombreProducto,
    required this.codigo,
    required this.color,
    required this.talla,
    required this.estado,
    this.idVariante,
    this.idProducto,
  });

  factory DetalleConteo.fromJson(Map<String, dynamic> json) {
    return DetalleConteo(
      id: json['id_detalle_conteo'],
      idConteo: json['id_conteo'],
      stockContado: json['stock_contado'] ?? 0,
      nombreProducto: json['nombre_producto'] ?? 'Desconocido',
      codigo: json['codigo'] ?? '',
      color: json['color'] ?? '',
      talla: json['talla'] ?? '',
      estado: json['estado'] ?? 'pendiente',
      idVariante: json['id_variante'],
      idProducto: json['id_producto'],
    );
  }
}
