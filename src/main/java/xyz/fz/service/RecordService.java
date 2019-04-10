package xyz.fz.service;

import xyz.fz.entity.Record;
import xyz.fz.model.RecordDTO;

import java.util.List;

public interface RecordService {
    void save(Record record);

    void updateResponse(long id, String response);

    List<RecordDTO> recordQuery(Long id);

    Record recordDetail(Long id);
}
