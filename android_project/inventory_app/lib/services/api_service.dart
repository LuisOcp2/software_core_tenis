import 'package:inventory_app/models/conteo_model.dart';
import 'package:inventory_app/models/detalle_conteo_model.dart';
import 'package:inventory_app/services/database_service.dart';

class ApiService {
  final DatabaseService _dbService = DatabaseService();

  Future<Map<String, dynamic>> login(String username, String password) async {
    return await _dbService.login(username, password);
  }

  Future<List<Conteo>> getConteos() async {
    return await _dbService.getConteos();
  }

  Future<List<DetalleConteo>> getDetalles(int idConteo) async {
    return await _dbService.getDetalles(idConteo);
  }

  Future<bool> registrarConteo(
    int idDetalle,
    int cantidad,
    int idUsuario,
  ) async {
    return await _dbService.registrarConteo(idDetalle, cantidad, idUsuario);
  }

  Future<DetalleConteo?> buscarProducto(int idConteo, String codigo) async {
    return await _dbService.buscarProducto(idConteo, codigo);
  }
}
