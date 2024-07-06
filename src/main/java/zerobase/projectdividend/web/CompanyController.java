package zerobase.projectdividend.web;

import lombok.AllArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import zerobase.projectdividend.exception.impl.NotExistTickerException;
import zerobase.projectdividend.model.Company;
import zerobase.projectdividend.persist.entity.CompanyEntity;
import zerobase.projectdividend.service.CompanyService;

import java.util.List;

@RestController
@RequestMapping("/company")
@AllArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CacheManager cacheManager;

    @GetMapping("/autocomplete")
    @PreAuthorize("hasRole('READ')")
    public ResponseEntity<?> autoComplete(@RequestParam String keyword) {
        // Trie 이용한 검색
//        List<String> autocomplete = this.companyService.autocomplete(keyword);

        // Jpa 이용한 검색
        List<String> companyNameByKeyword = this.companyService.getCompanyNameByKeyword(keyword);

        return ResponseEntity.ok(companyNameByKeyword);
    }

    @GetMapping
    @PreAuthorize("hasRole('READ')")
    public ResponseEntity<?> searchCompany(final Pageable pageable) {
        Page<CompanyEntity> companies = this.companyService.getAllCompany(pageable);
        return ResponseEntity.ok(companies);
    }

    @PostMapping
    @PreAuthorize("hasRole('WRITE')")
    public ResponseEntity<?> addCompany(@RequestBody Company request) {
        String ticker = request.getTicker().trim();
        if (ObjectUtils.isEmpty(ticker)) {
            throw new NotExistTickerException();
        }

        Company company = this.companyService.save(ticker);
        this.companyService.addAutocompleteKeyword(company.getName());

        return ResponseEntity.ok(company);
    }

    @DeleteMapping("/{ticker}")
    @PreAuthorize("hasRole('WRITE')")
    public ResponseEntity<?> deleteCompany(@PathVariable String ticker) {
        // DB에 저장된 데이터 삭제
        String companyName = this.companyService.deleteCompany(ticker);

        // cache에 저장된 데이터도 삭제
        this.clearFinanceCache(companyName);
        return ResponseEntity.ok(companyName);
    }

    private void clearFinanceCache(String companyName) {
        this.cacheManager.getCache("finance").evict(companyName);
    }
}
