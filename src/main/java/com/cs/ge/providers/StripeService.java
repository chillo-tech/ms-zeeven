package com.cs.ge.providers;

import com.cs.ge.entites.ApplicationPayment;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.StockType;
import com.cs.ge.repositories.PaymentRepository;
import com.cs.ge.services.ProfileService;
import com.cs.ge.services.StockService;
import com.cs.ge.services.notifications.ASynchroniousNotifications;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.WebhookEndpoint;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.WebhookEndpointCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class StripeService {

    private final String webhooksecretToken;
    private final String front;
    private final String appHost;
    private final String contextPath;
    private final ProfileService profileService;
    private final StockService stockService;
    private final PaymentRepository paymentRepository;
    private final ASynchroniousNotifications aSynchroniousNotifications;
    private final Environment environment;

    public StripeService(
            @Value("${providers.stripe.key.secret}") final String secretToken,
            @Value("${app.host}") final String appHost,
            @Value("${providers.stripe.key.webhooksecret}") final String webhooksecretToken,
            @Value("${providers.front.host}") final String front,
            @Value("${server.servlet.context-path}") final String contextPath,
            final ProfileService profileService,
            final StockService stockService,
            final PaymentRepository paymentRepository,
            final ASynchroniousNotifications aSynchroniousNotifications,
            final Environment environment) {
        this.environment = environment;
        Stripe.apiKey = secretToken;
        this.appHost = appHost;
        this.contextPath = contextPath;
        this.profileService = profileService;
        this.stockService = stockService;
        this.paymentRepository = paymentRepository;
        this.aSynchroniousNotifications = aSynchroniousNotifications;
        this.webhooksecretToken = webhooksecretToken;
        this.front = front;
    }

    public String session(final ApplicationPayment payment) throws StripeException {
        final UserAccount account = this.profileService.getAuthenticateUser();
        payment.setUserId(account.getId());
        payment.setUserEmail(account.getEmail());
        payment.setUserName(String.format("%s %s", account.getFirstName(), account.getLastName()));
        payment.setPublicId(RandomStringUtils.random(10, false, true));
        final Map<String, String> paymentSession = this.getPaymentLink(payment, account.getEmail());
        payment.setProviderSessionId(paymentSession.get("id"));
        payment.setProviderSessionUrl(paymentSession.get("url"));
        payment.setProviderSessionStatus("initiation");
        this.paymentRepository.save(payment);
        return paymentSession.get("url");
    }

    private Map<String, String> getPaymentLink(final ApplicationPayment payment, final String email) throws StripeException {

        if (!Objects.equals(this.environment.getActiveProfiles()[0], "local")) {

            final WebhookEndpointCreateParams.Builder builder = WebhookEndpointCreateParams.builder()
                    .addAllEnabledEvent(Arrays.asList(
                            WebhookEndpointCreateParams.EnabledEvent.CHARGE__FAILED,
                            WebhookEndpointCreateParams.EnabledEvent.CHARGE__SUCCEEDED));

            builder.setUrl(String.format("%s%s/webhooks/stripe", this.appHost, this.contextPath));
            final WebhookEndpointCreateParams webhooksparams = builder.build();
            WebhookEndpoint.create(webhooksparams);
        }


        SessionCreateParams.Mode mode = SessionCreateParams.Mode.PAYMENT;
        if (Objects.equals("BILLING", payment.getType())) {
            mode = SessionCreateParams.Mode.SUBSCRIPTION;
        }
        final ProductCreateParams productParams = ProductCreateParams.builder().setName(payment.getProductName())
                .build();
        final Product product = Product.create(productParams);
        final double ttc = this.getTTC(payment.getAmountHT(), payment.getTva());
        final PriceCreateParams.Builder priceParams =
                PriceCreateParams.builder()
                        .setProduct(product.getId())
                        .setUnitAmount((long) (ttc * 100L))
                        .setCurrency("eur");
        if (Objects.equals("BILLING", payment.getType())) {
            priceParams.setRecurring(PriceCreateParams.Recurring.builder().setInterval(PriceCreateParams.Recurring.Interval.valueOf(payment.getFrequence())).build());
        }
        final Price price = Price.create(priceParams.build());
        final SessionCreateParams params =
                SessionCreateParams.builder()
                        .setCustomerEmail(email)
                        .setMode(mode)
                        .setSuccessUrl(String.format("%s/me/confirmation-achat", this.front))
                        .setCancelUrl(String.format("%s/me/acheter-une-offre?product=%s&option=%s", this.front, payment.getProductId(), payment.getOptionId()))
                        .addLineItem(new SessionCreateParams.LineItem.Builder()
                                // For metered billing, do not pass quantity
                                .setQuantity(1L)
                                .setPrice(price.getId())
                                .build()
                        )
                        .build();
        final Session session = Session.create(params);
        return Map.of("id", session.getId(), "url", session.getUrl());


    }

    public void webhook(final String body, final String signature) throws StripeException {

        try {
            final Event event = Webhook.constructEvent(body, signature, this.webhooksecretToken);
            if (Objects.equals("checkout.session.completed", event.getType())) {
                final EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
                if (dataObjectDeserializer.getObject().isPresent()) {
                    final JSONObject jsonObject = new JSONObject(body);
                    final JSONObject data = (JSONObject) jsonObject.get("data");
                    final JSONObject object = (JSONObject) data.get("object");
                    final String sessionId = (String) object.get("id");
                    final Optional<ApplicationPayment> optinnalCurrentPayment = this.paymentRepository.findByProviderSessionId(sessionId);
                    if (optinnalCurrentPayment.isPresent()) {
                        final ApplicationPayment applicationPayment = optinnalCurrentPayment.get();
                        applicationPayment.setId(null);
                        applicationPayment.setProviderSessionStatus("completed");
                        final ApplicationPayment payment = this.paymentRepository.save(applicationPayment);
                        this.sendPaiementNotifications(payment);
                        //@TODO enregistrer la vente dans le backoffice
                        //@TODO enregistrer Récupérer les crédits du backoffice pour incrémenter le stock du user
                        this.stockService.update(payment.getUserId(), payment, null, 5, payment.getChannel(), StockType.CREDIT);
                    }
                }
            }
        } catch (final StripeException | JSONException e) {
            e.printStackTrace();
        }

    }

    private void sendPaiementNotifications(final ApplicationPayment payment) {
        final UserDetails userDetails = this.profileService.loadUserByUsername(payment.getUserEmail());
        final String amount = String.valueOf(this.getTTC(payment.getAmountHT(), payment.getTva()));
        this.aSynchroniousNotifications.sendEmail(
                null,
                (UserAccount) userDetails,
                Map.of("publicId", List.of(payment.getPublicId()), "amount", List.of(amount)),
                "ZEEVEN",
                "payment-confirmation.html",
                null,
                "Votre paiement");
        this.aSynchroniousNotifications.sendEmail(
                null,
                null,
                Map.of(
                        "publicId", List.of(payment.getPublicId()),
                        "amount", List.of(amount),
                        "name", List.of(String.format("%s", payment.getUserName()))
                ),
                "ZEEVEN",
                "internal-payment-notification.html",
                null,
                "Nouveau paiement"
        );
    }

    private double getTTC(final Long ht, final Long tva) {
        final double tvaAsPercentage = Double.valueOf(tva) / 100;
        return (1 + tvaAsPercentage) * ht;
    }
}
