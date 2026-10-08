package ru.practicum.yakovlev.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.practicum.yakovlev.mymarketapp.api.enums.CartAction;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.dto.ItemPageDto;
import ru.practicum.yakovlev.mymarketapp.dto.PagingDto;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.support.WebFluxTestSupport;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.itemDto;

class ItemsControllerTest extends WebFluxTestSupport {
    @Test
    void rendersCatalog() {
        when(itemService.getItems(null, ItemSort.NO, 1, 20)).thenReturn(Mono.just(new ItemPageDto(List.of(itemDto(1, 2)), new PagingDto(20, 1, false, false))));
        client.get()
                .uri("/items?pageSize=20")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Coffee", "/items/1", "images/demo/coffee.jpg"));
    }

    @Test
    void forwardsSearchSortingAndPaging() {
        when(itemService.getItems("coffee", ItemSort.PRICE, 2, 20)).thenReturn(Mono.just(new ItemPageDto(List.of(), new PagingDto(20, 2, true, false))));
        client.get()
                .uri("/items?search=coffee&sort=PRICE&pageNumber=2&pageSize=20")
                .exchange()
                .expectStatus()
                .isOk();
        verify(itemService)
                .getItems("coffee", ItemSort.PRICE, 2, 20);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"tea & coffee", "C++/Java?", "чай #1", "100% {search}"})
    void renderedCartButtonsSubmitQueryParametersAndPreserveCatalogState(String search) {
        when(itemService.getItems(search, ItemSort.PRICE, 2, 20)).thenReturn(Mono.just(new ItemPageDto(List.of(itemDto(1, 0)), new PagingDto(20, 2, true, false))));
        when(cartService.updateItem(1, CartAction.MINUS)).thenReturn(Mono.empty());
        when(cartService.updateItem(1, CartAction.PLUS)).thenReturn(Mono.empty());

        String catalogUrl = "/items?sort=PRICE&pageNumber=2&pageSize=20";
        WebTestClient.RequestHeadersUriSpec<?> request = client.get();
        WebTestClient.RequestHeadersSpec<?> response = search == null ? request.uri(catalogUrl)
                : request.uri(catalogUrl + "&search={search}", search);
        String html = response.exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        List<URI> buttonUrls = Pattern.compile("formaction=\"([^\"]+)\"")
                .matcher(html)
                .results()
                .map(match -> URI.create(HtmlUtils.htmlUnescape(match.group(1))))
                .toList();
        assertThat(buttonUrls).hasSize(3);

        for (URI buttonUrl : buttonUrls) {
            client.post()
                    .uri(buttonUrl)
                    .exchange()
                    .expectStatus()
                    .isSeeOther()
                    .expectHeader()
                    .value("Location", location -> {
                        MultiValueMap<String, String> query = UriComponentsBuilder.fromUriString(location)
                                .build()
                                .getQueryParams();
                        assertThat(query.getFirst("sort")).isEqualTo("PRICE");
                        assertThat(query.getFirst("pageNumber")).isEqualTo("2");
                        assertThat(query.getFirst("pageSize")).isEqualTo("20");
                        String encodedSearch = query.getFirst("search");
                        String decodedSearch = encodedSearch == null ? ""
                                : org.springframework.web.util.UriUtils.decode(encodedSearch, java.nio.charset.StandardCharsets.UTF_8);
                        assertThat(decodedSearch).isEqualTo(search == null ? "" : search);
                    });
        }
        verify(cartService)
                .updateItem(1, CartAction.MINUS);
        verify(cartService, times(2))
                .updateItem(1, CartAction.PLUS);
    }

    @Test
    void postUsesQueryParametersWhenBodyContainsOtherValues() {
        when(cartService.updateItem(1, CartAction.PLUS)).thenReturn(Mono.empty());
        client.post()
                .uri("/items?id=1&action=PLUS&pageSize=15")
                .body(BodyInserters.fromFormData("id", "99")
                        .with("action", "MINUS")
                        .with("pageSize", "20"))
                .exchange()
                .expectStatus()
                .isSeeOther()
                .expectHeader()
                .location("/items?sort=NO&pageNumber=1&pageSize=15");
        verify(cartService)
                .updateItem(1, CartAction.PLUS);
    }

    @Test
    void formBodyDoesNotReplaceRequiredQueryParameters() {
        client.post()
                .uri("/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(cartService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"id=1", "action=PLUS", "id=1&action=", "id=1&action=UNKNOWN",
            "id=&action=PLUS", "id=0&action=PLUS", "id=-1&action=PLUS", "id=abc&action=PLUS"})
    void missingOrInvalidRequiredQueryParametersReturn400(String query) {
        client.post()
                .uri("/items?" + query)
                .exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(cartService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"UNKNOWN"})
    void invalidOrMissingDetailActionReturns400BeforeMutation(String action) {
        WebTestClient.RequestBodyUriSpec request = client.post();
        WebTestClient.RequestHeadersSpec<?> response = action == null ? request.uri("/items/1") : request.uri("/items/1?action={action}", action);
        response.exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(cartService, itemService);
    }

    @Test
    void detailFormBodyDoesNotReplaceRequiredQueryAction() {
        client.post()
                .uri("/items/1")
                .body(BodyInserters.fromFormData("action", "PLUS"))
                .exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(cartService, itemService);
    }

    @ParameterizedTest
    @EnumSource(CartAction.class)
    void successfulDetailUpdateRedirectsToItem(CartAction action) {
        when(cartService.updateItem(42, action)).thenReturn(Mono.empty());

        client.post()
                .uri("/items/42?action={action}", action)
                .exchange()
                .expectStatus()
                .isSeeOther()
                .expectHeader()
                .location("/items/42");

        verify(cartService).updateItem(42, action);
        verifyNoMoreInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @ParameterizedTest
    @CsvSource({
            "sort, /items?sort=NO&pageNumber=2&pageSize=20",
            "pageNumber, /items?sort=PRICE&pageNumber=1&pageSize=20"
    })
    void emptyQueryParameterUsesDefaultAndPreservesOtherParameters(String field, String location) {
        when(cartService.updateItem(1, CartAction.PLUS)).thenReturn(Mono.empty());
        LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("id", "1");
        parameters.add("action", "PLUS");
        parameters.add("sort", "PRICE");
        parameters.add("pageNumber", "2");
        parameters.add("pageSize", "20");
        parameters.set(field, "");
        client.post()
                .uri(builder -> builder.path("/items")
                        .queryParams(parameters)
                        .build())
                .exchange()
                .expectStatus()
                .isSeeOther()
                .expectHeader()
                .value("Location", actual -> assertRedirect(actual, location));
        verify(cartService)
                .updateItem(1, CartAction.PLUS);
    }

    @Test
    void emptyQueryParametersUseDefaults() {
        when(cartService.updateItem(1, CartAction.PLUS)).thenReturn(Mono.empty());
        client.post()
                .uri("/items?id=1&action=PLUS&sort=&pageNumber=&pageSize=20")
                .exchange()
                .expectStatus()
                .isSeeOther()
                .expectHeader()
                .value("Location", location -> assertRedirect(location,
                        "/items?sort=NO&pageNumber=1&pageSize=20"));
        verify(cartService)
                .updateItem(1, CartAction.PLUS);
    }

    @Test
    void missingQueryParametersUseDefaults() {
        when(cartService.updateItem(1, CartAction.PLUS)).thenReturn(Mono.empty());
        client.post()
                .uri("/items?id=1&action=PLUS&pageSize=20")
                .exchange()
                .expectStatus()
                .isSeeOther()
                .expectHeader()
                .value("Location", location -> assertRedirect(location,
                        "/items?sort=NO&pageNumber=1&pageSize=20"));
        verify(cartService)
                .updateItem(1, CartAction.PLUS);
    }

    @Test
    void missingItemReturns404() {
        when(itemService.getItem(99)).thenReturn(Mono.error(new NotFoundException("missing")));
        client.get()
                .uri("/items/99")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void failedCartUpdateDoesNotRedirect() {
        when(cartService.updateItem(99, CartAction.PLUS)).thenReturn(Mono.error(new NotFoundException("missing")));
        client.post()
                .uri("/items?id=99&action=PLUS")
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectHeader()
                .doesNotExist("Location");
        verifyNoInteractions(itemService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pageNumber=0", "pageSize=0", "pageSize=101", "sort=INVALID", "pageNumber=abc"})
    void invalidQueryReturns400(String query) {
        client.get()
                .uri("/items?" + query)
                .exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(itemService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void invalidDetailIdReturns400(String id) {
        client.get()
                .uri("/items/" + id)
                .exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(itemService);
    }

    @ParameterizedTest
    @CsvSource({"sort, INVALID", "pageNumber, abc", "pageNumber, 0", "pageNumber, -1",
            "pageSize, abc", "pageSize, 0", "pageSize, -1", "pageSize, 101"})
    void invalidPostQueryValuesReturn400BeforeMutation(String field, String value) {
        client.post()
                .uri("/items?id=1&action=PLUS&{field}={value}", field, value)
                .exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(cartService);
    }

    private static void assertRedirect(String location, String expectedLocation) {
        UriComponents actual = UriComponentsBuilder.fromUriString(location)
                .build();
        UriComponents expected = UriComponentsBuilder.fromUriString(expectedLocation)
                .build();
        assertThat(actual.getPath()).isEqualTo(expected.getPath());
        assertThat(actual.getQueryParams()).isEqualTo(expected.getQueryParams());
    }
}
