package com.cs.ge.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "IPGeolocation", url = "${providers.iplocation.host}/${providers.iplocation.base-path}/?apiKey=${providers.iplocation.key}")
public interface IPGeolocation {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> ipgeo(
            @RequestParam(name = "ip") String ip
    );

}
