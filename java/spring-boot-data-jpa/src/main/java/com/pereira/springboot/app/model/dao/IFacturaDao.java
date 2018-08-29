package com.pereira.springboot.app.model.dao;

import org.springframework.data.repository.CrudRepository;

import com.pereira.springboot.app.model.entity.Factura;

public interface IFacturaDao extends CrudRepository<Factura, Long> {

}
