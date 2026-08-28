package com.ais.marketbackend.fel.infrastructure.certificador;

import com.ais.marketbackend.fel.application.ports.CertificadorFelPort;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Rechaza el arranque en el perfil {@code prod} si no hay ningún
 * {@link CertificadorFelPort} real configurado. {@link DevCertificadorFelAdapter}
 * está restringido a {@code @Profile("!prod")}, así que en producción solo hay una
 * definición de bean para ese puerto si se implementó y registró un adaptador real.
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
        if (certificadores.length == 0) {
            throw new IllegalStateException(
                    "Arranque en perfil 'prod' rechazado por configuración insegura: no hay ningún "
                            + "CertificadorFelPort real configurado (DevCertificadorFelAdapter está deshabilitado "
                            + "en 'prod' a propósito; implemente y registre un adaptador real antes de operar en "
                            + "producción).");
        }
    }
}
