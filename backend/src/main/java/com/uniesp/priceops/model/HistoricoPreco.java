package com.uniesp.priceops.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_historico_precos")
public class HistoricoPreco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "produto_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Produto produto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Fornecedor fornecedor;

    @Column(name = "preco_registrado", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal precoRegistrado;

    @Column(nullable = false, updatable = false, length = 40)
    private String origem; // Ex: "MANUAL", "IMPORTACAO_PDF"

    @Column(name = "importacao_pdf_id", updatable = false)
    private Long importacaoPdfId;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    private LocalDateTime registradoEm;

    public HistoricoPreco() {
        this.origem = "MANUAL";
        this.registradoEm = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public Fornecedor getFornecedor() { return fornecedor; }
    public void setFornecedor(Fornecedor fornecedor) { this.fornecedor = fornecedor; }

    public BigDecimal getPrecoRegistrado() { return precoRegistrado; }
    public void setPrecoRegistrado(BigDecimal precoRegistrado) { this.precoRegistrado = precoRegistrado; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public Long getImportacaoPdfId() { return importacaoPdfId; }
    public void setImportacaoPdfId(Long importacaoPdfId) { this.importacaoPdfId = importacaoPdfId; }

    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(LocalDateTime registradoEm) { this.registradoEm = registradoEm; }
}
