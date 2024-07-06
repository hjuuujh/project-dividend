package zerobase.projectdividend.service;

import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import zerobase.projectdividend.exception.impl.NoCompanyException;
import zerobase.projectdividend.model.Company;
import zerobase.projectdividend.model.Dividend;
import zerobase.projectdividend.model.ScrapedResult;
import zerobase.projectdividend.model.constants.CacheKey;
import zerobase.projectdividend.persist.entity.CompanyEntity;
import zerobase.projectdividend.persist.entity.DividendEntity;
import zerobase.projectdividend.persist.repository.CompanyRepository;
import zerobase.projectdividend.persist.repository.DividendRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class FinanceService {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    // 캐싱이 필요한가?
    // -> 요청이 자주 들어오는가? Y
    // -> 자주 변경되는 데이터 인가? N
    // redis 에 저장되면 repository를 이용해 가져오지않고 캐시서버에서 가져옴
    @Cacheable(key = "#companyName", value = CacheKey.KEY_FINANCE)
    public ScrapedResult getDividendByCompanyName(String companyName) {

        // 1. 회사명을 기준으로 회사 정보 조회
        CompanyEntity company = this.companyRepository.findByName(companyName)
                .orElseThrow(() -> new NoCompanyException());

        // 2. 조회된 회사 ID로 배당금 반환
        List<DividendEntity> dividendEntities = this.dividendRepository.findAllByCompanyId(company.getId());

        // 3. 결과 조합 후 변환
        //        List<Dividend> dividends = new ArrayList<>();
//        for(var entity : dividendEntities) {
//            dividends.add(Dividend.builder()
//                    .date(entity.getDate())
//                    .dividend(entity.getDividend())
//                    .build());
//        }

        List<Dividend> dividends = dividendEntities.stream()
                .map(e -> new Dividend(e.getDate(), e.getDividend())).collect(Collectors.toList());

        return new ScrapedResult(new Company(company.getTicker(), company.getName()),
                dividends
        );
    }
}
