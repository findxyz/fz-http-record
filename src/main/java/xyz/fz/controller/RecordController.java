package xyz.fz.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.fz.model.Result;

@RestController
@RequestMapping("/api/record")
public class RecordController {
    @RequestMapping("/")
    public Result proxyStart() {
        return Result.ofSuccess();
    }
}
