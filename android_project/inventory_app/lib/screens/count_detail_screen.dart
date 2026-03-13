import 'package:flutter/material.dart';
import 'package:inventory_app/models/conteo_model.dart';
import 'package:inventory_app/models/detalle_conteo_model.dart';
import 'package:inventory_app/services/database_service.dart';
import 'package:inventory_app/screens/scan_screen.dart';
import 'package:inventory_app/widgets/product_image_widget.dart';

class CountDetailScreen extends StatefulWidget {
  final Conteo conteo;

  const CountDetailScreen({super.key, required this.conteo});

  @override
  _CountDetailScreenState createState() => _CountDetailScreenState();
}

class _CountDetailScreenState extends State<CountDetailScreen> {
  final _dbService = DatabaseService();
  List<DetalleConteo> _detalles = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadDetalles();
  }

  Future<void> _loadDetalles() async {
    try {
      final detalles = await _dbService.getDetalles(widget.conteo.id);
      setState(() {
        _detalles = detalles;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
      });
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Error: $e')));
    }
  }

  void _navigateToScan() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ScanScreen(conteoId: widget.conteo.id),
      ),
    );

    // Reload if data changed
    if (result == true) {
      _loadDetalles();
    }
  }

  void _editDetail(DetalleConteo detalle) async {
    // Allow manual edit by passing the product straight to scan screen logic (adapted)
    // For now, simpler to just open scan/count screen with pre-filled data if we had that logic,
    // but let's stick to the Scan Screen as the primary input method.
    // We can pass the barcode or detail ID to ScanScreen to "pre-select" it.

    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) =>
            ScanScreen(conteoId: widget.conteo.id, preSelectedDetail: detalle),
      ),
    );
    if (result == true) {
      _loadDetalles();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.conteo.nombre),
        actions: [
          IconButton(
            icon: const Icon(Icons.qr_code_scanner),
            onPressed: _navigateToScan,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Card(
                    color: Colors.blue[50],
                    child: Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: [
                          Column(
                            children: [
                              const Text(
                                'Total Productos',
                                style: TextStyle(color: Colors.grey),
                              ),
                              Text(
                                '${_detalles.length}',
                                style: const TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 18,
                                ),
                              ),
                            ],
                          ),
                          Column(
                            children: [
                              const Text(
                                'Contados',
                                style: TextStyle(color: Colors.grey),
                              ),
                              Text(
                                '${_detalles.where((d) => d.estado == "contado").length}',
                                style: const TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 18,
                                  color: Colors.green,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                Expanded(
                  child: ListView.builder(
                    itemCount: _detalles.length,
                    itemBuilder: (context, index) {
                      final detalle = _detalles[index];
                      final isContado = detalle.estado == 'contado';

                      return ListTile(
                        leading: ProductImageWidget(
                          idProducto: detalle.idProducto ?? 0,
                          idVariante: detalle.idVariante,
                          size: 40,
                        ),
                        title: Text(detalle.nombreProducto),
                        subtitle: Text(
                          'Talla: ${detalle.talla} | Color: ${detalle.color}\nCodigo: ${detalle.codigo}',
                        ),
                        trailing: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text(
                              'Cant: ${detalle.stockContado}',
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 16,
                              ),
                            ),
                            if (isContado)
                              const Icon(
                                Icons.verified,
                                size: 14,
                                color: Colors.green,
                              ),
                          ],
                        ),
                        onTap: () => _editDetail(detalle),
                      );
                    },
                  ),
                ),
              ],
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: _navigateToScan,
        child: const Icon(Icons.camera_alt),
      ),
    );
  }
}
