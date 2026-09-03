import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';

/// Decoración compartida por las pantallas de auth (login, olvidé/restablecer
/// contraseña) — mismo campo tipo "pill" en las tres.
InputDecoration authPillDecoration(BuildContext context, String hint) {
  final colors = AppColors.of(context);
  final radius = BorderRadius.circular(999);
  return InputDecoration(
    hintText: hint,
    filled: true,
    fillColor: colors.surface,
    contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
    border: OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide(color: colors.border),
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide(color: colors.border),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide(color: colors.primary, width: 2),
    ),
  );
}
