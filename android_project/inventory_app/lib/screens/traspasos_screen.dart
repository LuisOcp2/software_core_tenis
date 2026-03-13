import 'package:flutter/material.dart';
import '../models/traspaso_model.dart';
import '../services/database_service.dart';
import 'detalle_traspaso_screen.dart';
import 'crear_traspaso_screen.dart';

class TraspasosScreen extends StatefulWidget {
  const TraspasosScreen({super.key});

  @override
  _TraspasosScreenState createState() => _TraspasosScreenState();
}

class _TraspasosScreenState extends State<TraspasosScreen> {
  final _dbService = DatabaseService();
  List<Traspaso> _traspasos = [];
  bool _isLoading = true;
  String _filtroEstado = 'Todos';

  @override
  void initState() {
    super.initState();
    _loadTraspasos();
  }

  Future<void> _loadTraspasos() async {
    setState(() => _isLoading = true);
    try {
      final traspasos = await _dbService.getTraspasos(estado: _filtroEstado);
      setState(() {
        _traspasos = traspasos;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Error al cargar traspasos: $e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Traspasos'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadTraspasos,
          ),
        ],
      ),
      body: Column(
        children: [
          _buildFilters(),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _traspasos.isEmpty
                ? const Center(child: Text('No se encontraron traspasos'))
                : ListView.builder(
                    itemCount: _traspasos.length,
                    itemBuilder: (context, index) {
                      final t = _traspasos[index];
                      return _buildTraspasoCard(t);
                    },
                  ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () async {
          final result = await Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) => const CrearTraspasoScreen(),
            ),
          );
          if (result == true) _loadTraspasos();
        },
        label: const Text('Solicitar'),
        icon: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildFilters() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      color: Colors.grey[200],
      child: Row(
        children: [
          const Text('Estado: ', style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(width: 8),
          DropdownButton<String>(
            value: _filtroEstado,
            items: [
              'Todos',
              'pendiente',
              'autorizado',
              'enviado',
              'recibido',
              'cancelado',
            ].map((e) => DropdownMenuItem(value: e, child: Text(e))).toList(),
            onChanged: (val) {
              if (val != null) {
                setState(() => _filtroEstado = val);
                _loadTraspasos();
              }
            },
          ),
        ],
      ),
    );
  }

  Widget _buildTraspasoCard(Traspaso t) {
    Color estadoColor = Colors.grey;
    switch (t.estado) {
      case 'pendiente':
        estadoColor = Colors.orange;
        break;
      case 'autorizado':
        estadoColor = Colors.blue;
        break;
      case 'enviado':
        estadoColor = Colors.purple;
        break;
      case 'recibido':
        estadoColor = Colors.green;
        break;
      case 'cancelado':
        estadoColor = Colors.red;
        break;
    }

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      elevation: 2,
      child: ListTile(
        title: Text(
          t.numeroTraspaso,
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Desde: ${t.nombreBodegaOrigen ?? t.idBodegaOrigen}'),
            Text('Hacia: ${t.nombreBodegaDestino ?? t.idBodegaDestino}'),
            Text(
              'Fecha: ${t.fechaSolicitud?.toString().substring(0, 16) ?? 'N/A'}',
            ),
          ],
        ),
        trailing: Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          decoration: BoxDecoration(
            color: estadoColor.withOpacity(0.2),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: estadoColor),
          ),
          child: Text(
            t.estado.toUpperCase(),
            style: TextStyle(
              color: estadoColor,
              fontSize: 10,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
        onTap: () async {
          final result = await Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) => DetalleTraspasoScreen(traspaso: t),
            ),
          );
          if (result == true) _loadTraspasos();
        },
      ),
    );
  }
}
