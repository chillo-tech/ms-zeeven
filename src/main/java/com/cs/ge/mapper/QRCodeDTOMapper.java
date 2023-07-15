package com.cs.ge.mapper;

import com.cs.ge.dto.QRCodeEntityDTO;
import com.cs.ge.entites.QRCodeEntity;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class QRCodeDTOMapper implements Function<QRCodeEntity, QRCodeEntityDTO> {

    @Override
    public QRCodeEntityDTO apply(final QRCodeEntity qrCodeEntity) {
        return QRCodeEntityDTO.builder()
                .track(qrCodeEntity.isTrack())
                .enabled(qrCodeEntity.isEnabled())
                .type(qrCodeEntity.getType())
                .data(qrCodeEntity.getData())
                .attributes(qrCodeEntity.getAttributes())
                .publicId(qrCodeEntity.getPublicId())
                .author(qrCodeEntity.getAuthor())
                .name(qrCodeEntity.getName())
                .file(qrCodeEntity.getFile())
                .finalContent(qrCodeEntity.getFinalContent())
                .tempContent(qrCodeEntity.getTempContent())
                .location(qrCodeEntity.getLocation())
                .creation(qrCodeEntity.getCreation())
                .update(qrCodeEntity.getUpdate())
                .id(qrCodeEntity.getId())
                .build();
    }
}
