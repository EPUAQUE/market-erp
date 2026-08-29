package com.ais.marketbackend.shared.exceptions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fase 3 (PLAN_MEJORAS.md): confirma que {@code GlobalExceptionHandler} traduce
 * {@code ConcurrencyFailureException} (y sus subclases — deadlock detectado,
 * lock no adquirido) a 409 con un {@code errorCode} consistente, en vez de caer
 * en el handler genérico de 500. Usa un controlador mínimo de prueba porque el
 * handler es transversal a todos los controladores, no específico de ninguno.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class ControladorDePrueba {

        @GetMapping("/prueba/lock-no-adquirido")
        public void lanzarLockNoAdquirido() {
            throw new CannotAcquireLockException("no se pudo adquirir el lock");
        }

        @GetMapping("/prueba/deadlock")
        public void lanzarDeadlock() {
            throw new DeadlockLoserDataAccessException("deadlock detectado por Postgres", null);
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void lockNoAdquiridoResponde409ConCodigoConsistente() throws Exception {
        mockMvc.perform(get("/prueba/lock-no-adquirido"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICTO_CONCURRENCIA"));
    }

    @Test
    void deadlockResponde409ConCodigoConsistente() throws Exception {
        mockMvc.perform(get("/prueba/deadlock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICTO_CONCURRENCIA"));
    }
}
