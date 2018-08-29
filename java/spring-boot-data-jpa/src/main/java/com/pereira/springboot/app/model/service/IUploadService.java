package com.pereira.springboot.app.model.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface IUploadService {

	public String upload(MultipartFile foto) throws IOException;

}
