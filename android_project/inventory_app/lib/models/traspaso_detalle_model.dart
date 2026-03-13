class TraspasoDetalle {
  final int? idDetalleTraspaso;
  final int? idTraspaso;
  final int idProducto;
  final int? idVariante;
  final String nombreProducto;
  final String? color;
  final String? talla;
  final String tipo; // 'caja' o 'par'
  int cantidadSolicitada;
  int cantidadEnviada;
  int cantidadRecibida;
  final String? observaciones;
  final String? ean;
  final int stockPar;
  final int stockCaja;
  final String? marca;

  TraspasoDetalle({
    this.idDetalleTraspaso,
    this.idTraspaso,
    required this.idProducto,
    this.idVariante,
    required this.nombreProducto,
    this.color,
    this.talla,
    this.tipo = 'par',
    this.cantidadSolicitada = 0,
    this.cantidadEnviada = 0,
    this.cantidadRecibida = 0,
    this.observaciones,
    this.ean,
    this.stockPar = 0,
    this.stockCaja = 0,
    this.marca,
  });

  factory TraspasoDetalle.fromMap(Map<String, dynamic> map) {
    int parseInt(dynamic val) {
      if (val == null || val.toString() == 'null') return 0;
      return int.tryParse(val.toString()) ?? 0;
    }

    int? parseNullableInt(dynamic val) {
      if (val == null || val.toString() == 'null') return null;
      return int.tryParse(val.toString());
    }

    return TraspasoDetalle(
      idDetalleTraspaso: parseNullableInt(map['id_detalle_traspaso']),
      idTraspaso: parseNullableInt(map['id_traspaso']),
      idProducto: parseInt(map['id_producto']),
      idVariante: parseNullableInt(map['id_variante']),
      nombreProducto: map['nombre_producto']?.toString() ?? '',
      color: map['color']?.toString(),
      talla: map['talla']?.toString(),
      tipo: map['Tipo']?.toString() ?? 'par',
      cantidadSolicitada: parseInt(map['cantidad_solicitada']),
      cantidadEnviada: parseInt(map['cantidad_enviada']),
      cantidadRecibida: parseInt(map['cantidad_recibida']),
      observaciones: map['observaciones']?.toString(),
      ean: map['ean']?.toString(),
      stockPar: parseInt(map['Stock_par']),
      stockCaja: parseInt(map['Stock_caja']),
      marca: map['marca']?.toString(),
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id_detalle_traspaso': idDetalleTraspaso,
      'id_traspaso': idTraspaso,
      'id_producto': idProducto,
      'id_variante': idVariante,
      'cantidad_solicitada': cantidadSolicitada,
      'cantidad_enviada': cantidadEnviada,
      'cantidad_recibida': cantidadRecibida,
      'Tipo': tipo,
      'observaciones': observaciones,
    };
  }

  String get nombreCompleto {
    String prod = nombreProducto;
    List<String> details = [];
    if (color != null && color!.isNotEmpty) details.add(color!);
    if (talla != null && talla!.isNotEmpty) details.add(talla!);

    if (details.isNotEmpty) {
      return "$prod (${details.join(' - ')})";
    }
    return prod;
  }
}
