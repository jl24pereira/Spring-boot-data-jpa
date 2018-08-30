package com.pereira.springboot.app.model.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.pereira.springboot.app.model.entity.Cliente;

public interface IClienteDao extends PagingAndSortingRepository<Cliente, Long>{

	@Query("select c from Cliente c left join fetch c.facturas where c.id=?1")
	public Cliente fetchByIdWithFacturas(Long id);
}
