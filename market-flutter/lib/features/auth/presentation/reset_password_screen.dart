import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/theme/app_colors.dart';
import '../application/auth_notifier.dart';
import 'auth_pill_decoration.dart';

/// A diferencia del backoffice (que lee el token de `?token=` en la URL del
/// enlace del correo), esta pantalla lo pide pegado a mano — la app no tiene
/// deep-linking configurado para abrir el enlace del correo directamente.
class ResetPasswordScreen extends ConsumerStatefulWidget {
  const ResetPasswordScreen({super.key});

  @override
  ConsumerState<ResetPasswordScreen> createState() =>
      _ResetPasswordScreenState();
}

class _ResetPasswordScreenState extends ConsumerState<ResetPasswordScreen> {
  final _tokenController = TextEditingController();
  final _nuevaPasswordController = TextEditingController();
  final _confirmarPasswordController = TextEditingController();
  bool _loading = false;
  bool _completado = false;
  String? _errorMessage;

  @override
  void dispose() {
    _tokenController.dispose();
    _nuevaPasswordController.dispose();
    _confirmarPasswordController.dispose();
    super.dispose();
  }

  Future<void> _onSubmit() async {
    final token = _tokenController.text.trim();
    final nuevaPassword = _nuevaPasswordController.text;
    if (token.isEmpty) {
      setState(
        () => _errorMessage = 'Pega el código que recibiste por correo.',
      );
      return;
    }
    if (nuevaPassword.length < 12) {
      setState(
        () =>
            _errorMessage = 'La contraseña debe tener al menos 12 caracteres.',
      );
      return;
    }
    if (nuevaPassword != _confirmarPasswordController.text) {
      setState(() => _errorMessage = 'Las contraseñas no coinciden.');
      return;
    }
    setState(() {
      _loading = true;
      _errorMessage = null;
    });
    try {
      await ref.read(authApiProvider).resetPassword(token, nuevaPassword);
      if (mounted) setState(() => _completado = true);
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _errorMessage = error.status == 400
            ? 'El código es inválido o ya expiró. Solicita uno nuevo.'
            : 'No se pudo conectar con el servidor.';
      });
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    return Scaffold(
      backgroundColor: colors.bg,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: EdgeInsets.only(
            bottom: MediaQuery.of(context).viewInsets.bottom,
          ),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 360),
              child: Padding(
                padding: const EdgeInsets.all(28),
                child: _completado
                    ? _mensajeCompletado(colors)
                    : _formulario(colors),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _mensajeCompletado(AppColors colors) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          'Contraseña actualizada',
          style: TextStyle(
            fontSize: 22,
            fontWeight: FontWeight.w800,
            color: colors.text,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 8),
        Text(
          'Ya puedes iniciar sesión con tu nueva contraseña.',
          style: TextStyle(color: colors.textMuted),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 20),
        SizedBox(
          width: double.infinity,
          child: FilledButton(
            onPressed: () => context.go('/login'),
            style: FilledButton.styleFrom(
              backgroundColor: colors.primary,
              shape: const StadiumBorder(),
              padding: const EdgeInsets.symmetric(vertical: 16),
            ),
            child: const Text('Iniciar sesión'),
          ),
        ),
      ],
    );
  }

  Widget _formulario(AppColors colors) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          'Restablecer contraseña',
          style: TextStyle(
            fontSize: 22,
            fontWeight: FontWeight.w800,
            color: colors.text,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 8),
        Text(
          'Pega el código que recibiste por correo y elige tu nueva contraseña.',
          style: TextStyle(color: colors.textMuted),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 24),
        TextField(
          controller: _tokenController,
          decoration: authPillDecoration(context, 'Código recibido por correo'),
          style: TextStyle(color: colors.text),
          textInputAction: TextInputAction.next,
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _nuevaPasswordController,
          decoration: authPillDecoration(context, 'Nueva contraseña'),
          style: TextStyle(color: colors.text),
          obscureText: true,
          textInputAction: TextInputAction.next,
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _confirmarPasswordController,
          decoration: authPillDecoration(context, 'Confirmar contraseña'),
          style: TextStyle(color: colors.text),
          obscureText: true,
          onSubmitted: (_) => _onSubmit(),
        ),
        if (_errorMessage != null) ...[
          const SizedBox(height: 14),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
            decoration: BoxDecoration(
              color: colors.danger.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(
              _errorMessage!,
              style: TextStyle(
                color: colors.danger,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
        const SizedBox(height: 20),
        SizedBox(
          width: double.infinity,
          child: FilledButton(
            onPressed: _loading ? null : _onSubmit,
            style: FilledButton.styleFrom(
              backgroundColor: colors.primary,
              shape: const StadiumBorder(),
              padding: const EdgeInsets.symmetric(vertical: 16),
            ),
            child: _loading
                ? const SizedBox(
                    height: 18,
                    width: 18,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white,
                    ),
                  )
                : const Text('Guardar nueva contraseña'),
          ),
        ),
        const SizedBox(height: 8),
        TextButton(
          onPressed: () => context.go('/login'),
          child: Text(
            'Volver a iniciar sesión',
            style: TextStyle(color: colors.primary),
          ),
        ),
      ],
    );
  }
}
