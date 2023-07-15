package com.cs.ge.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "FeignIPGeolocation", url = "${providers.iplocation.host}/${providers.iplocation.base-path}/?apiKey=${providers.iplocation.key}")
public interface FeignIPGeolocation {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> ipgeo(
            @RequestParam(name = "ip") String ip
    );

}
