package com.pereira.springboot.app.model.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.pereira.springboot.app.model.entity.Factura;

public interface IFacturaDao extends CrudRepository<Factura, Long> {
	
	@Query("select f from Factura f join fetch f.cliente c join fetch f.detalle d join fetch d.producto where f.id=?1")
	public Factura fetchByIdWithClienteWhitDetalleFacturaWithProducto(Long id);
}
