package likelion.traditional_market.CreateMission.Service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class PriceService {

    private final Map<String, Map<String, Integer>> priceData = new HashMap<>();
    private final String csvFile = "경기도 부천시_생필품가격 현황_20250514.csv";

    @PostConstruct
    public void init() {
        try (CSVReader reader = new CSVReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(csvFile))))) {
            String[] header = reader.readNext();
            if (header == null || header.length < 2) {
                return;
            }
            int itemColumnIndex = 0;
            String[] line;
            while ((line = reader.readNext()) != null) {
                String item = line[itemColumnIndex];
                Map<String, Integer> marketPrice = new HashMap<>();
                for (int i = 2; i < header.length; i++) {
                    try {
                        int price = Integer.parseInt(line[i].trim());
                        marketPrice.put(header[i], price);
                    } catch (NumberFormatException e) {
                        marketPrice.put(header[i], 0);
                    }
                }
                priceData.put(item, marketPrice);
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
    }

    public int getPrice(String item, String market) {
        if (priceData.containsKey(item)) {
            return priceData.get(item).getOrDefault(market, 0);
        }
        return 0;
    }
}
