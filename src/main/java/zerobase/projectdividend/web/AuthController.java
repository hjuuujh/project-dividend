package zerobase.projectdividend.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zerobase.projectdividend.model.Auth;
import zerobase.projectdividend.persist.entity.MemberEntity;
import zerobase.projectdividend.security.TokenProvider;
import zerobase.projectdividend.service.MemberService;
import zerobase.projectdividend.web.response.SignIn;

@RestController
@Slf4j
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final TokenProvider tokenProvider;

    // controller로 요청이 들어오기 전에 filter를 먼저 거침
    // filter -> servlet -> interceptor -> controller
    // 나갈때는 반대 방향

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody Auth.SignUp request) {
        // 회원강비을 위한 API
        MemberEntity result = this.memberService.register(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/signin")
    public SignIn.Response signIn(@RequestBody Auth.SignIn request) {
        // 로그인용 API
        // id, pw 일치 확인
        MemberEntity member = this.memberService.authenticate(request);

        // token 생성해 반환
        String token = this.tokenProvider.generateToken(member.getUsername(), member.getRoles());
        log.info("user login -> {}", member.getUsername());
        return SignIn.Response.from(token);


    }

}
