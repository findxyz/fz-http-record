package xyz.fz.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.fz.entity.Record;
import xyz.fz.model.RecordDTO;

public interface RecordRepository extends JpaRepository<Record, Long> {
    @Query(value = "select new xyz.fz.model.RecordDTO(concat(r.id, ''), r.method, r.url) from Record r where r.id > :id")
    Page<RecordDTO> recordQuery(@Param("id") Long id, Pageable pageable);
}
