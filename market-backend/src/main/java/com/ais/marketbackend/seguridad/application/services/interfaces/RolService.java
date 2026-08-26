package com.ais.marketbackend.seguridad.application.services.interfaces;

import com.ais.marketbackend.seguridad.application.dtos.RolResumen;
import java.util.List;

public interface RolService {

    List<RolResumen> listar();
}
