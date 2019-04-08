package xyz.fz.service.impl;

import org.springframework.stereotype.Service;
import xyz.fz.repository.RecordRepository;
import xyz.fz.service.RecordService;

import javax.annotation.Resource;

@Service
public class RecordServiceImpl implements RecordService {

    @Resource
    private RecordRepository recordRepository;
}
