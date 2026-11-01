package com.uniesp.priceops.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_itens_importacao_pdf")
public class ItemImportadoPdf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "importacao_pdf_id", nullable = false)
    private ImportacaoPdf importacaoPdf;

    @Column(name = "codigo_fornecedor", length = 100)
    private String codigoFornecedor;

    @Column(nullable = false, length = 300)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "quantidade_estoque")
    private Integer quantidadeEstoque;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(nullable = false, length = 30)
    private String statusItem; // VINCULADO, PENDENTE_VINCULO, IGNORADO

    @Column(name = "motivo_pendencia", length = 255)
    private String motivoPendencia;

    @Column(name = "confianca_matching")
    private Double confiancaMatching;

    @Column(name = "linha_tabela")
    private Integer linhaTabela;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public ItemImportadoPdf() {
        this.statusItem = "PENDENTE_VINCULO";
        this.criadoEm = LocalDateTime.now();
    }

    public ItemImportadoPdf(ImportacaoPdf importacaoPdf, String codigoFornecedor, String descricao,
                            BigDecimal precoUnitario, Integer quantidadeEstoque, Integer linhaTabela) {
        this();
        this.importacaoPdf = importacaoPdf;
        this.codigoFornecedor = codigoFornecedor;
        this.descricao = descricao;
        this.precoUnitario = precoUnitario;
        this.quantidadeEstoque = quantidadeEstoque;
        this.linhaTabela = linhaTabela;
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

    public ImportacaoPdf getImportacaoPdf() {
        return importacaoPdf;
    }

    public void setImportacaoPdf(ImportacaoPdf importacaoPdf) {
        this.importacaoPdf = importacaoPdf;
    }

    public String getCodigoFornecedor() {
        return codigoFornecedor;
    }

    public void setCodigoFornecedor(String codigoFornecedor) {
        this.codigoFornecedor = codigoFornecedor;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public Integer getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(Integer quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public String getStatusItem() {
        return statusItem;
    }

    public void setStatusItem(String statusItem) {
        this.statusItem = statusItem;
    }

    public String getMotivoPendencia() {
        return motivoPendencia;
    }

    public void setMotivoPendencia(String motivoPendencia) {
        this.motivoPendencia = motivoPendencia;
    }

    public Double getConfiancaMatching() {
        return confiancaMatching;
    }

    public void setConfiancaMatching(Double confiancaMatching) {
        this.confiancaMatching = confiancaMatching;
    }

    public Integer getLinhaTabela() {
        return linhaTabela;
    }

    public void setLinhaTabela(Integer linhaTabela) {
        this.linhaTabela = linhaTabela;
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
