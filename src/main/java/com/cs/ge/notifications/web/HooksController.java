package com.cs.ge.notifications.web;

import com.cs.ge.notifications.records.whatsapp.WhatsappNotification;
import com.cs.ge.notifications.service.hooks.HooksService;
import com.cs.ge.providers.StripeService;
import com.stripe.exception.StripeException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(path = "hooks", produces = APPLICATION_JSON_VALUE)
public class HooksController {

    private HooksService hooksService;
    private StripeService stripeService;

    @PostMapping(path = "vonage", consumes = APPLICATION_JSON_VALUE)
    public void vonage(@RequestBody final Map<String, Object> params) {
        this.hooksService.vonage(params);
    }

    @PostMapping(path = "twilio", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public void twilio(@RequestBody final MultiValueMap<String, Object> params) {
        this.hooksService.twilio(params);
    }

    @PostMapping(path = "whatsapp", consumes = APPLICATION_JSON_VALUE)
    public void whatsapp(@RequestBody final WhatsappNotification notification) {
        this.hooksService.whatsapp(notification);
    }

    @PostMapping(path = "brevo", consumes = APPLICATION_JSON_VALUE)
    public void sendingblue(@RequestBody final Map<String, Object> params) {
        this.hooksService.brevo(params);
    }

    @GetMapping(path = "whatsapp")
    public String whatsapp(
            @RequestParam(required = false, name = "hub.verify_token") final String token,
            @RequestParam(required = false, name = "hub.challenge") final String challenge,
            @RequestParam(required = false, name = "hub.mode") final String mode) {
        return challenge;
    }


    @PostMapping(path = "stripe", consumes = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    public void stripe(
            @RequestHeader("Stripe-Signature") final String signature,
            @RequestBody final String body) throws StripeException {
        this.stripeService.webhook(body, signature);

    }
}
