package com.cs.ge.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Service
@FeignClient(name = "FeignMaxMindIPGeolocation", url = "${providers.maxmind.host}/${providers.maxmind.base-path}")
public interface FeignMaxMindIPGeolocation {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "{ip}")
    Map<String, Object> ipgeo(@PathVariable final String ip);
}
