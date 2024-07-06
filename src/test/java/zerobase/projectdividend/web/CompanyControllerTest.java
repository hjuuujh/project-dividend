package zerobase.projectdividend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.connection.RedisServer;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import zerobase.projectdividend.config.CacheConfig;
import zerobase.projectdividend.model.Auth;
import zerobase.projectdividend.model.Company;
import zerobase.projectdividend.model.Dividend;
import zerobase.projectdividend.model.ScrapedResult;
import zerobase.projectdividend.persist.entity.CompanyEntity;
import zerobase.projectdividend.persist.entity.MemberEntity;
import zerobase.projectdividend.security.JwtAuthenticationFilter;
import zerobase.projectdividend.security.TokenProvider;
import zerobase.projectdividend.service.CompanyService;
import zerobase.projectdividend.service.FinanceService;
import zerobase.projectdividend.service.MemberService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CompanyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CacheConfig.class})
class CompanyControllerTest {

    @MockBean
    private CompanyService companyService;

    @MockBean
    private MemberService memberService;

    @MockBean
    private CacheManager cacheManager;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private FinanceService financeService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private Cache cache;

    @Test
    @WithMockUser(roles = {"READ"})
    void successAutoComplete() throws Exception {
        //given
        List<String> companies = Arrays.asList("Citigroup Inc.", "Coca-Cola Consolidated, Inc.", "CAC 40", "Cisco Systems, Inc.", "Cisco Systems, Inc.", "CEL-SCI Corporation");

        given(companyService.getCompanyNameByKeyword(anyString()))
                .willReturn(companies);

        //when

        //then
        mockMvc.perform(get("/company/autocomplete?keyword=c"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0]").value("Citigroup Inc."))
                .andExpect(jsonPath("$.[1]").value("Coca-Cola Consolidated, Inc."))
        ;

    }

    @Test
    @WithMockUser(roles = {"READ"})
    void successSearchCompany() throws Exception {
        //given
        Page<CompanyEntity> pages = new PageImpl<>(getCompanies().stream().filter(s -> s.getName().startsWith("C")).collect(Collectors.toList()));

        given(companyService.getAllCompany(any()))
                .willReturn(pages);


        //when

        //then
        mockMvc.perform(get("/company"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ticker").value("C"))
                .andExpect(jsonPath("$.content[0].name").value("Citigroup Inc."))
        ;
    }


    @Test
    @WithMockUser(roles = {"WRITE"})
    void successAddCompany() throws Exception {
        //given
        String ticker = "MMM";
        Company company = Company.builder()
                .name("3M Company")
                .ticker("MMM")
                .build();

        given(companyService.save(anyString()))
                .willReturn(company);

        companyService.addAutocompleteKeyword(anyString());

        //when
        Company request = Company.builder()
                .ticker("MMM")
                .build();

        //then
        mockMvc.perform(post("/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("MMM"))
                .andExpect(jsonPath("$.name").value("3M Company"))
        ;
    }

    @Test
    @WithMockUser(roles = {"WRITE"})
    void failAddCompany_NotExistTickerException() throws Exception {
        //given
        String ticker = "MMM";
        Company company = Company.builder()
                .name("3M Company")
                .ticker("MMM")
                .build();

        given(companyService.save(anyString()))
                .willReturn(company);

        companyService.addAutocompleteKeyword(anyString());

        //when
        Company request = Company.builder()
                .ticker("")
                .build();

        //then
        mockMvc.perform(post("/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ticker 정보가 존재하지 않습니다."));
    }

    @Test
    @WithMockUser(roles = {"WRITE"})
    void successDeleteCompany() throws Exception {

        //given
        given(companyService.deleteCompany(anyString()))
                .willReturn("3M Company");

        given(cacheManager.getCache(anyString()))
                .willReturn(cache);

        //when

        //then
        mockMvc.perform(delete("/company/MMM"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("3M Company"))
        ;
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