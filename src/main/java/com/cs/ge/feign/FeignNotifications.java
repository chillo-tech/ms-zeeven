package com.cs.ge.feign;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "FeignNotifications", url = "${providers.notifications.host}/${providers.notifications.base-path}")
public interface FeignNotifications {

    @PostMapping({"/notification"})
    void message(
            @RequestHeader(name = "X-application-name") String requester,
            @RequestParam(name = "types") List<String> types,
            @RequestBody Map<String, Object> params);

    @GetMapping({"statistic"})
    List<Map<String, String>> getStatistic(@RequestParam String id);

}
