package com.cs.ge.feign;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "files handler", url = "${app.images-inner-host}")
public interface FIleHandler {

    @PostMapping({"/v1/files"})
    void send(
            @RequestBody Map<String, Object> params);

}
