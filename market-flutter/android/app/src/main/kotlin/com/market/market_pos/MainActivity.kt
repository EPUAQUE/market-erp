package com.market.market_pos

import io.flutter.embedding.android.FlutterFragmentActivity

// `local_auth` requiere una Activity basada en FragmentActivity para mostrar
// el BiometricPrompt nativo — FlutterActivity (plana) no sirve, hay que usar
// FlutterFragmentActivity.
class MainActivity : FlutterFragmentActivity()
