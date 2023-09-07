package com.cs.ge.dto;

import com.cs.ge.enums.QRCodeShapeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class QRCodeShape {
    private String bgColor;
    private QRCodeShapeType selected = QRCodeShapeType.NONE;
    private String text;
    private String textColor;
}
