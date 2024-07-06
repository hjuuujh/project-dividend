package zerobase.projectdividend.scraper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import zerobase.projectdividend.exception.impl.NotExistCompanyWithTickerException;
import zerobase.projectdividend.model.Company;
import zerobase.projectdividend.model.Dividend;
import zerobase.projectdividend.model.ScrapedResult;
import zerobase.projectdividend.model.constants.Month;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class YahooFinanceScraper implements Scraper{
    private static final String STATISTICS_URL = "https://finance.yahoo.com/quote/%s/history/?p=%s&frequency=1mo&period1=%d&period2=%d";
    private static final String SUMMARY_URL = "https://finance.yahoo.com/quote/%s/?p=%s";

    private static final long START_TIME = 86400; // 68*60*24

    @Override
    public ScrapedResult scrap(Company company){
        var scrapResult = new ScrapedResult();
        scrapResult.setCompany(company);
        try {
            long start = START_TIME;
            long end = System.currentTimeMillis()/1000;
            String url = String.format(STATISTICS_URL, company.getTicker(),company.getTicker(), start, end);

            Connection connect = Jsoup.connect(url);
            Document doc = connect.get();

            Elements parsingDivs = doc.getElementsByClass("table svelte-ewueuo");
            Element tableEle = parsingDivs.get(0);


            Element tbody = tableEle.children().get(1);
            List<Dividend> dividends = new ArrayList<>();
            for (Element e : tbody.children()) {
                String txt = e.text();
                if (!txt.endsWith("Dividend")) {
                    continue;
                }
                String[] splits = txt.split(" ");
                int  month = Month.stringToNumber(splits[0]);
                int day = Integer.valueOf(splits[1].replace(",", ""));
                int year = Integer.valueOf(splits[2]);
                String dividend = splits[3];

                if(month<0){
                    throw new RuntimeException("Unexpected Month Value: " + splits[0]);
                }

                dividends.add(new Dividend(LocalDateTime.of(year, month, day, 0, 0), dividend));

            }
            scrapResult.setDividends(dividends);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return scrapResult;
    }

    @Override
    public Company scrapCompanyByTicker(String ticker){
        String url = String.format(SUMMARY_URL, ticker, ticker);

        try{
            Document document = Jsoup.connect(url).get();
            Element titleElement = document.getElementsByClass("svelte-3a2v0c").get(1);
            String title = titleElement.text().split("\\(")[0].trim();

            return new Company(ticker, title);
        }catch (IOException e){
            e.printStackTrace();
        }
        catch (IndexOutOfBoundsException e){
            throw new NotExistCompanyWithTickerException();
        }
        return null;

    }
}
