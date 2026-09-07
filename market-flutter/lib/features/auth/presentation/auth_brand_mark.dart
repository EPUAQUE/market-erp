import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';

/// Badge "i365" — mismo mark que `LoginScreen`, extraído para que
/// `ForgotPasswordScreen`/`ResetPasswordScreen` no se vean "desnudas" al
/// llegar desde el login (antes solo el login tenía identidad de marca).
class AuthBrandMark extends StatelessWidget {
  const AuthBrandMark({super.key});

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Container(
      width: 52,
      height: 52,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [colors.brand, colors.primary],
        ),
        borderRadius: BorderRadius.circular(16),
      ),
      alignment: Alignment.center,
      child: const Text(
        'i365',
        style: TextStyle(
          color: Colors.white,
          fontWeight: FontWeight.w800,
          fontSize: 14,
        ),
      ),
    );
  }
}
