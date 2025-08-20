package likelion.traditional_market.KakaoMap.dto;

import lombok.*;
import org.apache.catalina.Store;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketStoresResponse {
    private String marketName;
    private String signPost;
    private List<StoreInfoDto> meat;
    private List<StoreInfoDto> fish;
    private List<StoreInfoDto> vegetable;
    private List<StoreInfoDto> fruit;
}
