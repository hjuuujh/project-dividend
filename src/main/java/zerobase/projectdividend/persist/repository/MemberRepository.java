package zerobase.projectdividend.persist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zerobase.projectdividend.persist.entity.MemberEntity;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findByUsername(String username);

    boolean existsByUsername(String username);

}
