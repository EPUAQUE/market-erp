import 'package:flutter/material.dart';

/// Paleta única de la app — mismos tokens que `market-backoffice/src/styles/
/// tokens.css` (petróleo/esmeralda/ámbar), para que el POS y el backoffice
/// se vean como el mismo producto. La marca (`brand`/`primary`/`accent`) NO
/// cambia entre claro y oscuro a propósito, igual que en el backoffice —
/// solo cambian superficies/texto/borde y los tonos semánticos.
class AppColors {
  const AppColors({
    required this.brand,
    required this.primary,
    required this.accent,
    required this.bg,
    required this.surface,
    required this.surface2,
    required this.text,
    required this.textMuted,
    required this.border,
    required this.success,
    required this.pending,
    required this.overdue,
    required this.danger,
    required this.info,
  });

  final Color brand;
  final Color primary;
  final Color accent;
  final Color bg;
  final Color surface;
  final Color surface2;
  final Color text;
  final Color textMuted;
  final Color border;
  final Color success;
  final Color pending;
  final Color overdue;
  final Color danger;
  final Color info;

  static const light = AppColors(
    brand: Color(0xFF0F4C5C),
    primary: Color(0xFF2E8B57),
    accent: Color(0xFFD9A441),
    bg: Color(0xFFF8FAFC),
    surface: Color(0xFFFFFFFF),
    surface2: Color(0xFFF1F5F9),
    text: Color(0xFF1F2937),
    textMuted: Color(0xFF6B7280),
    border: Color(0xFFE2E8F0),
    success: Color(0xFF3BAA68),
    pending: Color(0xFFF4B942),
    overdue: Color(0xFFE08A3F),
    danger: Color(0xFFDC6B6B),
    info: Color(0xFF64748B),
  );

  static const dark = AppColors(
    brand: Color(0xFF0F4C5C),
    primary: Color(0xFF2E8B57),
    accent: Color(0xFFD9A441),
    bg: Color(0xFF10171A),
    surface: Color(0xFF171F22),
    surface2: Color(0xFF232B2E),
    text: Color(0xFFE7EDEC),
    textMuted: Color(0xFF93A3A2),
    border: Color(0xFF2A3437),
    success: Color(0xFF55C78A),
    pending: Color(0xFFE8B969),
    overdue: Color(0xFFE89E69),
    danger: Color(0xFFF2897D),
    info: Color(0xFF94A3B8),
  );

  static AppColors of(BuildContext context) =>
      Theme.of(context).brightness == Brightness.dark ? dark : light;
}
