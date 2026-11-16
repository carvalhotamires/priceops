package com.uniesp.priceops.dto;

import java.math.BigDecimal;

public class VinculoItemFornecedorRequestDto {

    private Long fornecedorId;
    private Long produtoId;
    private String codigoFornecedor;
    private String descricaoNoFornecedor;
    private BigDecimal ultimoPrecoRegistrado;
    private Boolean ativo;

    public VinculoItemFornecedorRequestDto() {
    }

    public VinculoItemFornecedorRequestDto(Long fornecedorId, Long produtoId, String codigoFornecedor,
                                          String descricaoNoFornecedor, BigDecimal ultimoPrecoRegistrado) {
        this.fornecedorId = fornecedorId;
        this.produtoId = produtoId;
        this.codigoFornecedor = codigoFornecedor;
        this.descricaoNoFornecedor = descricaoNoFornecedor;
        this.ultimoPrecoRegistrado = ultimoPrecoRegistrado;
        this.ativo = true;
    }

    public Long getFornecedorId() {
        return fornecedorId;
    }

    public void setFornecedorId(Long fornecedorId) {
        this.fornecedorId = fornecedorId;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
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
}
