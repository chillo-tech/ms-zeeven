package com.cs.ge.controllers;

import com.cs.ge.providers.StripeService;
import com.stripe.exception.StripeException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("webhooks")
public class WebHooks {
    StripeService stripeService;

    @ResponseBody
    @GetMapping
    public String fetchGuests(@RequestParam(name = "hub.mode") final String mode,
                              @RequestParam(name = "hub.challenge") final String challenge,
                              @RequestParam(name = "hub.verify_token") final String token) {
        log.info("Verification whatapp mode {} challenge {} token {}", mode, challenge, token);
        return challenge;
    }

    @PostMapping(consumes = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public void status(@RequestBody final Map<String, Object> status) {
        log.info("### B STATUS ####");
        log.info("{}", status);
        log.info("### A STATUS ####");
    }

    @PostMapping(path = "stripe", consumes = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public void stripe(
            @RequestHeader("Stripe-Signature") final String signature,
            @RequestBody final String body) throws StripeException {
        this.stripeService.webhook(body, signature);

    }

}
