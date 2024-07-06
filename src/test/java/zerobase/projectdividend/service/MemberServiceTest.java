package zerobase.projectdividend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import zerobase.projectdividend.exception.impl.AlreadyExistUserException;
import zerobase.projectdividend.exception.impl.EmailAndPasswordNotMatchException;
import zerobase.projectdividend.exception.impl.NotExistUserException;
import zerobase.projectdividend.model.Auth;
import zerobase.projectdividend.persist.entity.MemberEntity;
import zerobase.projectdividend.persist.repository.MemberRepository;
import zerobase.projectdividend.security.TokenProvider;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private MemberService memberService;

    private String secretKey = "c3ByaW5nLWJvb3QtZGl2aWVuZC1wcm9qZWN0LXR1dG9yaWFsLWp3dC1zZWNyZXQta2V5Cg";

    private final long TOKEN_EXPIRE_TIME = 1000 * 60 * 60;


    @BeforeEach
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(tokenProvider, "secretKey", "c3ByaW5nLWJvb3QtZGl2aWVuZC1wcm9qZWN0LXR1dG9yaWFsLWp3dC1zZWNyZXQta2V5Cg");

    }

    /**
     * 회원가입
     * 1. 성공
     * 2. 실패 -  이메일이 이미 존재할 경우
     */
    @Test
    @DisplayName("회원가입 - 성공")
    void successRegister() throws Exception {
        //given
        Auth.SignUp request = new Auth.SignUp();
        request.setUsername("user1@gmail.com");
        request.setPassword("qwerty");
        request.setRoles(Arrays.asList("ROLE_WRITE", "ROLE_READ"));

        given(memberRepository.existsByUsername(anyString()))
                .willReturn(false);

        given(passwordEncoder.encode(anyString()))
                .willReturn("password");

        ArgumentCaptor<MemberEntity> captor = ArgumentCaptor.forClass(MemberEntity.class);

        //when
        MemberEntity register = memberService.register(request);

        //then
        verify(memberRepository, times(1)).save(captor.capture());
        assertEquals("user1@gmail.com", captor.getValue().getUsername());
        assertEquals("password", captor.getValue().getPassword());
        assertEquals(Arrays.asList("ROLE_WRITE", "ROLE_READ"), captor.getValue().getRoles());

    }

    @Test
    @DisplayName("회원가입 - 실패 - 이메일이 이미 존재할 경우")
    void failRegister_AlreadyExitsEmail() throws Exception {
        //given
        Auth.SignUp request = new Auth.SignUp();
        request.setUsername("user1@gmail.com");
        request.setPassword("qwerty");
        request.setRoles(Arrays.asList("ROLE_WRITE", "ROLE_READ"));

        given(memberRepository.existsByUsername(anyString()))
                .willReturn(true);

        //when
        AlreadyExistUserException exception = assertThrows(AlreadyExistUserException.class,
                () -> memberService.register(request));

        //then
        assertEquals("이미 존재하는 사용자명입니다.", exception.getMessage());
    }

    /**
     * 로그인
     * 1. 성공
     * 2. 실패 - 가입하지 않은 이메일인 경우
     * 3. 실패 - 이메일과 비밀번호가 일치하지 않는 경우
     */
    @Test
    @DisplayName("로그인 - 성공")
    void successLogin() throws Exception {
        //given
        Auth.SignIn request = new Auth.SignIn();
        request.setUsername("user1@gmail.com");
        request.setPassword("qwerty");

        MemberEntity member = MemberEntity.builder()
                .username("user1@gmail.com")
                .password("qwerty")
                .build();

        given(memberRepository.findByUsername(anyString()))
                .willReturn(Optional.of(member));

        given(passwordEncoder.matches(request.getPassword(), member.getPassword()))
                .willReturn(true);

//        Claims claims = Jwts.claims().setSubject("user1@gmail.com");
//        claims.put("roles", Arrays.asList("ROLE_WRITE", "ROLE_READ"));
//        Date now = new Date();
//        Date expiryDate = new Date(now.getTime() + TOKEN_EXPIRE_TIME);
//
//        String token = Jwts.builder()
//                .setClaims(claims)
//                .setIssuedAt(now)
//                .setExpiration(expiryDate)
//                .signWith(SignatureAlgorithm.HS512, secretKey)
//                .compact();

//        given()
//
//        System.out.println(token);


        //when
        MemberEntity loginMember = memberService.authenticate(request);

        //then
        assertEquals("user1@gmail.com", loginMember.getUsername());
        assertEquals("qwerty", loginMember.getPassword());
//        assertThat(token, notNullValue());

    }

    @Test
    @DisplayName("로그인 - 실패 - 가입하지 않은 이메일인 경우")
    void failLogin_NoExistUser() throws Exception {
        //given
        Auth.SignIn request = new Auth.SignIn();
        request.setUsername("user1@gmail.com");
        request.setPassword("qwerty");

        given(memberRepository.findByUsername(anyString()))
                .willReturn(Optional.empty());

        //when
        NotExistUserException exception = assertThrows(NotExistUserException.class,
                () -> memberService.authenticate(request));

        //then
        assertEquals("가입되지 않은 사용자명입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("로그인 - 실패 - 이메일과 비밀번호가 일치하지 않는 경우")
    void failLogin_EmailAndPasswordNotMatch() throws Exception {
        //given
        Auth.SignIn request = new Auth.SignIn();
        request.setUsername("user1@gmail.com");
        request.setPassword("qwerty");

        MemberEntity member = MemberEntity.builder()
                .username("user1@gmail.com")
                .password("qwerti")
                .build();

        given(memberRepository.findByUsername(anyString()))
                .willReturn(Optional.of(member));

        given(passwordEncoder.matches(request.getPassword(), member.getPassword()))
                .willReturn(false);

        //when
        EmailAndPasswordNotMatchException exception = assertThrows(EmailAndPasswordNotMatchException.class,
                () -> memberService.authenticate(request));

        //then
        assertEquals("비밀번호가 일치하지 않습니다.", exception.getMessage());
    }
}