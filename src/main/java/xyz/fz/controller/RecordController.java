package xyz.fz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class RecordController {
    @RequestMapping("/")
    public String index() {
        return "index";
    }
}
