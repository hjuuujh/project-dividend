package zerobase.projectdividend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zerobase.projectdividend.exception.impl.NoCompanyException;
import zerobase.projectdividend.model.Dividend;
import zerobase.projectdividend.model.ScrapedResult;
import zerobase.projectdividend.persist.entity.CompanyEntity;
import zerobase.projectdividend.persist.entity.DividendEntity;
import zerobase.projectdividend.persist.repository.CompanyRepository;
import zerobase.projectdividend.persist.repository.DividendRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private DividendRepository dividendRepository;

    @InjectMocks
    private FinanceService financeService;

    /**
     * 해당 회사의 정보와 배당금 정보 반환
     * 1. 성공
     * 2. 실패 - 회사명이 없는 경우
     */
    @Test
    @DisplayName("해당 회사의 정보와 배당금 정보 반환 - 성공")
    void successGetDividendByCompanyName() {
        //given
        CompanyEntity company = CompanyEntity.builder()
                .id(1L)
                .name("3M Company")
                .ticker("MMM")
                .build();
        given(companyRepository.findByName(anyString()))
                .willReturn(Optional.of(company));
        List<DividendEntity> dividendEntities = getDividend();
        given(dividendRepository.findAllByCompanyId(anyLong()))
                .willReturn(dividendEntities);
        List<Dividend> dividends = dividendEntities.stream()
                .map(e -> new Dividend(e.getDate(), e.getDividend())).collect(Collectors.toList());

        //when

        ScrapedResult dividendByCompanyName = financeService.getDividendByCompanyName("3M Company");

        //then
        assertEquals(company.getName(), new CompanyEntity(dividendByCompanyName.getCompany()).getName());
        assertEquals(company.getTicker(), new CompanyEntity(dividendByCompanyName.getCompany()).getTicker());
        assertEquals(dividends, dividendByCompanyName.getDividends());
    }

    @Test
    @DisplayName("해당 회사의 정보와 배당금 정보 반환 - 실패 - 회사명이 없는 경우 ")
    void failGetDividendByCompanyName_NoCompanyException() {
        //given
        given(companyRepository.findByName(anyString()))
                .willReturn(Optional.empty());

        //when
        NoCompanyException exception = assertThrows(NoCompanyException.class, () -> financeService.getDividendByCompanyName("3M Company"));

        //then
        assertEquals(exception.getMessage(), "존재하지 않는 회사명입니다.");
    }

    private List<DividendEntity> getDividend() {
        List<DividendEntity> dividends = new ArrayList<>();
        dividends.add(DividendEntity.builder()
                .date(LocalDateTime.parse("2023-05-18T00:00:00"))
                .dividend("1.25").build());
        dividends.add(DividendEntity.builder()
                .date(LocalDateTime.parse("2023-08-18T00:00:00"))
                .dividend("1.25").build());
        dividends.add(DividendEntity.builder()
                .date(LocalDateTime.parse("2023-11-16T00:00:00"))
                .dividend("1.25").build());
        dividends.add(DividendEntity.builder()
                .date(LocalDateTime.parse("2024-02-15T00:00:00"))
                .dividend("1.26").build());
        dividends.add(DividendEntity.builder()
                .date(LocalDateTime.parse("2024-05-23T00:00:00"))
                .dividend("0.70").build());
        return dividends;
    }
}