package fit.hutech.HuynhLeHongXuyen.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sepay")
@Getter
@Setter
public class SepayConfig {
    private String apiToken;
    private String accountNumber;
    private String bankCode;
    private String accountName;
}
