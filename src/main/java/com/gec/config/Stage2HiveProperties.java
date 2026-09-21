package com.gec.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "stage2.hive")
public class Stage2HiveProperties {
    private String url;
    private String username;
    private String password;
    private String driverJar;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDriverJar() { return driverJar; }
    public void setDriverJar(String driverJar) { this.driverJar = driverJar; }
}
