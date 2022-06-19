package com.cs.ge.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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

}
