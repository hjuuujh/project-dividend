package zerobase.projectdividend.scraper;

import zerobase.projectdividend.model.Company;
import zerobase.projectdividend.model.ScrapedResult;

public interface Scraper {
    Company scrapCompanyByTicker(String ticker);
    ScrapedResult scrap(Company company);
}
