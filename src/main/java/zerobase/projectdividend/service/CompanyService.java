package zerobase.projectdividend.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.Trie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import zerobase.projectdividend.exception.impl.*;
import zerobase.projectdividend.model.Company;
import zerobase.projectdividend.model.ScrapedResult;
import zerobase.projectdividend.persist.entity.CompanyEntity;
import zerobase.projectdividend.persist.entity.DividendEntity;
import zerobase.projectdividend.persist.repository.CompanyRepository;
import zerobase.projectdividend.persist.repository.DividendRepository;
import zerobase.projectdividend.scraper.Scraper;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;
    private final Scraper yahooFinanceScraper;

    private final Trie trie;

    public Company save(String ticker) {

        // ticker를 이용해 회사 존재여부를 먼저 확인
        boolean exists = this.companyRepository.existsByTicker(ticker);
        if (exists) {
            throw new AlreadyExistCompanyException();
        }

        // 없으면 스크래핑해서 저장
        return this.storeCompanyAndDividend(ticker);
    }

    public Company storeCompanyAndDividend(String ticker) {
        // ticker 를 기준으로 회사를 스크래핑
        Company company = this.yahooFinanceScraper.scrapCompanyByTicker(ticker);
        if (ObjectUtils.isEmpty(company)) {
            throw new FailToScrapException();
        }

        // 해당 회사 존재할 경우 회사의 배당금 정보 스크래핑
        ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(company);

        // 스크래핑 결과
        CompanyEntity companyEntity = this.companyRepository.save(new CompanyEntity(company));
        List<DividendEntity> dividendEntities = scrapedResult.getDividends().stream()
                .map(e -> new DividendEntity(companyEntity.getId(), e))
                .collect(Collectors.toList());

        this.dividendRepository.saveAll(dividendEntities);
        return company;
    }

    // sql like & jpa 이용한 검색 & 자동완성
    // trie 에 회사 저장하는 로직 생략 가능
    public List<String> getCompanyNameByKeyword(String keyword) {
        Pageable limit = PageRequest.of(0, 10); // 10개만 가져오도록
        Page<CompanyEntity> companyEntities = this.companyRepository.findByNameStartingWithIgnoreCase(keyword, limit);
        if (companyEntities.isEmpty()) {
            throw new NoCompanyPrefixException();
        }
        log.info("getTotalPages : {}", companyEntities.getTotalPages());
        log.info("getTotalElements : {}", companyEntities.getTotalElements());
        return companyEntities.stream().map(CompanyEntity::getName).collect(Collectors.toList());

    }

    public List<String> autocomplete(String keyword) {
        List<String> list = (List<String>) this.trie.prefixMap(keyword).keySet()
                .stream().collect(Collectors.toList());
        if (list.isEmpty()) {
            throw new NoCompanyPrefixException();
        }
        return list;
    }

    public Page<CompanyEntity> getAllCompany(Pageable pageable) {

        Page<CompanyEntity> list = this.companyRepository.findAll(pageable);
        if (list.isEmpty()) {
            throw new CompanyIsEmptyException();
        }
        return list;
    }

    // Tire를 이용해 겁색하는 경우
    // 회사명들을 메모리에 저장해 검색
    public void addAutocompleteKeyword(String keyword) {
        this.trie.put(keyword, null);
    }

    public String deleteCompany(String ticker) {
        CompanyEntity companyEntity = this.companyRepository.findByTicker(ticker)
                .orElseThrow(() -> new NoCompanyException());
        this.dividendRepository.deleteAllByCompanyId(companyEntity.getId());
        this.companyRepository.delete(companyEntity);

        // trie 검색 이용한경우 삭제
        this.deleteAutocompleteKeyword(companyEntity.getName());

        return companyEntity.getName();
    }

    private void deleteAutocompleteKeyword(String keyword) {
        this.trie.remove(keyword);
    }
}
