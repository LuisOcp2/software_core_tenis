import 'package:flutter/material.dart';
import 'package:inventory_app/models/detalle_conteo_model.dart';
import 'package:inventory_app/models/traspaso_detalle_model.dart';
import 'package:inventory_app/services/database_service.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ScanScreen extends StatefulWidget {
  final int? conteoId;
  final DetalleConteo? preSelectedDetail;
  final bool isForTraspaso;
  final int? idBodegaOrigen;

  const ScanScreen({super.key, 
    this.conteoId,
    this.preSelectedDetail,
    this.isForTraspaso = false,
    this.idBodegaOrigen,
  });

  @override
  _ScanScreenState createState() => _ScanScreenState();
}

class _ScanScreenState extends State<ScanScreen> with WidgetsBindingObserver {
  final _dbService = DatabaseService();
  final _qtyController = TextEditingController(text: '1');
  final _barcodeController = TextEditingController();

  final MobileScannerController _scannerController = MobileScannerController(
    detectionSpeed: DetectionSpeed.noDuplicates,
  );

  dynamic _currentResult; // Can be DetalleConteo or TraspasoDetalle
  bool _isProcessing = false;
  String? _message;
  bool _isError = false;

  @override
  void initState() {
    super.initState();
    if (widget.preSelectedDetail != null) {
      _currentResult = widget.preSelectedDetail;
      _barcodeController.text = widget.preSelectedDetail!.codigo;
      _qtyController.text = widget.preSelectedDetail!.stockContado > 0
          ? widget.preSelectedDetail!.stockContado.toString()
          : '1';
      _message = "Producto: ${widget.preSelectedDetail!.nombreProducto}";
    }
  }

  @override
  void dispose() {
    _scannerController.dispose();
    super.dispose();
  }

  void _onDetect(BarcodeCapture capture) {
    if (_isProcessing || _currentResult != null) return;

    final List<Barcode> barcodes = capture.barcodes;
    for (final barcode in barcodes) {
      if (barcode.rawValue != null) {
        final code = barcode.rawValue!.trim();
        setState(() {
          _barcodeController.text = code;
        });
        _searchProduct(code);
        break;
      }
    }
  }

  Future<void> _searchProduct(String code) async {
    if (code.isEmpty) return;

    setState(() {
      _isProcessing = true;
      _message = "Buscando '$code'...";
      _isError = false;
    });

    try {
      if (widget.isForTraspaso) {
        final result = await _dbService.buscarProductoParaTraspaso(
          code,
          idBodega: widget.idBodegaOrigen,
        );
        if (result != null) {
          setState(() {
            _currentResult = result;
            _message = "Encontrado: ${result.nombreProducto}";
            _qtyController.text = '1';
          });
        } else {
          setState(() {
            _message = "Producto no encontrado en bodega origen";
            _isError = true;
          });
        }
      } else {
        if (widget.conteoId == null) {
          _showError('No se especificó ID de conteo');
          return;
        }
        final result = await _dbService.buscarProducto(widget.conteoId!, code);
        if (result != null) {
          setState(() {
            _currentResult = result;
            _message = "Encontrado: ${result.nombreProducto}";
            if (result.stockContado > 0) {
              _qtyController.text = result.stockContado.toString();
            } else {
              _qtyController.text = '1';
            }
          });
        } else {
          setState(() {
            _message = "No encontrado en lista de conteo";
            _isError = true;
          });
        }
      }
    } catch (e) {
      setState(() {
        _message = "Error: $e";
        _isError = true;
      });
    } finally {
      setState(() => _isProcessing = false);
    }
  }

  void _showError(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  Future<void> _processFoundProduct() async {
    if (_currentResult == null) return;

    if (widget.isForTraspaso) {
      final TraspasoDetalle item = _currentResult;
      item.cantidadSolicitada = int.tryParse(_qtyController.text) ?? 1;
      Navigator.pop(context, item);
    } else {
      setState(() => _isProcessing = true);
      try {
        final DetalleConteo item = _currentResult;
        final prefs = await SharedPreferences.getInstance();
        final userId = prefs.getInt('userId') ?? 0;
        final qty = int.tryParse(_qtyController.text) ?? 1;

        final success = await _dbService.registrarConteo(item.id, qty, userId);
        if (success) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Conteo guardado'),
              backgroundColor: Colors.green,
            ),
          );
          if (widget.preSelectedDetail != null) {
            Navigator.pop(context, true);
          } else {
            // Reset for next scan
            setState(() {
              _currentResult = null;
              _barcodeController.clear();
              _qtyController.text = '1';
              _message = "Listo para el siguiente escaneo";
            });
          }
        } else {
          _showError('Error al guardar conteo');
        }
      } catch (e) {
        _showError('Error: $e');
      } finally {
        setState(() => _isProcessing = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.isForTraspaso
              ? 'Escanear para Traspaso'
              : 'Escanear Inventario',
        ),
      ),
      body: Column(
        children: [
          // Camera Area
          if (widget.preSelectedDetail == null && _currentResult == null)
            Expanded(
              flex: 2,
              child: Stack(
                children: [
                  MobileScanner(
                    controller: _scannerController,
                    onDetect: _onDetect,
                  ),
                  Center(
                    child: Container(
                      width: 250,
                      height: 150,
                      decoration: BoxDecoration(
                        border: Border.all(color: Colors.white, width: 2),
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                ],
              ),
            ),

          // Found Product / Search Result Area
          if (_currentResult != null)
            Expanded(
              flex: 2,
              child: Container(
                color: Colors.blue[50],
                padding: const EdgeInsets.all(20),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    _buildProductImageOrIcon(),
                    const SizedBox(height: 16),
                    Text(
                      widget.isForTraspaso
                          ? (_currentResult as TraspasoDetalle).nombreProducto
                          : (_currentResult as DetalleConteo).nombreProducto,
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    Text(
                      widget.isForTraspaso
                          ? "Talla: ${(_currentResult as TraspasoDetalle).talla} | Color: ${(_currentResult as TraspasoDetalle).color}"
                          : "Talla: ${(_currentResult as DetalleConteo).talla} | Color: ${(_currentResult as DetalleConteo).color}",
                      style: TextStyle(fontSize: 16, color: Colors.grey[700]),
                    ),
                    if (widget.isForTraspaso)
                      Text(
                        "Stock: ${(_currentResult as TraspasoDetalle).stockPar} P / ${(_currentResult as TraspasoDetalle).stockCaja} C",
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: Colors.blue[900],
                        ),
                      ),
                  ],
                ),
              ),
            ),

          // Manual Entry / Info Area
          Expanded(
            flex: 3,
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  TextField(
                    controller: _barcodeController,
                    decoration: InputDecoration(
                      labelText: 'Búsqueda Manual (EAN/SKU/Nombre)',
                      hintText: 'Ingrese código o nombre...',
                      suffixIcon: IconButton(
                        icon: const Icon(Icons.search),
                        onPressed: () =>
                            _searchProduct(_barcodeController.text),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),

                  if (_message != null)
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: _isError ? Colors.red[100] : Colors.green[100],
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        _message!,
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: _isError ? Colors.red[900] : Colors.green[900],
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),

                  if (_currentResult != null) ...[
                    const SizedBox(height: 20),
                    const Text(
                      'Cantidad:',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        _qtyBtn(Icons.remove_circle, Colors.red, () {
                          int val = int.tryParse(_qtyController.text) ?? 1;
                          if (val > 1) {
                            _qtyController.text = (val - 1).toString();
                          }
                        }),
                        Container(
                          width: 80,
                          margin: const EdgeInsets.symmetric(horizontal: 16),
                          child: TextField(
                            controller: _qtyController,
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              fontSize: 24,
                              fontWeight: FontWeight.bold,
                            ),
                            keyboardType: TextInputType.number,
                          ),
                        ),
                        _qtyBtn(Icons.add_circle, Colors.green, () {
                          int val = int.tryParse(_qtyController.text) ?? 1;
                          _qtyController.text = (val + 1).toString();
                        }),
                      ],
                    ),
                    const SizedBox(height: 30),
                    ElevatedButton.icon(
                      icon: _isProcessing
                          ? const CircularProgressIndicator(color: Colors.white)
                          : const Icon(Icons.check_circle),
                      label: Text(
                        widget.isForTraspaso
                            ? 'AGREGAR A SOLICITUD'
                            : 'CONFIRMAR CONTEO',
                        style: const TextStyle(fontSize: 18),
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.blue[800],
                        foregroundColor: Colors.white,
                        minimumSize: const Size(double.infinity, 56),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                      onPressed: _isProcessing ? null : _processFoundProduct,
                    ),
                    TextButton(
                      child: const Text('Cancelar / Escanear otro'),
                      onPressed: () => setState(() {
                        _currentResult = null;
                        _barcodeController.clear();
                        _message = null;
                      }),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _qtyBtn(IconData icon, Color color, VoidCallback t) {
    return IconButton(
      icon: Icon(icon, color: color, size: 40),
      onPressed: t,
    );
  }

  Widget _buildProductImageOrIcon() {
    int? idVariante;
    if (_currentResult is TraspasoDetalle) {
      idVariante = (_currentResult as TraspasoDetalle).idVariante;
    }
    if (_currentResult is DetalleConteo) {
      // We don't have idVariante in DetalleConteo model easily,
      // Need to either update model or fetch it.
      // For now, let's just show an icon if it's not transparent.
    }

    if (idVariante != null) {
      return FutureBuilder(
        future: _dbService.getImagenVariante(idVariante),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.done &&
              snapshot.hasData &&
              snapshot.data != null) {
            return ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: Image.memory(
                snapshot.data!,
                width: 120,
                height: 120,
                fit: BoxFit.cover,
              ),
            );
          }
          return Icon(Icons.inventory_2, size: 100, color: Colors.blue[800]);
        },
      );
    }
    return Icon(Icons.inventory_2, size: 100, color: Colors.blue[800]);
  }
}
