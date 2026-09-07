import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/theme/app_colors.dart';
import '../application/auth_notifier.dart';
import 'auth_brand_mark.dart';
import 'auth_pill_decoration.dart';

/// Misma clave lógica que `USUARIO_RECORDADO_KEY` en `LoginView.vue` del
/// backoffice — solo recuerda el usuario tecleado, nunca la contraseña ni la
/// sesión (esa persistencia ya la maneja la cookie de refresh, aparte).
const _usuarioRecordadoKey = 'inven365-usuario-recordado';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  String? _errorMessage;
  bool _recordarme = false;

  @override
  void initState() {
    super.initState();
    _cargarUsuarioRecordado();
  }

  Future<void> _cargarUsuarioRecordado() async {
    final prefs = await SharedPreferences.getInstance();
    final recordado = prefs.getString(_usuarioRecordadoKey);
    if (recordado != null && mounted) {
      setState(() {
        _usernameController.text = recordado;
        _recordarme = true;
      });
    }
  }

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _onSubmit() async {
    final username = _usernameController.text.trim();
    final password = _passwordController.text;
    if (username.isEmpty || password.isEmpty) {
      setState(() => _errorMessage = 'Ingrese usuario y contraseña.');
      return;
    }
    setState(() => _errorMessage = null);
    await ref.read(authNotifierProvider.notifier).login(username, password);
    final state = ref.read(authNotifierProvider);
    if (state.hasError) {
      final error = state.error;
      final mensaje = error is ApiException && error.isNetworkError
          ? 'No se pudo conectar con el servidor. Verifica tu conexión.'
          : 'Usuario o contraseña incorrectos.';
      setState(() => _errorMessage = mensaje);
      return;
    }
    final prefs = await SharedPreferences.getInstance();
    if (_recordarme) {
      await prefs.setString(_usuarioRecordadoKey, username);
    } else {
      await prefs.remove(_usuarioRecordadoKey);
    }
  }

  @override
  Widget build(BuildContext context) {
    final loading = ref.watch(authNotifierProvider).isLoading;
    final colors = AppColors.of(context);

    return Scaffold(
      backgroundColor: colors.bg,
      body: LayoutBuilder(
        builder: (context, constraints) {
          return SingleChildScrollView(
            padding: EdgeInsets.only(
              bottom: MediaQuery.of(context).viewInsets.bottom,
            ),
            child: ConstrainedBox(
              constraints: BoxConstraints(minHeight: constraints.maxHeight),
              child: Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 360),
                  child: Padding(
                    padding: const EdgeInsets.all(28),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const AuthBrandMark(),
                        const SizedBox(height: 18),
                        Text(
                          'Bienvenido de nuevo',
                          style: TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.w800,
                            color: colors.text,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'POS · Punto de Venta',
                          style: TextStyle(color: colors.textMuted),
                        ),
                        const SizedBox(height: 28),
                        TextField(
                          controller: _usernameController,
                          decoration: authPillDecoration(context, 'Usuario'),
                          style: TextStyle(color: colors.text),
                          textInputAction: TextInputAction.next,
                        ),
                        const SizedBox(height: 12),
                        TextField(
                          controller: _passwordController,
                          decoration: authPillDecoration(context, 'Contraseña'),
                          style: TextStyle(color: colors.text),
                          obscureText: true,
                          onSubmitted: (_) => _onSubmit(),
                        ),
                        const SizedBox(height: 4),
                        // `Wrap` en vez de `Row`: a este ancho (tarjeta de
                        // login, max 360px) "Recordarme" + "¿Olvidaste tu
                        // contraseña?" casi no caben en una sola línea —
                        // un `Row` con spaceBetween desborda (RenderFlex
                        // overflowed), y `WrapAlignment.spaceBetween` sin
                        // `spacing` los deja pegados (cero aire entre
                        // textos) cuando el espacio libre es casi nulo.
                        // `spacing` fijo garantiza un separador mínimo
                        // siempre, y sigue bajando de línea sin romper si
                        // algún día no cabe.
                        Wrap(
                          spacing: 16,
                          runSpacing: 4,
                          children: [
                            GestureDetector(
                              onTap: () =>
                                  setState(() => _recordarme = !_recordarme),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  SizedBox(
                                    height: 24,
                                    width: 24,
                                    child: Checkbox(
                                      value: _recordarme,
                                      activeColor: colors.primary,
                                      materialTapTargetSize:
                                          MaterialTapTargetSize.shrinkWrap,
                                      onChanged: (value) => setState(
                                        () => _recordarme = value ?? false,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  Text(
                                    'Recordarme',
                                    style: TextStyle(color: colors.textMuted),
                                  ),
                                ],
                              ),
                            ),
                            TextButton(
                              style: TextButton.styleFrom(
                                padding: EdgeInsets.zero,
                                minimumSize: Size.zero,
                                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                              ),
                              onPressed: () => context.push('/olvide-password'),
                              child: Text(
                                '¿Olvidaste tu contraseña?',
                                style: TextStyle(color: colors.primary),
                              ),
                            ),
                          ],
                        ),
                        if (_errorMessage != null) ...[
                          const SizedBox(height: 14),
                          Container(
                            width: double.infinity,
                            padding: const EdgeInsets.symmetric(
                              horizontal: 16,
                              vertical: 10,
                            ),
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
                        const SizedBox(height: 22),
                        SizedBox(
                          width: double.infinity,
                          child: FilledButton(
                            onPressed: loading ? null : _onSubmit,
                            style: FilledButton.styleFrom(
                              backgroundColor: colors.primary,
                              shape: const StadiumBorder(),
                              padding: const EdgeInsets.symmetric(vertical: 16),
                            ),
                            child: loading
                                ? const SizedBox(
                                    height: 18,
                                    width: 18,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                      color: Colors.white,
                                    ),
                                  )
                                : const Text('Ingresar'),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
