package ru.practicum.yakovlev.mymarketapp.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import reactor.test.StepVerifier;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.support.IntegrationTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class ItemRepositoryTest extends IntegrationTestSupport {
    @Autowired
    private ItemRepository items;

    @Test
    void savesAndLoadsScalarFields() {
        Item saved = items.save(item("Coffee", "12.50"))
                .block();
        StepVerifier.create(items.findById(saved.getId()))
                .assertNext(loaded -> {
                    assertThat(loaded.getTitle()).isEqualTo("Coffee");
                    assertThat(loaded.getDescription()).isEqualTo("Description of Coffee");
                    assertThat(loaded.getPrice()).isEqualByComparingTo("12.50");
                    assertThat(loaded.getImagePath()).isEqualTo("demo/photo.jpg");
                })
                .verifyComplete();
    }

    @Test
    void searchMatchesTitleAndDescriptionWithoutCase() {
        Item tea = item("Tea", "3.25");
        tea.setDescription("Coffee flavoured tea");
        items.save(item("Coffee", "12.50"))
                .then(items.save(tea))
                .then(items.save(item("Mug", "7.00")))
                .block();
        PageRequest request = PageRequest.of(0, 10, Sort.by("title")
                .and(Sort.by("id")));
        StepVerifier.create(items.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("cOfFeE", "cOfFeE", request))
                .assertNext(item -> assertThat(item.getTitle()).isEqualTo("Coffee"))
                .assertNext(item -> assertThat(item.getTitle()).isEqualTo("Tea"))
                .verifyComplete();
        StepVerifier.create(items.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("coffee", "coffee"))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void pagingAndPriceOrderingUseDatabaseLimitAndOffset() {
        items.save(item("A", "10"))
                .then(items.save(item("B", "2")))
                .then(items.save(item("C", "5")))
                .block();
        StepVerifier.create(items.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("", "",
                        PageRequest.of(1, 1, Sort.by("price")
                                .and(Sort.by("id")))))
                .assertNext(item -> assertThat(item.getTitle()).isEqualTo("C"))
                .verifyComplete();
    }

    @Test
    void searchTreatsPercentAndUnderscoreLiterally() {
        items.save(item("100% cotton", "10"))
                .then(items.save(item("A_B", "2")))
                .then(items.save(item("AB", "5")))
                .block();
        StepVerifier.create(items.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("%", "%",
                        PageRequest.of(0, 10, Sort.by("id"))))
                .assertNext(item -> assertThat(item.getTitle()).isEqualTo("100% cotton"))
                .verifyComplete();
        StepVerifier.create(items.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("_", "_",
                        PageRequest.of(0, 10, Sort.by("id"))))
                .assertNext(item -> assertThat(item.getTitle()).isEqualTo("A_B"))
                .verifyComplete();
    }
}
