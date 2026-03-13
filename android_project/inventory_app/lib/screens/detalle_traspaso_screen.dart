import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/traspaso_model.dart';
import '../models/traspaso_detalle_model.dart';
import '../services/database_service.dart';
import '../widgets/product_image_widget.dart';

class DetalleTraspasoScreen extends StatefulWidget {
  final Traspaso traspaso;

  const DetalleTraspasoScreen({super.key, required this.traspaso});

  @override
  _DetalleTraspasoScreenState createState() => _DetalleTraspasoScreenState();
}

class _DetalleTraspasoScreenState extends State<DetalleTraspasoScreen> {
  final _dbService = DatabaseService();
  List<TraspasoDetalle> _detalles = [];
  bool _isLoading = true;
  int? _userId;
  int? _idBodegaUser;
  String? _rol;
  List<String> _permissions = [];
  bool _isAdmin = false;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final items = await _dbService.getTraspasoDetalles(
        widget.traspaso.idTraspaso!,
      );
      setState(() {
        _userId = prefs.getInt('userId');
        _idBodegaUser = prefs.getInt('id_bodega');
        _rol = prefs.getString('rol')?.toLowerCase();
        _permissions = prefs.getStringList('permissions') ?? [];
        _isAdmin = _rol == 'admin' || _permissions.contains('admin_traspasos');

        _detalles = items;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
      _showError('Error al cargar detalles: $e');
    }
  }

  void _showError(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  Future<void> _cambiarEstado(String nuevoEstado) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Confirmar Acción'),
        content: Text('¿Está seguro de cambiar el estado a $nuevoEstado?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('CANCELAR'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('CONFIRMAR'),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    setState(() => _isLoading = true);
    final success = await _dbService.actualizarEstadoTraspaso(
      widget.traspaso.numeroTraspaso,
      nuevoEstado,
      _userId!,
    );
    setState(() => _isLoading = false);

    if (success) {
      Navigator.pop(context, true);
    } else {
      _showError('No se pudo actualizar el estado del traspaso');
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = widget.traspaso;
    return Scaffold(
      appBar: AppBar(title: Text('Detalle: ${t.numeroTraspaso}')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                _buildHeader(t),
                const Divider(),
                Expanded(child: _buildItemsList()),
                _buildActionButtons(t),
              ],
            ),
    );
  }

  Widget _buildHeader(Traspaso t) {
    return Container(
      padding: const EdgeInsets.all(16),
      width: double.infinity,
      color: Colors.blue[50],
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _infoRow(Icons.warehouse, 'Origen: ', t.nombreBodegaOrigen ?? 'N/A'),
          _infoRow(
            Icons.local_shipping,
            'Destino: ',
            t.nombreBodegaDestino ?? 'N/A',
          ),
          _infoRow(
            Icons.info_outline,
            'Estado: ',
            t.estado.toUpperCase(),
            bold: true,
          ),
          if (t.motivo != null && t.motivo!.isNotEmpty)
            _infoRow(Icons.comment, 'Motivo: ', t.motivo!),
        ],
      ),
    );
  }

  Widget _infoRow(
    IconData icon,
    String label,
    String value, {
    bool bold = false,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        children: [
          Icon(icon, size: 16, color: Colors.blue[800]),
          const SizedBox(width: 8),
          Text(label, style: const TextStyle(fontWeight: FontWeight.bold)),
          Text(
            value,
            style: TextStyle(
              fontWeight: bold ? FontWeight.bold : FontWeight.normal,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildItemsList() {
    return ListView.builder(
      itemCount: _detalles.length,
      itemBuilder: (ctx, i) {
        final d = _detalles[i];
        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
          child: ListTile(
            leading: ProductImageWidget(
              idProducto: d.idProducto,
              idVariante: d.idVariante,
              size: 50,
            ),
            title: Text(d.nombreCompleto),
            subtitle: Text('EAN: ${d.ean ?? 'N/A'}'),
            trailing: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  'Cant: ${d.cantidadSolicitada}',
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
                Text(
                  d.tipo,
                  style: const TextStyle(fontSize: 10, color: Colors.grey),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildActionButtons(Traspaso t) {
    List<Widget> buttons = [];

    final state = t.estado.toLowerCase();

    // Permissions logic matching Java TraspasoPermissionValidator
    final canAuthorize =
        state == 'pendiente' &&
        (_isAdmin ||
            (_permissions.contains('autorizar_traspaso') &&
                _idBodegaUser == t.idBodegaOrigen));
    final canSend =
        state == 'autorizado' &&
        (_isAdmin ||
            (_permissions.contains('enviar_traspaso') &&
                _idBodegaUser == t.idBodegaOrigen));
    final canReceive =
        (state == 'enviado' || state == 'en_transito') &&
        (_isAdmin ||
            (_permissions.contains('recibir_traspaso') &&
                _idBodegaUser == t.idBodegaDestino));
    final canCancel =
        (state != 'recibido' && state != 'cancelado') &&
        (_isAdmin ||
            (_permissions.contains('cancelar_traspaso') &&
                (_idBodegaUser == t.idBodegaOrigen ||
                    _idBodegaUser == t.idBodegaDestino)));

    if (canAuthorize) {
      buttons.add(
        _actionButton(
          'AUTORIZAR',
          Colors.blue,
          () => _cambiarEstado('autorizado'),
        ),
      );
    }

    if (canSend) {
      buttons.add(
        _actionButton('ENVIAR', Colors.purple, () => _cambiarEstado('enviado')),
      );
    }

    if (canReceive) {
      buttons.add(
        _actionButton(
          'RECIBIR',
          Colors.green,
          () => _cambiarEstado('recibido'),
        ),
      );
    }

    if (canCancel) {
      buttons.add(
        _actionButton(
          'CANCELAR',
          Colors.red,
          () => _cambiarEstado('cancelado'),
        ),
      );
    }

    if (buttons.isEmpty) return const SizedBox.shrink();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: const BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black12,
            blurRadius: 4,
            offset: Offset(0, -2),
          ),
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: buttons
            .map(
              (b) => Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4),
                  child: b,
                ),
              ),
            )
            .toList(),
      ),
    );
  }

  Widget _actionButton(String text, Color color, VoidCallback onPressed) {
    return ElevatedButton(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: color,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(vertical: 12),
      ),
      child: Text(text),
    );
  }
}
