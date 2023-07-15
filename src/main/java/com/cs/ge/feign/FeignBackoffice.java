package com.cs.ge.feign;

import com.cs.ge.dto.prices.BackofficePrice;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(name = "backoffice", url = "${providers.backoffice.host}/${providers.backoffice.base-path}")
public interface FeignBackoffice {

    @GetMapping(path = "page/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    BackofficePrice page(@PathVariable(name = "id") String id, @RequestParam String fields);

}
