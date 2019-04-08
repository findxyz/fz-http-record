package xyz.fz.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.fz.entity.Record;

public interface RecordRepository extends JpaRepository<Record, Long> {
}
