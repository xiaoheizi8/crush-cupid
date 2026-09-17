package cn.yzfy.crushcupidserver.controller;

import cn.yzfy.crushcupidserver.agent.VoiceService;
import cn.yzfy.crushcupidserver.common.Result;
import cn.yzfy.crushcupidserver.logic.AuthLogic;
import cn.yzfy.crushcupidserver.model.dto.VoiceConfigDTO;
import cn.yzfy.crushcupidserver.model.dto.VoiceDesignDTO;
import cn.yzfy.crushcupidserver.model.dto.VoiceRequestDTO;
import cn.yzfy.crushcupidserver.model.entity.SysUser;
import cn.yzfy.crushcupidserver.model.vo.UserVO;
import cn.yzfy.crushcupidserver.model.vo.VoiceConfigVO;
import cn.yzfy.crushcupidserver.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceService voiceService;
    private final AuthLogic authLogic;
    private final SysUserService sysUserService;

    @PostMapping
    public Result<String> synthesize(@RequestBody VoiceRequestDTO dto) {
        byte[] audio = voiceService.synthesize(dto.getText(), dto.getVoice());
        String base64 = Base64.getEncoder().encodeToString(audio);
        return Result.ok(base64);
    }

    @PostMapping("/design")
    public Result<String> design(@RequestBody VoiceDesignDTO dto) {
        return Result.ok(voiceService.designVoice(dto.getVoicePrompt(), dto.getPreviewText()));
    }

    @GetMapping("/models")
    public Result<List<Map<String, Object>>> listModels() {
        return Result.ok(voiceService.listAvailableModels());
    }

    @GetMapping("/voices")
    public Result<List<Map<String, Object>>> listVoices(
            @RequestParam(value = "model", required = false) String model) {
        return Result.ok(voiceService.listVoices(model));
    }

    @GetMapping("/config")
    public Result<VoiceConfigVO> getConfig() {
        UserVO user = authLogic.me();
        long userId = user.getId();
        VoiceConfigVO vo = buildConfig(userId);
        vo.setCurrentVoice(user.getPreferredVoice());
        return Result.ok(vo);
    }

    @PostMapping("/config")
    public Result<VoiceConfigVO> saveConfig(@RequestBody VoiceConfigDTO dto) {
        long userId = authLogic.me().getId();
        SysUser user = sysUserService.getById(userId);
        if (user != null) {
            user.setPreferredVoice(dto.getPreferredVoice());
            user.setUpdatedAt(new java.util.Date());
            sysUserService.updateById(user);
        }
        VoiceConfigVO vo = buildConfig(userId);
        vo.setCurrentVoice(dto.getPreferredVoice());
        return Result.ok(vo);
    }

    private VoiceConfigVO buildConfig(long userId) {
        VoiceConfigVO vo = new VoiceConfigVO();
        vo.setCurrentModel(voiceService.getDefaultModel());
        vo.setCurrentVoice(voiceService.getDefaultVoice());
        vo.setAvailableModels(voiceService.listAvailableModels());
        return vo;
    }
}
