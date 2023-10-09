package com.cs.ge.services.messages;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.EventMessage;
import com.cs.ge.entites.EventMessageNotificationStatus;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Channel;
import com.cs.ge.enums.StockType;
import com.cs.ge.repositories.EventMessageRepository;
import com.cs.ge.repositories.EventRepository;
import com.cs.ge.services.ProfileService;
import com.cs.ge.services.StockService;
import com.cs.ge.services.notifications.ASynchroniousNotifications;
import com.google.common.base.Strings;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventMessageService {

    private final EventRepository eventsRepository;
    private final StockService stockService;
    private final ProfileService profileService;
    private final ASynchroniousNotifications aSynchroniousNotifications;
    private final EventMessageRepository eventMessageRepository;

    public EventMessageService(final EventRepository eventsRepository, final StockService stockService, final ProfileService profileService, final ASynchroniousNotifications aSynchroniousNotifications, final EventMessageRepository eventMessageRepository) {
        this.eventsRepository = eventsRepository;
        this.stockService = stockService;
        this.profileService = profileService;
        this.aSynchroniousNotifications = aSynchroniousNotifications;
        this.eventMessageRepository = eventMessageRepository;
    }

    public void eventToEventMessages(final Event event) {
        final List<EventMessage> eventMessagesToHandle = new ArrayList<>();
        event.getChannels().forEach(channel -> event.getGuests().forEach(
                        guest -> event.getMessages().forEach(
                                message -> message.getSchedules().forEach(
                                        applicationMessageSchedule -> {
                                            eventMessagesToHandle.add(
                                                    EventMessage
                                                            .builder()
                                                            .schedule(applicationMessageSchedule)
                                                            .message(message)
                                                            .channel(channel)
                                                            .eventId(event.getId())
                                                            .authorId(event.getAuthorId())
                                                            .guest(guest)
                                                            .status(
                                                                    List.of(
                                                                            EventMessageNotificationStatus.builder()
                                                                                    .status("SCHEDULED")
                                                                                    .creation(Instant.now())
                                                                                    .build()
                                                                    )
                                                            )
                                                            .publicId(RandomStringUtils.randomNumeric(8))
                                                            .creation(Instant.now())
                                                            .isHandled(false)
                                                            .build()
                                            );
                                        }
                                )
                        )
                )
        );
        this.eventMessageRepository.saveAll(eventMessagesToHandle);
    }

    public void handleMessages(final List<Channel> channels, final Event event) {
        channels.forEach(channel -> {
            List<EventMessage> eventMessages = this.eventMessageRepository.findMessagesToSend(channel, event.getId()).collect(Collectors.toList());
            eventMessages = eventMessages.stream().filter(eventMessage -> eventMessage.getMessage() != null && !Strings.isNullOrEmpty(eventMessage.getMessage().getText()))
                    .filter(eventMessage -> !eventMessage.isHandled())
                    .peek(eventMessage -> {
                                final UserAccount author = this.profileService.findById(event.getAuthorId());
                                this.aSynchroniousNotifications.sendEventMessage(
                                        event,
                                        author,
                                        eventMessage.getMessage(),
                                        List.of(channel),
                                        null,
                                        new HashMap<>()
                                );
                                eventMessage.setStatus(
                                        List.of(
                                                EventMessageNotificationStatus.builder()
                                                        .status("SENT")
                                                        .creation(Instant.now())
                                                        .build()
                                        )
                                );
                                eventMessage.setHandle(Instant.now());
                                eventMessage.setHandled(true);
                            }
                    ).collect(Collectors.toList());
            this.eventMessageRepository.saveAll(eventMessages);
            this.updateStocks(event.getAuthorId(), channels, 1);
        });
    }

    private void updateStocks(final String userId, final List<Channel> channelsToHandle, final int consumed) {
        channelsToHandle.parallelStream()
                .forEach(channel -> this.stockService
                        .update(userId, null, null, consumed, channel, StockType.DEBIT));
    }
}
