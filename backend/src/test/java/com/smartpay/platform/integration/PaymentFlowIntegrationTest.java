package com.smartpay.platform.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PaymentFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void addMoneyThenSendMoneyUpdatesBothWalletsAndHistory() throws Exception {
        String bobEmail = uniqueEmail("bob");
        String alice = registerAndLogin("Alice", uniqueEmail("alice"));
        String bob = registerAndLogin("Bob", bobEmail);

        addMoney(alice, "1000.00", key()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.transaction.status").value("SUCCESS"));
        authGet(alice, "/api/wallet").andExpect(jsonPath("$.data.balance").value(1000.0));

        pay(alice, bobEmail, "250.00", key())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.transaction.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.transaction.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.data.transaction.transactionReference").exists())
                .andExpect(jsonPath("$.data.idempotentReplay").value(false));

        authGet(alice, "/api/wallet").andExpect(jsonPath("$.data.balance").value(750.0));
        authGet(bob, "/api/wallet").andExpect(jsonPath("$.data.balance").value(250.0));

        authGet(alice, "/api/transactions").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
        authGet(bob, "/api/payments/history").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].direction").value("CREDIT"));
        authGet(alice, "/api/transactions/status/SUCCESS").andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void sameIdempotencyKeyDoesNotChargeTwice() throws Exception {
        String bobEmail = uniqueEmail("bob");
        String alice = registerAndLogin("Alice", uniqueEmail("alice"));
        registerAndLogin("Bob", bobEmail);
        addMoney(alice, "1000.00", key()).andExpect(status().isCreated());
        String idemKey = key();

        String first = pay(alice, bobEmail, "300.00", idemKey).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String second = pay(alice, bobEmail, "300.00", idemKey).andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.data.idempotentReplay").value(true))
                .andReturn().getResponse().getContentAsString();

        Assertions.assertThat((Integer) JsonPath.read(second, "$.data.transaction.id"))
                .isEqualTo((Integer) JsonPath.read(first, "$.data.transaction.id"));
        authGet(alice, "/api/wallet").andExpect(jsonPath("$.data.balance").value(700.0));
        authGet(alice, "/api/transactions").andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void sameKeyWithDifferentAmountIsConflict() throws Exception {
        String bobEmail = uniqueEmail("bob");
        String alice = registerAndLogin("Alice", uniqueEmail("alice"));
        registerAndLogin("Bob", bobEmail);
        addMoney(alice, "1000.00", key()).andExpect(status().isCreated());
        String idemKey = key();

        pay(alice, bobEmail, "100.00", idemKey).andExpect(status().isCreated());
        pay(alice, bobEmail, "200.00", idemKey).andExpect(status().isConflict());
        authGet(alice, "/api/wallet").andExpect(jsonPath("$.data.balance").value(900.0));
    }

    @Test
    void repeatedAddMoneyKeyCreditsOnlyOnce() throws Exception {
        String alice = registerAndLogin("Alice", uniqueEmail("alice"));
        String idemKey = key();

        addMoney(alice, "500.00", idemKey).andExpect(status().isCreated());
        addMoney(alice, "500.00", idemKey).andExpect(status().isOk());

        authGet(alice, "/api/wallet").andExpect(jsonPath("$.data.balance").value(500.0));
    }

    @Test
    void insufficientBalanceIsRecordedAsFailedTransaction() throws Exception {
        String bobEmail = uniqueEmail("bob");
        String alice = registerAndLogin("Alice", uniqueEmail("alice"));
        registerAndLogin("Bob", bobEmail);
        addMoney(alice, "100.00", key()).andExpect(status().isCreated());

        String body = pay(alice, bobEmail, "500.00", key())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Insufficient wallet balance"))
                .andExpect(jsonPath("$.transactionReference").exists())
                .andReturn().getResponse().getContentAsString();

        authGet(alice, "/api/wallet").andExpect(jsonPath("$.data.balance").value(100.0));
        authGet(alice, "/api/transactions/status/FAILED").andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].failureReason").value("Insufficient wallet balance"));
        String reference = JsonPath.read(body, "$.transactionReference");
        authGet(alice, "/api/payments/reference/" + reference).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transaction.status").value("FAILED"))
                .andExpect(jsonPath("$.data.events[?(@.eventType=='PAYMENT_FAILED')]").exists());
    }

    @Test
    void unknownReceiverReturns404() throws Exception {
        String alice = registerAndLogin("Alice", uniqueEmail("alice"));

        pay(alice, "nobody-" + UUID.randomUUID() + "@example.com", "10.00", key())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Receiver not found"));
    }

    @Test
    void selfTransferIsBlockedByRiskRules() throws Exception {
        String email = uniqueEmail("alice");
        String alice = registerAndLogin("Alice", email);
        addMoney(alice, "100.00", key()).andExpect(status().isCreated());

        pay(alice, email, "10.00", key())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("same user")));
        authGet(alice, "/api/wallet").andExpect(jsonPath("$.data.balance").value(100.0));
    }

    @Test
    void amountAboveConfiguredLimitIsBlocked() throws Exception {
        String bobEmail = uniqueEmail("bob");
        String alice = registerAndLogin("Alice", uniqueEmail("alice"));
        registerAndLogin("Bob", bobEmail);
        addMoney(alice, "100000.00", key()).andExpect(status().isCreated());

        pay(alice, bobEmail, "60000.00", key())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("per-transaction limit")));
        authGet(alice, "/api/wallet").andExpect(jsonPath("$.data.balance").value(100000.0));
    }

    @Test
    void invalidPaymentBodyAndMissingHeaderAreRejected() throws Exception {
        String alice = registerAndLogin("Alice", uniqueEmail("alice"));

        mvc.perform(post("/api/payments").header("Authorization", "Bearer " + alice)
                        .header("Idempotency-Key", key()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverEmail\":\"bad\",\"amount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.receiverEmail").exists())
                .andExpect(jsonPath("$.errors.amount").exists());

        mvc.perform(post("/api/payments").header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverEmail\":\"a@b.com\",\"amount\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Idempotency-Key")));
    }

    @Test
    void usersCannotReadEachOthersTransactions() throws Exception {
        String alice = registerAndLogin("Alice", uniqueEmail("alice"));
        String mallory = registerAndLogin("Mallory", uniqueEmail("mallory"));
        String body = addMoney(alice, "100.00", key()).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer txId = JsonPath.read(body, "$.data.transaction.id");

        authGet(alice, "/api/transactions/" + txId).andExpect(status().isOk());
        authGet(mallory, "/api/transactions/" + txId).andExpect(status().isForbidden());
    }

    @Test
    void analyticsReflectTransactions() throws Exception {
        String bobEmail = uniqueEmail("bob");
        String alice = registerAndLogin("Alice", uniqueEmail("alice"));
        registerAndLogin("Bob", bobEmail);
        addMoney(alice, "1000.00", key()).andExpect(status().isCreated());
        pay(alice, bobEmail, "100.00", key()).andExpect(status().isCreated());
        pay(alice, bobEmail, "5000.00", key()).andExpect(status().isUnprocessableEntity());

        authGet(alice, "/api/analytics/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTransactions").value(3))
                .andExpect(jsonPath("$.data.successfulTransactions").value(2))
                .andExpect(jsonPath("$.data.failedTransactions").value(1))
                .andExpect(jsonPath("$.data.successRate").value(66.67))
                .andExpect(jsonPath("$.data.transactionsByDate.length()").value(7));
    }
}
