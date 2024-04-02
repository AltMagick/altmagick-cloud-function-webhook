package com.generaltor.cloudfunction.webhook.googlecloudfunctions;

import com.generaltor.cloudfunction.webhook.entities.License;
import com.generaltor.cloudfunction.webhook.entities.Sub;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.Timestamp;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    private Sub extractOrderAttributesAndCreateOrder(JsonObject attributes) {
        boolean pause = attributes.has("pause") && !attributes.get("pause").isJsonNull() && attributes.get("pause").getAsBoolean();
        String status = attributes.has("status") && !attributes.get("status").isJsonNull() ? attributes.get("status").getAsString() : "";
        String endAt = attributes.has("end_at") && !attributes.get("end_at").isJsonNull() ? attributes.get("end_at").getAsString() : "";
        boolean cancelled = attributes.has("cancelled") && !attributes.get("cancelled").isJsonNull() && attributes.get("cancelled").getAsBoolean();
        String renewsAt = attributes.has("renews_at") && !attributes.get("renews_at").isJsonNull() ? attributes.get("renews_at").getAsString() : "";
        String userName = attributes.has("user_name") && !attributes.get("user_name").isJsonNull() ? attributes.get("user_name").getAsString() : "";
        String createdAt = attributes.has("created_at") && !attributes.get("created_at").isJsonNull() ? attributes.get("created_at").getAsString() : "";
        String updatedAt = attributes.has("updated_at") && !attributes.get("updated_at").isJsonNull() ? attributes.get("updated_at").getAsString() : "";
        String userEmail = attributes.has("user_email") && !attributes.get("user_email").isJsonNull() ? attributes.get("user_email").getAsString() : "";

        Instant createdAtInstant = !Objects.equals(createdAt, "") ? Instant.parse(createdAt) : null;
        Instant renewsAtInstant = !Objects.equals(renewsAt, "") ? Instant.parse(renewsAt) : null;
        Instant updatedAtInstant = !Objects.equals(updatedAt, "") ? Instant.parse(updatedAt) : null;
        Instant endAtInstant = !Objects.equals(endAt, "") ? Instant.parse(endAt) : null;

        Timestamp createdAtTimestamp = createdAtInstant != null ? Timestamp.ofTimeSecondsAndNanos(createdAtInstant.getEpochSecond(), createdAtInstant.getNano()) : null;
        Timestamp renewsAtTimestamp = renewsAtInstant != null ? Timestamp.ofTimeSecondsAndNanos(renewsAtInstant.getEpochSecond(), renewsAtInstant.getNano()) : null;
        Timestamp updatedAtTimestamp = updatedAtInstant != null ? Timestamp.ofTimeSecondsAndNanos(updatedAtInstant.getEpochSecond(), updatedAtInstant.getNano()) : null;
        Timestamp endAtTimestamp = endAtInstant != null ? Timestamp.ofTimeSecondsAndNanos(endAtInstant.getEpochSecond(), endAtInstant.getNano()) : null;


        return new Sub(pause, status, endAtTimestamp, cancelled, renewsAtTimestamp, null, null, userName, createdAtTimestamp, updatedAtTimestamp, userEmail, 0, new License(null, false));
    }

    private void handleSubscriptionCreated(JsonObject eventData) {
        try {
            JsonObject dataObject = eventData.getAsJsonObject("data");
            if (dataObject != null && dataObject.has("attributes")) {
                JsonObject attributes = dataObject.getAsJsonObject("attributes");
                Sub sub = extractOrderAttributesAndCreateOrder(attributes);
                String subId = attributes.has("order_id") && !attributes.get("order_id").isJsonNull() ? attributes.get("order_id").getAsString() : "";
                sub.setNextReset(sub.getRenewsAt());
                sub.setLastReset(sub.getCreatedAt());
                CollectionReference subs = firestore.collection("subs");
                List<ApiFuture<WriteResult>> futures = new ArrayList<>();
                futures.add(subs.document(subId).set(sub));
                ApiFutures.allAsList(futures).get();
                LOG.info("Order created: " + subId);
            } else {
                LOG.error("Missing data in the webhook payload for the event subscription_created.");
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
                String subId = attributes.has("order_id") && !attributes.get("order_id").isJsonNull() ? attributes.get("order_id").getAsString() : "";
                Sub sub = extractOrderAttributesAndCreateOrder(attributes);
                DocumentReference orderRef = firestore.collection("subs").document(subId);
                ApiFuture<DocumentSnapshot> future = orderRef.get();

                try {
                    DocumentSnapshot document = future.get();
                    if (document.exists()) {
                        orderRef.update("pause", sub.isPause());
                        orderRef.update("status", sub.getStatus());
                        orderRef.update("endAt", sub.getEndAt());
                        orderRef.update("cancelled", sub.isCancelled());
                        orderRef.update("renewsAt", sub.getRenewsAt());
                        orderRef.update("userName", sub.getUserName());
                        orderRef.update("createdAt", sub.getCreatedAt());
                        orderRef.update("updatedAt", sub.getUpdatedAt());
                        orderRef.update("userEmail", sub.getUserEmail());
                        LOG.info("Subscription updated for order: " + subId);
                    } else {
                        LOG.error("No such document: " + subId);
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

    private void updateLicenseKeyInSub(String subId, License license, String logMessage) {
        DocumentReference orderRef = firestore.collection("subs").document(subId);
        ApiFuture<DocumentSnapshot> future = orderRef.get();
        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                orderRef.update("license", license);
                LOG.info(logMessage + subId);
            } else {
                LOG.error("Document not found: " + subId);
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
                String subId = attributes.get("order_id").getAsString();
                String licenseKey = attributes.get("key").getAsString();
                String disabled = attributes.get("disabled").getAsString();
                License license = new License(licenseKey, Boolean.parseBoolean(disabled));
                updateLicenseKeyInSub(subId, license, "License key added to order: ");
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
                String subId = attributes.get("order_id").getAsString();
                String licenseKey = attributes.get("key").getAsString();
                String disabled = attributes.get("disabled").getAsString();
                License license = new License(licenseKey, Boolean.parseBoolean(disabled));
                updateLicenseKeyInSub(subId, license, "License key updated for order: ");
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
