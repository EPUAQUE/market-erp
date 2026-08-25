/// Tamaño de página a pedir cuando el POS necesita "todo" en una sola
/// respuesta (catálogo, inventario, cuentas por cobrar) en vez de paginar de
/// verdad como hace el backoffice — coincide con el tope que el backend
/// acepta (`PaginacionParams.TAMANO_MAXIMO`); pedir más se recorta ahí, no
/// aquí.
const int tamanoPaginaCompleta = 5000;

/// Envelope `{contenido, pagina, tamano, totalElementos, totalPaginas}` que
/// devuelven ahora los listados del backend en vez de un array plano.
List<dynamic> contenidoDePagina(dynamic data) =>
    (data as Map<String, dynamic>)['contenido'] as List<dynamic>;
