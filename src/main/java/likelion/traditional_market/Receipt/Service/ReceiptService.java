package likelion.traditional_market.Receipt.Service;

import likelion.traditional_market.Receipt.Dto.ExtractedFields;
import likelion.traditional_market.Receipt.Dto.ReceiptCheckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReceiptService {
    private final ClovaOcrClient clovaOcrClient;
    private final ReceiptParser receiptParser;
    private final ReceiptScorer receiptScorer;
    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    public ReceiptCheckResponse handleReceiptCheck(MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) {
                return ReceiptCheckResponse.fail("이미지가 비어있습니다.");
            }
            byte[] imgBytes = image.getBytes();
            String filename = image.getOriginalFilename() == null ? "unknown.jpg" : image.getOriginalFilename();
            String ocrJson = clovaOcrClient.recognize(filename, imgBytes);

            ExtractedFields ex = receiptParser.parse(ocrJson);

            List<String> hits = receiptParser.findKeywordHits(ocrJson);
            int score = receiptScorer.score(ex, hits);

            if (ex.getVisitDate() != null && ex.getVisitDate().isAfter(LocalDate.now(ZONE_SEOUL))) {
                return ReceiptCheckResponse.builder()
                        .status("FAIL").score(score)
                        .merchantName(ex.getMerchantName())
                        .visitDate(ex.getVisitDate())
                        .totalAmount(ex.getTotalAmount())
                        .message("영수증 날짜가 미래입니다. 재촬영해주세요.")
                        .build();
            }
            boolean hasCoreField = (ex.getTotalAmount() != null) || (ex.getVisitDate() != null);
            if (!hasCoreField) {
                return ReceiptCheckResponse.builder()
                        .status("FAIL").score(score)
                        .merchantName(ex.getMerchantName())
                        .visitDate(ex.getVisitDate())
                        .totalAmount(ex.getTotalAmount())
                        .message("핵심 정보(날짜/금액)를 인식하지 못했습니다. 재촬영해주세요.")
                        .build();
            }
            String status = (score>=7) ? "SUCCESS" : "FAIL";

            return ReceiptCheckResponse.builder()
                    .status(status)
                    .score(score)
                    .merchantName(ex.getMerchantName())
                    .visitDate(ex.getVisitDate())
                    .totalAmount(ex.getTotalAmount())
                    .message(status.equals("FAIL") ? "영수증 인식이 충분하지 않습니다. 재촬영해주세요." : null)
                    .build();

        } catch (Exception e) {
            return ReceiptCheckResponse.fail("OCR 처리 실패: " + e.getMessage());
        }
    }
}

