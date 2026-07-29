package homework.week4.FileUpload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Component
public class FileStorage implements FileStorageService {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region}")
    private String region;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private S3Client s3Client;

    private S3Client getS3Client() {
        if (s3Client == null) {
            s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        }
        return s3Client;
    }

    @Override
    public String storeProfileImage(MultipartFile file) {
        return uploadToS3(file, "profile");
    }

    @Override
    public String storePostImage(MultipartFile file) {
        return uploadToS3(file, "post");
    }

    private String uploadToS3(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String storedFilename = folder + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(storedFilename)
                    .contentType(file.getContentType())
                    .build();

            getS3Client().putObject(request, RequestBody.fromInputStream(
                    file.getInputStream(), file.getSize()));

        } catch (IOException e) {
            throw new RuntimeException("S3 파일 업로드에 실패했습니다.", e);
        }

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, storedFilename);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}