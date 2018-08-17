package com.pereira.springboot.app.model.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pereira.springboot.app.model.dao.IClienteDao;
import com.pereira.springboot.app.model.entity.Cliente;

@Service
@Transactional
public class ClienteServiceImpl implements IClienteService {
	
	@Autowired
	IClienteDao clienteDao;

	@Override
	public List<Cliente> findAll() {
		// TODO Auto-generated method stub
		return (List<Cliente>) clienteDao.findAll();
	}
	
	@Override
	public Page<Cliente> findAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return clienteDao.findAll(pageable);
	}

	@Override
	public void save(Cliente cliente) {
		// TODO Auto-generated method stub
		clienteDao.save(cliente);
	}

	@Override
	public Cliente findOne(Long id) {
		// TODO Auto-generated method stub
		if(clienteDao.existsById(id)) {
			return clienteDao.findById(id).get();	
		}else {
			return null;
		}
	}

	@Override
	public void delete(Long id) {
		// TODO Auto-generated method stub
		if(clienteDao.existsById(id)) {
			clienteDao.deleteById(id);	
		}
	}

}
