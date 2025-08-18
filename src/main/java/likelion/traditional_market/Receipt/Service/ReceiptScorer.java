package likelion.traditional_market.Receipt.Service;

import likelion.traditional_market.Receipt.Dto.ExtractedFields;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReceiptScorer {
    public int score(ExtractedFields ex, List<String> keywordHits) {
        int s = 0;
        int k=(keywordHits==null) ? 0 : keywordHits.size();
        if(k>=6) {
            s+=7;
        }
        else if (k>=2) {
            s+=2;
        }
        if (ex.getVisitDate() != null) s += 2;
        if (ex.getSpentAmount() != null) s += 3;

        return s;
    }
}


