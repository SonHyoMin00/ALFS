package com.world.alfs.domain.special.repository;

import com.world.alfs.domain.special.Special;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SpecialRepository extends JpaRepository<Special, Long> {

        List<Special> findByStart(LocalDateTime time);

        List<Special> findByEnd(LocalDateTime time);

        @Query("SELECT s.status FROM Special s WHERE s.product.id =:id")
        int findByStatus(Long id);
}
