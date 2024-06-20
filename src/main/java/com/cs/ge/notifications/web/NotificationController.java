package com.cs.ge.notifications.web;

import com.cs.ge.enums.Channel;
import com.cs.ge.notifications.entity.Notification;
import com.cs.ge.notifications.entity.NotificationStatus;
import com.cs.ge.notifications.service.NotificationService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@AllArgsConstructor
@RestController
@RequestMapping(path = "v1", produces = APPLICATION_JSON_VALUE)
public class NotificationController {
    private NotificationService notificationService;

    @PostMapping(consumes = APPLICATION_JSON_VALUE, path = "notification")
    public void send(
            @RequestHeader(name = "X-application-name", required = false) final String applicationName,
            @RequestParam final List<Channel> types,
            @RequestBody final Notification notification) {

        this.notificationService.send(applicationName, notification, types);
    }


    @GetMapping(value = "statistic")
    public List<NotificationStatus> fetchStatistic(@RequestParam final String id) {

        return this.notificationService.statistics(id);
    }

}
