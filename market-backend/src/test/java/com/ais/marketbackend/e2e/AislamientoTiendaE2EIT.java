package com.ais.marketbackend.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Fase 5 (PLAN_MEJORAS.md), "Cobertura prioritaria" — flujo E2E de aislamiento
 * entre tiendas: un usuario con rol de alcance por tienda (no ADMIN, cuyo
 * {@code alcanceGlobal=true} bypassa todo scoping) asignado SOLO a la Tienda A
 * no debe poder operar sobre la Tienda B, aunque tenga el permiso genérico
 * (ej. {@code CAJA_VER}) que sí tiene para su propia tienda. La distinción de
 * status HTTP importa: 404 en Tienda A (el interceptor de tienda lo dejó
 * pasar, el servicio simplemente no encontró una caja abierta) vs. 403 en
 * Tienda B (el interceptor lo rechazó antes de llegar al servicio) — mismo
 * endpoint, mismo usuario, la única variable es la tienda del path.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AislamientoTiendaE2EIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private ApoyoE2E apoyo;

    @BeforeEach
    void setUp() {
        apoyo = new ApoyoE2E(mockMvc, objectMapper);
    }

    @Test
    void usuarioAsignadoSoloATiendaANoPuedeOperarSobreTiendaB() throws Exception {
        String sufijo = String.valueOf(System.nanoTime() % 100_000_000L);
        String tokenAdmin = apoyo.login("admin", "Admin1234!Seguro");

        Long grupoId = apoyo.crearGrupoTienda(tokenAdmin, "G-AIS" + sufijo, "Grupo aislamiento");
        Long tiendaAId = apoyo.crearTienda(tokenAdmin, "T-AISA" + sufijo, "Tienda A aislamiento", grupoId);
        Long tiendaBId = apoyo.crearTienda(tokenAdmin, "T-AISB" + sufijo, "Tienda B aislamiento", grupoId);

        String rolesJson = apoyo.doGet("/api/v1/roles", tokenAdmin)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Number> idsEncargado = JsonPath.read(rolesJson, "$[?(@.nombre == 'ENCARGADO_TIENDA')].id");
        assertThat(idsEncargado).as("rol ENCARGADO_TIENDA seedeado por Liquibase").hasSize(1);
        Long rolEncargadoId = idsEncargado.get(0).longValue();

        String username = "encargado.ais" + sufijo;
        String password = "ClaveSeguraE2E123!";
        Long usuarioBId = apoyo.extraerId(apoyo.doPost(
                "/api/v1/usuarios",
                Map.of(
                        "username", username, "password", password, "nombre", "Encargado Aislamiento", "telefono",
                        "12345678", "correo", "encargado.ais" + sufijo + "@example.test"),
                tokenAdmin));

        apoyo.doPost(
                "/api/v1/usuarios/" + usuarioBId + "/tiendas",
                Map.of("tiendaId", tiendaAId, "rolId", rolEncargadoId),
                tokenAdmin)
                .andExpect(status().isCreated());

        String tokenEncargado = apoyo.login(username, password);

        // Tienda A: en su alcance — el interceptor lo deja pasar, el servicio responde
        // 404 porque simplemente no hay una caja abierta todavía (no por permisos).
        apoyo.doGet("/api/v1/caja/tiendas/" + tiendaAId + "/abierta", tokenEncargado)
                .andExpect(status().isNotFound());

        // Tienda B: fuera de su alcance — el interceptor lo rechaza antes de llegar al
        // servicio, sin importar que el rol sí tenga CAJA_VER en general.
        apoyo.doGet("/api/v1/caja/tiendas/" + tiendaBId + "/abierta", tokenEncargado)
                .andExpect(status().isForbidden());

        // GET /tiendas no exige TIENDAS_VER (ver TiendaController) — el mismo usuario,
        // sin ese permiso, sí puede listar tiendas para poblar el selector de
        // Ventas/Caja/Inventario, pero el servicio lo limita a la suya (A), nunca ve B.
        String tiendasJson = apoyo.doGet("/api/v1/tiendas", tokenEncargado)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Number> idsVisibles = JsonPath.read(tiendasJson, "$[*].id");
        assertThat(idsVisibles).contains(tiendaAId.intValue()).doesNotContain(tiendaBId.intValue());
    }
}
