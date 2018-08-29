package com.pereira.springboot.app.controller;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pereira.springboot.app.model.entity.Cliente;
import com.pereira.springboot.app.model.entity.DetalleFactura;
import com.pereira.springboot.app.model.entity.Producto;
import com.pereira.springboot.app.model.entity.Factura;
import com.pereira.springboot.app.model.service.IClienteService;

@Controller
@RequestMapping("/factura")
@SessionAttributes("factura")
public class FacturaController {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private IClienteService clienteService;
	
	@GetMapping("/ver/{id}")
	private String ver(@PathVariable(value="id") Long id, Model model, RedirectAttributes flash) {
		Factura factura = clienteService.findFacturaById(id);
		if(factura == null) {
			flash.addFlashAttribute("error","Factura no existe!");
			return"redirect:/listar";
		}
		model.addAttribute("factura", factura);
		model.addAttribute("titulo","Factura: ".concat(factura.getDescripcion()));
		return "factura/ver";
	}

	@GetMapping("/form/{clienteId}")
	public String crear(@PathVariable(value = "clienteId") Long clienteId, Map<String, Object> model,
			RedirectAttributes flash) {
		Cliente cliente = clienteService.findOne(clienteId);
		if (cliente == null) {
			flash.addAttribute("error", "Cliente no se encuentra en la BD!");
			return "redirect:/listar";
		}
		Factura factura = new Factura();
		factura.setCliente(cliente);
		model.put("factura", factura);
		model.put("titulo", "Crear Factura");
		return "factura/form";
	}

	@GetMapping(value = "/cargar-productos/{term}", produces = { "application/json" })
	public @ResponseBody List<Producto> cargarProductos(@PathVariable String term) {
		return clienteService.findByNombre(term);
	}

	@PostMapping("/form")
	public String guardar(@Valid Factura factura, BindingResult result, Model model,
			@RequestParam(name = "item_id[]", required = false) Long[] itemId,
			@RequestParam(name = "cantidad[]", required = false) Integer[] cantidades, RedirectAttributes flash,
			SessionStatus status) {
		if(result.hasErrors()) {
			model.addAttribute("titulo", "Crear Factura");
			return "factura/form";
		}
		if(itemId == null || itemId.length==0) {
			model.addAttribute("titulo", "Crear Factura");
			model.addAttribute("error","Factura no tiene Productos!");
			return "factura/form";
		}
		for (int i = 0; i < itemId.length; i++) {
			Producto producto = clienteService.findProductoById(itemId[i]);
			DetalleFactura linea = new DetalleFactura();
			linea.setCantidad(cantidades[i]);
			linea.setProducto(producto);
			factura.addDetalle(linea);
			log.info("ID: " + itemId[i] + ", CANTIDAD: " + cantidades[i]);
		}
		clienteService.saveFactura(factura);
		status.setComplete();
		flash.addAttribute("success", "Factura creada con exito!");
		return "redirect:/ver/" + factura.getCliente().getId();
	}

}
