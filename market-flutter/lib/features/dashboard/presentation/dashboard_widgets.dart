import 'dart:math' as math;
import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';

/// Paleta compartida por los dos dashboards (encargado/vendedor) — mismos
/// tokens de marca (`market-backoffice/CLAUDE.md`: petróleo/esmeralda/ámbar)
/// más violeta/coral/azul para dar variedad visual a las tarjetas sin
/// inventar un segundo sistema de diseño.
class DashboardPalette {
  const DashboardPalette._();

  static const brand = Color(0xFF0F4C5C);
  static const primary = Color(0xFF2E8B57);
  static const accent = Color(0xFFD9A441);
  static const violet = Color(0xFF7C5CFC);
  static const coral = Color(0xFFFF6F61);
  static const info = Color(0xFF3B82F6);
  static const danger = Color(0xFFDC6B6B);
  static const warning = Color(0xFFF4B942);
  static const surface = Color(0xFFF8FAFC);
  static const ink = Color(0xFF1E293B);
  static const inkMuted = Color(0xFF64748B);
}

/// Fondo con acento de color suave detrás de un ícono — usado como "avatar"
/// de sección y dentro de las tarjetas de estadística.
class DashboardIconBadge extends StatelessWidget {
  const DashboardIconBadge({
    super.key,
    required this.icon,
    required this.color,
    this.size = 40,
  });

  final IconData icon;
  final Color color;
  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.14),
        borderRadius: BorderRadius.circular(size * 0.32),
      ),
      child: Icon(icon, color: color, size: size * 0.52),
    );
  }
}

/// Encabezado de sección con barra de acento + ícono, reemplaza el texto
/// plano en mayúsculas que tenía cada dashboard antes.
class DashboardSectionHeader extends StatelessWidget {
  const DashboardSectionHeader({
    super.key,
    required this.titulo,
    required this.icon,
    required this.color,
  });

  final String titulo;
  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 24, bottom: 12),
      child: Row(
        children: [
          Container(
            width: 4,
            height: 18,
            decoration: BoxDecoration(
              color: color,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(width: 8),
          Icon(icon, size: 16, color: color),
          const SizedBox(width: 6),
          Text(
            titulo.toUpperCase(),
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w800,
              color: DashboardPalette.ink,
              letterSpacing: 0.6,
            ),
          ),
        ],
      ),
    );
  }
}

/// Tarjeta de KPI con degradado sutil de `color`, ícono y valor grande —
/// reemplazo colorido de la `_StatCard` blanca plana original.
class DashboardStatCard extends StatelessWidget {
  const DashboardStatCard({
    super.key,
    required this.titulo,
    required this.valor,
    required this.icon,
    required this.color,
    this.subtitulo,
  });

  final String titulo;
  final String valor;
  final String? subtitulo;
  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [color.withValues(alpha: 0.10), color.withValues(alpha: 0.02)],
        ),
        border: Border.all(color: color.withValues(alpha: 0.18)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  titulo,
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: DashboardPalette.inkMuted,
                  ),
                ),
              ),
              DashboardIconBadge(icon: icon, color: color, size: 32),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            valor,
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w800,
              color: DashboardPalette.ink,
              height: 1.1,
            ),
          ),
          if (subtitulo != null) ...[
            const SizedBox(height: 4),
            Text(
              subtitulo!,
              style: TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w600,
                color: color,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

/// Anillo tipo "donut" con 3 segmentos (0-30 / 31-60 / +60 días) y el total
/// al centro — visualiza la antigüedad de saldo sin inventar series de
/// tiempo que el backend no expone.
class DashboardAgingRing extends StatelessWidget {
  const DashboardAgingRing({
    super.key,
    required this.a0a30,
    required this.a31a60,
    required this.aMas60,
  });

  final Decimal a0a30;
  final Decimal a31a60;
  final Decimal aMas60;

  @override
  Widget build(BuildContext context) {
    final total = a0a30 + a31a60 + aMas60;
    final v1 = a0a30.toDouble();
    final v2 = a31a60.toDouble();
    final v3 = aMas60.toDouble();
    final sum = v1 + v2 + v3;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        color: Colors.white,
        border: Border.all(color: const Color(0xFFE7ECF1)),
      ),
      child: Row(
        children: [
          SizedBox(
            width: 64,
            height: 64,
            child: CustomPaint(
              painter: _AgingRingPainter(
                fracciones: sum <= 0 ? [1] : [v1 / sum, v2 / sum, v3 / sum],
                colores: sum <= 0
                    ? [const Color(0xFFE7ECF1)]
                    : const [
                        DashboardPalette.primary,
                        DashboardPalette.warning,
                        DashboardPalette.danger,
                      ],
              ),
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text(
                  'Antigüedad de saldo',
                  style: TextStyle(fontSize: 11, color: DashboardPalette.inkMuted),
                ),
                const SizedBox(height: 2),
                Text(
                  'Q $total',
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w800,
                    color: DashboardPalette.ink,
                  ),
                ),
                const SizedBox(height: 6),
                _LeyendaAging(color: DashboardPalette.primary, etiqueta: '0-30d', valor: a0a30),
                _LeyendaAging(color: DashboardPalette.warning, etiqueta: '31-60d', valor: a31a60),
                _LeyendaAging(color: DashboardPalette.danger, etiqueta: '+60d', valor: aMas60),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _LeyendaAging extends StatelessWidget {
  const _LeyendaAging({required this.color, required this.etiqueta, required this.valor});

  final Color color;
  final String etiqueta;
  final Decimal valor;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 1),
      child: Row(
        children: [
          Container(
            width: 7,
            height: 7,
            decoration: BoxDecoration(color: color, shape: BoxShape.circle),
          ),
          const SizedBox(width: 5),
          Text(etiqueta, style: const TextStyle(fontSize: 10.5, color: DashboardPalette.inkMuted)),
          const Spacer(),
          Text(
            'Q $valor',
            style: const TextStyle(fontSize: 10.5, fontWeight: FontWeight.w700, color: DashboardPalette.ink),
          ),
        ],
      ),
    );
  }
}

class _AgingRingPainter extends CustomPainter {
  _AgingRingPainter({required this.fracciones, required this.colores});

  final List<double> fracciones;
  final List<Color> colores;

  @override
  void paint(Canvas canvas, Size size) {
    const strokeWidth = 9.0;
    final rect = Rect.fromLTWH(
      strokeWidth / 2,
      strokeWidth / 2,
      size.width - strokeWidth,
      size.height - strokeWidth,
    );
    var start = -math.pi / 2;
    for (var i = 0; i < fracciones.length; i++) {
      final sweep = fracciones[i] * 2 * math.pi;
      final paint = Paint()
        ..color = colores[i]
        ..style = PaintingStyle.stroke
        ..strokeWidth = strokeWidth
        ..strokeCap = StrokeCap.round;
      canvas.drawArc(rect, start, math.max(sweep - 0.04, 0), false, paint);
      start += sweep;
    }
  }

  @override
  bool shouldRepaint(covariant _AgingRingPainter oldDelegate) =>
      oldDelegate.fracciones != fracciones || oldDelegate.colores != colores;
}

/// Compara dos montos (p.ej. ventas de este mes vs. mes anterior) con dos
/// barras horizontales y el cambio porcentual — el ancho es geometría de UI
/// (double), nunca el valor monetario mostrado, que sigue siendo `Decimal`.
class DashboardComparisonBars extends StatelessWidget {
  const DashboardComparisonBars({
    super.key,
    required this.titulo,
    required this.etiquetaActual,
    required this.valorActual,
    required this.etiquetaAnterior,
    required this.valorAnterior,
  });

  final String titulo;
  final String etiquetaActual;
  final Decimal valorActual;
  final String etiquetaAnterior;
  final Decimal valorAnterior;

  @override
  Widget build(BuildContext context) {
    final actual = valorActual.toDouble();
    final anterior = valorAnterior.toDouble();
    final maxValor = math.max(actual, math.max(anterior, 0.01));
    final cambio = anterior == 0
        ? null
        : ((actual - anterior) / anterior * 100);

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        color: Colors.white,
        border: Border.all(color: const Color(0xFFE7ECF1)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                titulo,
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                  color: DashboardPalette.inkMuted,
                ),
              ),
              if (cambio != null)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: (cambio >= 0 ? DashboardPalette.primary : DashboardPalette.danger)
                        .withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    '${cambio >= 0 ? '+' : ''}${cambio.toStringAsFixed(1)}%',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      color: cambio >= 0 ? DashboardPalette.primary : DashboardPalette.danger,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 14),
          _BarraComparativa(
            etiqueta: etiquetaActual,
            valor: valorActual,
            fraccion: actual / maxValor,
            color: DashboardPalette.violet,
          ),
          const SizedBox(height: 10),
          _BarraComparativa(
            etiqueta: etiquetaAnterior,
            valor: valorAnterior,
            fraccion: anterior / maxValor,
            color: const Color(0xFFCBD5E1),
            colorTexto: DashboardPalette.inkMuted,
          ),
        ],
      ),
    );
  }
}

class _BarraComparativa extends StatelessWidget {
  const _BarraComparativa({
    required this.etiqueta,
    required this.valor,
    required this.fraccion,
    required this.color,
    this.colorTexto,
  });

  final String etiqueta;
  final Decimal valor;
  final double fraccion;
  final Color color;
  final Color? colorTexto;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(etiqueta, style: const TextStyle(fontSize: 11.5, color: DashboardPalette.inkMuted)),
            Text(
              'Q $valor',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w800,
                color: colorTexto ?? color,
              ),
            ),
          ],
        ),
        const SizedBox(height: 5),
        ClipRRect(
          borderRadius: BorderRadius.circular(6),
          child: LayoutBuilder(
            builder: (context, constraints) {
              return Stack(
                children: [
                  Container(height: 8, color: const Color(0xFFF1F5F9)),
                  Container(
                    height: 8,
                    width: constraints.maxWidth * fraccion.clamp(0, 1),
                    decoration: BoxDecoration(
                      color: color,
                      borderRadius: BorderRadius.circular(6),
                    ),
                  ),
                ],
              );
            },
          ),
        ),
      ],
    );
  }
}

/// Banner de alertas con degradado — versión colorida del `Card` plano
/// original, mismo contenido/lógica (crítica vs. preventiva).
class DashboardAlertBanner extends StatelessWidget {
  const DashboardAlertBanner({
    super.key,
    required this.criticas,
    required this.preventivas,
  });

  final int criticas;
  final int preventivas;

  @override
  Widget build(BuildContext context) {
    final color = criticas > 0 ? DashboardPalette.danger : DashboardPalette.warning;
    return Container(
      margin: const EdgeInsets.only(bottom: 4),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        gradient: LinearGradient(
          colors: [color.withValues(alpha: 0.16), color.withValues(alpha: 0.05)],
        ),
        border: Border.all(color: color.withValues(alpha: 0.3)),
      ),
      child: Row(
        children: [
          DashboardIconBadge(icon: Icons.warning_amber_rounded, color: color, size: 36),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              '$criticas alerta(s) crítica(s) · $preventivas preventiva(s) sin leer',
              style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 12.5, color: DashboardPalette.ink),
            ),
          ),
        ],
      ),
    );
  }
}

/// Fila de lista con badge circular de color — reemplaza los `ListTile`
/// planos de sugerencias de compra/traslado y cuentas pendientes.
class DashboardListRow extends StatelessWidget {
  const DashboardListRow({
    super.key,
    required this.icon,
    required this.color,
    required this.titulo,
    required this.subtitulo,
    required this.trailing,
  });

  final IconData icon;
  final Color color;
  final String titulo;
  final String subtitulo;
  final String trailing;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 14),
      child: Row(
        children: [
          DashboardIconBadge(icon: icon, color: color, size: 34),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(titulo, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13, color: DashboardPalette.ink)),
                const SizedBox(height: 2),
                Text(subtitulo, style: const TextStyle(fontSize: 11.5, color: DashboardPalette.inkMuted)),
              ],
            ),
          ),
          Text(
            trailing,
            style: TextStyle(fontWeight: FontWeight.w800, fontSize: 12.5, color: color),
          ),
        ],
      ),
    );
  }
}

/// Contenedor blanco redondeado para agrupar [DashboardListRow]s, con
/// separadores — reemplazo del `Card` + `Column` de `ListTile`s.
class DashboardListCard extends StatelessWidget {
  const DashboardListCard({super.key, required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        color: Colors.white,
        border: Border.all(color: const Color(0xFFE7ECF1)),
      ),
      child: Column(
        children: [
          for (var i = 0; i < children.length; i++) ...[
            if (i > 0) const Divider(height: 1, indent: 14, endIndent: 14, color: Color(0xFFF1F5F9)),
            children[i],
          ],
        ],
      ),
    );
  }
}
