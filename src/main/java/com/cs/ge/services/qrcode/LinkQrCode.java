package com.cs.ge.services.qrcode;

import com.cs.ge.entites.QRCodeEntity;
import com.cs.ge.utils.UtilitaireService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class LinkQrCode extends SharedQRCodeService {

    public LinkQrCode(final UtilitaireService utilitaireService, final Environment environment) {
        super(utilitaireService, environment);
    }

    @Override
    public Map<String, Object> qrCodeParamsFromType(final QRCodeEntity qrCodeEntity, final String imagesHost, final String imagesRootfolder, final String imagesFolder) {
        final Map<String, Object> params = super.qrCodeParamsFromType(qrCodeEntity, imagesHost, imagesRootfolder, imagesFolder);
        params.put("name", qrCodeEntity.getData().get("name"));
        params.put("finalContent", qrCodeEntity.getData().get("url"));
        return params;
    }
}
