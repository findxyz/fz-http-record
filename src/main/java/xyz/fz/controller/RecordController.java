package xyz.fz.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.fz.entity.Record;
import xyz.fz.model.Result;
import xyz.fz.record.intercept.RecordIntercept;
import xyz.fz.record.server.RecordServer;
import xyz.fz.service.RecordService;
import xyz.fz.util.BaseUtil;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/record")
public class RecordController {

    @Resource
    private ConfigurableApplicationContext applicationContext;

    @Resource
    private RecordService recordService;

    @RequestMapping("/proxyStart")
    public Result proxyStart(@RequestBody Map map) {
        List<RecordIntercept> list = new ArrayList<>();
        Map<String, RecordIntercept> interceptMap = applicationContext.getBeansOfType(RecordIntercept.class);
        for (Map.Entry<String, RecordIntercept> entry : interceptMap.entrySet()) {
            list.add(entry.getValue());
        }
        String[] hosts = map.get("host").toString().split(" ");
        if (hosts.length > 0) {
            boolean isAll = false;
            for (String h : hosts) {
                if (h.equals("*")) {
                    isAll = true;
                    break;
                }
            }
            if (isAll) {
                RecordServer.startInterceptAll(list);
            } else {
                RecordServer.startInterceptHost(list, hosts);
            }
        } else {
            RecordServer.startInterceptNone();
        }
        return Result.ofSuccess();
    }

    @RequestMapping("/proxyStop")
    public Result proxyStop() {
        RecordServer.stop();
        return Result.ofSuccess();
    }

    @RequestMapping("/recordQuery")
    public Result recordQuery(@RequestBody Map params) {
        Long lastRecordId = Long.parseLong(params.get("lastRecordId").toString());
        return Result.ofData(recordService.recordQuery(lastRecordId));
    }

    @RequestMapping("/recordDetail")
    public Result recordDetail(@RequestBody Map params) {
        Long recordId = Long.parseLong(params.get("recordId").toString());
        Record record = recordService.recordDetail(recordId);
        String request = record.getRequest();
        String response = record.getResponse();
        Map<String, List> data = new HashMap<>();
        data.put("request", BaseUtil.parseJson(StringUtils.defaultString(request, "[]"), List.class));
        data.put("response", BaseUtil.parseJson(StringUtils.defaultString(response, "[]"), List.class));
        return Result.ofData(data);
    }
}
