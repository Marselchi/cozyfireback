package com.cozyfireplace.server.images;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;

import java.net.URI;

@Slf4j
@Configuration
@EnableConfigurationProperties(ImageStorageProperties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(ImageStorageProperties props) {
        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(props.getEndpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())))
                // RustFS/S3-compatible endpoint: keep the bucket in the path, not the hostname.
                .forcePathStyle(true)
                .build();
        ensureBucket(client, props.getBucket());
        return client;
    }

    private void ensureBucket(S3Client client, String bucket) {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            try {
                client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                // Public-read so stored URLs are directly renderable by the frontend.
                String policy = """
                        {"Version":"2012-10-17","Statement":[{"Effect":"Allow",\
                        "Principal":{"AWS":["*"]},"Action":["s3:GetObject"],\
                        "Resource":["arn:aws:s3:::%s/*"]}]}""".formatted(bucket);
                client.putBucketPolicy(PutBucketPolicyRequest.builder()
                        .bucket(bucket)
                        .policy(policy)
                        .build());
                log.info("Created bucket '{}' with public-read policy", bucket);
            } catch (Exception inner) {
                log.warn("Could not create bucket '{}' or apply its policy: {}", bucket, inner.getMessage());
            }
        } catch (Exception e) {
            log.warn("Could not verify bucket '{}': {}", bucket, e.getMessage());
        }
    }
}
