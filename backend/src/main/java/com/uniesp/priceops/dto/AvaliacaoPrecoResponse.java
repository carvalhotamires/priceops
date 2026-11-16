package com.uniesp.priceops.dto;

import com.uniesp.priceops.model.AlertaPreco;

import java.math.BigDecimal;
import java.util.List;

public class AvaliacaoPrecoResponse {

    private Long produtoId;
    private String produtoCodigo;
    private String produtoNome;
    private BigDecimal precoCompraAtual;
    private BigDecimal precoVendaAtual;
    private BigDecimal precoOfertado;
    private BigDecimal percentualVariacaoCompra;
    private boolean anomaliaDetectada;
    private String resumo;
    private List<AlertaPreco> alertasGerados;

    public AvaliacaoPrecoResponse() {
    }

    public Long getProdutoId() { return produtoId; }
    public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }

    public String getProdutoCodigo() { return produtoCodigo; }
    public void setProdutoCodigo(String produtoCodigo) { this.produtoCodigo = produtoCodigo; }

    public String getProdutoNome() { return produtoNome; }
    public void setProdutoNome(String produtoNome) { this.produtoNome = produtoNome; }

    public BigDecimal getPrecoCompraAtual() { return precoCompraAtual; }
    public void setPrecoCompraAtual(BigDecimal precoCompraAtual) { this.precoCompraAtual = precoCompraAtual; }

    public BigDecimal getPrecoVendaAtual() { return precoVendaAtual; }
    public void setPrecoVendaAtual(BigDecimal precoVendaAtual) { this.precoVendaAtual = precoVendaAtual; }

    public BigDecimal getPrecoOfertado() { return precoOfertado; }
    public void setPrecoOfertado(BigDecimal precoOfertado) { this.precoOfertado = precoOfertado; }

    public BigDecimal getPercentualVariacaoCompra() { return percentualVariacaoCompra; }
    public void setPercentualVariacaoCompra(BigDecimal percentualVariacaoCompra) {
        this.percentualVariacaoCompra = percentualVariacaoCompra;
    }

    public boolean isAnomaliaDetectada() { return anomaliaDetectada; }
    public void setAnomaliaDetectada(boolean anomaliaDetectada) { this.anomaliaDetectada = anomaliaDetectada; }

    public String getResumo() { return resumo; }
    public void setResumo(String resumo) { this.resumo = resumo; }

    public List<AlertaPreco> getAlertasGerados() { return alertasGerados; }
    public void setAlertasGerados(List<AlertaPreco> alertasGerados) { this.alertasGerados = alertasGerados; }
}
