package br.edu.ufape.alugafacil.services;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import br.edu.ufape.alugafacil.services.interfaces.IFileStorageService;

@Service
@Profile("prod")
public class CloudinaryFileStorageService implements IFileStorageService {
	private final Cloudinary cloudinary;

	public CloudinaryFileStorageService(
			@Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
			) {
		this.cloudinary = new Cloudinary(ObjectUtils.asMap(
				"cloud_name", cloudName,
				"api_key", apiKey,
				"api_secret", apiSecret));
	}
	
	@Override
	public String uploadFile(MultipartFile file) {
		try {
			Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
					ObjectUtils.asMap("public_id", UUID.randomUUID().toString()));
			
			return uploadResult.get("secure_url").toString();
			
		} catch (IOException e) {
			throw new RuntimeException("Erro ao subir imagem para o Cloudinary", e);
		}
	}

	@Override
	public void deleteFile(String fileUrl) {
		// TODO Auto-generated method stub
		
	}
}
