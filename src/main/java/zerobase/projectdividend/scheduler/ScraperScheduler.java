package zerobase.projectdividend.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import zerobase.projectdividend.model.Company;
import zerobase.projectdividend.model.ScrapedResult;
import zerobase.projectdividend.model.constants.CacheKey;
import zerobase.projectdividend.persist.entity.CompanyEntity;
import zerobase.projectdividend.persist.entity.DividendEntity;
import zerobase.projectdividend.persist.repository.CompanyRepository;
import zerobase.projectdividend.persist.repository.DividendRepository;
import zerobase.projectdividend.scraper.Scraper;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
@EnableCaching
public class ScraperScheduler {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;
    private final Scraper yahooFinanceScraper;

    // 일정 주기마다 수행
    @CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true)
    // value : key의 prefix allEntries : finance에 해당하는 캐시 전부 삭제
    @Scheduled(cron = "${scheduler.scrap.yahoo}")
    public void yahooFinanceScheduling() {
        log.info("Scraping scheduler is started");
        // 저장된 회사 목록 조회
        List<CompanyEntity> companies = companyRepository.findAll();

        // 회사마다 배당금 정보 새로 스크래핑
        for (CompanyEntity company : companies) {
            log.info("Scraping scheduler is started -> {}", company.getName());
            ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(new Company(company.getTicker(), company.getName()));

            // 스크래핑한 대방금 정보 중 데이터베이스에 없는 값은 저장
            scrapedResult.getDividends().stream()
                    // 디비든 모델을 디비든 엔티티로 매핑
                    .map(e -> new DividendEntity((company.getId()), e))
                    // 엘리먼트를 없으면 하나씩 디비든 레파지토리에 삽입
                    .forEach(e -> {
                        boolean exists = this.dividendRepository.existsByCompanyIdAndDate(e.getCompanyId(), e.getDate());
                        if (!exists) {
                            this.dividendRepository.save(e);
                            log.info("insert new dividend -> " + e.toString());
                        }
                    });

            // 연속적으로 스크래핑 대상 사이트 서버에 요청을 날리지 않도록 일시 정지
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }


        }

    }

}
