package com.cs.ge.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("webhooks")
public class WhatsappWebHooks {
    @ResponseBody
    @GetMapping
    public String fetchGuests(@RequestParam(name = "hub.mode") String mode,
                              @RequestParam(name = "hub.challenge") String challenge,
                              @RequestParam(name = "hub.verify_token") String token) {
        log.info("Verification whatapp mode {} challenge {} token {}", mode, challenge, token);
        return challenge;
    }

    @PostMapping(consumes = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public void status(@RequestBody Map<String, Object> status) {
        log.info("### B STATUS ####");
        log.info("{}", status);
        log.info("### A STATUS ####");
    }

}
