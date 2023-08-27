package com.cs.ge.services.files;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.Invitation;
import com.itextpdf.io.IOException;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.color.Color;
import com.itextpdf.kernel.color.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.border.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.VerticalAlignment;
import net.glxn.qrgen.javase.QRCode;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;

import static net.glxn.qrgen.core.image.ImageType.JPG;

@Service
public class TicketFileService {

    public static final String NEWLINE = "\n";
    public static final int IMAGE_WIDTH = 50;
    public static final int IMAGE_HEIGHT = 50;
    public static final float FONT_SIZE = 4;

    public Object generateTicket(final Event event, final Guest guest, final Invitation invitation) throws IOException, java.io.IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PdfWriter writer = new PdfWriter(byteArrayOutputStream);
        final PdfDocument pdfDoc = new PdfDocument(writer);
        final Document doc = new Document(pdfDoc, PageSize.A6);
        doc.setBackgroundColor(new DeviceRgb(226, 232, 240));

        final float[] columnWidths = {6, 1};
        final Table table = new Table(columnWidths).setWidthPercent(100);
        table.setBorder(Border.NO_BORDER);
        table.setMargin(20);
        table.setBackgroundColor(new DeviceRgb(226, 232, 240));
        final Cell leftCell = new Cell();

        leftCell.setPaddings(0, 5, 0, 5);
        leftCell.setVerticalAlignment(VerticalAlignment.MIDDLE);

        leftCell.add(
                new Paragraph(
                        new Text(
                                String.format(
                                        "%s%s",
                                        String.valueOf(invitation.getTemplate().getTitle().charAt(0)).toUpperCase(),
                                        invitation.getTemplate().getTitle().substring(1).toLowerCase()
                                )
                        ).setBold()
                                .setFontSize(10)
                ).setMarginBottom(8)
        );

        invitation
                .getTemplate()
                .getSchedules()
                .forEach(schedule ->
                        leftCell.add(new Paragraph(
                                new Text(
                                        String.format(
                                                "%s",
                                                Date.from(schedule.getDate())
                                        )
                                )
                                        .setFontSize(6)
                        ))
                );

        leftCell.add(
                new Paragraph(
                        new Text(
                                String.format(
                                        "%s%s",
                                        String.valueOf(invitation.getTemplate().getAddress().charAt(0)).toUpperCase(),
                                        invitation.getTemplate().getAddress().substring(1).toLowerCase()
                                )
                        )
                                .setFontSize(6)
                )
        );
        leftCell.add(
                new Paragraph(
                        new Text(
                                String.format(
                                        "%s%s",
                                        String.valueOf(invitation.getTemplate().getText().charAt(0)).toUpperCase(),
                                        invitation.getTemplate().getText().substring(1).toLowerCase()
                                )
                        )
                                .setFontSize(FONT_SIZE)
                ).setMarginTop(10)
        );

        leftCell.setBorder(Border.NO_BORDER);
        table.addCell(leftCell);

        final Cell cellImage = new Cell();
        cellImage.setPaddings(4, 4, 4, 4);
        cellImage.setBorder(Border.NO_BORDER);
        cellImage.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cellImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
        cellImage.setBackgroundColor(Color.WHITE);

        final Text firstText = new Text(
                String.format(
                        "%s %s%s %s",
                        guest.getCivility(),
                        String.valueOf(guest.getFirstName().charAt(0)).toUpperCase(),
                        guest.getFirstName().substring(1).toLowerCase(),
                        guest.getLastName().toUpperCase()
                )
        );
        firstText.setFontSize(FONT_SIZE);
        firstText.setTextAlignment(TextAlignment.CENTER);
        cellImage.add(new Paragraph(firstText).setTextAlignment(TextAlignment.CENTER));

        final File qrcodeFile = QRCode.from(
                        String.format("%s|%s|%s", event.getPublicId(), invitation.getPublicId(), guest.getPublicId()))
                .to(JPG)
                .withSize(IMAGE_WIDTH, IMAGE_HEIGHT)
                .file();

        final Image image = new Image(ImageDataFactory.create(qrcodeFile.getCanonicalPath()));

        cellImage.add(
                new Paragraph()
                        .setHorizontalAlignment(HorizontalAlignment.CENTER)
                        .add(image)
        );

        final Text secondText = new Text("Ticket No.");
        secondText.setTextAlignment(TextAlignment.CENTER);
        secondText.setFontSize(FONT_SIZE);
        cellImage.add(new Paragraph(secondText).setTextAlignment(TextAlignment.CENTER));

        final Text thirdText = new Text(guest.getPublicId());
        thirdText.setTextAlignment(TextAlignment.CENTER);
        thirdText.setBold();
        thirdText.setFontSize(FONT_SIZE);
        cellImage.add(new Paragraph(thirdText).setTextAlignment(TextAlignment.CENTER));

        table.addCell(cellImage);

        doc.add(table);
        doc.close();
        final String dest = "/Users/chillo/projets/zeeven/data/tickets/" + System.currentTimeMillis() + ".pdf";
        final String destJpg = "/Users/chillo/projets/zeeven/data/tickets/" + System.currentTimeMillis() + ".jpg";
        final byte[] data = byteArrayOutputStream.toByteArray();
        FileUtils.writeByteArrayToFile(new File(dest), data);

        return null; //out.toByteArray();
    }

}
