import 'package:flutter/material.dart';
import 'app_colors.dart';

/// `ThemeData` claro/oscuro construidos desde [AppColors] — mismo criterio
/// que el backoffice: la marca no cambia entre temas, solo superficies.
class AppTheme {
  const AppTheme._();

  static ThemeData light = _build(AppColors.light, Brightness.light);
  static ThemeData dark = _build(AppColors.dark, Brightness.dark);

  static ThemeData _build(AppColors colors, Brightness brightness) {
    final colorScheme = ColorScheme(
      brightness: brightness,
      primary: colors.primary,
      onPrimary: Colors.white,
      secondary: colors.accent,
      onSecondary: const Color(0xFF2E2008),
      error: colors.danger,
      onError: Colors.white,
      surface: colors.surface,
      onSurface: colors.text,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: colors.bg,
      cardColor: colors.surface,
      dividerColor: colors.border,
      appBarTheme: AppBarTheme(
        backgroundColor: colors.brand,
        foregroundColor: Colors.white,
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(backgroundColor: colors.primary),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: colors.primary,
          foregroundColor: Colors.white,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: colors.surface,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: colors.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: colors.border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: colors.primary, width: 2),
        ),
      ),
    );
  }
}
