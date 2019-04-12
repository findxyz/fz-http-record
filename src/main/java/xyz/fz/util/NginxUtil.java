package xyz.fz.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class NginxUtil {

    private static Logger LOGGER = LoggerFactory.getLogger(NginxUtil.class);

    private static final File HOME_DIRECTORY = new File("");

    private static final String NGINX_DIRECTORY = HOME_DIRECTORY.getAbsolutePath() + "/nginx-1.14.2";

    private static final String NGINX_EXE = NGINX_DIRECTORY + "/nginx.exe";

    private static final String NGINX_PARAM_CONF = "conf/httprecord-nginx.conf";

    public synchronized static void start() {
        ProcessUtil.startAsync(NGINX_DIRECTORY, new String[]{NGINX_EXE, "-c", NGINX_PARAM_CONF});
        LOGGER.info("nginx startup @ 80");
        LOGGER.info("服务启动完毕，请打开 http://localhost 进行访问");
    }

    public synchronized static void stop() {
        ProcessUtil.ProcessResult processResult = ProcessUtil.exists("nginx.exe");
        if (StringUtils.isNotBlank(processResult.getStd())) {
            ProcessUtil.startSync(NGINX_DIRECTORY, new String[]{NGINX_EXE, "-s", "stop"});
        }
    }
}
