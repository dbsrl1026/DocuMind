package com.gloud.document.hwpxlib;

import com.gloud.document.util.FontUtil;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor;
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

public class HwpxPdfRenderer {

    public static File renderToPdf(HWPXFile hwpxFile) throws Exception {
        File outputPdf = File.createTempFile("preview-", ".pdf");

        TextExtractMethod method = TextExtractMethod.InsertControlTextBetweenParagraphText;
        TextMarks marks = new TextMarks()
                .paraSeparatorAnd("\n\n")
                .lineBreakAnd("\n")
                .tabAnd("\t")
                .tableRowSeparatorAnd("\n")
                .tableCellSeparatorAnd("\t");

        String extractedText = TextExtractor.extract(hwpxFile, method, false, marks);

        try (FileOutputStream fos = new FileOutputStream(outputPdf)) {
            Document document = new Document();
            PdfWriter.getInstance(document, fos);
            document.open();
            BaseFont baseFont = BaseFont.createFont(FontUtil.detectSystemFontPath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font font = new Font(baseFont, 12);

            String[] lines = extractedText.split(marks.tableRowSeparator(), -1);
            for (String line : lines) {
                if (line.contains(marks.tableCellSeparator())) {
                    String[] cells = line.split(marks.tableCellSeparator(), -1);
                    PdfPTable table = new PdfPTable(cells.length);
                    float[] columnWidths = new float[cells.length];
                    Arrays.fill(columnWidths, 1f);
                    table.setWidths(columnWidths);
                    table.setWidthPercentage(100);

                    for (String cell : cells) {
                        Phrase phrase = new Phrase(cell.replace("\t", "    "), font);
                        PdfPCell cellObj = new PdfPCell(phrase);
                        cellObj.setPadding(5);
                        table.addCell(cellObj);
                    }
                    document.add(table);
                } else if (line.trim().isEmpty()) {
                    document.add(Chunk.NEWLINE);
                } else {
                    document.add(new Paragraph(line, font));
                }
            }

            document.close();
        }

        return outputPdf;
    }

}
