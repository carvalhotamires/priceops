package com.uniesp.priceops.dto;

public class ResolucaoPendenciaRequestDto {

    private Long produtoId;
    private String acao; // "VINCULAR" ou "IGNORAR"
    private String observacao;

    public ResolucaoPendenciaRequestDto() {
    }

    public ResolucaoPendenciaRequestDto(Long produtoId, String acao) {
        this.produtoId = produtoId;
        this.acao = acao;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public String getAcao() {
        return acao;
    }

    public void setAcao(String acao) {
        this.acao = acao;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
