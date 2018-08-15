package com.pereira.springboot.app.model.dao;

import java.util.List;

import com.pereira.springboot.app.model.entity.Cliente;

public interface IClienteDao {
	
	public List<Cliente> findAll();
	public void save(Cliente cliente);
	public Cliente findOne(Long id);

}
