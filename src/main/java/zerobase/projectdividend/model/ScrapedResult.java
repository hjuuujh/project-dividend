package zerobase.projectdividend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class ScrapedResult {

    private Company company;
    private List<Dividend> dividends;

    public ScrapedResult() {
        this.dividends = new ArrayList<>();
    }
}
