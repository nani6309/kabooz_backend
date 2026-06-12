package com.kabooz.backend.service;

import com.kabooz.backend.entity.Order;
import com.kabooz.backend.entity.OrderItem;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Service that generates a professional PDF invoice using iText 8.
 *
 * <h3>Invoice Layout</h3>
 * <ol>
 *   <li>Gold gradient header with company name and contact details</li>
 *   <li>Invoice metadata (No, Date, Due Date)</li>
 *   <li>Bill To / Ship To customer block</li>
 *   <li>Items table with GST breakdown</li>
 *   <li>Tax summary and payment details</li>
 *   <li>Bank details and UPI</li>
 *   <li>Terms &amp; Conditions</li>
 *   <li>Signature block and footer</li>
 * </ol>
 */
@Service
@Slf4j
public class InvoiceService {

    // ── Brand colours ──────────────────────────────────────────────────────
    private static final DeviceRgb GOLD_DARK   = new DeviceRgb(180, 130, 0);
    private static final DeviceRgb GOLD_LIGHT  = new DeviceRgb(255, 215, 0);
    private static final DeviceRgb DARK_BG     = new DeviceRgb(30, 30, 30);
    private static final DeviceRgb LIGHT_GREY  = new DeviceRgb(245, 245, 245);
    private static final DeviceRgb TABLE_HEADER= new DeviceRgb(50, 50, 50);
    private static final DeviceRgb ACCENT      = new DeviceRgb(200, 150, 0);

    // ── Company details ────────────────────────────────────────────────────
    private static final String COMPANY_NAME    = "SRI RAMA KRUPA ENTERPRISES";
    private static final String BRAND_NAME      = "KABOOZ GOLI SODA";
    private static final String GSTIN           = "GSTIN: 29JAMPK0701B1ZY";
    private static final String PHONE           = "Ph: 8123980893";
    private static final String EMAIL           = "kaboozgolisoda1528@gmail.com";
    private static final String ADDRESS_LINE1   = "GROUND NO.46/4, MUTHANALLUR CROSS,";
    private static final String ADDRESS_LINE2   = "OFF-SARJAPURA ROAD, DOMMASANDRA,";
    private static final String ADDRESS_LINE3   = "BENGALURU - 562125";

    private static final String BANK_NAME       = "Shri Rama Krupa Enterprises";
    private static final String BANK_IFSC       = "CNRB0004789";
    private static final String BANK_ACCOUNT    = "120033317947";
    private static final String BANK_BRANCH     = "Canara Bank, DOMMASANDRA";
    private static final String UPI_ID          = "kushalreddy1680-1@okaxis";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * Generate a PDF invoice for the given order and return the bytes.
     *
     * @param order the fully loaded order entity (with customer and items)
     * @return PDF as byte array ready to stream to the client
     * @throws IOException if PDF generation fails
     */
    public byte[] generateInvoicePdf(Order order) throws IOException {
        log.info("Generating PDF for invoice {}", order.getInvoiceNo());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(20, 36, 36, 36);

        PdfFont boldFont   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // ── 1. Header ───────────────────────────────────────────────────
        addHeader(doc, boldFont, regularFont);

        // ── 2. Invoice meta ─────────────────────────────────────────────
        addInvoiceMeta(doc, boldFont, regularFont, order);

        // ── 3. Customer block ───────────────────────────────────────────
        addCustomerBlock(doc, boldFont, regularFont, order);

        // ── 4. Items table ──────────────────────────────────────────────
        addItemsTable(doc, boldFont, regularFont, order);

        // ── 5. Tax summary ──────────────────────────────────────────────
        addTaxSummary(doc, boldFont, regularFont, order);

        // ── 6. Amount in words ──────────────────────────────────────────
        addAmountInWords(doc, boldFont, regularFont, order);

        // ── 7. Bank details & UPI ───────────────────────────────────────
        addBankDetails(doc, boldFont, regularFont);

        // ── 8. Terms ────────────────────────────────────────────────────
        addTerms(doc, boldFont, regularFont);

        // ── 9. Signature block ──────────────────────────────────────────
        addSignatureBlock(doc, boldFont, regularFont);

        // ── 10. Footer ──────────────────────────────────────────────────
        addFooter(doc, boldFont, regularFont);

        doc.close();

        log.info("PDF generated successfully for invoice {}", order.getInvoiceNo());
        return baos.toByteArray();
    }

    // ── Section renderers ─────────────────────────────────────────────────

    private void addHeader(Document doc, PdfFont boldFont, PdfFont regularFont) {
        // Gold background header table
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth();

        // Company info cell
        Cell companyCell = new Cell()
                .setBackgroundColor(DARK_BG)
                .setBorder(Border.NO_BORDER)
                .setPadding(14);
        companyCell.add(new Paragraph(COMPANY_NAME)
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(GOLD_LIGHT)
                .setMarginBottom(2));
        companyCell.add(new Paragraph(BRAND_NAME)
                .setFont(boldFont)
                .setFontSize(10)
                .setFontColor(GOLD_DARK)
                .setMarginBottom(6));
        companyCell.add(new Paragraph(GSTIN)
                .setFont(regularFont)
                .setFontSize(8)
                .setFontColor(ColorConstants.WHITE));
        companyCell.add(new Paragraph(PHONE + " | " + EMAIL)
                .setFont(regularFont)
                .setFontSize(8)
                .setFontColor(ColorConstants.WHITE));
        companyCell.add(new Paragraph(ADDRESS_LINE1)
                .setFont(regularFont)
                .setFontSize(8)
                .setFontColor(ColorConstants.LIGHT_GRAY));
        companyCell.add(new Paragraph(ADDRESS_LINE2)
                .setFont(regularFont)
                .setFontSize(8)
                .setFontColor(ColorConstants.LIGHT_GRAY));
        companyCell.add(new Paragraph(ADDRESS_LINE3)
                .setFont(regularFont)
                .setFontSize(8)
                .setFontColor(ColorConstants.LIGHT_GRAY));

        // Invoice title cell
        Cell titleCell = new Cell()
                .setBackgroundColor(GOLD_DARK)
                .setBorder(Border.NO_BORDER)
                .setPadding(14)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
        titleCell.add(new Paragraph("TAX INVOICE")
                .setFont(boldFont)
                .setFontSize(22)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT));

        headerTable.addCell(companyCell);
        headerTable.addCell(titleCell);
        doc.add(headerTable);
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    private void addInvoiceMeta(Document doc, PdfFont boldFont, PdfFont regularFont, Order order) {
        Table metaTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER);

        Cell leftMeta = new Cell().setBorder(Border.NO_BORDER).setPadding(4);
        leftMeta.add(metaLine(boldFont, regularFont, "Invoice No:", "#" + order.getInvoiceNo()));
        leftMeta.add(metaLine(boldFont, regularFont, "Invoice Date:", order.getInvoiceDate().format(DATE_FMT)));
        if (order.getDueDate() != null) {
            leftMeta.add(metaLine(boldFont, regularFont, "Due Date:", order.getDueDate().format(DATE_FMT)));
        }

        Cell rightMeta = new Cell().setBorder(Border.NO_BORDER).setPadding(4);
        rightMeta.add(metaLine(boldFont, regularFont, "Place of Supply:", order.getCustomer().getPlaceOfSupply()));
        rightMeta.add(metaLine(boldFont, regularFont, "Source:", order.getSource().name()));

        metaTable.addCell(leftMeta);
        metaTable.addCell(rightMeta);
        doc.add(metaTable);
        doc.add(divider());
    }

    private void addCustomerBlock(Document doc, PdfFont boldFont, PdfFont regularFont, Order order) {
        Table custTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setBorder(new SolidBorder(GOLD_DARK, 1f))
                .setMarginBottom(8);

        Cell billTo = new Cell().setPadding(8).setBorder(Border.NO_BORDER);
        billTo.add(new Paragraph("BILL TO")
                .setFont(boldFont).setFontSize(9).setFontColor(GOLD_DARK));
        billTo.add(new Paragraph(order.getCustomer().getName())
                .setFont(boldFont).setFontSize(11));
        if (order.getCustomerShopName() != null && !order.getCustomerShopName().isBlank()) {
            billTo.add(new Paragraph(order.getCustomerShopName())
                    .setFont(boldFont).setFontSize(10).setFontColor(ColorConstants.GRAY));
        }
        billTo.add(new Paragraph("Mobile: " + order.getCustomer().getMobile())
                .setFont(regularFont).setFontSize(9));
        if (hasPrintableGst(order)) {
            billTo.add(new Paragraph("GSTIN: " + order.getGstNumber())
                    .setFont(regularFont).setFontSize(9));
        }
        if (order.getCustomer().getAddress() != null) {
            billTo.add(new Paragraph(order.getCustomer().getAddress())
                    .setFont(regularFont).setFontSize(9));
        }
        billTo.add(new Paragraph("State: " + order.getCustomer().getPlaceOfSupply())
                .setFont(regularFont).setFontSize(9));

        Cell shipTo = new Cell().setPadding(8)
                .setBorder(Border.NO_BORDER)
                .setBorderLeft(new SolidBorder(GOLD_DARK, 0.5f));
        shipTo.add(new Paragraph("SHIP TO")
                .setFont(boldFont).setFontSize(9).setFontColor(GOLD_DARK));
        shipTo.add(new Paragraph("Same as Bill To")
                .setFont(regularFont).setFontSize(9).setFontColor(ColorConstants.GRAY));

        custTable.addCell(billTo);
        custTable.addCell(shipTo);
        doc.add(custTable);
    }

    private void addItemsTable(Document doc, PdfFont boldFont, PdfFont regularFont, Order order) {
        // Columns: No | Items | Qty | Rate/Unit | Tax (40%) | Total
        Table table = new Table(UnitValue.createPercentArray(new float[]{5, 35, 10, 18, 18, 14}))
                .useAllAvailableWidth()
                .setMarginBottom(8);

        // Header row
        String[] headers = {"No", "Items", "Qty", "Rate/Unit (₹)", "Tax 40% (₹)", "Total (₹)"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(TABLE_HEADER)
                    .setPadding(6)
                    .add(new Paragraph(h)
                            .setFont(boldFont)
                            .setFontSize(9)
                            .setFontColor(GOLD_LIGHT)
                            .setTextAlignment(TextAlignment.CENTER)));
        }

        // Item rows
        int i = 1;
        for (OrderItem item : order.getItems()) {
            boolean alt = (i % 2 == 0);
            DeviceRgb rowBg = alt ? LIGHT_GREY : new DeviceRgb(255, 255, 255);

            String desc = item.getFlavor() + "\n" +
                    item.getBottleType().name() + " | " +
                    item.getBottlesPerUnit() + " btls/" +
                    (item.getBottleType() == OrderItem.BottleType.GLASS ? "crate" : "case");

            table.addCell(bodyCell(String.valueOf(i), regularFont, rowBg, TextAlignment.CENTER));
            table.addCell(bodyCell(desc, regularFont, rowBg, TextAlignment.LEFT));
            table.addCell(bodyCell(String.valueOf(item.getQuantity()), regularFont, rowBg, TextAlignment.CENTER));
            table.addCell(bodyCell(fmt(item.getRatePerUnit()), regularFont, rowBg, TextAlignment.RIGHT));
            table.addCell(bodyCell(fmt(item.getTaxSubtotal()), regularFont, rowBg, TextAlignment.RIGHT));
            table.addCell(bodyCell(fmt(item.getLineTotal()), regularFont, rowBg, TextAlignment.RIGHT));
            i++;
        }

        // Subtotal row
        Cell subtotalLabel = new Cell(1, 5)
                .setBackgroundColor(LIGHT_GREY)
                .setPadding(6)
                .add(new Paragraph("Sub Total")
                        .setFont(boldFont).setFontSize(9).setTextAlignment(TextAlignment.RIGHT));
        Cell subtotalVal = new Cell()
                .setBackgroundColor(LIGHT_GREY)
                .setPadding(6)
                .add(new Paragraph("₹" + fmt(order.getGrandTotal()))
                        .setFont(boldFont).setFontSize(9).setTextAlignment(TextAlignment.RIGHT));
        table.addCell(subtotalLabel);
        table.addCell(subtotalVal);

        doc.add(table);
    }

    private void addTaxSummary(Document doc, PdfFont boldFont, PdfFont regularFont, Order order) {
        Table tax = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth()
                .setMarginBottom(6);

        addTaxRow(tax, boldFont, regularFont, "Taxable Amount", "₹" + fmt(order.getTaxableAmount()), false);
        addTaxRow(tax, boldFont, regularFont, "CGST @ 20%",    "₹" + fmt(order.getCgst()), false);
        addTaxRow(tax, boldFont, regularFont, "SGST @ 20%",    "₹" + fmt(order.getSgst()), false);
        addTaxRow(tax, boldFont, regularFont, "Total Amount",  "₹" + fmt(order.getGrandTotal()), true);
        addTaxRow(tax, boldFont, regularFont, "Received Amount","₹" + fmt(order.getReceivedAmount()), false);

        BigDecimal balance = order.getGrandTotal().subtract(order.getReceivedAmount()).max(BigDecimal.ZERO);
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            addTaxRow(tax, boldFont, regularFont, "Balance Due", "₹" + fmt(balance), true);
        }

        doc.add(tax);
    }

    private void addAmountInWords(Document doc, PdfFont boldFont, PdfFont regularFont, Order order) {
        String words = amountInWords(order.getGrandTotal());
        doc.add(new Paragraph("Amount in Words: " + words)
                .setFont(boldFont)
                .setFontSize(9)
                .setFontColor(DARK_BG)
                .setMarginBottom(8));
    }

    private void addBankDetails(Document doc, PdfFont boldFont, PdfFont regularFont) {
        doc.add(divider());
        doc.add(new Paragraph("BANK DETAILS")
                .setFont(boldFont).setFontSize(10).setFontColor(GOLD_DARK).setMarginBottom(2));

        Table bank = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .useAllAvailableWidth()
                .setBorder(new SolidBorder(GOLD_DARK, 0.5f))
                .setMarginBottom(8);

        addBankRow(bank, boldFont, regularFont, "Account Name", BANK_NAME);
        addBankRow(bank, boldFont, regularFont, "IFSC Code",    BANK_IFSC);
        addBankRow(bank, boldFont, regularFont, "Account No.",  BANK_ACCOUNT);
        addBankRow(bank, boldFont, regularFont, "Bank",         BANK_BRANCH);
        addBankRow(bank, boldFont, regularFont, "UPI ID",       UPI_ID);
        doc.add(bank);
    }

    private void addTerms(Document doc, PdfFont boldFont, PdfFont regularFont) {
        doc.add(new Paragraph("TERMS & CONDITIONS")
                .setFont(boldFont).setFontSize(9).setFontColor(GOLD_DARK).setMarginBottom(2));
        String[] terms = {
                "1. Payment is due within 30 days of invoice date.",
                "2. Goods once sold will not be taken back or exchanged.",
                "3. Interest @ 18% p.a. will be charged on overdue payments.",
                "4. All disputes subject to Bengaluru jurisdiction only."
        };
        for (String term : terms) {
            doc.add(new Paragraph(term).setFont(regularFont).setFontSize(8).setMarginBottom(1));
        }
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    private void addSignatureBlock(Document doc, PdfFont boldFont, PdfFont regularFont) {
        Table sig = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)
                .setMarginBottom(8);

        Cell leftSig = new Cell().setBorder(Border.NO_BORDER);
        leftSig.add(new Paragraph("Customer Acknowledgement")
                .setFont(boldFont).setFontSize(9).setFontColor(GOLD_DARK));
        leftSig.add(new Paragraph("\n\n").setFontSize(4));
        leftSig.add(new Paragraph("___________________________")
                .setFont(regularFont).setFontSize(9));
        leftSig.add(new Paragraph("Signature")
                .setFont(regularFont).setFontSize(8).setFontColor(ColorConstants.GRAY));

        Cell rightSig = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        rightSig.add(new Paragraph("For " + COMPANY_NAME)
                .setFont(boldFont).setFontSize(9).setFontColor(GOLD_DARK));
        rightSig.add(new Paragraph("\n\n").setFontSize(4));
        rightSig.add(new Paragraph("___________________________")
                .setFont(regularFont).setFontSize(9).setTextAlignment(TextAlignment.RIGHT));
        rightSig.add(new Paragraph("Proprietor / Authorised Signatory")
                .setFont(regularFont).setFontSize(8).setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.RIGHT));

        sig.addCell(leftSig);
        sig.addCell(rightSig);
        doc.add(sig);
    }

    private void addFooter(Document doc, PdfFont boldFont, PdfFont regularFont) {
        doc.add(divider());
        doc.add(new Paragraph(BRAND_NAME + " — FEEL THE BOOZ")
                .setFont(boldFont)
                .setFontSize(10)
                .setFontColor(GOLD_DARK)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(4));
        doc.add(new Paragraph("This is a computer-generated invoice and does not require a physical signature.")
                .setFont(regularFont)
                .setFontSize(7)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER));
    }

    // ── Cell & row helpers ────────────────────────────────────────────────

    private Cell bodyCell(String text, PdfFont font, DeviceRgb bg, TextAlignment align) {
        return new Cell()
                .setBackgroundColor(bg)
                .setPadding(5)
                .add(new Paragraph(text).setFont(font).setFontSize(9).setTextAlignment(align));
    }

    private void addTaxRow(Table table, PdfFont boldFont, PdfFont regularFont,
                            String label, String value, boolean highlight) {
        // Both DeviceRgb (our constants) and ColorConstants.* (type Color) share
        // the Color supertype — use Color here to avoid incompatible-types error.
        DeviceRgb bg = highlight ? GOLD_DARK : new DeviceRgb(255, 255, 255);
        Color fg = highlight ? ColorConstants.WHITE : DARK_BG;

        table.addCell(new Cell()
                .setBackgroundColor(bg)
                .setPadding(5)
                .setBorder(new SolidBorder(LIGHT_GREY, 0.5f))
                .add(new Paragraph(label)
                        .setFont(boldFont).setFontSize(9)
                        .setFontColor(fg)
                        .setTextAlignment(TextAlignment.RIGHT)));

        table.addCell(new Cell()
                .setBackgroundColor(bg)
                .setPadding(5)
                .setBorder(new SolidBorder(LIGHT_GREY, 0.5f))
                .add(new Paragraph(value)
                        .setFont(boldFont).setFontSize(9)
                        .setFontColor(fg)
                        .setTextAlignment(TextAlignment.RIGHT)));
    }

    private void addBankRow(Table table, PdfFont boldFont, PdfFont regularFont, String key, String value) {
        table.addCell(new Cell()
                .setPadding(4)
                .setBorder(new SolidBorder(LIGHT_GREY, 0.5f))
                .add(new Paragraph(key).setFont(boldFont).setFontSize(8)));
        table.addCell(new Cell()
                .setPadding(4)
                .setBorder(new SolidBorder(LIGHT_GREY, 0.5f))
                .add(new Paragraph(value).setFont(regularFont).setFontSize(8)));
    }

    private Paragraph metaLine(PdfFont boldFont, PdfFont regularFont, String key, String value) {
        return new Paragraph()
                .add(new Text(key + " ").setFont(boldFont).setFontSize(9))
                .add(new Text(value).setFont(regularFont).setFontSize(9))
                .setMarginBottom(2);
    }

    private LineSeparator divider() {
        return new LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
                .setStrokeColor(GOLD_DARK)
                .setMarginTop(6)
                .setMarginBottom(6);
    }

    private String fmt(BigDecimal value) {
        if (value == null) return "0.00";
        return String.format("%,.2f", value.setScale(2, RoundingMode.HALF_UP));
    }

    private boolean hasPrintableGst(Order order) {
        return order != null
                && Boolean.TRUE.equals(order.getWithGst())
                && order.getGstNumber() != null
                && !order.getGstNumber().isBlank();
    }

    // ── Indian number-to-words ────────────────────────────────────────────

    /**
     * Convert a BigDecimal amount to Indian number words (Lakh/Crore system).
     *
     * @param amount the amount to convert
     * @return human-readable string like "Nine Hundred Sixty Rupees Only"
     */
    private String amountInWords(BigDecimal amount) {
        long rupees = amount.longValue();
        long paise = amount.subtract(BigDecimal.valueOf(rupees))
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        String words = convertToWords(rupees) + " Rupees";
        if (paise > 0) {
            words += " and " + convertToWords(paise) + " Paise";
        }
        return words + " Only";
    }

    private String convertToWords(long number) {
        if (number == 0) return "Zero";

        String[] ones = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven",
                "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen",
                "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty",
                "Sixty", "Seventy", "Eighty", "Ninety"};

        StringBuilder sb = new StringBuilder();

        if (number >= 10000000) { // Crore
            sb.append(convertToWords(number / 10000000)).append(" Crore ");
            number %= 10000000;
        }
        if (number >= 100000) { // Lakh
            sb.append(convertToWords(number / 100000)).append(" Lakh ");
            number %= 100000;
        }
        if (number >= 1000) { // Thousand
            sb.append(convertToWords(number / 1000)).append(" Thousand ");
            number %= 1000;
        }
        if (number >= 100) { // Hundred
            sb.append(ones[(int) (number / 100)]).append(" Hundred ");
            number %= 100;
        }
        if (number >= 20) {
            sb.append(tens[(int) (number / 10)]).append(" ");
            number %= 10;
        }
        if (number > 0) {
            sb.append(ones[(int) number]).append(" ");
        }

        return sb.toString().trim();
    }
}
