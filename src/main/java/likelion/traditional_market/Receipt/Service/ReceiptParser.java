package likelion.traditional_market.Receipt.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.traditional_market.Receipt.Dto.ExtractedFields;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReceiptParser {
    private final ObjectMapper mapper = new ObjectMapper();

    //영수증성 키워드(스코어링 용)
    private static final List<String> KEYWORDS = List.of(
            "영수증", "상품명", "메뉴명","신용카드매출전표", "부가세",
            "합계","현금영수증","승인번호","단가","수량","결제금액","금액","매장명","주소");
    private static final Pattern P_AMOUNT_KEYED = Pattern.compile("(합계|총\\s*금액|결제\\s*금액)[^0-9]*([0-9,]+)");
    private static final Pattern P_AMOUNT_FREE  = Pattern.compile("([₩]?[0-9]{1,3}(?:,[0-9]{3})+|[0-9]{4,})");
    private static final String[] DATE_PATTERNS = new String[] {
            "(20\\d{2})[-./](\\d{1,2})[-./](\\d{1,2})",
            "(\\d{2})[-./](\\d{1,2})[-./](\\d{1,2})",
            "(20\\d{2})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일"
    };
    public ExtractedFields parse(String ocrJson){
        try{
            JsonNode root = mapper.readTree(ocrJson);
            JsonNode images = root.get("images");
            if (!images.isArray() || images.isEmpty()) {
                throw new IllegalArgumentException("OCR 응답에 images가 없습니다.");
            }
            JsonNode fields = images.get(0).path("fields");
            List<String> tokens = new ArrayList<>();
            for(JsonNode field : fields){
                String t = field.path("inferText").asText("");
                if(!t.isBlank()) tokens.add(t);
            }

            String all = String.join(" ", tokens).replaceAll("\\s+", " ");

            Integer total = findAmount(all);
            LocalDate date = findDate(all);
            String merchant = pickMerchantName(tokens);

            return ExtractedFields.builder()
                    .merchantName(merchant)
                    .visitDate(date)
                    .totalAmount(total)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("parse failed", e);
        }
    }

    public List<String> findKeywordHits(String ocrJson) {
        try {
            JsonNode root = mapper.readTree(ocrJson);
            JsonNode fields = root.path("images").get(0).path("fields");
            Set<String> hits = new LinkedHashSet<>();

            List<String> loweredKeywords = KEYWORDS.stream()
                    .map(k -> k.toLowerCase())
                    .toList();
            List<String> loweredKeywordsNoSpace = KEYWORDS.stream()
                    .map(k -> k.toLowerCase().replace(" ", ""))
                    .toList();

            for (JsonNode f : fields) {
                String t = f.path("inferText").asText("");
                if (t.isBlank()) continue;

                String s = t.toLowerCase();
                String sNoSpace = s.replace(" ", "");

                for (int i = 0; i < loweredKeywords.size(); i++) {
                    String k = loweredKeywords.get(i);
                    String kNoSpace = loweredKeywordsNoSpace.get(i);
                    if (s.contains(k) || sNoSpace.contains(kNoSpace)) {
                        hits.add(KEYWORDS.get(i));
                    }
                }
            }
            return new ArrayList<>(hits);
        }
        catch (Exception e) { return List.of(); }
    }

    private Integer findAmount(String text) {
        Matcher m = P_AMOUNT_KEYED.matcher(text);
        if (m.find()) return normalizeMoney(m.group(2));
        int best = -1;
        Matcher m2 = P_AMOUNT_FREE.matcher(text);
        while (m2.find()) {
            int v = normalizeMoney(m2.group(1));
            if (v > best && v < 5_000_000) best = v;
        }
        return best == -1 ? null : best;
    }
    private LocalDate findDate(String text) {
        for (String p : DATE_PATTERNS) {
            Matcher m = Pattern.compile(p).matcher(text);
            if (m.find()) {
                int y, mo, d;
                if (p.startsWith("(\\d{2})")) {
                    y = 2000 + Integer.parseInt(m.group(1));
                    mo = Integer.parseInt(m.group(2));
                    d = Integer.parseInt(m.group(3)); }
                else {
                    y = Integer.parseInt(m.group(1));
                    mo = Integer.parseInt(m.group(2));
                    d = Integer.parseInt(m.group(3)); }
                try { return java.time.LocalDate.of(y, mo, d); } catch (Exception ignore) {}
            }
        }
        return null;
    }
    private int normalizeMoney(String raw) {
        return Integer.parseInt(raw.replaceAll("[^0-9]", "")); }

    private String pickMerchantName(List<String> tokens) {
        for (String t : tokens) {
            String s = t.trim();
            if (s.length() >= 2 && !s.matches(
                    ".*(영수증|고객용|매출전표|카드|일시불|신용|No|합계|부가세|금액).*")) {
                return s;
            }
        }
        return null;
    }
}
