import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import xyz.fz.Application;
import xyz.fz.entity.Record;
import xyz.fz.repository.RecordRepository;

import javax.annotation.Resource;
import javax.transaction.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {Application.class})
public class ATest {
    @Resource
    private RecordRepository recordRepository;

    @Test
    // @Transactional 会使测试数据自动回滚
    @Transactional
    // @Rollback 可以设置是否自动回滚
    @Rollback(false)
    public void jpaAnnotationTest() {
        Record record = new Record();
        record.setId(1L);
        record.setMethod("post");
        record.setUrl("http://localhost/api/test");
        record = recordRepository.save(record);
        System.out.println(recordRepository.recordQuery(0L, PageRequest.of(0, 10)).getContent());
        recordRepository.delete(record);
    }
}
