package likelion.traditional_market.Receipt.Dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ReceiptCheckResponse {
    private String status;
    private Integer score;
    private String merchantName;
    private java.time.LocalDate visitDate;
    private Integer totalAmount;
    private String message;

    public static ReceiptCheckResponse success(String status, int score, ExtractedFields ex, List<String> reasons) {
        return ReceiptCheckResponse.builder()
                .status(status)
                .score(score)
                .merchantName(ex.getMerchantName())
                .visitDate(ex.getVisitDate())
                .totalAmount(ex.getTotalAmount())
                .message(reasons == null || reasons.isEmpty() ? null : String.join("; ", reasons))
                .build();
    }
    public static ReceiptCheckResponse fail(String msg) {
        return ReceiptCheckResponse.builder()
                .status("FAIL").score(0).message(msg).build();
    }
}
