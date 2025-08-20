package likelion.traditional_market.KakaoMap.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StoreInfoDto {
    private String name;
    private String address;
    private String phoneNumber;
    private String x;
    private String y;
    private String industry;
    private String subwayName;
    private String subwayDistance;
}
