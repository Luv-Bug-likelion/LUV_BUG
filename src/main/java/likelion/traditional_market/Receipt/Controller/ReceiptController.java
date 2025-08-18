package likelion.traditional_market.Receipt.Controller;

import likelion.traditional_market.Receipt.Dto.ExtractedFields;
import likelion.traditional_market.Receipt.Dto.ReceiptCheckResponse;
import likelion.traditional_market.Receipt.Dto.ReceiptTestRequest;
import likelion.traditional_market.Receipt.Service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/mission")

public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping(value="/receiptcheck", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReceiptCheckResponse check(
            @RequestParam("image") @NotNull MultipartFile image,
            @RequestParam("user_key") @NotBlank String userkey,
            @RequestParam("mission_id") @NotNull Integer missionid

    )
    {
        return receiptService.handleReceiptCheck(image, userkey, missionid);
    }

    @PostMapping("/receipt-test")
    public ResponseEntity<ReceiptCheckResponse> checkTest(@RequestBody ReceiptTestRequest req) {
        ExtractedFields fields = ExtractedFields.builder()
                .merchantName(req.getMerchantName())
                .visitDate(req.getVisitDate())
                .spentAmount(req.getSpentAmount())
                .build();

        ReceiptCheckResponse response = receiptService.handleReceiptTest(req.getUserKey(), req.getMissionId(), fields, req.getKeywordHits());
        return ResponseEntity.ok(response);
    }
}
