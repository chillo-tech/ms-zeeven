package com.cs.ge.enums;

public class ChannelEnumGestionnaire {
    public static Channel getCorrespondingChannel(String channelStr){
        switch (channelStr){
            case "SMS" :
                return Channel.SMS;
            case "WHATSAPP" :
                return Channel.WHATSAPP;
            case "EMAIL" :
                return Channel.EMAIL;
            case "APPLICATION" :
                return Channel.APPLICATION;
            default:
                return Channel.QRCODE;
        }
    }
}
