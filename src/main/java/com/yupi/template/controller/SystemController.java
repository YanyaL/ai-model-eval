package com.yupi.template.controller;

import com.yupi.template.common.BaseResponse;
import com.yupi.template.common.ResultUtils;
import com.yupi.template.service.AiModeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统状态（含 AI Mock / Live）
 */
@RestController
@RequestMapping("/system")
public class SystemController {

    @Resource
    private AiModeService aiModeService;

    @GetMapping("/ai-status")
    public BaseResponse<Map<String, Object>> aiStatus() {
        return ResultUtils.success(aiModeService.status());
    }
}
