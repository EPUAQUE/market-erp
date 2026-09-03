package com.ais.marketbackend.seguridad.infrastructure.external;

import com.ais.marketbackend.seguridad.domain.service.PasswordResetMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Adaptador de correo del flujo "olvidé mi contraseña" — usa el mismo bean
 * {@code JavaMailSender}/SMTP ya configurado (ALERT_SMTP_*, ver application.yml) que
 * {@code AlertaEmailService}, pero es un servicio separado a propósito: destinatario
 * dinámico (el correo del usuario que pide el reset) y asunto/cuerpo propios — no se
 * reutiliza ni se modifica {@code AlertaEmailService}, que es para alertas internas
 * con destinatario fijo.
 *
 * <p>Nunca propaga {@link MailException}: un fallo de envío (SMTP caído, etc.) no debe
 * cambiar la respuesta HTTP de {@code AuthController.forgotPassword}, que siempre es
 * genérica sea cual sea el resultado real del envío — evita filtrar por ese lado si
 * el correo existe o el SMTP está sano.
 */
@Service
public class PasswordResetMailSenderImpl implements PasswordResetMailSender {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailSenderImpl.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String resetUrlBase;

    public PasswordResetMailSenderImpl(
            JavaMailSender mailSender,
            @Value("${app.alertas.email.from:}") String from,
            @Value("${app.frontend.password-reset-url:https://inven365.com.gt/restablecer-password}") String resetUrlBase) {
        this.mailSender = mailSender;
        this.from = from;
        this.resetUrlBase = resetUrlBase;
    }

    @Override
    public void enviar(String correoDestino, String tokenPlano) {
        String enlace = resetUrlBase + "?token=" + tokenPlano;
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            if (from != null && !from.isBlank()) {
                mensaje.setFrom(from);
            }
            mensaje.setTo(correoDestino);
            mensaje.setSubject("Restablecer tu contraseña");
            mensaje.setText(
                    "Recibimos una solicitud para restablecer tu contraseña.\n\n"
                            + "Para continuar, abre este enlace (válido por 30 minutos):\n" + enlace + "\n\n"
                            + "Si tú no solicitaste esto, puedes ignorar este correo — tu contraseña no cambiará.");
            mailSender.send(mensaje);
        } catch (MailException e) {
            log.warn("No se pudo enviar el correo de restablecimiento de contraseña (revisar ALERT_SMTP_*).", e);
        }
    }
}
