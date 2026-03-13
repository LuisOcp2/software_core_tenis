import 'dart:typed_data';
import 'package:flutter/material.dart';
import '../services/database_service.dart';

class ProductImageWidget extends StatelessWidget {
  final int idProducto;
  final int? idVariante;
  final double size;
  final double? zoomSize;

  const ProductImageWidget({
    super.key,
    required this.idProducto,
    this.idVariante,
    this.size = 50.0,
    this.zoomSize,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => _showZoomedImage(context),
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: Colors.grey[100],
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.grey[300]!),
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(7),
          child: FutureBuilder<Uint8List?>(
            future: _loadImage(),
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(
                  child: SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                );
              }
              if (snapshot.hasData && snapshot.data != null) {
                return Image.memory(
                  snapshot.data!,
                  fit: BoxFit.cover,
                  cacheWidth: (size * 2).toInt(), // Memory optimization
                  errorBuilder: (context, error, stackTrace) =>
                      _buildPlaceholder(),
                );
              }
              return _buildPlaceholder();
            },
          ),
        ),
      ),
    );
  }

  Future<Uint8List?> _loadImage() async {
    final db = DatabaseService();
    if (idVariante != null && idVariante! > 0) {
      final img = await db.getImagenVariante(idVariante!);
      if (img != null) return img;
    }
    return db.getImagenProducto(idProducto);
  }

  Widget _buildPlaceholder() {
    return Container(
      color: Colors.grey[200],
      child: Icon(
        Icons.image_not_supported_outlined,
        size: size * 0.5,
        color: Colors.grey[400],
      ),
    );
  }

  void _showZoomedImage(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => Dialog(
        backgroundColor: Colors.transparent,
        insetPadding: const EdgeInsets.all(10),
        child: Stack(
          alignment: Alignment.center,
          children: [
            GestureDetector(
              onTap: () => Navigator.pop(context),
              child: Container(
                width: double.infinity,
                height: double.infinity,
                color: Colors.black54,
              ),
            ),
            FutureBuilder<Uint8List?>(
              future: _loadImage(),
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const CircularProgressIndicator();
                }
                if (snapshot.hasData && snapshot.data != null) {
                  return Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(15),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.3),
                              blurRadius: 15,
                              spreadRadius: 5,
                            ),
                          ],
                        ),
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(15),
                          child: InteractiveViewer(
                            panEnabled: true,
                            boundaryMargin: const EdgeInsets.all(20),
                            minScale: 0.5,
                            maxScale: 4,
                            child: Image.memory(
                              snapshot.data!,
                              fit: BoxFit.contain,
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),
                      IconButton(
                        onPressed: () => Navigator.pop(context),
                        icon: const Icon(
                          Icons.close,
                          color: Colors.white,
                          size: 40,
                        ),
                      ),
                    ],
                  );
                }
                // Placeholder for zoom if not found
                return Container(
                  padding: const EdgeInsets.all(40),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(15),
                  ),
                  child: const Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        Icons.image_not_supported,
                        size: 50,
                        color: Colors.grey,
                      ),
                      SizedBox(height: 10),
                      Text("Imagen no disponible"),
                    ],
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}
