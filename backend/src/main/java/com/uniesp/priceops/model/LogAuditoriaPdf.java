package com.uniesp.priceops.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_auditoria_pdf")
public class LogAuditoriaPdf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String statusAuditoria; // UPLOAD_SUCESSO, DUPLICIDADE_BLOQUEADA, ARQUIVO_INVALIDO, FORNECEDOR_INATIVO, DOWNLOAD_ARQUIVO, STATUS_ATUALIZADO, ARQUIVO_REMOVIDO

    @Column(nullable = false, length = 255)
    private String nomeArquivo;

    @Column(name = "hash_sha256", length = 64)
    private String hashSha256;

    @Column
    private Long tamanhoBytes;

    @Column(name = "importacao_pdf_id")
    private Long importacaoPdfId;

    @Column(name = "fornecedor_id")
    private Long fornecedorId;

    @Column(nullable = false, length = 100)
    private String usuarioResponsavel;

    @Column(nullable = false, length = 500)
    private String detalhes;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;

    public LogAuditoriaPdf() {
        this.usuarioResponsavel = "SISTEMA";
        this.dataHora = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatusAuditoria() { return statusAuditoria; }
    public void setStatusAuditoria(String statusAuditoria) { this.statusAuditoria = statusAuditoria; }

    public String getNomeArquivo() { return nomeArquivo; }
    public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }

    public String getHashSha256() { return hashSha256; }
    public void setHashSha256(String hashSha256) { this.hashSha256 = hashSha256; }

    public Long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(Long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }

    public Long getImportacaoPdfId() { return importacaoPdfId; }
    public void setImportacaoPdfId(Long importacaoPdfId) { this.importacaoPdfId = importacaoPdfId; }

    public Long getFornecedorId() { return fornecedorId; }
    public void setFornecedorId(Long fornecedorId) { this.fornecedorId = fornecedorId; }

    public String getUsuarioResponsavel() { return usuarioResponsavel; }
    public void setUsuarioResponsavel(String usuarioResponsavel) { this.usuarioResponsavel = usuarioResponsavel; }

    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
