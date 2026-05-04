package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.service.KitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@CrossOrigin
@RequiredArgsConstructor
public class KitchenAlertController {

    private final KitchenService kitchenService;
}
