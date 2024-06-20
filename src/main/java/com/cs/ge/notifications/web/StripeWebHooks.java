package com.cs.ge.notifications.web;

import com.cs.ge.providers.StripeService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("webhooks")
public class StripeWebHooks {
    StripeService stripeService;


}
