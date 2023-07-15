package com.cs.ge.services.qrcode;

import com.cs.ge.entites.QRCodeEntity;
import com.cs.ge.utils.UtilitaireService;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class WIFIQrCode extends SharedQRCodeService {

    public WIFIQrCode(final UtilitaireService utilitaireService, final Environment environment) {
        super(utilitaireService, environment);
    }

    @Override
    public Map<String, Object> qrCodeParamsFromType(final QRCodeEntity qrCodeEntity, final String imagesHost, final String imagesRootfolder, final String imagesFolder) {
        final Map<String, Object> params = super.qrCodeParamsFromType(qrCodeEntity, imagesHost, imagesRootfolder, imagesFolder);
        params.put("name", qrCodeEntity.getData().get("name"));
        final String pattern = "WIFI:S:\"%s\";T:%s;P:%s;H:%s;;";
        String encodage = qrCodeEntity.getData().get("encodage");
        if (Strings.isNullOrEmpty(qrCodeEntity.getData().get("password"))) {
            encodage = "nopass";
        }
        final String finalContent = String.format(pattern, qrCodeEntity.getData().get("name"), encodage, qrCodeEntity.getData().get("password"), qrCodeEntity.getData().get("hidden"));
        params.put("finalContent", finalContent);
        params.put("tempContent", params.get("finalContent"));
        return params;
    }
}
