package com.uniesp.priceops.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tb_importacoes_pdf",
        uniqueConstraints = @UniqueConstraint(columnNames = {"fornecedor_id", "hash_sha256"})
)
public class ImportacaoPdf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nomeOriginal;

    @Column(nullable = false, length = 120)
    private String nomeArmazenado;

    @Column(nullable = false, length = 500)
    private String caminhoArmazenamento;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long tamanhoBytes;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @Column(nullable = false, length = 100)
    private String usuarioResponsavel;

    @Column(length = 500)
    private String observacao;

    @Column(nullable = false, length = 30)
    private String statusImportacao; // RECEBIDO, EM_PROCESSAMENTO, PROCESSADO, ERRO

    @Column(name = "importado_em", nullable = false, updatable = false)
    private LocalDateTime importadoEm;

    public ImportacaoPdf() {
        this.contentType = "application/pdf";
        this.usuarioResponsavel = "SISTEMA";
        this.statusImportacao = "RECEBIDO";
        this.importadoEm = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNomeOriginal() { return nomeOriginal; }
    public void setNomeOriginal(String nomeOriginal) { this.nomeOriginal = nomeOriginal; }

    public String getNomeArmazenado() { return nomeArmazenado; }
    public void setNomeArmazenado(String nomeArmazenado) { this.nomeArmazenado = nomeArmazenado; }

    public String getCaminhoArmazenamento() { return caminhoArmazenamento; }
    public void setCaminhoArmazenamento(String caminhoArmazenamento) { this.caminhoArmazenamento = caminhoArmazenamento; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(Long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }

    public String getHashSha256() { return hashSha256; }
    public void setHashSha256(String hashSha256) { this.hashSha256 = hashSha256; }

    public Fornecedor getFornecedor() { return fornecedor; }
    public void setFornecedor(Fornecedor fornecedor) { this.fornecedor = fornecedor; }

    public String getUsuarioResponsavel() { return usuarioResponsavel; }
    public void setUsuarioResponsavel(String usuarioResponsavel) { this.usuarioResponsavel = usuarioResponsavel; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public String getStatusImportacao() { return statusImportacao; }
    public void setStatusImportacao(String statusImportacao) { this.statusImportacao = statusImportacao; }

    public LocalDateTime getImportadoEm() { return importadoEm; }
    public void setImportadoEm(LocalDateTime importadoEm) { this.importadoEm = importadoEm; }
}
