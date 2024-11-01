package com.github.fractalo.streaming_settlement.controller;

import com.github.fractalo.streaming_settlement.dto.AdvertisementVideoMeta;
import com.github.fractalo.streaming_settlement.dto.AdvertisementVideoWatchingContext;
import com.github.fractalo.streaming_settlement.service.AdvertisementVideoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/advertisement/videos")
@RequiredArgsConstructor
public class AdvertisementVideoController {
    private final AdvertisementVideoService advertisementVideoService;

    @GetMapping
    public List<AdvertisementVideoMeta> getAdvertisementVideos(
            @RequestBody @Valid AdvertisementVideoWatchingContext context) {
        return advertisementVideoService.getAdvertisementVideoMeta(context);
    }

    @PostMapping("/{adVideoId}/watching-contexts")
    public void addAdvertisementVideoWatchingContext(@PathVariable("adVideoId") Long adVideoId,
                                                     @RequestBody @Valid AdvertisementVideoWatchingContext context,
                                                     @AuthenticationPrincipal Long memberId,
                                                     HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();

    }

    @PostMapping("/{adVideoId}/video-exposures/{videoId}/view-count/increment")
    public void increaseViewCount(@PathVariable("adVideoId") Long adVideoId,
                                  @PathVariable Long videoId,
                                  @AuthenticationPrincipal Long memberId,
                                  HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        advertisementVideoService.increaseViewCount(adVideoId, videoId, memberId, ipAddress);
    }

}
