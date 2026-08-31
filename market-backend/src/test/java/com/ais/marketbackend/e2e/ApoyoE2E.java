package com.ais.marketbackend.e2e;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Helper compartido por los IT de flujo de negocio en {@code e2e/} — construye
 * requests HTTP reales (login real + JWT real, `MockMvc` corre la cadena
 * completa de Spring Security/MVC) contra el catálogo mínimo que cada flujo
 * necesita. No es un test en sí — ningún método está anotado `@Test`.
 */
final class ApoyoE2E {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    ApoyoE2E(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    String login(String username, String password) throws Exception {
        MvcResult result = doPost("/api/v1/auth/login", Map.of("username", username, "password", password), null)
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    Long crearGrupoTienda(String token, String codigo, String nombre) throws Exception {
        return extraerId(doPost("/api/v1/grupos-tienda", Map.of("codigo", codigo, "nombre", nombre), token));
    }

    Long crearTienda(String token, String codigo, String nombre, Long grupoId) throws Exception {
        return extraerId(
                doPost("/api/v1/tiendas", Map.of("codigo", codigo, "nombre", nombre, "grupoId", grupoId), token));
    }

    Long crearCategoria(String token, String nombre) throws Exception {
        return extraerId(doPost("/api/v1/categorias", Map.of("nombre", nombre), token));
    }

    Long crearMarca(String token, String nombre) throws Exception {
        return extraerId(doPost("/api/v1/marcas", Map.of("nombre", nombre), token));
    }

    Long crearUnidadMedida(String token, String nombre, String abreviacion) throws Exception {
        return extraerId(
                doPost("/api/v1/unidades-medida", Map.of("nombre", nombre, "abreviacion", abreviacion), token));
    }

    Long crearProducto(
            String token, String codigoInterno, String nombre, Long categoriaId, Long marcaId, Long unidadId)
            throws Exception {
        return extraerId(doPost(
                "/api/v1/productos",
                Map.of(
                        "codigoInterno", codigoInterno, "nombre", nombre, "categoriaId", categoriaId, "marcaId",
                        marcaId, "unidadMedidaId", unidadId),
                token));
    }

    void asignarProductoATienda(String token, Long productoId, Long tiendaId, String precioVenta) throws Exception {
        doPost(
                "/api/v1/productos/" + productoId + "/tiendas",
                Map.of(
                        "tiendaId", tiendaId, "precioVenta", new BigDecimal(precioVenta), "stockMinimo",
                        BigDecimal.ZERO, "stockMaximo", new BigDecimal("999999"), "permitirVenta", true,
                        "permitirIngreso", true),
                token)
                .andExpect(status().isCreated());
    }

    Long crearProveedor(String token, String nit, String nombre) throws Exception {
        return extraerId(doPost("/api/v1/proveedores", Map.of("nit", nit, "nombre", nombre), token));
    }

    Long crearCliente(String token, String nombre, BigDecimal limiteCredito) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nombre", nombre);
        if (limiteCredito != null) body.put("limiteCredito", limiteCredito);
        return extraerId(doPost("/api/v1/clientes", body, token));
    }

    /** Crea una compra en BORRADOR, la recibe, y devuelve su id — el kardex/CxP nacen al recibir. */
    Long crearYRecibirCompra(
            String token, Long tiendaId, Long proveedorId, Long productoId, String cantidad, String costoUnitario)
            throws Exception {
        Long compraId = extraerId(doPost(
                "/api/v1/compras/tiendas/" + tiendaId,
                Map.of(
                        "proveedorId", proveedorId, "lineas",
                        List.of(Map.of(
                                "productoId", productoId, "cantidad", new BigDecimal(cantidad), "costoUnitario",
                                new BigDecimal(costoUnitario)))),
                token));
        doPost("/api/v1/compras/tiendas/" + tiendaId + "/" + compraId + "/recibir", null, token)
                .andExpect(status().isOk());
        return compraId;
    }

    String existenciaActual(String token, Long tiendaId, Long productoId) throws Exception {
        MvcResult result = doGet("/api/v1/inventario/tiendas/" + tiendaId + "/productos/" + productoId, token)
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.existenciaActual");
    }

    ResultActions doGet(String path, String token) throws Exception {
        var request = MockMvcRequestBuilders.get(path).accept(MediaType.APPLICATION_JSON);
        if (token != null) request.header("Authorization", "Bearer " + token);
        return mockMvc.perform(request);
    }

    ResultActions doPost(String path, Object body, String token) throws Exception {
        var request = MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON);
        if (body != null) request.content(objectMapper.writeValueAsString(body));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return mockMvc.perform(request);
    }

    Long extraerId(ResultActions resultActions) throws Exception {
        MvcResult result = resultActions.andReturn();
        return Long.valueOf(JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString());
    }
}
