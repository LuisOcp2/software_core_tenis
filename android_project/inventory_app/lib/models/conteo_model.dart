class Conteo {
  final int id;
  final String nombre;
  final String fechaProgramada;
  final String tipo;
  final String estado;

  Conteo({
    required this.id,
    required this.nombre,
    required this.fechaProgramada,
    required this.tipo,
    required this.estado,
  });

  factory Conteo.fromJson(Map<String, dynamic> json) {
    return Conteo(
      id: json['id_conteo'],
      nombre: json['nombre'] ?? 'Sin nombre',
      fechaProgramada: json['fecha_programada'] ?? '',
      tipo: json['tipo'] ?? '',
      estado: json['estado'] ?? '',
    );
  }
}
