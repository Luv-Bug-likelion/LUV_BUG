package likelion.traditional_market.Receipt.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@JsonPropertyOrder({"code", "message", "status", "score", "merchantName", "visitDate", "totalAmount"})
public class ReceiptCheckResponse {

    private int code;
    private String message;

    private String status;
    private Integer score;
    private String merchantName;
    private LocalDate visitDate;

    @JsonProperty("totalAmount")
    private Integer totalAmount;

    public static ReceiptCheckResponse success(String message, int score, ExtractedFields ex) {
        return ReceiptCheckResponse.builder()
                .code(200)
                .message(message)
                .status("SUCCESS")
                .score(score)
                .merchantName(ex.getMerchantName())
                .visitDate(ex.getVisitDate())
                .totalAmount(ex.getSpentAmount()) // totalAmount에서 spentAmount로 변경
                .build();
    }

    public static ReceiptCheckResponse fail(String message) {
        return ReceiptCheckResponse.builder()
                .code(400) // 예시: 실패 시 400 코드 사용
                .message(message)
                .status("FAIL")
                .score(0)
                .build();
    }
}
