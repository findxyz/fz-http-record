package xyz.fz.runner;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class H2InitRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(H2InitRunner.class);

    private JdbcTemplate jdbcTemplate;

    public H2InitRunner(ApplicationContext context) {
        this.jdbcTemplate = context.getBean("jdbcTemplate", JdbcTemplate.class);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            String testSql = "SELECT * FROM t_test ";
            jdbcTemplate.execute(testSql);
            // already init
            LOGGER.debug("Db already init");
        } catch (BadSqlGrammarException badSqlGrammarException) {
            LOGGER.debug("Do db init");
            // do init
            try {
                dbInit();
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("数据库初始化异常：{}", e.getMessage());
            }
        }
    }

    private void dbInit() {
        String basePath = "/database/schemas/";
        String[] tables = new String[]{
                "table_record.sql",
                "table_test.sql"
        };
        initTables(basePath, tables);
        LOGGER.info("数据库初始化完成");
    }

    private void initTables(String basePath, String[] tables) {
        for (String table : tables) {
            try (InputStream inputStream = H2InitRunner.class.getResourceAsStream(basePath + table)) {
                jdbcTemplate.execute(IOUtils.toString(inputStream, "utf-8"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
