package com.example.poc;

import com.example.poc.entity.A;
import com.example.poc.entity.AB;
import com.example.poc.entity.B;
import com.example.poc.repository.ARepository;
import com.example.poc.repository.ABRepository;
import com.example.poc.repository.BRepository;
import com.example.poc.spec.ASpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ASpecificationTest {

    @Autowired
    private ARepository aRepository;

    @Autowired
    private BRepository bRepository;

    @Autowired
    private ABRepository abRepository;

    @BeforeEach
    void setUp() {
        abRepository.deleteAll();
        aRepository.deleteAll();
        bRepository.deleteAll();

        // Seed B entities
        B b1 = bRepository.save(new B("red"));
        B b2 = bRepository.save(new B("blue"));

        // Seed A entities
        A a1 = aRepository.save(new A("alpha"));
        A a2 = aRepository.save(new A("beta"));
        A a3 = aRepository.save(new A("gamma"));

        // Seed AB bridge entities
        // A1("alpha") --[priority=1]--> B1("red")
        abRepository.save(new AB(a1, b1, 1));
        // A1("alpha") --[priority=5]--> B2("blue")
        abRepository.save(new AB(a1, b2, 5));
        // A2("beta")  --[priority=3]--> B1("red")
        abRepository.save(new AB(a2, b1, 3));
        // A3("gamma") — no associations
    }

    @Test
    void testChainedJoinFilterByBLabel() {
        List<A> result = aRepository.findAll(ASpecifications.hasBWithLabel("red"));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(A::getName).containsExactlyInAnyOrder("alpha", "beta");
    }

    @Test
    void testChainedJoinNoMatch() {
        List<A> result = aRepository.findAll(ASpecifications.hasBWithLabel("green"));

        assertThat(result).isEmpty();
    }

    @Test
    void testLeftJoinIncludesUnassociated() {
        Specification<A> spec = ASpecifications.hasBWithLabelLeftJoin("red")
                .or((root, query, cb) -> {
                    query.distinct(true);
                    return cb.conjunction();
                });

        List<A> result = aRepository.findAll(spec);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(A::getName)
                .containsExactlyInAnyOrder("alpha", "beta", "gamma");
    }

    @Test
    void testFilterByBridgePriority() {
        List<A> result = aRepository.findAll(ASpecifications.hasAbWithPriorityGreaterThan(2));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(A::getName).containsExactlyInAnyOrder("alpha", "beta");
    }

    @Test
    void testCompoundPredicate() {
        List<A> result = aRepository.findAll(
                ASpecifications.hasBWithLabelAndMinPriority("red", 2));

        assertThat(result).hasSize(1);
        assertThat(result).extracting(A::getName).containsExactly("beta");
    }

    @Test
    void testWithPageable() {
        Page<A> page = aRepository.findAll(
                ASpecifications.hasBWithLabel("red"),
                PageRequest.of(0, 1, Sort.by("name")));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("alpha");
    }

    @Test
    void testSpecComposition() {
        Specification<A> spec = ASpecifications.hasBWithLabel("red")
                .and(ASpecifications.hasAbWithPriorityGreaterThan(2));

        List<A> result = aRepository.findAll(spec);

        assertThat(result).hasSize(1);
        assertThat(result).extracting(A::getName).containsExactly("beta");
    }
}
