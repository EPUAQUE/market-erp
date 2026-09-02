package com.ais.marketbackend.fel.infrastructure.certificador;

import com.ais.marketbackend.fel.application.ports.CertificadorFelPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Rechaza el arranque en el perfil {@code prod} si no hay ningún
 * {@link CertificadorFelPort} real configurado, salvo que
 * {@code app.fel.requerido-real=false} (bandera temporal — cliente aún en fase de
 * pruebas, sin proveedor FEL contratado; ver {@link FelSimuladoEnProdCondition} y
 * docs/plan-mejoras.md Fase 1). Con la bandera en false, {@link DevCertificadorFelAdapter}
 * sí se registra en {@code prod}, así que en producción solo hay una definición de
 * bean para ese puerto si se implementó y registró un adaptador real, o si se aceptó
 * explícitamente usar el simulado vía la bandera.
 *
 * <p>Implementado como {@link BeanFactoryPostProcessor} (no como una validación en el
 * constructor, como {@code ProdSafetyGuard} en el módulo de seguridad) a propósito:
 * {@code postProcessBeanFactory} corre durante el arranque de Spring ANTES de que se
 * instancie ningún singleton de negocio, incluido {@code FelServiceImpl} — que
 * depende directamente de {@code CertificadorFelPort}. Con una validación en el
 * constructor, el orden de creación de beans no está garantizado, y en la práctica
 * Spring intenta construir {@code FelServiceImpl} primero, fallando con un
 * {@code UnsatisfiedDependencyException} genérico en vez de este mensaje claro —
 * aquí se comprueban las *definiciones* de bean registradas (no se instancia nada
 * todavía), así que el resultado no depende del orden de instanciación.
 *
 * <p>Constructor vacío + {@link EnvironmentAware} en vez de inyectar
 * {@code Environment} por constructor: los {@code BeanFactoryPostProcessor} se
 * instancian en {@code invokeBeanFactoryPostProcessors}, antes de que
 * {@code registerBeanPostProcessors} registre el post-processor que resuelve
 * constructores autowireados — con un constructor no vacío, Spring intenta
 * instanciar la clase sin argumentos y falla con {@code BeanCreationException}.
 */
@Component
public class FelProdSafetyGuard implements BeanFactoryPostProcessor, EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(FelProdSafetyGuard.class);

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        String[] certificadores = beanFactory.getBeanNamesForType(CertificadorFelPort.class, true, false);
        if (certificadores.length > 0) {
            return;
        }
        if (!environment.getProperty("app.fel.requerido-real", Boolean.class, true)) {
            log.warn("Arranque en 'prod' con app.fel.requerido-real=false: se certificarán documentos FEL con el "
                    + "adaptador SIMULADO (UUID aleatorio, NUNCA un DTE fiscalmente válido). Aceptable solo "
                    + "mientras no se facture de verdad — ver docs/plan-mejoras.md Fase 1.");
            return;
        }
        throw new IllegalStateException(
                "Arranque en perfil 'prod' rechazado por configuración insegura: no hay ningún "
                        + "CertificadorFelPort real configurado (implemente y registre un adaptador real antes de "
                        + "emitir facturas reales, o ponga app.fel.requerido-real=false/FEL_REQUERIDO_REAL=false "
                        + "mientras el cliente siga en fase de pruebas).");
    }
}
