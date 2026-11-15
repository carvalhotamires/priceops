package com.uniesp.priceops.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ComparativoPrecoProdutoResponseDto {

    private Long produtoId;
    private String codigo;
    private String nome;
    private Long melhorFornecedorId;
    private String melhorFornecedorNome;
    private BigDecimal menorPreco;
    private BigDecimal maiorPreco;
    private BigDecimal precoMedioCotado;
    private BigDecimal economiaMaximaAbsoluta; // MaiorPreco - MenorPreco
    private BigDecimal economiaMaximaPercentual; // ((MaiorPreco - MenorPreco) / MaiorPreco) * 100
    private Integer totalCotacoes;
    private List<CotacaoFornecedorDto> cotacoes = new ArrayList<>();
    private String recomendacaoCompra;

    public ComparativoPrecoProdutoResponseDto() {
    }

    public Long getProdutoId() { return produtoId; }
    public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Long getMelhorFornecedorId() { return melhorFornecedorId; }
    public void setMelhorFornecedorId(Long melhorFornecedorId) { this.melhorFornecedorId = melhorFornecedorId; }

    public String getMelhorFornecedorNome() { return melhorFornecedorNome; }
    public void setMelhorFornecedorNome(String melhorFornecedorNome) { this.melhorFornecedorNome = melhorFornecedorNome; }

    public BigDecimal getMenorPreco() { return menorPreco; }
    public void setMenorPreco(BigDecimal menorPreco) { this.menorPreco = menorPreco; }

    public BigDecimal getMaiorPreco() { return maiorPreco; }
    public void setMaiorPreco(BigDecimal maiorPreco) { this.maiorPreco = maiorPreco; }

    public BigDecimal getPrecoMedioCotado() { return precoMedioCotado; }
    public void setPrecoMedioCotado(BigDecimal precoMedioCotado) { this.precoMedioCotado = precoMedioCotado; }

    public BigDecimal getEconomiaMaximaAbsoluta() { return economiaMaximaAbsoluta; }
    public void setEconomiaMaximaAbsoluta(BigDecimal economiaMaximaAbsoluta) { this.economiaMaximaAbsoluta = economiaMaximaAbsoluta; }

    public BigDecimal getEconomiaMaximaPercentual() { return economiaMaximaPercentual; }
    public void setEconomiaMaximaPercentual(BigDecimal economiaMaximaPercentual) { this.economiaMaximaPercentual = economiaMaximaPercentual; }

    public Integer getTotalCotacoes() { return totalCotacoes; }
    public void setTotalCotacoes(Integer totalCotacoes) { this.totalCotacoes = totalCotacoes; }

    public List<CotacaoFornecedorDto> getCotacoes() { return cotacoes; }
    public void setCotacoes(List<CotacaoFornecedorDto> cotacoes) { this.cotacoes = cotacoes; }

    public String getRecomendacaoCompra() { return recomendacaoCompra; }
    public void setRecomendacaoCompra(String recomendacaoCompra) { this.recomendacaoCompra = recomendacaoCompra; }
}
