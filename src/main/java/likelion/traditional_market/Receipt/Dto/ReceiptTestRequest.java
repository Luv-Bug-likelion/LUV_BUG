package likelion.traditional_market.Receipt.Dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class ReceiptTestRequest {
    private String userKey;
    private int missionId;
    private String merchantName;
    private LocalDate visitDate;
    private Integer spentAmount;
    private List<String> keywordHits;
}