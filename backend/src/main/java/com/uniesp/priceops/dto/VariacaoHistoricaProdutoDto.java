package com.uniesp.priceops.dto;

import java.math.BigDecimal;

public class VariacaoHistoricaProdutoDto {

    private Long produtoId;
    private String codigo;
    private String nome;
    private BigDecimal precoCompraBase;
    private BigDecimal ultimoPrecoRegistrado;
    private BigDecimal precoAnterior;
    private BigDecimal percentualVariacaoRecente;
    private BigDecimal menorPrecoHistorico;
    private BigDecimal maiorPrecoHistorico;
    private BigDecimal percentualVariacaoMenorHistorico;
    private BigDecimal precoMedioHistorico;
    private Integer totalRegistrosHistorico;
    private String tendencia; // "ALTA", "QUEDA", "ESTAVEL", "SEM_HISTORICO_SUFICIENTE"
    private String classificacaoOportunidade; // "OPORTUNIDADE_COMPRA", "PRECO_ESTAVEL", "PRECO_DESFAVORAVEL", "SEM_HISTORICO"
    private String mensagemAnalise;

    public VariacaoHistoricaProdutoDto() {
    }

    public Long getProdutoId() { return produtoId; }
    public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public BigDecimal getPrecoCompraBase() { return precoCompraBase; }
    public void setPrecoCompraBase(BigDecimal precoCompraBase) { this.precoCompraBase = precoCompraBase; }

    public BigDecimal getUltimoPrecoRegistrado() { return ultimoPrecoRegistrado; }
    public void setUltimoPrecoRegistrado(BigDecimal ultimoPrecoRegistrado) { this.ultimoPrecoRegistrado = ultimoPrecoRegistrado; }

    public BigDecimal getPrecoAnterior() { return precoAnterior; }
    public void setPrecoAnterior(BigDecimal precoAnterior) { this.precoAnterior = precoAnterior; }

    public BigDecimal getPercentualVariacaoRecente() { return percentualVariacaoRecente; }
    public void setPercentualVariacaoRecente(BigDecimal percentualVariacaoRecente) { this.percentualVariacaoRecente = percentualVariacaoRecente; }

    public BigDecimal getMenorPrecoHistorico() { return menorPrecoHistorico; }
    public void setMenorPrecoHistorico(BigDecimal menorPrecoHistorico) { this.menorPrecoHistorico = menorPrecoHistorico; }

    public BigDecimal getMaiorPrecoHistorico() { return maiorPrecoHistorico; }
    public void setMaiorPrecoHistorico(BigDecimal maiorPrecoHistorico) { this.maiorPrecoHistorico = maiorPrecoHistorico; }

    public BigDecimal getPercentualVariacaoMenorHistorico() { return percentualVariacaoMenorHistorico; }
    public void setPercentualVariacaoMenorHistorico(BigDecimal percentualVariacaoMenorHistorico) { this.percentualVariacaoMenorHistorico = percentualVariacaoMenorHistorico; }

    public BigDecimal getPrecoMedioHistorico() { return precoMedioHistorico; }
    public void setPrecoMedioHistorico(BigDecimal precoMedioHistorico) { this.precoMedioHistorico = precoMedioHistorico; }

    public Integer getTotalRegistrosHistorico() { return totalRegistrosHistorico; }
    public void setTotalRegistrosHistorico(Integer totalRegistrosHistorico) { this.totalRegistrosHistorico = totalRegistrosHistorico; }

    public String getTendencia() { return tendencia; }
    public void setTendencia(String tendencia) { this.tendencia = tendencia; }

    public String getClassificacaoOportunidade() { return classificacaoOportunidade; }
    public void setClassificacaoOportunidade(String classificacaoOportunidade) { this.classificacaoOportunidade = classificacaoOportunidade; }

    public String getMensagemAnalise() { return mensagemAnalise; }
    public void setMensagemAnalise(String mensagemAnalise) { this.mensagemAnalise = mensagemAnalise; }
}
