package zerobase.projectdividend.persist.entity;

import lombok.*;
import zerobase.projectdividend.model.Company;

import javax.persistence.*;

@Entity(name = "COMPANY")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class CompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String ticker;

    private String name;

    public CompanyEntity(Company company) {
        this.ticker = company.getTicker();
        this.name = company.getName();
    }
}
