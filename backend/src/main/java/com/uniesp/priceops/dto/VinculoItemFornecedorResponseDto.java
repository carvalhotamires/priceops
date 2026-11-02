package com.uniesp.priceops.dto;

import com.uniesp.priceops.model.VinculoItemFornecedor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VinculoItemFornecedorResponseDto {

    private Long id;
    private Long fornecedorId;
    private String fornecedorNome;
    private Long produtoId;
    private String produtoCodigo;
    private String produtoNome;
    private String codigoFornecedor;
    private String descricaoNoFornecedor;
    private BigDecimal ultimoPrecoRegistrado;
    private Boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public VinculoItemFornecedorResponseDto() {
    }

    public static VinculoItemFornecedorResponseDto fromEntity(VinculoItemFornecedor entity) {
        VinculoItemFornecedorResponseDto dto = new VinculoItemFornecedorResponseDto();
        dto.setId(entity.getId());
        if (entity.getFornecedor() != null) {
            dto.setFornecedorId(entity.getFornecedor().getId());
            dto.setFornecedorNome(entity.getFornecedor().getNomeFantasia());
        }
        if (entity.getProduto() != null) {
            dto.setProdutoId(entity.getProduto().getId());
            dto.setProdutoCodigo(entity.getProduto().getCodigo());
            dto.setProdutoNome(entity.getProduto().getNome());
        }
        dto.setCodigoFornecedor(entity.getCodigoFornecedor());
        dto.setDescricaoNoFornecedor(entity.getDescricaoNoFornecedor());
        dto.setUltimoPrecoRegistrado(entity.getUltimoPrecoRegistrado());
        dto.setAtivo(entity.getAtivo());
        dto.setCriadoEm(entity.getCriadoEm());
        dto.setAtualizadoEm(entity.getAtualizadoEm());
        return dto;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFornecedorId() {
        return fornecedorId;
    }

    public void setFornecedorId(Long fornecedorId) {
        this.fornecedorId = fornecedorId;
    }

    public String getFornecedorNome() {
        return fornecedorNome;
    }

    public void setFornecedorNome(String fornecedorNome) {
        this.fornecedorNome = fornecedorNome;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public String getProdutoCodigo() {
        return produtoCodigo;
    }

    public void setProdutoCodigo(String produtoCodigo) {
        this.produtoCodigo = produtoCodigo;
    }

    public String getProdutoNome() {
        return produtoNome;
    }

    public void setProdutoNome(String produtoNome) {
        this.produtoNome = produtoNome;
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
