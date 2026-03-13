class Bodega {
  final int idBodega;
  final String? codigo;
  final String nombre;
  final String? direccion;
  final String? telefono;
  final String? responsable;
  final String? tipo;
  final bool activa;

  Bodega({
    required this.idBodega,
    this.codigo,
    required this.nombre,
    this.direccion,
    this.telefono,
    this.responsable,
    this.tipo,
    this.activa = true,
  });

  factory Bodega.fromMap(Map<String, dynamic> map) {
    return Bodega(
      idBodega: int.parse(map['id_bodega'].toString()),
      codigo: map['codigo'],
      nombre: map['nombre'] ?? '',
      direccion: map['direccion'],
      telefono: map['telefono'],
      responsable: map['responsable'],
      tipo: map['tipo'],
      activa:
          map['activa'] == 1 || map['activa'] == true || map['activa'] == '1',
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id_bodega': idBodega,
      'codigo': codigo,
      'nombre': nombre,
      'direccion': direccion,
      'telefono': telefono,
      'responsable': responsable,
      'tipo': tipo,
      'activa': activa ? 1 : 0,
    };
  }
}
