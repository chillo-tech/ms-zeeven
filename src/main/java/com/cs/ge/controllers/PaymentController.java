package com.cs.ge.controllers;

import com.cs.ge.entites.ApplicationPayment;
import com.cs.ge.providers.StripeService;
import com.stripe.exception.StripeException;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@AllArgsConstructor
@RestController
@RequestMapping(path = "payment", produces = APPLICATION_JSON_VALUE)
public class PaymentController {
    private final StripeService stripeService;

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<String> add(@RequestBody final ApplicationPayment payment) throws StripeException {
        final String url = this.stripeService.session(payment);
        return ResponseEntity.ok(url);
    }

}
