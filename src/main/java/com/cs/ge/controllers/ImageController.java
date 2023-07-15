package com.cs.ge.controllers;

import com.cs.ge.services.EventService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@AllArgsConstructor
@RestController
@RequestMapping("ticket")
public class ImageController {
    private EventService eventService;

    @GetMapping(produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody
    byte[] getImage(@RequestParam String event, @RequestParam String guest) throws IOException {
        return this.eventService.getGuestEventTicket(guest, event);
    }
}
