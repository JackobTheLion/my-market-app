package ru.practicum.yakovlev.mymarketapp.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.codec.multipart.FilePart;
import ru.practicum.yakovlev.mymarketapp.api.dto.ItemCreateRequest;
import ru.practicum.yakovlev.mymarketapp.service.ImageService;
import java.math.BigDecimal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.dto.PagingDto;
import ru.practicum.yakovlev.mymarketapp.exception.NotFoundException;
import ru.practicum.yakovlev.mymarketapp.mapper.ItemMapper;
import ru.practicum.yakovlev.mymarketapp.model.CartItem;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.repository.CartItemRepository;
import org.springframework.data.domain.PageRequest;
import ru.practicum.yakovlev.mymarketapp.repository.ItemRepository;
import ru.practicum.yakovlev.mymarketapp.repository.ProductCacheRepository;
import ru.practicum.yakovlev.mymarketapp.cache.CachedItem;
import ru.practicum.yakovlev.mymarketapp.cache.CachedItemPage;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {
    @Mock
    private ItemRepository items;
    @Mock
    private ProductCacheRepository cache;
    @Mock
    private CartItemRepository cart;
    @Mock private ImageService images;
    @Mock private FilePart image;

    private ItemServiceImpl service;

    @BeforeEach
    void setUp() {
        ItemMapper mapper = Mappers.getMapper(ItemMapper.class);
        ReflectionTestUtils.setField(mapper, "defaultImage", "default-image.svg");
        service = new ItemServiceImpl(items, cache, cart, mapper, images);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t\n"})
    void blankSearchReturnsEmptyCatalog(String search) {
        when(cache.findPage("", ItemSort.NO, 1, 10)).thenReturn(Mono.just(new CachedItemPage(List.of(), 0)));
        StepVerifier.create(service.getItems(search, ItemSort.NO, 1, 10))
                .assertNext(page -> {
                    assertThat(page.items()).isEmpty();
                    assertThat(page.paging()).isEqualTo(new PagingDto(10, 1, false, false));
                })
                .verifyComplete();
        verifyNoInteractions(cart, items);
    }

    @Test
    void includesCartCountsAndPagingForCachedPage() {
        Item coffee = item(1, "Coffee", "12.50");
        when(cache.findPage("Coffee", ItemSort.PRICE, 2, 10))
                .thenReturn(Mono.just(new CachedItemPage(List.of(CachedItem.from(coffee)), 25)));
        when(cart.findAllById(List.of(1L))).thenReturn(Flux.just(new CartItem(coffee, 4)));
        StepVerifier.create(service.getItems("  Coffee  ", ItemSort.PRICE, 2, 10))
                .assertNext(page -> {
                    assertThat(page.paging()).isEqualTo(new PagingDto(10, 2, true, true));
                    assertThat(page.items().getFirst().count()).isEqualTo(4);
                })
                .verifyComplete();
    }

    @Test
    void outsideCartUsesZeroCountAndDefaultImage() {
        Item coffee = item(1, "Coffee", "12.50");
        coffee.setImagePath(null);
        when(cache.findById(1L)).thenReturn(Mono.just(CachedItem.from(coffee)));
        when(cart.findById(1L)).thenReturn(Mono.empty());
        StepVerifier.create(service.getItem(1))
                .assertNext(dto -> {
                    assertThat(dto.count()).isZero();
                    assertThat(dto.imgPath()).isEqualTo("images/default-image.svg");
                })
                .verifyComplete();
    }

    @Test
    void detailsIncludeCartQuantity() {
        Item coffee = item(1, "Coffee", "12.50");
        when(cache.findById(1L)).thenReturn(Mono.just(CachedItem.from(coffee)));
        when(cart.findById(1L)).thenReturn(Mono.just(new CartItem(coffee, 3)));
        StepVerifier.create(service.getItem(1))
                .assertNext(dto -> assertThat(dto.count()).isEqualTo(3))
                .verifyComplete();
    }

    @Test
    void missingItemEmitsNotFound() {
        when(cache.findById(99L)).thenReturn(Mono.empty());
        when(items.findById(99L)).thenReturn(Mono.empty());
        StepVerifier.create(service.getItem(99))
                .expectError(NotFoundException.class)
                .verify();
        verifyNoInteractions(cart);
    }
    @Test
    void detailMissLoadsDatabaseAndPopulatesCache() {
        Item coffee = item(1, "Coffee", "12.50");
        when(cache.findById(1L)).thenReturn(Mono.empty());
        when(items.findById(1L)).thenReturn(Mono.just(coffee));
        when(cache.save(CachedItem.from(coffee))).thenReturn(Mono.empty());
        when(cart.findById(1L)).thenReturn(Mono.empty());
        StepVerifier.create(service.getItem(1)).expectNextMatches(dto -> dto.title().equals("Coffee")).verifyComplete();
        verify(cache).save(CachedItem.from(coffee));
    }

    @Test
    void cacheReadAndWriteFailuresStillReturnDatabaseProduct() {
        Item coffee = item(1, "Coffee", "12.50");
        when(cache.findById(1L)).thenReturn(Mono.error(new IllegalStateException("Redis unavailable")));
        when(items.findById(1L)).thenReturn(Mono.just(coffee));
        when(cache.save(any())).thenReturn(Mono.error(new IllegalStateException("Redis unavailable")));
        when(cart.findById(1L)).thenReturn(Mono.empty());
        StepVerifier.create(service.getItem(1)).expectNextMatches(dto -> dto.title().equals("Coffee")).verifyComplete();
    }

    @Test
    void databaseFailureIsNotSwallowedByCacheFallback() {
        when(cache.findById(1L)).thenReturn(Mono.empty());
        when(items.findById(1L)).thenReturn(Mono.error(new IllegalStateException("Database unavailable")));
        StepVerifier.create(service.getItem(1)).expectErrorMessage("Database unavailable").verify();
        verify(cache, never()).save(any());
    }

    @Test
    void pageMissNormalizesSearchLoadsDatabaseAndPopulatesCache() {
        Item coffee = item(1, "Coffee", "12.50");
        CachedItemPage expected = new CachedItemPage(List.of(CachedItem.from(coffee)), 12);
        when(cache.findPage("Coffee", ItemSort.PRICE, 2, 5)).thenReturn(Mono.empty());
        when(items.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("Coffee", "Coffee",
                PageRequest.of(1, 5, ItemSort.PRICE.getSort()))).thenReturn(Flux.just(coffee));
        when(items.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("Coffee", "Coffee")).thenReturn(Mono.just(12L));
        when(cache.savePage("Coffee", ItemSort.PRICE, 2, 5, expected)).thenReturn(Mono.empty());
        when(cart.findAllById(List.of(1L))).thenReturn(Flux.empty());
        StepVerifier.create(service.getItems("  Coffee  ", ItemSort.PRICE, 2, 5)).assertNext(page -> {
            assertThat(page.items().getFirst().title()).isEqualTo("Coffee");
            assertThat(page.paging()).isEqualTo(new PagingDto(5, 2, true, true));
        }).verifyComplete();
        verify(cache).savePage("Coffee", ItemSort.PRICE, 2, 5, expected);
    }

    @Test
    void batchLoadsOnlyMissingItemsWithOneDatabaseQuery() {
        Item coffee = item(1, "Coffee", "12.50");
        Item tea = item(2, "Tea", "3.25");
        when(cache.findAllById(List.of(1L, 2L))).thenReturn(Mono.just(Map.of(1L, CachedItem.from(coffee))));
        when(items.findAllById(List.of(2L))).thenReturn(Flux.just(tea));
        when(cache.saveAll(List.of(CachedItem.from(tea)))).thenReturn(Mono.empty());
        StepVerifier.create(service.getItemsByIds(List.of(1L, 2L))).assertNext(products -> {
            assertThat(products).hasSize(2);
            assertThat(products.get(1L).getTitle()).isEqualTo("Coffee");
            assertThat(products.get(2L).getTitle()).isEqualTo("Tea");
        }).verifyComplete();
        verify(items).findAllById(List.of(2L));
        verify(items, never()).findById(anyLong());
    }

    @Test
    void cachedBatchDoesNotReadDatabase() {
        when(cache.findAllById(List.of(1L))).thenReturn(Mono.just(Map.of(1L, CachedItem.from(item(1, "Coffee", "12.50")))));
        StepVerifier.create(service.getItemsByIds(List.of(1L))).expectNextMatches(products -> products.size() == 1).verifyComplete();
        verifyNoInteractions(items, cart);
        verify(cache, never()).saveAll(anyList());
    }

    @Test
    void emptyBatchDoesNotAccessRepositories() {
        StepVerifier.create(service.getItemsByIds(List.of())).expectNext(Map.of()).verifyComplete();
        verifyNoInteractions(items, cache, cart);
    }

    @Test
    void batchCacheReadFailureSkipsWritesAndLoadsDatabase() {
        when(cache.findAllById(List.of(1L))).thenReturn(Mono.error(new IllegalStateException("Redis unavailable")));
        when(items.findAllById(List.of(1L))).thenReturn(Flux.just(item(1, "Coffee", "12.50")));
        StepVerifier.create(service.getItemsByIds(List.of(1L))).expectNextMatches(products -> products.size() == 1).verifyComplete();
        verify(cache, never()).saveAll(anyList());
    }

    @Test
    void batchCacheWriteFailureStillReturnsDatabaseProducts() {
        when(cache.findAllById(List.of(1L))).thenReturn(Mono.just(Map.of()));
        when(items.findAllById(List.of(1L))).thenReturn(Flux.just(item(1, "Coffee", "12.50")));
        when(cache.saveAll(anyList())).thenReturn(Mono.error(new IllegalStateException("Redis unavailable")));
        StepVerifier.create(service.getItemsByIds(List.of(1L))).expectNextMatches(products -> products.size() == 1).verifyComplete();
    }

    @Test
    void missingBatchProductIsNotCached() {
        when(cache.findAllById(List.of(99L))).thenReturn(Mono.just(Map.of()));
        when(items.findAllById(List.of(99L))).thenReturn(Flux.empty());
        StepVerifier.create(service.getItemsByIds(List.of(99L))).expectError(NotFoundException.class).verify();
        verify(cache, never()).saveAll(anyList());
    }

    @Test
    void createsSingleProductAndInvalidatesCatalogAfterSave() {
        when(images.saveImage(image)).thenReturn(Mono.just("uploads/generated.png"));
        when(items.save(any(Item.class))).thenAnswer(invocation -> {
            Item product = invocation.getArgument(0);
            product.setId(42L);
            return Mono.just(product);
        });
        when(cache.invalidatePages()).thenReturn(Mono.empty());
        StepVerifier.create(service.createItem(createRequest())).expectNext(42L).verifyComplete();
        org.mockito.InOrder order = inOrder(images, items, cache);
        order.verify(images).saveImage(image);
        order.verify(items).save(argThat(product -> product.getTitle().equals("Coffee")
                && product.getDescription().equals("Beans") && product.getPrice().compareTo(new BigDecimal("12.50")) == 0
                && product.getImagePath().equals("uploads/generated.png")));
        order.verify(cache).invalidatePages();
        verify(images, never()).deleteImage(any());
    }

    @Test
    void failedUploadDoesNotWriteProduct() {
        when(images.saveImage(image)).thenReturn(Mono.error(new IllegalArgumentException("Invalid image")));
        StepVerifier.create(service.createItem(createRequest())).expectErrorMessage("Invalid image").verify();
        verifyNoInteractions(items, cache);
        verify(images, never()).deleteImage(any());
    }

    @Test
    void failedInsertRemovesUploadedImage() {
        when(images.saveImage(image)).thenReturn(Mono.just("uploads/generated.png"));
        when(items.save(any(Item.class))).thenReturn(Mono.error(new IllegalStateException("Database unavailable")));
        when(images.deleteImage("uploads/generated.png")).thenReturn(Mono.empty());
        StepVerifier.create(service.createItem(createRequest())).expectErrorMessage("Database unavailable").verify();
        verify(images).deleteImage("uploads/generated.png");
        verifyNoInteractions(cache);
    }

    @Test
    void cancellingCreationRemovesUploadedImage() {
        when(images.saveImage(image)).thenReturn(Mono.just("uploads/generated.png"));
        when(items.save(any(Item.class))).thenReturn(Mono.never());
        when(images.deleteImage("uploads/generated.png")).thenReturn(Mono.empty());
        StepVerifier.create(service.createItem(createRequest()))
                .then(() -> verify(items).save(any(Item.class))).thenCancel().verify();
        verify(images).deleteImage("uploads/generated.png");
        verifyNoInteractions(cache);
    }

    @Test
    void cacheFailureDoesNotFailCreatedProduct() {
        when(images.saveImage(image)).thenReturn(Mono.just("uploads/generated.png"));
        when(items.save(any(Item.class))).thenReturn(Mono.just(item(42, "Coffee", "12.50")));
        when(cache.invalidatePages()).thenReturn(Mono.error(new IllegalStateException("Redis unavailable")));
        StepVerifier.create(service.createItem(createRequest())).expectNext(42L).verifyComplete();
        verify(images, never()).deleteImage(any());
    }

    @Test
    void cleanupFailureDoesNotHideInsertFailure() {
        when(images.saveImage(image)).thenReturn(Mono.just("uploads/generated.png"));
        when(items.save(any(Item.class))).thenReturn(Mono.error(new IllegalStateException("Insert failed")));
        when(images.deleteImage("uploads/generated.png")).thenReturn(Mono.error(new IllegalStateException("Delete failed")));
        StepVerifier.create(service.createItem(createRequest())).expectErrorMessage("Insert failed").verify();
        verifyNoInteractions(cache);
    }

    private ItemCreateRequest createRequest() {
        return new ItemCreateRequest("Coffee", "Beans", new BigDecimal("12.50"), image);
    }

}
