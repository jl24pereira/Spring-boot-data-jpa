package com.pereira.springboot.app.model.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pereira.springboot.app.model.dao.IClienteDao;
import com.pereira.springboot.app.model.dao.IFacturaDao;
import com.pereira.springboot.app.model.dao.IProductoDao;
import com.pereira.springboot.app.model.entity.Cliente;
import com.pereira.springboot.app.model.entity.Factura;
import com.pereira.springboot.app.model.entity.Producto;

@Service
@Transactional
public class ClienteServiceImpl implements IClienteService {
	
	@Autowired
	IClienteDao clienteDao;
	
	@Autowired
	IProductoDao productoDao;
	
	@Autowired
	IFacturaDao facturaDao;

	@Override
	@Transactional(readOnly = true)
	public List<Cliente> findAll() {
		// TODO Auto-generated method stub
		return (List<Cliente>) clienteDao.findAll();
	}

	@Override
	@Transactional
	public void save(Cliente cliente) {
		clienteDao.save(cliente);
		
	}

	@Override
	@Transactional(readOnly = true)
	public Cliente findOne(Long id) {
		// TODO Auto-generated method stub
		return clienteDao.findById(id).orElse(null);
	}
	
	@Override
	@Transactional(readOnly=true)
	public Cliente fetchByIdWithFacturas(Long id) {
		// TODO Auto-generated method stub
		return clienteDao.fetchByIdWithFacturas(id);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		clienteDao.deleteById(id);
		
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Cliente> findAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return clienteDao.findAll(pageable);
	}

	@Override
	public List<Producto> findByNombre(String nombre) {
		// TODO Auto-generated method stub
		return productoDao.findByNombreLikeIgnoreCase("%"+nombre+"%");
	}

	@Override
	public void saveFactura(Factura factura) {
		// TODO Auto-generated method stub
		facturaDao.save(factura);
	}

	@Override
	public Producto findProductoById(Long id) {
		// TODO Auto-generated method stub
		return productoDao.findById(id).orElse(null);
	}

	@Override
	@Transactional(readOnly=true)
	public Factura findFacturaById(Long id) {
		// TODO Auto-generated method stub
		return facturaDao.findById(id).orElse(null);
	}

	@Override
	public void deleteFactura(Long id) {
		// TODO Auto-generated method stub
		facturaDao.deleteById(id);
	}

	@Override
	@Transactional(readOnly=true)
	public Factura fetchByIdWithClienteWhitDetalleFacturaWithProducto(Long id) {
		// TODO Auto-generated method stub
		return facturaDao.fetchByIdWithClienteWhitDetalleFacturaWithProducto(id);
	}
	
}
