package likelion.traditional_market.KakaoMap.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MarketStoreListResponse {
    private String marketName;
    private List<StoreInfoDto> stores;
}