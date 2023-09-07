package com.cs.ge.services.graphics;

import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;

@Component
public class GraphicsService {
    public void centerString(
            final Graphics g,
            final Rectangle r,
            final String s,
            final Font font,
            final int xPosition,
            final int yPosition
    ) {
        final FontRenderContext frc =
                new FontRenderContext(null, true, true);

        final Rectangle2D r2D = font.getStringBounds(s, frc);
        final int rWidth = (int) Math.round(r2D.getWidth());
        final int rHeight = (int) Math.round(r2D.getHeight());
        final int rX;
        final int a;
        if (xPosition != 0) {
            a = xPosition;
        } else {
            rX = (int) Math.round(r2D.getX());
            a = (r.width / 2) - (rWidth / 2) - rX;
        }
        final int rY;
        final int b;
        if (yPosition != 0) {
            b = yPosition;
        } else {
            rY = (int) Math.round(r2D.getY());
            b = (r.height / 2) - (rHeight / 2) - rY;
        }

        g.setFont(font);
        g.drawString(s, r.x + a, r.y + b);
    }
}
