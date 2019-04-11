package xyz.fz.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import xyz.fz.entity.Record;
import xyz.fz.model.RecordDTO;
import xyz.fz.record.server.RecordServer;
import xyz.fz.repository.RecordRepository;
import xyz.fz.service.RecordService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RecordServiceImpl implements RecordService {

    @Resource
    private RecordRepository recordRepository;

    @Override
    public void save(Record record) {
        recordRepository.save(record);
    }

    @Override
    public void updateResponse(long id, String response) {
        Optional<Record> fRecord = recordRepository.findById(id);
        if (fRecord.isPresent()) {
            Record record = fRecord.get();
            record.setResponse(response);
            recordRepository.save(record);
        }
    }

    @Override
    public List<RecordDTO> recordQuery(Long id) {
        if (id == 0) {
            id = RecordServer.getStartTime();
        }
        Page<RecordDTO> recordPage = recordRepository.recordQuery(id, PageRequest.of(0, 10));
        if (recordPage.hasContent()) {
            return recordPage.getContent();
        }
        return new ArrayList<>();
    }

    @Override
    public Record recordDetail(Long id) {
        return recordRepository.findById(id).orElseThrow(() -> new RuntimeException("记录不存在"));
    }
}
