package zerobase.projectdividend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.annotation.BeforeTestExecution;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import zerobase.projectdividend.config.AppConfig;
import zerobase.projectdividend.model.Auth;
import zerobase.projectdividend.model.Company;
import zerobase.projectdividend.model.Dividend;
import zerobase.projectdividend.model.ScrapedResult;
import zerobase.projectdividend.persist.entity.MemberEntity;
import zerobase.projectdividend.persist.repository.MemberRepository;
import zerobase.projectdividend.security.JwtAuthenticationFilter;
import zerobase.projectdividend.security.SecurityConfiguration;
import zerobase.projectdividend.security.TokenProvider;
import zerobase.projectdividend.service.FinanceService;
import zerobase.projectdividend.service.MemberService;

import javax.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FinanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class FinanceControllerTest {

    @MockBean
    private FinanceService financeService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private MemberService memberService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = {"READ"})
    void successSearchFinance() throws Exception{
        //given
        Company company = Company.builder()
                .name("3M Company")
                .ticker("MMM")
                .build();

        List<Dividend> dividends = getDividend();

        ScrapedResult result = ScrapedResult.builder()
                .company(company)
                .dividends(dividends)
                .build();

        given(financeService.getDividendByCompanyName(anyString()))
                .willReturn(result);

        //when

        //then
        mockMvc.perform(get("/finance/dividend/3M Company"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").value(company))
                .andExpect(jsonPath("$.dividends[0].date").value("2023-05-18T00:00:00"))
                .andExpect(jsonPath("$.dividends[0].dividend").value("1.25"));

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