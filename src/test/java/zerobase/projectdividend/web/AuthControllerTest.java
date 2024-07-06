package zerobase.projectdividend.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import zerobase.projectdividend.model.Auth;
import zerobase.projectdividend.persist.entity.MemberEntity;
import zerobase.projectdividend.security.TokenProvider;
import zerobase.projectdividend.service.MemberService;

import java.util.Arrays;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @MockBean
    private MemberService memberService;

    @MockBean
    private TokenProvider tokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String secretKey = "c3ByaW5nLWJvb3QtZGl2aWVuZC1wcm9qZWN0LXR1dG9yaWFsLWp3dC1zZWNyZXQta2V5Cg";

    private final long TOKEN_EXPIRE_TIME = 1000 * 60 * 60;

    @Test
    void successSignUp() throws Exception {
        //given
        Auth.SignUp request = new Auth.SignUp();
        request.setUsername("user1@gmail.com");
        request.setPassword("qwerty");
        request.setRoles(Arrays.asList("ROLE_WRITE", "ROLE_READ"));

        given(memberService.register(any()))
                .willReturn(request.toEntity());

        //when

        //then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username",is("user1@gmail.com")))
                .andExpect(jsonPath("$.roles[0]",is("ROLE_WRITE")))
                .andExpect(jsonPath("$.roles[1]",is("ROLE_READ")))
                .andDo(print());
    }

    @Test
    void successSignIn() throws Exception {
        //given
        Auth.SignIn request = new Auth.SignIn();
        request.setUsername("user1@gmail.com");
        request.setPassword("qwerty");

        MemberEntity member = MemberEntity.builder()
                .username("user1@gmail.com")
                .password("qwerty")
                .build();

        given(memberService.authenticate(any()))
                .willReturn(member);

        given(passwordEncoder.matches(request.getPassword(), member.getPassword()))
                .willReturn(true);

        Claims claims = Jwts.claims().setSubject("user1@gmail.com");
        claims.put("roles", Arrays.asList("ROLE_WRITE", "ROLE_READ"));
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TOKEN_EXPIRE_TIME);

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();;

        System.out.println(token);

        given(tokenProvider.generateToken(anyString(), any()))
                .willReturn(token);

        //when

        //then
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andDo(print());
    }



}