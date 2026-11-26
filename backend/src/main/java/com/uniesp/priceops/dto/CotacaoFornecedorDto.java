package com.uniesp.priceops.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CotacaoFornecedorDto {

    private Long fornecedorId;
    private String fornecedorNome;
    private BigDecimal precoCotado;
    private LocalDateTime dataCotacao;
    private String origem; // "MANUAL", "IMPORTACAO_PDF"
    private boolean melhorOferta;
    private BigDecimal diferencaMenorPreco; // PrecoCotado - MenorPreco
    private BigDecimal percentualDiferencaMelhor; // % de acrescimo em relacao ao menor preco

    public CotacaoFornecedorDto() {
    }

    public CotacaoFornecedorDto(Long fornecedorId, String fornecedorNome, BigDecimal precoCotado,
                                LocalDateTime dataCotacao, String origem, boolean melhorOferta,
                                BigDecimal diferencaMenorPreco, BigDecimal percentualDiferencaMelhor) {
        this.fornecedorId = fornecedorId;
        this.fornecedorNome = fornecedorNome;
        this.precoCotado = precoCotado;
        this.dataCotacao = dataCotacao;
        this.origem = origem;
        this.melhorOferta = melhorOferta;
        this.diferencaMenorPreco = diferencaMenorPreco;
        this.percentualDiferencaMelhor = percentualDiferencaMelhor;
    }

    public Long getFornecedorId() { return fornecedorId; }
    public void setFornecedorId(Long fornecedorId) { this.fornecedorId = fornecedorId; }

    public String getFornecedorNome() { return fornecedorNome; }
    public void setFornecedorNome(String fornecedorNome) { this.fornecedorNome = fornecedorNome; }

    public BigDecimal getPrecoCotado() { return precoCotado; }
    public void setPrecoCotado(BigDecimal precoCotado) { this.precoCotado = precoCotado; }

    public LocalDateTime getDataCotacao() { return dataCotacao; }
    public void setDataCotacao(LocalDateTime dataCotacao) { this.dataCotacao = dataCotacao; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public boolean isMelhorOferta() { return melhorOferta; }
    public void setMelhorOferta(boolean melhorOferta) { this.melhorOferta = melhorOferta; }

    public BigDecimal getDiferencaMenorPreco() { return diferencaMenorPreco; }
    public void setDiferencaMenorPreco(BigDecimal diferencaMenorPreco) { this.diferencaMenorPreco = diferencaMenorPreco; }

    public BigDecimal getPercentualDiferencaMelhor() { return percentualDiferencaMelhor; }
    public void setPercentualDiferencaMelhor(BigDecimal percentualDiferencaMelhor) { this.percentualDiferencaMelhor = percentualDiferencaMelhor; }
}
