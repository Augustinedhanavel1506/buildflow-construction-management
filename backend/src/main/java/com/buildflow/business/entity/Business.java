package com.buildflow.business.entity;

import com.buildflow.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "businesses")
public class Business extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column
    private String phone;

    @Column
    private String address;

    @Column(name = "gstin")
    private String gstin;

    @Column(name = "default_gst_rate", precision = 5, scale = 2)
    private BigDecimal defaultGstRate = new BigDecimal("18.00");

    @Column(name = "state_name")
    private String stateName;

    // The local material-sourcing hub (e.g. "Coimbatore, Tamil Nadu") used to label the Rate
    // Master price index. Deliberately separate from stateName, which is for GST purposes only.
    @Column(name = "material_region")
    private String materialRegion;

    public Business(String name) {
        this.name = name;
    }
}
