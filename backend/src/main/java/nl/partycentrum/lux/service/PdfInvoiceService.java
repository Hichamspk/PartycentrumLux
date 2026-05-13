package nl.partycentrum.lux.service;

import nl.partycentrum.lux.config.InvoiceProperties;
import nl.partycentrum.lux.domain.Invoice;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PdfInvoiceService {

    private final InvoiceProperties invoiceProperties;
    private final CompanySettingsService companySettingsService;
    private final InvoiceTemplateService invoiceTemplateService;

    public PdfInvoiceService(
            InvoiceProperties invoiceProperties,
            CompanySettingsService companySettingsService,
            InvoiceTemplateService invoiceTemplateService
    ) {
        this.invoiceProperties = invoiceProperties;
        this.companySettingsService = companySettingsService;
        this.invoiceTemplateService = invoiceTemplateService;
    }

    public byte[] generate(Invoice invoice) {
        var settings = companySettingsService.resolve();
        var html = invoiceTemplateService.render(invoice, settings);
        return renderHtml(html);
    }

    public byte[] renderHtml(String html) {
        try (var outputStream = new ByteArrayOutputStream()) {
            var renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("PDF kon niet worden gegenereerd.", exception);
        }
    }

    public Path save(Invoice invoice, byte[] pdfBytes) {
        try {
            var directory = Path.of(invoiceProperties.storageDir()).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            var path = directory.resolve(invoice.getInvoiceNumber() + ".pdf");
            Files.write(path, pdfBytes);
            return path;
        } catch (IOException exception) {
            throw new IllegalStateException("PDF kon niet worden opgeslagen.", exception);
        }
    }
}
