package com.pereira.springboot.app.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pereira.springboot.app.model.entity.Cliente;
import com.pereira.springboot.app.model.service.IClienteService;
import com.pereira.springboot.app.model.service.IUploadService;
import com.pereira.springboot.app.util.paginator.PageRender;

@Controller
public class ClienteController {

	@Autowired
	private IClienteService service;

	@Autowired
	private IUploadService uploadService;

	@RequestMapping(value = "/listar", method = RequestMethod.GET)
	public String listar(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
		Pageable pageRequest = PageRequest.of(page, 5);
		Page<Cliente> clientes = service.findAll(pageRequest);
		PageRender<Cliente> pageRender = new PageRender<>("/listar", clientes);
		model.addAttribute("titulo", "Listado de Clientes");
		model.addAttribute("clientes", clientes);
		model.addAttribute("page", pageRender);
		return "listar";
	}

	@RequestMapping(value = "/form")
	public String crear(Map<String, Object> model) {
		Cliente cliente = new Cliente();
		model.put("cliente", cliente);
		model.put("titulo", "Crear de Cliente");
		return "form";
	}

	@RequestMapping(value = "/form", method = RequestMethod.POST)
	public String guardar(@Valid Cliente cliente, BindingResult result, Map<String, Object> model,
			RedirectAttributes flash, @RequestParam("file") MultipartFile foto) {
		model.put("titulo", "Crear de Cliente");
		if (result.hasErrors()) {
			return "form";
		}
		try {
			uploadService.upload(foto);
			flash.addAttribute("info", "Foto subida con exito!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		service.save(cliente);
		flash.addFlashAttribute("success", "Cliente Registrado con Exito!");
		return "redirect:/listar";
	}

	@RequestMapping(value = "/form/{id}")
	public String editar(@PathVariable(value = "id") Long id, Map<String, Object> model, RedirectAttributes flash) {
		Cliente cliente = null;
		if (id > 0) {
			cliente = service.findOne(id);
			System.out.println("CLIENTE: " + cliente);
			if (cliente == null) {
				flash.addFlashAttribute("error", "ID de Cliente no valido!!");
				return "redirect:/listar";
			}
		} else {
			flash.addFlashAttribute("error", "ID de Cliente no valido!");
			return "redirect:/listar";
		}
		model.put("cliente", cliente);
		model.put("btnAction", "Editar");
		return "form";
	}

	@RequestMapping(value = "/eliminar/{id}")
	public String eliminar(@PathVariable(value = "id") Long id, RedirectAttributes flash) {
		if (id > 0) {
			service.delete(id);
		}
		flash.addFlashAttribute("success", "Cliente Eliminado con Exito!");
		return "redirect:/listar";
	}

	@GetMapping(value = "/ver/{id}")
	public String ver(@PathVariable(value = "id") Long id, Map<String, Object> model, RedirectAttributes flash) {
		Cliente cliente = service.findOne(id);
		if (cliente == null) {
			flash.addFlashAttribute("error", "ID de Cliente no valido!!");
			return "redirect:/listar";
		}
		model.put("cliente", cliente);
		model.put("titulo", "Detalle de Cliente: " + cliente.getNombre() + " " + cliente.getApellido());
		return "ver";
	}
}
