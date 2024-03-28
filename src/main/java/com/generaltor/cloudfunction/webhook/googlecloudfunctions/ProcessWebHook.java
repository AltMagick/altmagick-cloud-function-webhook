package com.generaltor.cloudfunction.webhook.googlecloudfunctions;

import com.generaltor.cloudfunction.webhook.entities.Order;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Named("processWebHook")
@ApplicationScoped
public class ProcessWebHook implements HttpFunction {
    private static final Logger LOG = Logger.getLogger(ProcessWebHook.class);
    private static final Gson gson = new Gson();
    private static final String HMAC_SHA256 = "HmacSHA256";
    @Inject
    Firestore firestore;
    @ConfigProperty(name = "quarkus.secrets.lemonsqueezy.signingsecret")
    String signingSecret;

    @Override
    public void service(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        String requestBody = readRequestBody(httpRequest);

        String signatureHeader = httpRequest.getFirstHeader("X-Signature").orElse("");

        if (!isSignatureValid(signatureHeader, requestBody)) {
            httpResponse.setStatusCode(403);
            LOG.error("Invalid signature");
            return;
        }

        JsonObject requestJson = gson.fromJson(requestBody, JsonObject.class);
        String eventType = requestJson.getAsJsonObject("meta").get("event_name").getAsString();

        switch (eventType) {
            case "subscription_created":
                handleSubscriptionCreated(requestJson);
                break;
            case "subscription_updated":
                handleSubscriptionUpdated(requestJson);
                break;
            case "license_key_created":
                handleLicenseKeyCreated(requestJson);
                break;
            case "license_key_updated":
                handleLicenseKeyUpdated(requestJson);
                break;
            default:
                LOG.error("Unknown event type: " + eventType);
                break;
        }

        httpResponse.setStatusCode(200);
    }

    private boolean isSignatureValid(String signatureHeader, String payload) {
        try {
            Mac sha256_HMAC = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secret_key = new SecretKeySpec(signingSecret.getBytes(), HMAC_SHA256);
            sha256_HMAC.init(secret_key);

            byte[] hash = sha256_HMAC.doFinal(payload.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            String hashHex = sb.toString();

            return hashHex.equals(signatureHeader);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
    }

    private void handleSubscriptionCreated(JsonObject eventData) {
        try {
            JsonObject dataObject = eventData.getAsJsonObject("data");

            if (dataObject != null && dataObject.has("attributes")) {
                JsonObject attributes = dataObject.getAsJsonObject("attributes");

                String orderId = attributes.get("order_id").getAsString();
                Order order = new Order(
                        attributes.get("created_at").getAsString(),
                        attributes.get("created_at").getAsString(),
                        0,
                        null,
                        false
                );
                CollectionReference orders = firestore.collection("orders");
                List<ApiFuture<WriteResult>> futures = new ArrayList<>();
                futures.add(orders.document(orderId).set(order));
                ApiFutures.allAsList(futures).get();

                LOG.info("Order created: " + orderId);
            } else {
                System.out.println("Missing data in the webhook payload for the event subscription_expired.");
            }
        } catch (Exception e) {
            LOG.error("Error while processing subscription_created event", e);

        }
    }

    private void handleSubscriptionUpdated(JsonObject eventData) {
        try {
            JsonObject dataObject = eventData.getAsJsonObject("data");

            if (dataObject != null && dataObject.has("attributes")) {
                JsonObject attributes = dataObject.getAsJsonObject("attributes");

                String orderId = attributes.get("order_id").getAsString();
                String status = attributes.get("status").getAsString();

                DocumentReference orderRef = firestore.collection("orders").document(orderId);
                ApiFuture<DocumentSnapshot> future = orderRef.get();

                try {
                    DocumentSnapshot document = future.get();
                    if (document.exists()) {
                        if (status.equals("active")) {
                            orderRef.update("isExpired", false);
                        } else {
                            orderRef.update("isExpired", true);
                        }
                        LOG.info("Subscription updated for order: " + orderId + " with status: " + status);
                    } else {
                        LOG.error("No such document: " + orderId);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Error updating document: " + e.getMessage());
                }

            } else {
                LOG.error("Missing data in the webhook payload for the event subscription_updated.");
            }
        } catch (Exception e) {
            LOG.error("Error while processing subscription_updated event", e);
        }
    }

    private void updateLicenseKeyInOrder(String orderId, String licenseKey, String logMessage) {
        DocumentReference orderRef = firestore.collection("orders").document(orderId);
        ApiFuture<DocumentSnapshot> future = orderRef.get();
        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                orderRef.update("licenceKey", licenseKey);
                LOG.info(logMessage + orderId);
            } else {
                LOG.error("Document not found: " + orderId);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error updating document: " + e.getMessage());
        }
    }

    private void handleLicenseKeyCreated(JsonObject eventData) {
        try {
            JsonObject dataObject = eventData.getAsJsonObject("data");
            if (dataObject != null && dataObject.has("attributes")) {
                JsonObject attributes = dataObject.getAsJsonObject("attributes");
                String orderId = attributes.get("order_id").getAsString();
                String licenseKey = attributes.get("key").getAsString();
                updateLicenseKeyInOrder(orderId, licenseKey, "License key added to order: ");
            } else {
                LOG.error("Missing data in the webhook payload for the event license_key_created.");
            }
        } catch (Exception e) {
            LOG.error("Error while processing license_key_created event", e);
        }
    }

    private void handleLicenseKeyUpdated(JsonObject eventData) {
        try {
            JsonObject dataObject = eventData.getAsJsonObject("data");
            if (dataObject != null && dataObject.has("attributes")) {
                JsonObject attributes = dataObject.getAsJsonObject("attributes");
                String orderId = attributes.get("order_id").getAsString();
                String licenseKey = attributes.get("key").getAsString();
                updateLicenseKeyInOrder(orderId, licenseKey, "License key updated for order: ");
            } else {
                LOG.error("Missing data in the webhook payload for the event license_key_updated.");
            }
        } catch (Exception e) {
            LOG.error("Error while processing license_key_updated event", e);
        }
    }

    private String readRequestBody(HttpRequest request) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        return reader.lines().collect(Collectors.joining());
    }
}
