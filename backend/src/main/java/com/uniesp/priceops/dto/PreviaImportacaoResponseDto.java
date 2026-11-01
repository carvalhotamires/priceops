package com.uniesp.priceops.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PreviaImportacaoResponseDto {

    private Long importacaoId;
    private String nomeArquivoOriginal;
    private Long fornecedorId;
    private String fornecedorNome;
    private String statusImportacao;
    private LocalDateTime dataImportacao;

    private int totalItens;
    private int totalVinculados;
    private int totalPendentes;
    private int totalIgnorados;

    private List<ItemImportadoDto> itens = new ArrayList<>();

    public PreviaImportacaoResponseDto() {
    }

    public Long getImportacaoId() {
        return importacaoId;
    }

    public void setImportacaoId(Long importacaoId) {
        this.importacaoId = importacaoId;
    }

    public String getNomeArquivoOriginal() {
        return nomeArquivoOriginal;
    }

    public void setNomeArquivoOriginal(String nomeArquivoOriginal) {
        this.nomeArquivoOriginal = nomeArquivoOriginal;
    }

    public Long getFornecedorId() {
        return fornecedorId;
    }

    public void setFornecedorId(Long fornecedorId) {
        this.fornecedorId = fornecedorId;
    }

    public String getFornecedorNome() {
        return fornecedorNome;
    }

    public void setFornecedorNome(String fornecedorNome) {
        this.fornecedorNome = fornecedorNome;
    }

    public String getStatusImportacao() {
        return statusImportacao;
    }

    public void setStatusImportacao(String statusImportacao) {
        this.statusImportacao = statusImportacao;
    }

    public LocalDateTime getDataImportacao() {
        return dataImportacao;
    }

    public void setDataImportacao(LocalDateTime dataImportacao) {
        this.dataImportacao = dataImportacao;
    }

    public int getTotalItens() {
        return totalItens;
    }

    public void setTotalItens(int totalItens) {
        this.totalItens = totalItens;
    }

    public int getTotalVinculados() {
        return totalVinculados;
    }

    public void setTotalVinculados(int totalVinculados) {
        this.totalVinculados = totalVinculados;
    }

    public int getTotalPendentes() {
        return totalPendentes;
    }

    public void setTotalPendentes(int totalPendentes) {
        this.totalPendentes = totalPendentes;
    }

    public int getTotalIgnorados() {
        return totalIgnorados;
    }

    public void setTotalIgnorados(int totalIgnorados) {
        this.totalIgnorados = totalIgnorados;
    }

    public List<ItemImportadoDto> getItens() {
        return itens;
    }

    public void setItens(List<ItemImportadoDto> itens) {
        this.itens = itens;
    }
}
