package com.itheima.pinda.zuul.adapter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 忽略token 配置类
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "zuul.ignore.auth")
public class IgnoreTokenConfig {

    public List<String> url;

    //public List<String> route;


    private static List<String> URL;
    //private static List<String> ROUTE;

    @PostConstruct
    public void init() {
        this.URL = url;
        //this.ROUTE = route;
        log.info("初始化 TOKEN 忽略配置 URL:{}", url);
    }

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    public static boolean isIgnoreToken(String currentUri) {
        return isIgnore(URL, currentUri);
    }

    public static boolean isIgnore(List<String> list, String currentUri) {
        if (list.isEmpty()) {
            return false;
        }
        return list.stream().anyMatch((url) -> currentUri.startsWith(url) || ANT_PATH_MATCHER.match(url, currentUri));
    }
}
