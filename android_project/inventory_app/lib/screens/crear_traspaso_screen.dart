import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/traspaso_model.dart';
import '../models/traspaso_detalle_model.dart';
import '../models/bodega_model.dart';
import '../services/database_service.dart';
import 'scan_screen.dart';
import '../widgets/product_image_widget.dart';

class CrearTraspasoScreen extends StatefulWidget {
  const CrearTraspasoScreen({super.key});

  @override
  _CrearTraspasoScreenState createState() => _CrearTraspasoScreenState();
}

class _CrearTraspasoScreenState extends State<CrearTraspasoScreen> {
  final _dbService = DatabaseService();
  final _motivoController = TextEditingController();
  final _obsController = TextEditingController();
  final _searchController = TextEditingController();

  List<Bodega> _bodegas = [];
  Bodega? _bodegaOrigen;
  Bodega? _bodegaDestino;
  final List<TraspasoDetalle> _productosSeleccionados = [];
  bool _isLoading = true;
  String _numeroTraspaso = '';
  int? _userId;

  // Search/Filters state
  List<TraspasoDetalle> _searchResults = [];
  Map<int, String> _marcas = {};
  Map<int, String> _tallas = {};
  Map<int, String> _colores = {};
  int? _marcaFiltro;
  int? _tallaFiltro;
  int? _colorFiltro;
  bool _isSearching = false;

  @override
  void initState() {
    super.initState();
    _initData();
  }

  Future<void> _initData() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      _userId = prefs.getInt('userId');
      final userBodegaId = prefs.getInt('id_bodega');

      final bodegas = await _dbService.getBodegas();
      final numero = await _dbService.generarNumeroTraspaso();
      final marcas = await _dbService.getMarcas();
      final tallas = await _dbService.getTallas();
      final colores = await _dbService.getColores();

      setState(() {
        _bodegas = bodegas;
        _numeroTraspaso = numero;
        _marcas = marcas;
        _tallas = tallas;
        _colores = colores;

        // Baki (ID 4) as default origin
        if (bodegas.any((b) => b.idBodega == 4)) {
          _bodegaOrigen = bodegas.firstWhere((b) => b.idBodega == 4);
        } else if (bodegas.isNotEmpty) {
          _bodegaOrigen = bodegas.first;
        }

        // User's warehouse as default destination
        if (userBodegaId != null &&
            bodegas.any((b) => b.idBodega == userBodegaId)) {
          _bodegaDestino = bodegas.firstWhere(
            (b) => b.idBodega == userBodegaId,
          );
        } else if (bodegas.isNotEmpty && _bodegaOrigen != bodegas.first) {
          _bodegaDestino = bodegas.first;
        } else if (bodegas.length > 1) {
          _bodegaDestino = bodegas[1];
        }

        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
      _showError('Error al inicializar: $e');
    }
  }

  void _showError(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  Future<void> _ejecutarBusqueda() async {
    if (_bodegaOrigen == null) {
      _showError('Seleccione primero una bodega de origen');
      return;
    }
    setState(() => _isSearching = true);
    try {
      final results = await _dbService.buscarProductosOptimizado(
        texto: _searchController.text,
        idMarca: _marcaFiltro,
        idTalla: _tallaFiltro,
        idColor: _colorFiltro,
        idBodega: _bodegaOrigen!.idBodega,
      );
      setState(() {
        _searchResults = results;
        _isSearching = false;
      });
    } catch (e) {
      setState(() => _isSearching = false);
      _showError('Error en búsqueda: $e');
    }
  }

  void _agregarProducto(TraspasoDetalle item) {
    setState(() {
      final index = _productosSeleccionados.indexWhere(
        (p) => p.idVariante == item.idVariante,
      );
      if (index != -1) {
        _productosSeleccionados[index].cantidadSolicitada += 1;
      } else {
        item.cantidadSolicitada = 1;
        _productosSeleccionados.add(item);
      }
    });
    _showError('Agregado: ${item.nombreProducto}');
  }

  Future<void> _guardarTraspaso() async {
    if (_bodegaDestino == null) {
      _showError('Seleccione bodega destino');
      return;
    }
    if (_bodegaOrigen?.idBodega == _bodegaDestino?.idBodega) {
      _showError('Las bodegas deben ser differentes');
      return;
    }
    if (_productosSeleccionados.isEmpty) {
      _showError('Agregue al menos un producto');
      return;
    }

    setState(() => _isLoading = true);
    final traspaso = Traspaso(
      numeroTraspaso: _numeroTraspaso,
      idBodegaOrigen: _bodegaOrigen!.idBodega,
      idBodegaDestino: _bodegaDestino!.idBodega,
      idUsuarioSolicita: _userId,
      motivo: _motivoController.text,
      observaciones: _obsController.text,
      productos: _productosSeleccionados,
    );

    final success = await _dbService.crearTraspaso(traspaso);
    setState(() => _isLoading = false);

    if (success) {
      Navigator.pop(context, true);
    } else {
      _showError('Error al guardar el traspaso');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Nueva Solicitud')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                Expanded(
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      Text(
                        'Número: $_numeroTraspaso',
                        style: const TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 18,
                        ),
                      ),
                      const SizedBox(height: 16),
                      _buildBodegaSelectors(),
                      const SizedBox(height: 16),
                      _buildAdvancedSearch(),
                      const SizedBox(height: 16),
                      if (_searchResults.isNotEmpty || _isSearching)
                        _buildSearchResults(),
                      const SizedBox(height: 16),
                      const Text(
                        'Lista de Solicitud:',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                        ),
                      ),
                      _buildSelectedProductsList(),
                      const SizedBox(height: 16),
                      TextField(
                        controller: _motivoController,
                        decoration: const InputDecoration(
                          labelText: 'Motivo',
                          border: OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _obsController,
                        decoration: const InputDecoration(
                          labelText: 'Observaciones',
                          border: OutlineInputBorder(),
                        ),
                        maxLines: 2,
                      ),
                    ],
                  ),
                ),
                _buildBottomActions(),
              ],
            ),
    );
  }

  Widget _buildBodegaSelectors() {
    return Card(
      color: Colors.blue[50],
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Column(
          children: [
            DropdownButtonFormField<Bodega>(
              initialValue: _bodegaOrigen,
              decoration: const InputDecoration(
                labelText: 'Bodega Origen (Desde)',
                prefixIcon: Icon(Icons.outbound),
              ),
              items: _bodegas
                  .map(
                    (b) => DropdownMenuItem(
                      value: b,
                      child: Text(
                        b.idBodega == 4 ? '${b.nombre} (Baki)' : b.nombre,
                      ),
                    ),
                  )
                  .toList(),
              onChanged: (val) {
                setState(() {
                  _bodegaOrigen = val;
                  _searchResults
                      .clear(); // Clear results if origin changes as stock might differ
                });
              },
            ),
            const SizedBox(height: 8),
            DropdownButtonFormField<Bodega>(
              initialValue: _bodegaDestino,
              decoration: const InputDecoration(
                labelText: 'Bodega Destino (Hacia)',
                prefixIcon: Icon(Icons.login),
              ),
              items: _bodegas
                  .map((b) => DropdownMenuItem(value: b, child: Text(b.nombre)))
                  .toList(),
              onChanged: (val) => setState(() => _bodegaDestino = val),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAdvancedSearch() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Buscador de Productos',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _searchController,
                decoration: InputDecoration(
                  hintText: 'Nombre, código...',
                  prefixIcon: const Icon(Icons.search),
                  border: const OutlineInputBorder(),
                  suffixIcon: _searchController.text.isNotEmpty
                      ? IconButton(
                          icon: const Icon(Icons.clear),
                          onPressed: () =>
                              setState(() => _searchController.clear()),
                        )
                      : null,
                ),
                onSubmitted: (_) => _ejecutarBusqueda(),
              ),
            ),
            const SizedBox(width: 8),
            IconButton(
              icon: Icon(
                Icons.qr_code_scanner,
                size: 36,
                color: Colors.blue[800],
              ),
              onPressed: () async {
                final result = await Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => ScanScreen(
                      isForTraspaso: true,
                      idBodegaOrigen: _bodegaOrigen?.idBodega,
                    ),
                  ),
                );
                if (result != null && result is TraspasoDetalle) {
                  _agregarProducto(result);
                }
              },
            ),
          ],
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: DropdownButtonHideUnderline(
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  decoration: BoxDecoration(
                    border: Border.all(color: Colors.grey),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: DropdownButton<int>(
                    hint: const Text('Color'),
                    isExpanded: true,
                    value: _colorFiltro,
                    items: [
                      const DropdownMenuItem(
                        value: null,
                        child: Text('Todos los Colores'),
                      ),
                      ..._colores.entries.map(
                        (e) => DropdownMenuItem(
                          value: e.key,
                          child: Text(e.value),
                        ),
                      ),
                    ],
                    onChanged: (val) => setState(() => _colorFiltro = val),
                  ),
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: DropdownButtonHideUnderline(
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  decoration: BoxDecoration(
                    border: Border.all(color: Colors.grey),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: DropdownButton<int>(
                    hint: const Text('Marca'),
                    isExpanded: true,
                    value: _marcaFiltro,
                    items: [
                      const DropdownMenuItem(
                        value: null,
                        child: Text('Todas las Marcas'),
                      ),
                      ..._marcas.entries.map(
                        (e) => DropdownMenuItem(
                          value: e.key,
                          child: Text(e.value),
                        ),
                      ),
                    ],
                    onChanged: (val) => setState(() => _marcaFiltro = val),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: DropdownButtonHideUnderline(
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  decoration: BoxDecoration(
                    border: Border.all(color: Colors.grey),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: DropdownButton<int>(
                    hint: const Text('Talla'),
                    isExpanded: true,
                    value: _tallaFiltro,
                    items: [
                      const DropdownMenuItem(
                        value: null,
                        child: Text('Todas las Tallas'),
                      ),
                      ..._tallas.entries.map(
                        (e) => DropdownMenuItem(
                          value: e.key,
                          child: Text(e.value),
                        ),
                      ),
                    ],
                    onChanged: (val) => setState(() => _tallaFiltro = val),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 8),
            ElevatedButton(
              onPressed: _ejecutarBusqueda,
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.all(12),
              ),
              child: const Icon(Icons.search),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildSearchResults() {
    if (_isSearching) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(20),
          child: CircularProgressIndicator(),
        ),
      );
    }
    if (_searchResults.isEmpty) {
      return const Center(
        child: Text('No se encontraron productos con estos filtros'),
      );
    }

    return Container(
      height: 250,
      decoration: BoxDecoration(
        border: Border.all(color: Colors.blue[100]!),
        borderRadius: BorderRadius.circular(8),
      ),
      child: ListView.builder(
        itemCount: _searchResults.length,
        itemBuilder: (context, index) {
          final p = _searchResults[index];
          return ListTile(
            leading: ProductImageWidget(
              idProducto: p.idProducto,
              idVariante: p.idVariante,
              size: 45,
            ),
            title: Text(
              p.nombreProducto,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
            ),
            subtitle: Text(
              'Talla: ${p.talla} | Color: ${p.color}\nStock: ${p.stockPar} P / ${p.stockCaja} C',
              style: const TextStyle(fontSize: 12),
            ),
            trailing: IconButton(
              icon: const Icon(Icons.add_circle, color: Colors.green),
              onPressed: () => _agregarProducto(p),
            ),
            onTap: () => _agregarProducto(p),
          );
        },
      ),
    );
  }

  Widget _buildSelectedProductsList() {
    if (_productosSeleccionados.isEmpty) {
      return Container(
        height: 60,
        alignment: Alignment.center,
        child: const Text(
          'Ningún producto seleccionado',
          style: TextStyle(color: Colors.grey, fontStyle: FontStyle.italic),
        ),
      );
    }
    return ListView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: _productosSeleccionados.length,
      itemBuilder: (context, index) {
        final p = _productosSeleccionados[index];
        return Card(
          margin: const EdgeInsets.symmetric(vertical: 4),
          child: ListTile(
            title: Text(p.nombreCompleto),
            subtitle: Text('Tipo: ${p.tipo}'),
            trailing: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  icon: const Icon(Icons.remove_circle_outline),
                  onPressed: () => setState(() {
                    if (p.cantidadSolicitada > 1) {
                      p.cantidadSolicitada--;
                    } else {
                      _productosSeleccionados.removeAt(index);
                    }
                  }),
                ),
                Text(
                  '${p.cantidadSolicitada}',
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
                IconButton(
                  icon: const Icon(Icons.add_circle_outline),
                  onPressed: () => setState(() => p.cantidadSolicitada++),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildBottomActions() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: const BoxDecoration(
        color: Colors.white,
        boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 4)],
      ),
      child: Row(
        children: [
          Expanded(
            child: ElevatedButton.icon(
              icon: const Icon(Icons.send),
              onPressed: _isLoading ? null : _guardarTraspaso,
              label: const Text('ENVIAR SOLICITUD'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.blue[800],
                foregroundColor: Colors.white,
                padding: const EdgeInsets.all(16),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
