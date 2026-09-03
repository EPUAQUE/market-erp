package com.ais.marketbackend.seguridad.infrastructure.persistence.mappers;

import com.ais.marketbackend.seguridad.domain.model.PasswordResetToken;
import com.ais.marketbackend.seguridad.infrastructure.persistence.entities.PasswordResetTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PasswordResetTokenEntityMapper {

    PasswordResetToken toDomain(PasswordResetTokenEntity entity);

    PasswordResetTokenEntity toEntity(PasswordResetToken domain);
}
