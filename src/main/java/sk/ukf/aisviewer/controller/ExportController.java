package sk.ukf.aisviewer.controller;

import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import sk.ukf.aisviewer.model.Exam;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.service.LocalCacheService;

import java.io.File;
import java.io.IOException;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExportController {

    private final Button pdfButton;
    private final Button csvButton;
    private StudentInfo studentInfo;
    private LocalCacheService.CacheSnapshot cacheSnapshot;
    private int acquiredCredits;
    private int requiredCredits;
    private String averageGrade = "-";

    public ExportController(Button pdfButton, Button csvButton) {
        this.pdfButton = pdfButton;
        this.csvButton = csvButton;
        this.pdfButton.setOnAction(event -> showPdfPlaceholder());
        this.csvButton.setOnAction(event -> showCsvPlaceholder());
    }

    public void setExportData(StudentInfo studentInfo,
                              LocalCacheService.CacheSnapshot cacheSnapshot) {
        this.studentInfo = studentInfo;
        this.cacheSnapshot = cacheSnapshot;
    }

    public void setExportData(StudentInfo studentInfo,
                              LocalCacheService.CacheSnapshot cacheSnapshot,
                              int acquiredCredits, int requiredCredits,
                              String averageGrade) {
        setExportData(studentInfo, cacheSnapshot);
        this.acquiredCredits = acquiredCredits;
        this.requiredCredits = requiredCredits;
        this.averageGrade = valueOrDash(averageGrade);
    }

    private void showPdfPlaceholder() {
        exportPdf();
    }

    private void showCsvPlaceholder() {
        exportCsv();
    }

    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportovať študijný prehľad do CSV");
        chooser.setInitialFileName(createFileName());
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV súbory (*.csv)", "*.csv"));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    file.toPath(), StandardCharsets.UTF_8)) {
                writer.write('\uFEFF');
                writer.write(createCsv());
            }
            UiDialogs.showExportSuccess(window(), "CSV export bol úspešne vytvorený.");
        } catch (IOException | RuntimeException e) {
            UiDialogs.showExportError(window(), "CSV export sa nepodarilo vytvoriť.");
        }
    }

    private void exportPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportovať študijný prehľad do PDF");
        chooser.setInitialFileName(createFileName().replace(".csv", ".pdf"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF súbory (*.pdf)", "*.pdf"));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            createPdf(file.toPath());
            UiDialogs.showExportSuccess(window(), "PDF report bol úspešne vytvorený.");
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            UiDialogs.showExportError(window(), "PDF report sa nepodarilo vytvoriť.");
        }
    }

    private void createPdf(Path path) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDFont regular = loadFont(document, false);
            PDFont bold = loadFont(document, true);
            PdfWriter writer = new PdfWriter(document, regular, bold);
            writer.title("Študijný prehľad");
            writer.line("Študent: " + valueOrDash(studentInfo == null
                    ? null : studentInfo.getFullName()));
            writer.line("Získané kredity: " + acquiredCredits);
            int target = requiredCredits > 0 ? requiredCredits : 180;
            writer.line("Požadované kredity: " + target);
            writer.line("Zostáva: " + Math.max(0, target - acquiredCredits));
            writer.line("Splnenie: " + String.format("%.1f %%",
                    Math.min(100, acquiredCredits * 100.0 / target)));
            writer.line("Vážený priemer: " + averageGrade);
            writer.spacer();

            if (studentInfo != null && cacheSnapshot != null
                    && cacheSnapshot.getEnrollmentData() != null
                    && studentInfo.getEnrollmentListIds() != null) {
                List<String> ids = studentInfo.getEnrollmentListIds();
                List<String> names = studentInfo.getEnrollmentListNames();
                for (int i = 0; i < ids.size(); i++) {
                    String name = names != null && i < names.size() ? names.get(i) : ids.get(i);
                    LocalCacheService.EnrollmentData data =
                            cacheSnapshot.getEnrollmentData().get(ids.get(i));
                    if (data == null || data.getSubjects() == null
                            || data.getSubjects().isEmpty()) {
                        continue;
                    }
                    String sectionTitle = extractAcademicYear(name) + " — "
                            + extractEnrollmentCode(name);
                    String[] headers = new String[]{"Kategória", "Predmet", "Kód",
                            "Kredity", "Semester", "Známka"};
                    float[] widths = new float[]{145, 225, 65, 55, 60, 180};
                    int subjectCount = (int) data.getSubjects().stream()
                            .filter(subject -> subject != null)
                            .count();
                    writer.startTable(sectionTitle, headers, widths,
                            Math.min(3, subjectCount));
                    for (Subject subject : data.getSubjects()) {
                        if (subject != null) {
                            writer.tableRow(new String[]{valueOrDash(subject.getCategory()),
                                    valueOrDash(subject.getName()),
                                    valueOrDash(subject.getAbbreviation()),
                                    valueOrDash(subject.getCredits()),
                                    valueOrDash(subject.getSemester()),
                                    valueOrDash(subject.getGrade())},
                                    new float[]{145, 225, 65, 55, 60, 180});
                        }
                    }
                    writer.spacer();
                }
                addExams(writer);
            }
            writer.close();
            document.save(path.toFile());
        }
    }

    private void addExams(PdfWriter writer) throws IOException {
        boolean hasExams = cacheSnapshot.getEnrollmentData().values().stream()
                .anyMatch(data -> data != null && data.getExams() != null
                        && !data.getExams().isEmpty());
        if (!hasExams) {
            return;
        }
        writer.startTable("Skúšky",
                new String[]{"Predmet", "Dátum", "Čas", "Miestnosť"},
                new float[]{300, 110, 90, 230}, 1);
        for (LocalCacheService.EnrollmentData data : cacheSnapshot.getEnrollmentData().values()) {
            if (data == null || data.getExams() == null) {
                continue;
            }
            for (Exam exam : data.getExams()) {
                if (exam != null) {
                    writer.tableRow(new String[]{valueOrDash(exam.getSubjectName()),
                                    valueOrDash(exam.getDate()), valueOrDash(exam.getTime()),
                                    valueOrDash(exam.getRoom())},
                            new float[]{300, 110, 90, 230});
                }
            }
        }
    }

    private PDFont loadFont(PDDocument document, boolean bold) throws IOException {
        String fileName = bold ? "arialbd.ttf" : "arial.ttf";
        String windows = System.getenv("WINDIR");
        Path fontPath = Path.of(windows == null ? "C:\\Windows\\Fonts" : windows + "\\Fonts",
                fileName);
        if (!Files.isRegularFile(fontPath)) {
            fontPath = Path.of(windows == null ? "C:\\Windows\\Fonts" : windows + "\\Fonts",
                    bold ? "segoeuib.ttf" : "segoeui.ttf");
        }
        if (!Files.isRegularFile(fontPath)) {
            throw new IOException("Chýba font s podporou slovenskej diakritiky: " + fontPath);
        }
        return PDType0Font.load(document, fontPath.toFile());
    }

    private final class PdfWriter {
        private final PDDocument document;
        private final PDFont regular;
        private final PDFont bold;
        private PDPageContentStream stream;
        private float y;
        private String[] lastHeader;
        private float[] lastWidths;
        private String lastSection;
        private static final float LEFT = 36;
        private static final float TOP = 48;
        private static final float BOTTOM = 48;

        private PdfWriter(PDDocument document, PDFont regular, PDFont bold) throws IOException {
            this.document = document;
            this.regular = regular;
            this.bold = bold;
            newPage();
        }

        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            page.setRotation(90);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PDRectangle.A4.getWidth() - TOP;
        }

        private void ensure(float height) throws IOException {
            if (y - height < BOTTOM) {
                newPage();
                if (lastHeader != null) {
                    drawContinuationSection();
                    drawTableHeader();
                }
            }
        }

        private void title(String text) throws IOException {
            ensure(32);
            text(text, bold, 22);
            y -= 30;
        }

        private void section(String text) throws IOException {
            lastHeader = null;
            ensure(28);
            text(text, bold, 14);
            y -= 20;
        }

        private void startTable(String section, String[] headers, float[] widths,
                                int minimumRows) throws IOException {
            lastHeader = null;
            lastWidths = null;
            lastSection = null;
            float required = 28 + 6 + 22 + Math.max(0, minimumRows) * 18;
            ensure(required);
            lastSection = section;
            lastHeader = headers;
            lastWidths = widths;
            text(section, bold, 14);
            y -= 26;
            drawTableHeader();
        }

        private void line(String text) throws IOException {
            ensure(16);
            text(text, regular, 10);
            y -= 14;
        }

        private void spacer() {
            y -= 10;
        }

        private void text(String value, PDFont font, float size) throws IOException {
            stream.beginText();
            stream.setNonStrokingColor(25, 25, 25);
            stream.setFont(font, size);
            stream.newLineAtOffset(LEFT, y);
            stream.showText(value);
            stream.endText();
        }

        private void drawTableHeader() throws IOException {
            stream.setNonStrokingColor(225, 232, 240);
            stream.addRect(LEFT, y - 16, sum(lastWidths), 20);
            stream.fill();
            float x = 40;
            for (int i = 0; i < lastHeader.length; i++) {
                textAt(lastHeader[i], bold, 8, x, y - 11, lastWidths[i] - 8);
                x += lastWidths[i];
            }
            y -= 22;
        }

        private void tableRow(String[] values, float[] widths) throws IOException {
            ensure(18);
            float x = 40;
            for (int i = 0; i < values.length; i++) {
                textAt(values[i], regular, 8, x, y - 11, widths[i] - 8);
                x += widths[i];
            }
            stream.setStrokingColor(185, 185, 185);
            stream.moveTo(LEFT, y - 15);
            stream.lineTo(LEFT + sum(widths), y - 15);
            stream.stroke();
            y -= 18;
        }

        private void textAt(String value, PDFont font, float size, float x,
                            float baseline, float maxWidth) throws IOException {
            String clipped = valueOrDash(value);
            while (clipped.length() > 1
                    && font.getStringWidth(clipped) / 1000 * size > maxWidth) {
                clipped = clipped.substring(0, clipped.length() - 2) + "…";
            }
            stream.beginText();
            stream.setNonStrokingColor(25, 25, 25);
            stream.setFont(font, size);
            stream.newLineAtOffset(x, baseline);
            stream.showText(clipped);
            stream.endText();
        }

        private void drawContinuationSection() throws IOException {
            if (lastSection == null) {
                return;
            }
            text(lastSection + " (pokračovanie)", bold, 11);
            y -= 22;
        }

        private float sum(float[] widths) {
            float total = 0;
            for (float width : widths) {
                total += width;
            }
            return total;
        }

        private void close() throws IOException {
            stream.close();
        }
    }

    private String createCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("Akademický rok;Zápisný list;Kategória;Názov predmetu;"
                + "Kód predmetu;Kredity;Semester;Typ hodnotenia;Známka;Učiteľ\n");

        if (studentInfo == null || cacheSnapshot == null
                || cacheSnapshot.getEnrollmentData() == null) {
            return csv.toString();
        }

        List<String> ids = studentInfo.getEnrollmentListIds();
        List<String> names = studentInfo.getEnrollmentListNames();
        if (ids == null) {
            return csv.toString();
        }

        Map<String, LocalCacheService.EnrollmentData> enrollmentData =
                cacheSnapshot.getEnrollmentData();
        for (int i = 0; i < ids.size(); i++) {
            String id = valueOrDash(ids.get(i));
            String enrollmentName = names != null && i < names.size()
                    ? names.get(i) : id;
            String academicYear = extractAcademicYear(enrollmentName);
            String enrollmentCode = extractEnrollmentCode(enrollmentName);
            LocalCacheService.EnrollmentData data = enrollmentData.get(ids.get(i));
            if (data == null || data.getSubjects() == null) {
                continue;
            }

            for (Subject subject : data.getSubjects()) {
                if (subject == null) {
                    continue;
                }
                appendRow(csv, academicYear, enrollmentCode, subject);
            }
        }
        return csv.toString();
    }

    private void appendRow(StringBuilder csv, String academicYear,
                           String enrollmentCode, Subject subject) {
        List<String> values = new ArrayList<>();
        values.add(academicYear);
        values.add(enrollmentCode);
        values.add(subject.getCategory());
        values.add(subject.getName());
        values.add(subject.getAbbreviation());
        values.add(subject.getCredits());
        values.add(subject.getSemester());
        values.add(subject.getGradeType());
        values.add(subject.getGrade());
        values.add(subject.getTeacher());
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(';');
            }
            csv.append(escape(values.get(i)));
        }
        csv.append('\n');
    }

    private String extractAcademicYear(String enrollmentName) {
        if (enrollmentName == null || enrollmentName.isBlank()) {
            return "-";
        }
        int openingBracket = enrollmentName.lastIndexOf('(');
        return openingBracket > 0
                ? valueOrDash(enrollmentName.substring(0, openingBracket).trim())
                : valueOrDash(enrollmentName);
    }

    private String extractEnrollmentCode(String enrollmentName) {
        if (enrollmentName == null || enrollmentName.isBlank()) {
            return "-";
        }
        int openingBracket = enrollmentName.lastIndexOf('(');
        int closingBracket = enrollmentName.lastIndexOf(')');
        if (openingBracket >= 0 && closingBracket > openingBracket) {
            String code = enrollmentName.substring(openingBracket + 1, closingBracket).trim();
            if (!code.isBlank()) {
                return code;
            }
        }
        return valueOrDash(enrollmentName);
    }

    private String createFileName() {
        String name = studentInfo == null ? null : studentInfo.getFullName();
        if (name == null || name.isBlank()) {
            return "studijny_prehlad.csv";
        }
        String safeName = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return (safeName.isBlank() ? "studijny_prehlad" : safeName)
                + "_studijny_prehlad.csv";
    }

    private String escape(String value) {
        String normalized = valueOrDash(value);
        if (normalized.contains(";") || normalized.contains("\"")
                || normalized.contains("\n") || normalized.contains("\r")) {
            return "\"" + normalized.replace("\"", "\"\"") + "\"";
        }
        return normalized;
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private Window window() {
        return pdfButton.getScene().getWindow();
    }
}
