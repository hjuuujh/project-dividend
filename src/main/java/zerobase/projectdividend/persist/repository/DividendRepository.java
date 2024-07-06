package zerobase.projectdividend.persist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zerobase.projectdividend.persist.entity.DividendEntity;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DividendRepository extends JpaRepository<DividendEntity, Long> {
    List<DividendEntity> findAllByCompanyId(Long companyId);

    boolean existsByCompanyIdAndDate(Long companyId, LocalDateTime date);

    @Transactional
    void deleteAllByCompanyId(Long companyId);
}
