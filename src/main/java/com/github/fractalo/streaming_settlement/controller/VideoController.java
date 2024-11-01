package com.github.fractalo.streaming_settlement.controller;

import com.github.fractalo.streaming_settlement.dto.VideoMeta;
import com.github.fractalo.streaming_settlement.dto.VideoWatchingContext;
import com.github.fractalo.streaming_settlement.service.VideoService;
import com.github.fractalo.streaming_settlement.service.VideoWatchHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;
    private final VideoWatchHistoryService videoWatchHistoryService;

    @GetMapping("/{videoId}")
    public VideoMeta getVideo(@PathVariable Long videoId,
                              @AuthenticationPrincipal Long memberId) {
        return videoService.getVideoMeta(videoId, memberId);
    }

    @PostMapping("/{videoId}/watch-histories")
    public void updateVideoWatchHistory(@PathVariable Long videoId,
                                     @RequestBody @Valid VideoWatchingContext context,
                                     @AuthenticationPrincipal Long memberId,
                                     HttpServletRequest request) {

        String ipAddress = request.getRemoteAddr();
        videoWatchHistoryService.updateOrInsertWatchHistory(videoId, context, memberId, ipAddress);
    }

    @PostMapping("/{videoId}/view-count/increment")
    public void increaseViewCount(@PathVariable Long videoId,
                                  @AuthenticationPrincipal Long memberId,
                                  HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        videoService.increaseViewCount(videoId, memberId, ipAddress);
    }

}
