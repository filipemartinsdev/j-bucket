package com.mybucket4j.model;

import software.amazon.awssdk.regions.Region;

public record BucketSession (
    String bucketName,
    String accessKey,
    String secretKey,
    Region region
){
}
