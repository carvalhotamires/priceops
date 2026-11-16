package com.uniesp.priceops.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 100)
    private String categoria;

    @Column(nullable = false, length = 30)
    private String unidadeMedida; // Ex: "UN", "Pacote", "Caixa"

    @Column(precision = 10, scale = 2)
    private BigDecimal precoCompraAtual;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoVendaAtual;

    @Column(nullable = false)
    private Integer estoqueAtual;

    @Column(nullable = false)
    private Integer estoqueMinimo;

    @Column(nullable = false, length = 20)
    private String status; // "ATIVO" ou "INATIVO"

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    public Produto() {
        this.status = "ATIVO";
        this.criadoEm = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(String unidadeMedida) { this.unidadeMedida = unidadeMedida; }

    public BigDecimal getPrecoCompraAtual() { return precoCompraAtual; }
    public void setPrecoCompraAtual(BigDecimal precoCompraAtual) { this.precoCompraAtual = precoCompraAtual; }

    public BigDecimal getPrecoVendaAtual() { return precoVendaAtual; }
    public void setPrecoVendaAtual(BigDecimal precoVendaAtual) { this.precoVendaAtual = precoVendaAtual; }

    public Integer getEstoqueAtual() { return estoqueAtual; }
    public void setEstoqueAtual(Integer estoqueAtual) { this.estoqueAtual = estoqueAtual; }

    public Integer getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(Integer estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
