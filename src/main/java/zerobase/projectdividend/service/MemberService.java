package zerobase.projectdividend.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import zerobase.projectdividend.exception.impl.AlreadyExistUserException;
import zerobase.projectdividend.exception.impl.EmailAndPasswordNotMatchException;
import zerobase.projectdividend.exception.impl.NotExistUserException;
import zerobase.projectdividend.model.Auth;
import zerobase.projectdividend.persist.entity.MemberEntity;
import zerobase.projectdividend.persist.repository.MemberRepository;

import javax.transaction.Transactional;

@Service
@Slf4j
@AllArgsConstructor
@Transactional
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder; // AppConfig에 정책 정의

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Transactional
    public MemberEntity register(Auth.SignUp member) {
        boolean exists = memberRepository.existsByUsername(member.getUsername());
        if (exists) {
            throw new AlreadyExistUserException();
        }

        member.setPassword(passwordEncoder.encode(member.getPassword()));
        MemberEntity save = this.memberRepository.save(member.toEntity());
        return save;
    }

    @Transactional
    public MemberEntity authenticate(Auth.SignIn member) {

        MemberEntity memberEntity = this.memberRepository.findByUsername(member.getUsername())
                .orElseThrow(() -> new NotExistUserException());
        if (!this.passwordEncoder.matches(member.getPassword(), memberEntity.getPassword())) {
            throw new EmailAndPasswordNotMatchException();
        }

        return memberEntity;
    }
}

