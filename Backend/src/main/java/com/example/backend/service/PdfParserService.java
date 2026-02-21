package com.example.backend.service;

import com.example.backend.dto.ForensicReportDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfParserService {
    public ForensicReportDTO parse(String text) {
        text = normalize(text);
        String[] addressEmailPhone = extractAddressEmailPhone(text);

        return ForensicReportDTO.builder()
                .organizationName(extractOrganization(text))
                .address(addressEmailPhone[0])
                .email(addressEmailPhone[1])
                .phone(addressEmailPhone[2])
                .fileName(extractFileName(text))
                .classification(extractClassification(text))
                .hash(extractHash(text))
                .threatName(extractThreatName(text))
                .behaviorDescription(extractBehavior(text))
                .forensicExpert1(extractFirstExpert(text))
                .forensicExpert2(extractSecondExpert(text))
                .build();
    }

    private String extractOrganization(String text) {
        Pattern pattern = Pattern.compile("Organizacija:\\s*<([^>]+)>");
        return matchGroup(pattern, text, 1);
    }

    private String[] extractAddressEmailPhone(String text) {
        Pattern pattern = Pattern.compile(
                "<([^>]+)> \\(format:[^\\n]+\\)\\s*\\n<([^>]+)>\\s*\\n<([^>]+)>"
        );
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return new String[]{
                    matcher.group(1).trim(), // address
                    matcher.group(2).trim(), // email
                    matcher.group(3).trim()  // phone
            };
        }
        return new String[]{null, null, null};
    }

    private String extractFileName(String text) {
        Pattern pattern = Pattern.compile("\\n<([^>]+)>\\s*\\n\\s*Klasifikacija:");
        return matchGroup(pattern, text, 1);
    }

    private String extractClassification(String text) {
        Pattern pattern = Pattern.compile("Klasifikacija:\\s*<([^>]+)>,");
        return matchGroup(pattern, text, 1);
    }

    private String extractHash(String text) {
        Pattern pattern = Pattern.compile(",\\s*<([^>]+)>");
        return matchGroup(pattern, text, 1);
    }
    private String extractThreatName(String text) {
        Pattern pattern = Pattern.compile("ukazuje na\\s*<([^>]+)>");
        return matchGroup(pattern, text, 1);
    }

    private String extractBehavior(String text) {
        Pattern pattern = Pattern.compile("Opis ponašanja malvera/pretnje:\\s*\\n\\s*<([^>]+)>",
                Pattern.DOTALL);
        return matchGroup(pattern, text, 1);
    }

    private String extractFirstExpert(String text) {
        Pattern pattern = Pattern.compile("<([^>]+)>\\s+<([^>]+)>\\s+_{5,}\\s+Potpis forenzičara");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1) + " " + matcher.group(2);
        }
        return null;
    }

    private String extractSecondExpert(String text) {
        Pattern pattern = Pattern.compile("<([^>]+)>\\s+<([^>]+)>\\s+_{5,}\\s+Potpis drugog forenzičara");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1) + " " + matcher.group(2);
        }
        return null;
    }

    private String matchGroup(Pattern pattern, String text, int group) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(group).trim();
        }
        return null;
    }

    private String normalize(String text) {
        return text.replace("\r", "")
                .replaceAll("[ \\t]+", " ")
                .trim();
    }
}
