package likelion.traditional_market.Receipt.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;
import java.util.*;

@Component
public class ClovaOcrClient {
    private final WebClient webClient = WebClient.builder().build();

    @Value("${naver.clova.ocr.url}")
    private String invokeUrl;
    @Value("${naver.clova.ocr.secret}")
    private String secretKey;
    @Value("${naver.clova.ocr.mock}")
    private boolean mock;

    public String recognize(String filename, byte[] img) {
//        if (mock) {
//            return """
//            {"images":[{"fields":[
//              {"inferText":"영수증"},{"inferText":"엄지농산물"},
//              {"inferText":"결제일시 2025-08-09"},
//              {"inferText":"합계 8,000"}
//            ]}]}""";
//        }

        String fmt = detectFormat(filename);
        String base64 = Base64.getEncoder().encodeToString(img);

        Map<String, Object> payload = Map.of(
                "version", "V2",
                "requestId", UUID.randomUUID().toString(),
                "timestamp", System.currentTimeMillis(),
                "images", List.of(Map.of(
                        "format", fmt,
                        "name", "receipt",
                        "data", base64
                ))
        );

        return webClient.post()
                .uri(invokeUrl)
                .header("X-OCR-SECRET", secretKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("empty body")
                                .flatMap(body -> Mono.error(new RuntimeException
                                        ("CLOVA OCR error: " + resp.statusCode() + " / " + body))))
                .bodyToMono(String.class)
                .block();
    }

    private String detectFormat(String filename) {
        if (filename == null || filename.isBlank()) {
            return "jpg"; // 기본값
        }

        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "jpg"; // 확장자 없으면 기본값
        }

        String ext = filename.substring(dotIndex + 1).toLowerCase();
        if (ext.equals("png")) return "png";
        if (ext.equals("jpg") || ext.equals("jpeg")) return "jpg";
        return "jpg"; // 지원하지 않는 확장자는 기본 jpg
    }
}
