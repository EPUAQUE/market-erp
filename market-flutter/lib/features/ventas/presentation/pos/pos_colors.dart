import 'package:flutter/material.dart';

/// Paleta compartida entre `PosScreen` y sus widgets divididos
/// (`lib/features/ventas/presentation/pos/`) — mismos valores que
/// `AppColors.light` (`core/theme/app_colors.dart`, fuente única de verdad);
/// duplicados aquí como literales `const` porque Dart no permite acceder a
/// un campo de instancia de otra clase dentro de una expresión `const`. Si
/// cambia el claro en `AppColors`, actualizar también acá. Estos widgets
/// todavía no son sensibles al modo oscuro (siempre toman el valor claro) —
/// migrarlos a `AppColors.of(context)` queda para una fase aparte.
const posColorBrand = Color(0xFF0F4C5C);
const posColorPrimary = Color(0xFF2E8B57);
const posColorDanger = Color(0xFFDC6B6B);
