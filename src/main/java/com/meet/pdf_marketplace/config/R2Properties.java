package com.meet.pdf_marketplace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloudflare.r2")
public class R2Properties {

    private String accountId;

    private String accessKeyId;

    private String secretAccessKey;

    private String bucketName;

    private String endpoint;

    private String publicBaseUrl;
}
