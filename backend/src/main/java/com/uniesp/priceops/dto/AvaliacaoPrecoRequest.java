package com.uniesp.priceops.dto;

import java.math.BigDecimal;

public class AvaliacaoPrecoRequest {

    private Long produtoId;
    private Long fornecedorId;
    private BigDecimal precoOfertado;
    private BigDecimal limitePercentualCustomizado;

    public AvaliacaoPrecoRequest() {
    }

    public Long getProdutoId() { return produtoId; }
    public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }

    public Long getFornecedorId() { return fornecedorId; }
    public void setFornecedorId(Long fornecedorId) { this.fornecedorId = fornecedorId; }

    public BigDecimal getPrecoOfertado() { return precoOfertado; }
    public void setPrecoOfertado(BigDecimal precoOfertado) { this.precoOfertado = precoOfertado; }

    public BigDecimal getLimitePercentualCustomizado() { return limitePercentualCustomizado; }
    public void setLimitePercentualCustomizado(BigDecimal limitePercentualCustomizado) {
        this.limitePercentualCustomizado = limitePercentualCustomizado;
    }
}
