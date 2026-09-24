package ru.practicum.yakovlev.mymarketapp.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import ru.practicum.yakovlev.mymarketapp.api.enums.ItemSort;
import ru.practicum.yakovlev.mymarketapp.model.Item;
import ru.practicum.yakovlev.mymarketapp.support.JpaTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.practicum.yakovlev.mymarketapp.support.TestFixtures.item;

class ItemRepositoryTest extends JpaTestSupport {
    @Autowired
    private ItemRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void seed() {
        entityManager.persist(item("Coffee", "12.50"));
        Item tea = item("Tea", "3.25");
        tea.setDescription("Coffee flavoured tea");
        entityManager.persist(tea);
        entityManager.persist(item("Mug", "7.00"));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void searchMatchesTitleOrDescriptionIgnoringCaseWithoutDuplicates() {
        Page<Item> page = repository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                "cOfFeE", "cOfFeE", PageRequest.of(0, 10, ItemSort.ALPHA.getSort()));
        assertThat(page.getContent()).extracting(Item::getTitle).containsExactly("Coffee", "Tea");
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void priceSortAndPaginationUseDatabase() {
        Page<Item> page = repository.findAll(PageRequest.of(0, 2, ItemSort.PRICE.getSort()));
        assertThat(page.getContent()).extracting(Item::getTitle).containsExactly("Tea", "Mug");
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
        Page<Item> last = repository.findAll(PageRequest.of(1, 2, ItemSort.PRICE.getSort()));
        assertThat(last.getContent()).extracting(Item::getTitle).containsExactly("Coffee");
        assertThat(last.hasPrevious()).isTrue();
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    void unknownSearchReturnsEmptyPage() {
        Page<Item> page = repository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                "missing", "missing", PageRequest.of(0, 5));
        assertThat(page).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void persistsExactDecimalPrice() {
        Item saved = entityManager.persistAndFlush(item("Cable", "499.90"));
        entityManager.clear();
        assertThat(repository.findById(saved.getId()).orElseThrow().getPrice()).isEqualByComparingTo("499.90");
    }

    @Test
    void databaseRejectsMissingTitle() {
        assertThatThrownBy(() -> repository.saveAndFlush(item(null, "1.00")))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
