package com.uniesp.priceops.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_alertas_preco")
public class AlertaPreco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    @Column(nullable = false, length = 50)
    private String tipoAlerta; // VARIACAO_ABUSIVA_ALTA, DISCREPANCIA_QUEDA_ATIPICA, MARGEM_COMPROMETIDA

    @Column(nullable = false, length = 20)
    private String severidade; // BAIXA, MEDIA, ALTA, CRITICA

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoReferencia;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoInformado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal diferencaValor;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal percentualVariacao;

    @Column(precision = 10, scale = 2)
    private BigDecimal limiteToleradoPercentual;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Column(nullable = false, length = 25)
    private String statusNotificacao; // PENDENTE, VISUALIZADO, RESOLVIDO, IGNORADO

    @Column(length = 500)
    private String justificativaResolucao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public AlertaPreco() {
        this.statusNotificacao = "PENDENTE";
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public Fornecedor getFornecedor() { return fornecedor; }
    public void setFornecedor(Fornecedor fornecedor) { this.fornecedor = fornecedor; }

    public String getTipoAlerta() { return tipoAlerta; }
    public void setTipoAlerta(String tipoAlerta) { this.tipoAlerta = tipoAlerta; }

    public String getSeveridade() { return severidade; }
    public void setSeveridade(String severidade) { this.severidade = severidade; }

    public BigDecimal getPrecoReferencia() { return precoReferencia; }
    public void setPrecoReferencia(BigDecimal precoReferencia) { this.precoReferencia = precoReferencia; }

    public BigDecimal getPrecoInformado() { return precoInformado; }
    public void setPrecoInformado(BigDecimal precoInformado) { this.precoInformado = precoInformado; }

    public BigDecimal getDiferencaValor() { return diferencaValor; }
    public void setDiferencaValor(BigDecimal diferencaValor) { this.diferencaValor = diferencaValor; }

    public BigDecimal getPercentualVariacao() { return percentualVariacao; }
    public void setPercentualVariacao(BigDecimal percentualVariacao) { this.percentualVariacao = percentualVariacao; }

    public BigDecimal getLimiteToleradoPercentual() { return limiteToleradoPercentual; }
    public void setLimiteToleradoPercentual(BigDecimal limiteToleradoPercentual) { this.limiteToleradoPercentual = limiteToleradoPercentual; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public String getStatusNotificacao() { return statusNotificacao; }
    public void setStatusNotificacao(String statusNotificacao) { this.statusNotificacao = statusNotificacao; }

    public String getJustificativaResolucao() { return justificativaResolucao; }
    public void setJustificativaResolucao(String justificativaResolucao) { this.justificativaResolucao = justificativaResolucao; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
