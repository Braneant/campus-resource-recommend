package com.campus.recommend.controller;

import com.campus.recommend.common.Result;
import com.campus.recommend.context.LoginContext;
import com.campus.recommend.service.RecommendService;
import com.campus.recommend.vo.RecommendExplainVO;
import com.campus.recommend.vo.RecommendItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 推荐模块 /api/recommend（核心） */
@Slf4j
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    /**
     * 8.1 个性化推荐（三路混合 + 加权融合 + 可解释）
     *
     * @param kpIds 可选。用户感兴趣的知识点 id 列表（逗号分隔，如 kpIds=1001,1002,1003）。
     *              用于冷启动：无行为数据的新用户以此作为知识图谱路的种子。
     *              不传时行为与改造前完全一致。
     */
    @GetMapping("/personal")
    public Result<List<RecommendItem>> personal(@RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) Integer topN,
                                                @RequestParam(required = false) List<Long> kpIds) {
        Long uid = userId != null ? userId : LoginContext.getStudentId();
        if (uid == null) {
            return Result.error(1002, "缺少 userId 或未登录");
        }
        return Result.success(recommendService.personalRecommend(uid, topN, kpIds));
    }

    /** 8.2 热门推荐 */
    @GetMapping("/hot")
    public Result<List<RecommendItem>> hot(@RequestParam(required = false) Integer topN,
                                          @RequestParam(required = false) Long collegeId) {
        return Result.success(recommendService.hotRecommend(topN, collegeId));
    }

    /** 8.3 知识图谱语义相关（单资源） */
    @GetMapping("/related")
    public Result<List<RecommendItem>> related(@RequestParam String resourceId,
                                               @RequestParam(required = false) Integer topN) {
        return Result.success(recommendService.relatedRecommend(resourceId, topN));
    }

    /** 8.4 单条推荐解释 */
    @GetMapping("/explain")
    public Result<RecommendExplainVO> explain(@RequestParam(required = false) Long userId,
                                             @RequestParam Long resourceId) {
        Long uid = userId != null ? userId : LoginContext.getStudentId();
        if (uid == null) {
            return Result.error(1002, "缺少 userId 或未登录");
        }
        return Result.success(recommendService.explain(uid, resourceId));
    }
}
