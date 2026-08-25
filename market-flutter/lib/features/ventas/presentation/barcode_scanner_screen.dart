import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

const _primary = Color(0xFF2E8B57);
const _brand = Color(0xFF0F4C5C);

/// Pantalla completa de escaneo — devuelve el código detectado vía
/// `Navigator.pop(context, codigo)`. Si la cámara no está disponible (permiso
/// denegado, sin hardware, o el navegador web lo bloquea) degrada a un campo
/// de entrada manual en vez de fallar — ver CLAUDE.md, "no crashear".
class BarcodeScannerScreen extends StatefulWidget {
  const BarcodeScannerScreen({super.key});

  @override
  State<BarcodeScannerScreen> createState() => _BarcodeScannerScreenState();
}

class _BarcodeScannerScreenState extends State<BarcodeScannerScreen> {
  final _controller = MobileScannerController(
    detectionSpeed: DetectionSpeed.noDuplicates,
  );
  final _manualController = TextEditingController();
  bool _detectado = false;
  bool _modoManual = false;

  @override
  void dispose() {
    _controller.dispose();
    _manualController.dispose();
    super.dispose();
  }

  void _onDetect(BarcodeCapture capture) {
    if (_detectado) return;
    for (final barcode in capture.barcodes) {
      final valor = barcode.rawValue;
      if (valor != null && valor.isNotEmpty) {
        _detectado = true;
        Navigator.of(context).pop(valor);
        return;
      }
    }
  }

  void _confirmarManual() {
    final codigo = _manualController.text.trim();
    if (codigo.isEmpty) return;
    Navigator.of(context).pop(codigo);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: _brand,
        foregroundColor: Colors.white,
        title: const Text('Escanear código'),
      ),
      body: Column(
        children: [
          Expanded(
            child: _modoManual
                ? const Center(
                    child: Text(
                      'Apunta al código de barras',
                      style: TextStyle(color: Colors.white54),
                    ),
                  )
                : MobileScanner(
                    controller: _controller,
                    onDetect: _onDetect,
                    errorBuilder: (context, error) {
                      // Sin cámara / permiso denegado / no soportado — degradar
                      // a entrada manual en vez de mostrar una pantalla rota.
                      WidgetsBinding.instance.addPostFrameCallback((_) {
                        if (mounted) setState(() => _modoManual = true);
                      });
                      return const Center(
                        child: Text(
                          'No se pudo acceder a la cámara.',
                          style: TextStyle(color: Colors.white54),
                        ),
                      );
                    },
                  ),
          ),
          Container(
            color: const Color(0xFF0F172A),
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _manualController,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(
                      hintText: 'Ingresar código manual',
                      hintStyle: TextStyle(color: Colors.white38),
                      enabledBorder: OutlineInputBorder(
                        borderSide: BorderSide(color: Colors.white24),
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderSide: BorderSide(color: _primary),
                      ),
                    ),
                    onSubmitted: (_) => _confirmarManual(),
                  ),
                ),
                const SizedBox(width: 10),
                FilledButton(
                  style: FilledButton.styleFrom(backgroundColor: _primary),
                  onPressed: _confirmarManual,
                  child: const Text('Buscar'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
