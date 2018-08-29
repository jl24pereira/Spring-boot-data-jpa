package com.pereira.springboot.app.model.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadServiceImpl implements IUploadService {

	private final static String PATH = "uploads";

	@Override
	public String upload(MultipartFile foto) throws IOException {
		// TODO Auto-generated method stub
		if (!foto.isEmpty()) {
			String uniqueFilename = UUID.randomUUID().toString() + "_" + foto.getOriginalFilename();
			Path rootAbsolutePath = Paths.get(PATH).resolve(uniqueFilename).toAbsolutePath();
			Files.copy(foto.getInputStream(), rootAbsolutePath);
		}
		return null;
	}
}
