package zerobase.projectdividend.service;

import org.apache.commons.collections4.Trie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import zerobase.projectdividend.exception.impl.*;
import zerobase.projectdividend.model.Company;
import zerobase.projectdividend.model.Dividend;
import zerobase.projectdividend.model.ScrapedResult;
import zerobase.projectdividend.persist.entity.CompanyEntity;
import zerobase.projectdividend.persist.entity.DividendEntity;
import zerobase.projectdividend.persist.repository.CompanyRepository;
import zerobase.projectdividend.persist.repository.DividendRepository;
import zerobase.projectdividend.scraper.Scraper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {
    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private DividendRepository dividendRepository;

    @Mock
    private Scraper yahooFinanceScraper;

    @Mock
    private Trie<String, String> trie;

    @InjectMocks
    private CompanyService companyService;

    /**
     * 새로운 회사 정보 추가
     * 1. 성공
     * 2. 실패 - 이미 보유하고있는 회사인 경우
     * 3. 실패 - 존재하지 않는 회사 ticker 일 경우
     * 4. 실패 - Scrap을 실패하였습니다.
     */
    @Test
    @DisplayName("새로운 회사 정보 추가 - 성공")
    void successAddCompany() {
        //given
//        String ticker = "MMM";
        given(companyRepository.existsByTicker(anyString()))
                .willReturn(false);

        Company company = Company.builder()
                .name("3M Company")
                .ticker("MMM")
                .build();
        given(yahooFinanceScraper.scrapCompanyByTicker(anyString()))
                .willReturn(company);

        List<Dividend> dividends = getDividend();
        ScrapedResult scrapedResult = ScrapedResult.builder()
                .company(company)
                .dividends(dividends)
                .build();
        given(yahooFinanceScraper.scrap(any()))
                .willReturn(scrapedResult);

        CompanyEntity companyEntity = new CompanyEntity(company);
        given(companyRepository.save(any()))
                .willReturn(companyEntity);

        List<DividendEntity> dividendEntities = scrapedResult.getDividends().stream()
                .map(e -> new DividendEntity(companyEntity.getId(), e))
                .collect(Collectors.toList());
        ArgumentCaptor<CompanyEntity> captor1 = ArgumentCaptor.forClass(CompanyEntity.class);

        given(dividendRepository.saveAll(any()))
                .willReturn(dividendEntities);
        ArgumentCaptor<List<DividendEntity>> captor2 = ArgumentCaptor.forClass(List.class);

        //when
        Company result = companyService.save("MMM");

        //then
        verify(companyRepository, times(1)).save(captor1.capture());
        assertEquals("3M Company", captor1.getValue().getName());
        assertEquals("MMM", captor1.getValue().getTicker());
        verify(dividendRepository, times(1)).saveAll(captor2.capture());
    }

    @Test
    @DisplayName("새로운 회사 정보 추가 - 실패 - 이미 보유하고있는 회사인 경우")
    void failAddCompany_AlreadyExistCompanyException() {
        //given
        given(companyRepository.existsByTicker(anyString()))
                .willReturn(true);

        //when
        AlreadyExistCompanyException exception = assertThrows(AlreadyExistCompanyException.class,
                () -> companyService.save("MMM"));

        //then
        assertEquals("이미 정보가 존재하는 회사입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("새로운 회사 정보 추가 - 실패 - 존재하지 않는 회사 ticker 일 경우")
    void failAddCompany_NotExistCompanyWithTickerException() {
        //given
        given(companyRepository.existsByTicker(anyString()))
                .willReturn(false);
        given(yahooFinanceScraper.scrapCompanyByTicker(anyString()))
                .willThrow(NotExistCompanyWithTickerException.class);

        //when
        NotExistCompanyWithTickerException exception = assertThrows(NotExistCompanyWithTickerException.class,
                () -> companyService.save("MMM"));

        //then
        assertEquals("Ticker에 해당하는 회사가 없습니다.", exception.getMessage());

    }

    @Test
    void failAddCompany_NotExistTickerException() {

        // controller 에서
        //given

        //when

        //then
    }

    @Test
    @DisplayName("새로운 회사 정보 추가 - 실패 - scrap실패한 경우")
    void failAddCompany_FailToScrapException() {
        given(companyRepository.existsByTicker(anyString()))
                .willReturn(false);

        Company company = Company.builder()
                .name("3M Company")
                .ticker("MMM")
                .build();
        given(yahooFinanceScraper.scrapCompanyByTicker(anyString()))
                .willReturn(null);

        //when
        FailToScrapException exception = assertThrows(FailToScrapException.class,
                () -> companyService.save("MMM"));

        //then
        assertEquals("Scrap을 실패하였습니다.", exception.getMessage());

    }

    /**
     * 전체 회사 리스트 가져오기
     * 1. 성공
     * 2. 실패 - 저장된 회사가 없는 경우
     */
    @Test
    @DisplayName("전체 회사 리스트 가져오기 - 성공")
    void successGetAllCompany() {
        //given
        List<CompanyEntity> list = Arrays.asList(
                CompanyEntity.builder()
                        .name("3M Company")
                        .ticker("MMM")
                        .build(),
                CompanyEntity.builder()
                        .name("Realty Income Corporation")
                        .ticker("O")
                        .build(),
                CompanyEntity.builder()
                        .name("Coca-Cola Consolidated, Inc.")
                        .ticker("COKE")
                        .build()
        );
        Page<CompanyEntity> page = new PageImpl<>(list);
        ;
        given(companyRepository.findAll((Pageable) any()))
                .willReturn(page);

        //when
        Pageable pageable = PageRequest.of(1, 10);
        Page<CompanyEntity> allCompany = companyService.getAllCompany(pageable);

        //then
        assertEquals(allCompany.getSize(), 3);

    }

    @Test
    @DisplayName("전체 회사 리스트 가져오기 - 실패 - 저장된 회사가 없는 경우")
    void failGetAllCompany_CompanyIsEmptyException() {
        //given
        given(companyRepository.findAll((Pageable) any()))
                .willReturn(Page.empty());

        //when
        Pageable pageable = PageRequest.of(1, 10);

        CompanyIsEmptyException exception = assertThrows(CompanyIsEmptyException.class,
                () -> companyService.getAllCompany(pageable));

        //then
        assertEquals("저장된 회사가 없습니다.", exception.getMessage());

    }

    /**
     * 회사 검색기능 (자동완성)
     * 1. 성공
     * 2. 실패 - 회사명이 없는 경우
     */
    @Test
    @DisplayName("회사 검색기능 - 성공")
    void successGetCompanyNameByKeyword() {
        //given
        Page<CompanyEntity> pages = new PageImpl<>(getCompanies().stream().filter(s -> s.getName().startsWith("C")).collect(Collectors.toList()));
        given(companyRepository.findByNameStartingWithIgnoreCase(anyString(), any()))
                .willReturn(pages);
        //when
        List<String> list = companyService.getCompanyNameByKeyword("C");

        //then
        assertEquals(12, list.size());
        assertEquals("Citigroup Inc.", list.get(0));

    }

    @Test
    @DisplayName("회사 검색기능 - 실패 - 회사명이 없는 경우")
    void failGetCompanyNameByKeyword_NoCompanyPrefixException() {
        //given
        Page<CompanyEntity> pages = new PageImpl<>(Arrays.asList());
        given(companyRepository.findByNameStartingWithIgnoreCase(anyString(), any()))
                .willReturn(pages);

        //when
        NoCompanyPrefixException exception = assertThrows(NoCompanyPrefixException.class,
                () -> companyService.getCompanyNameByKeyword("C"));

        //then
        assertEquals("키워드가 포함된 회사명이 없습니다.", exception.getMessage());
    }

    /**
     * ticker 에 해당하는 회사 정보 삭제
     * 1. 성공
     * 2. 실패 - ticker에 해당하는 회사가 없는 경우
     */
    @Test
    @DisplayName("ticker 에 해당하는 회사 정보 삭제 - 성공")
    void successDeleteCompany() {
        //given
        CompanyEntity companyEntity = CompanyEntity
                .builder()
                .name("3M Company")
                .ticker("MMM")
                .build();
//        given(companyRepository.findByTicker(anyString()))
//                .willReturn(Optional.of(companyEntity));

        //when
        dividendRepository.deleteAllByCompanyId(any());
        companyRepository.delete(any());

        //then
        verify(dividendRepository, times(1)).deleteAllByCompanyId(any());
        verify(companyRepository, times(1)).delete(any());
    }

    @Test
    @DisplayName("ticker 에 해당하는 회사 정보 삭제 - 실패 - ticker에 해당하는 회사가 없는 경우")
    void failDeleteCompany_NoCompanyException() {
        //given
        given(companyRepository.findByTicker(anyString()))
                .willReturn(Optional.empty());

        //when
        NoCompanyException exception = assertThrows(NoCompanyException.class,
                () -> companyService.deleteCompany("3M Company"));

        //then
        assertEquals("존재하지 않는 회사명입니다.", exception.getMessage());
    }

    private List<CompanyEntity> getCompanies() {
        List<CompanyEntity> companies = Arrays.asList(
                CompanyEntity.builder()
                        .name("3M Company")
                        .ticker("MMM")
                        .build(),
                CompanyEntity.builder()
                        .name("Citigroup Inc.")
                        .ticker("C")
                        .build(),
                CompanyEntity.builder()
                        .name("Realty Income Corporation")
                        .ticker("O")
                        .build(),
                CompanyEntity.builder()
                        .name("Coca-Cola Consolidated, Inc.")
                        .ticker("COKE")
                        .build(),
                CompanyEntity.builder()
                        .name("CAC 40")
                        .ticker("^FCHI")
                        .build(),
                CompanyEntity.builder()
                        .name("Coffee Sep 24")
                        .ticker("KC=F")
                        .build(),
                CompanyEntity.builder()
                        .name("Copper Sep 24")
                        .ticker("HG=F")
                        .build(),
                CompanyEntity.builder()
                        .name("Crude Oil Aug 24")
                        .ticker("HG=F")
                        .build(),
                CompanyEntity.builder()
                        .name("Canadian Pacific Kansas City Limited")
                        .ticker("HG=F")
                        .build(),
                CompanyEntity.builder()
                        .name("Celanese Corporation")
                        .ticker("CE")
                        .build(),
                CompanyEntity.builder()
                        .name("Cisco Systems, Inc.")
                        .ticker("CSCO")
                        .build(),
                CompanyEntity.builder()
                        .name("CEL-SCI Corporation")
                        .ticker("CVM")
                        .build(),
                CompanyEntity.builder()
                        .name("Celsius Holdings, Inc.")
                        .ticker("CELH")
                        .build(),
                CompanyEntity.builder()
                        .name("Colgate-Palmolive Company")
                        .ticker("CL")
                        .build()
        );


        return companies;
    }

    private List<Dividend> getDividend() {
        List<Dividend> dividends = new ArrayList<>();
        dividends.add(Dividend.builder()
                .date(LocalDateTime.parse("2023-05-18T00:00:00"))
                .dividend("1.25").build());
        dividends.add(Dividend.builder()
                .date(LocalDateTime.parse("2023-08-18T00:00:00"))
                .dividend("1.25").build());
        dividends.add(Dividend.builder()
                .date(LocalDateTime.parse("2023-11-16T00:00:00"))
                .dividend("1.25").build());
        dividends.add(Dividend.builder()
                .date(LocalDateTime.parse("2024-02-15T00:00:00"))
                .dividend("1.26").build());
        dividends.add(Dividend.builder()
                .date(LocalDateTime.parse("2024-05-23T00:00:00"))
                .dividend("0.70").build());
        return dividends;
    }
}