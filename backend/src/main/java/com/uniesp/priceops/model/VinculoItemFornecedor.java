package com.uniesp.priceops.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tb_vinculos_item_fornecedor",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fornecedor_codigo_item",
                columnNames = {"fornecedor_id", "codigo_fornecedor"}
        )
)
public class VinculoItemFornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Fornecedor fornecedor;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Produto produto;

    @Column(name = "codigo_fornecedor", nullable = false, length = 100)
    private String codigoFornecedor;

    @Column(name = "descricao_no_fornecedor", length = 300)
    private String descricaoNoFornecedor;

    @Column(name = "ultimo_preco_registrado", precision = 10, scale = 2)
    private BigDecimal ultimoPrecoRegistrado;

    @Column(nullable = false)
    private Boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public VinculoItemFornecedor() {
        this.ativo = true;
        this.criadoEm = LocalDateTime.now();
    }

    public VinculoItemFornecedor(Fornecedor fornecedor, Produto produto, String codigoFornecedor,
                                  String descricaoNoFornecedor, BigDecimal ultimoPrecoRegistrado) {
        this();
        this.fornecedor = fornecedor;
        this.produto = produto;
        this.codigoFornecedor = codigoFornecedor;
        this.descricaoNoFornecedor = descricaoNoFornecedor;
        this.ultimoPrecoRegistrado = ultimoPrecoRegistrado;
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(Fornecedor fornecedor) {
        this.fornecedor = fornecedor;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public String getCodigoFornecedor() {
        return codigoFornecedor;
    }

    public void setCodigoFornecedor(String codigoFornecedor) {
        this.codigoFornecedor = codigoFornecedor;
    }

    public String getDescricaoNoFornecedor() {
        return descricaoNoFornecedor;
    }

    public void setDescricaoNoFornecedor(String descricaoNoFornecedor) {
        this.descricaoNoFornecedor = descricaoNoFornecedor;
    }

    public BigDecimal getUltimoPrecoRegistrado() {
        return ultimoPrecoRegistrado;
    }

    public void setUltimoPrecoRegistrado(BigDecimal ultimoPrecoRegistrado) {
        this.ultimoPrecoRegistrado = ultimoPrecoRegistrado;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
