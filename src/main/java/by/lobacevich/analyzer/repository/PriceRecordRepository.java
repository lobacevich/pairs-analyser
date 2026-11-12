package by.lobacevich.analyzer.repository;

import by.lobacevich.analyzer.entity.PriceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {

    Optional<PriceRecord> findTopByOrderByFetchedAtDesc();

    Page<PriceRecord> findByFetchedAtBetween(Instant from, Instant to, Pageable pageable);

    @Query("select avg(p.price) from PriceRecord p where p.fetchedAt between :from and :to")
    Optional<BigDecimal> findAveragePriceBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select p from PriceRecord p where p.fetchedAt between :from and :to order by p.fetchedAt asc")
    List<PriceRecord> findByFetchedAtBetweenOrderByFetchedAtAsc(@Param("from") Instant from, @Param("to") Instant to);
}
