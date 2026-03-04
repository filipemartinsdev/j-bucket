package com.jbucket.model.service;

import com.jbucket.model.BucketObject;
import com.jbucket.model.BucketSession;
import com.jbucket.model.exception.InvalidCredentialsException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.util.List;

public class BucketService {
    private final BucketSession bucketSession;
    private final S3Client s3Client;

    private final BucketObjectMapper bucketObjectMapper;

    public BucketService(BucketSession bucketSession) throws Exception{
        this.bucketObjectMapper = new BucketObjectMapper();
        this.bucketSession = bucketSession;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(bucketSession.accessKey(), bucketSession.secretKey());

        this.s3Client = S3Client.builder()
                .region(bucketSession.region())
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        connectionCheck();
    }

    public void connectionCheck(){
        try {
            s3Client.listBuckets();
        } catch (S3Exception e) {
            throw new InvalidCredentialsException("Invalid credentials for the bucket");
        }
    }

    public List<BucketObject> listFiles(){
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketSession.bucketName())
                .build();
        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        return response.contents().stream()
                .map(s3Object -> bucketObjectMapper.toBucketObject(s3Object))
                .toList();
    }

    public List<BucketObject> searchFile(String fileName){
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketSession.bucketName())
                .prefix(fileName)
                .build();
        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        return response.contents().stream()
                .map(s3Object -> bucketObjectMapper.toBucketObject(s3Object))
                .toList();
    }

    public void uploadFile(File file, String name){
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .key(name)
                    .bucket(bucketSession.bucketName())
                    .build();
            s3Client.putObject(request, RequestBody.fromFile(file));
        } catch (Exception e) {
            throw new RuntimeException("Can't upload file");
        }
    }

    public void downloadFile(String fileName){
//        GetObjectRequest request = GetObjectRequest.builder()
//                .bucket(bucketSession.bucketName())
//                .key(fileName)
//                .build();
//        GetObjectResponse response = s3Client.getObject(request).response();
    }

    public void deleteFile(String filename){
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketSession.bucketName())
                .key(filename)
                .build();
        s3Client.deleteObject(request);
    }

    public void close() {
        this.s3Client.close();
    }

    public BucketSession getSession(){
        return this.bucketSession;
    }

    public String getFormatedFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        String[] unities = {"B", "KB", "MB", "GB", "TB"};
        int index = (int) (Math.log(bytes) / Math.log(1024));

        double formatedValue = bytes / Math.pow(1024, index);
        return String.format("%.1f %s", formatedValue, unities[index])
                .replace(",0", "");
    }
}
