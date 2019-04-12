package xyz.fz.util;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;

public class ProcessUtil {

    private static Logger LOGGER = LoggerFactory.getLogger(ProcessUtil.class);

    private static final String DEFAULT_PROCESS_PATH = new File("").getAbsolutePath();

    public static void startAsync(String directory, String[] commands) {
        ThreadUtil.executorService().execute(() -> start0(directory, commands));
    }

    public static ProcessResult startSync(String directory, String[] commands) {
        return start0(directory, commands);
    }

    private static ProcessResult start0(String directory, String[] commands) {
        try {
            LOGGER.debug("commands start: {}", Arrays.asList(commands).toString());
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.directory(new File(directory));
            Process process = processBuilder.start();
            String std = IOUtils.toString(process.getInputStream(), Charset.forName("gbk"));
            String err = IOUtils.toString(process.getErrorStream(), Charset.forName("gbk"));
            LOGGER.debug("stdout: {}", std);
            LOGGER.debug("stderr: {}", err);
            return new ProcessResult(std, err);
        } catch (Exception e) {
            LOGGER.error("process start err: {}", BaseUtil.getExceptionStackTrace(e));
            return null;
        } finally {
            LOGGER.debug("commands end: {}", Arrays.asList(commands).toString());
        }
    }

    public static ProcessResult exists(String processName) {
        return startSync(DEFAULT_PROCESS_PATH, new String[]{"cmd", "/C", "tasklist", "|", "findstr", processName});
    }

    public static class ProcessResult {
        private String std;

        private String err;

        ProcessResult(String std, String err) {
            this.std = std;
            this.err = err;
        }

        public String getStd() {
            return std;
        }

        public void setStd(String std) {
            this.std = std;
        }

        public String getErr() {
            return err;
        }

        public void setErr(String err) {
            this.err = err;
        }

        @Override
        public String toString() {
            return "ProcessResult{" +
                    "std='" + std + '\'' +
                    ", err='" + err + '\'' +
                    '}';
        }
    }

    public static void main(String[] args) {
        File dir = new File("");
        String directory = dir.getAbsolutePath() + "/nginx-1.14.2/";
        startAsync(directory, new String[]{directory + "nginx.exe", "-s", "stop"});
        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startAsync(directory, new String[]{directory + "nginx.exe", "-c", "conf/helper-nginx.conf"});
        try {
            int blockNo = System.in.read();
            System.out.println(blockNo);
            startAsync(directory, new String[]{directory + "nginx.exe", "-s", "stop"});
            Thread.sleep(3000L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
