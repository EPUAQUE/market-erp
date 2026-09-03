import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/theme/app_colors.dart';
import '../application/auth_notifier.dart';
import 'auth_pill_decoration.dart';

/// Pide el usuario y dispara `POST /auth/forgot-password`. El backend siempre
/// responde igual exista o no el usuario (ver `AuthController`), así que esta
/// pantalla muestra el mismo mensaje de éxito sin importar el resultado real
/// — nunca revela si el usuario existe.
class ForgotPasswordScreen extends ConsumerStatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  ConsumerState<ForgotPasswordScreen> createState() =>
      _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends ConsumerState<ForgotPasswordScreen> {
  final _usernameController = TextEditingController();
  bool _loading = false;
  bool _enviado = false;
  String? _errorMessage;

  @override
  void dispose() {
    _usernameController.dispose();
    super.dispose();
  }

  Future<void> _onSubmit() async {
    final username = _usernameController.text.trim();
    if (username.isEmpty) {
      setState(() => _errorMessage = 'Ingrese su usuario.');
      return;
    }
    setState(() {
      _loading = true;
      _errorMessage = null;
    });
    try {
      await ref.read(authApiProvider).forgotPassword(username);
      if (mounted) setState(() => _enviado = true);
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _errorMessage = error.status == 429
            ? 'Demasiados intentos. Intenta de nuevo en unos momentos.'
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
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 360),
            child: Padding(
              padding: const EdgeInsets.all(28),
              child: _enviado ? _mensajeEnviado(colors) : _formulario(colors),
            ),
          ),
        ),
      ),
    );
  }

  Widget _mensajeEnviado(AppColors colors) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          'Revisa tu correo',
          style: TextStyle(
            fontSize: 22,
            fontWeight: FontWeight.w800,
            color: colors.text,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 8),
        Text(
          'Si el usuario existe y tiene un correo registrado, te enviamos un '
          'enlace para restablecer tu contraseña. Expira en 30 minutos.',
          style: TextStyle(color: colors.textMuted),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 20),
        SizedBox(
          width: double.infinity,
          child: FilledButton(
            onPressed: () => context.push('/restablecer-password'),
            style: FilledButton.styleFrom(
              backgroundColor: colors.primary,
              shape: const StadiumBorder(),
              padding: const EdgeInsets.symmetric(vertical: 16),
            ),
            child: const Text('Ya tengo un código'),
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

  Widget _formulario(AppColors colors) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          '¿Olvidaste tu contraseña?',
          style: TextStyle(
            fontSize: 22,
            fontWeight: FontWeight.w800,
            color: colors.text,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 8),
        Text(
          'Ingresa tu usuario y te enviaremos un enlace para restablecerla.',
          style: TextStyle(color: colors.textMuted),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 24),
        TextField(
          controller: _usernameController,
          decoration: authPillDecoration(context, 'Usuario'),
          style: TextStyle(color: colors.text),
          textInputAction: TextInputAction.done,
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
                : const Text('Enviar enlace'),
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
