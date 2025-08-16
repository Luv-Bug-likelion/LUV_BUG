package likelion.traditional_market.Receipt.Dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
public class ExtractedFields {
    private String merchantName;
    private LocalDate visitDate;
    private Integer totalAmount;
}
