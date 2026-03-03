package com.mybucket4j.model.service;

import com.mybucket4j.model.BucketObject;
import software.amazon.awssdk.services.s3.model.S3Object;

public class BucketObjectMapper {
    public BucketObject toBucketObject(S3Object s3Object) {
        return new BucketObject(
                s3Object.key(),
                s3Object.size(),
                s3Object.lastModified()
        );
    }
}
