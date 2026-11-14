package ru.practicum.yakovlev.mymarketapp.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.exception.PaymentException;
import ru.practicum.yakovlev.mymarketapp.exception.PaymentException.Reason;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.OrderRepository;
import ru.practicum.yakovlev.mymarketapp.service.CartService;
import ru.practicum.yakovlev.mymarketapp.service.OrderService;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.TEST_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class CheckoutPaymentIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private ItemRepository items;
    @Autowired
    private CartService cart;
    @Autowired
    private CartItemRepository cartItems;
    @Autowired
    private OrderRepository orders;
    @Autowired
    private OrderService checkout;

    @Test
    void successfulCheckoutPaysCalculatedServerSideAmount() {
        long id = items.save(item("Coffee", "12.50")).block().getId();
        cart.updateItem(TEST_USER_ID, id, CartAction.PLUS).then(cart.updateItem(TEST_USER_ID, id, CartAction.PLUS)).block();
        checkout.createOrder(TEST_USER_ID).block();
        verify(paymentGateway).pay(eq(TEST_USER_ID), argThat(amount -> amount.compareTo(new BigDecimal("25.00")) == 0));
        assertThat(orders.count().block()).isEqualTo(1);
        assertThat(cartItems.countByUserId(TEST_USER_ID).block()).isZero();
    }

    @ParameterizedTest
    @EnumSource(Reason.class)
    void unsuccessfulPaymentRollsBackOrderAndPreservesCart(Reason reason) {
        long id = items.save(item("Coffee", "12.50")).block().getId();
        cart.updateItem(TEST_USER_ID, id, CartAction.PLUS).block();
        when(paymentGateway.pay(anyLong(), any())).thenReturn(Mono.error(new PaymentException(reason, null)));
        StepVerifier.create(checkout.createOrder(TEST_USER_ID)).expectError(PaymentException.class).verify();
        assertThat(orders.count().block()).isZero();
        assertThat(cartItems.findItem(TEST_USER_ID, id).block().getQuantity()).isEqualTo(1);
    }
}
