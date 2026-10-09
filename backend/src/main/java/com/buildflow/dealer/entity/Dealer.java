package com.buildflow.dealer.entity;

import com.buildflow.business.entity.Business;
import com.buildflow.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "dealers")
public class Dealer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false)
    private String name;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column
    private String phone;

    @Column
    private String email;

    @Column(length = 500)
    private String address;

    @Column
    private String district;

    @Column(name = "delivery_radius_km")
    private Integer deliveryRadiusKm;

    @Column
    private String gstin;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "dealer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("itemName asc")
    private List<DealerRate> rates = new ArrayList<>();
}
