package com.hirecheck.controller;

import com.hirecheck.service.ImageKitService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ImageKitController {

    private final ImageKitService imageKitService;

    public ImageKitController(ImageKitService imageKitService) {
        this.imageKitService = imageKitService;
    }

    @GetMapping("/imagekit-auth")
    public Map<String, Object> auth() {
        return imageKitService.getAuthParameters();
    }
}
