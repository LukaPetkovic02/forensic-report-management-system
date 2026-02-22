package com.example.backend.service;

import com.example.backend.configuration.MinioConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioConfig minioConfig;
    private final MinioClient minioClient;

    public String uploadFile(MultipartFile file){
        try{
            String objectName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return objectName;
        }catch (Exception e){
            throw new RuntimeException("Error uploading to MinIO", e);
        }
    }
}
