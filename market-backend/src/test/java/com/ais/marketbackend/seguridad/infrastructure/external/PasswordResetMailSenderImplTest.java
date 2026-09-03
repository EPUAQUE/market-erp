package com.ais.marketbackend.seguridad.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class PasswordResetMailSenderImplTest {

    @Test
    void enviarArmaElEnlaceConElTokenEnClaroYLoManda() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        PasswordResetMailSenderImpl sender = new PasswordResetMailSenderImpl(
                mailSender, "no-reply@inven365.com.gt", "https://inven365.com.gt/restablecer-password");

        sender.enviar("usuario@correo.com", "token-plano-123");

        org.mockito.ArgumentCaptor<SimpleMailMessage> captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensaje = captor.getValue();
        assertThat(mensaje.getTo()).containsExactly("usuario@correo.com");
        assertThat(mensaje.getText()).contains("https://inven365.com.gt/restablecer-password?token=token-plano-123");
    }

    @Test
    void enviarNoPropagaExcepcionSiFallaElEnvio() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("SMTP no disponible")).when(mailSender).send(any(SimpleMailMessage.class));
        PasswordResetMailSenderImpl sender = new PasswordResetMailSenderImpl(
                mailSender, "", "https://inven365.com.gt/restablecer-password");

        sender.enviar("usuario@correo.com", "token-plano-123");
    }
}
