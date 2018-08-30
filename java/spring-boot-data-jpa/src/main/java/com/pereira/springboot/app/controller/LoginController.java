package com.pereira.springboot.app.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {
	
	@GetMapping("/login")
	public String login(@RequestParam(value="error", required=false) String error,@RequestParam(value="logout", required=false) String logout,Model model, Principal principal, RedirectAttributes flash) {
		if(principal!=null) {
			flash.addFlashAttribute("info", "Sesion iniciada!");
			return "redirect:/";
		}
		if(error !=null) {
			model.addAttribute("error","Error en login, nombre de usuario o contraseña incorrecta");
		}
		if(logout!=null) {
			model.addAttribute("success", "Sesion cerrada exitosamente!");
		}
		return "login";
	}

}
