package com.uniesp.priceops.dto;

import com.uniesp.priceops.model.ItemImportadoPdf;

import java.math.BigDecimal;

public class ItemImportadoDto {

    private Long id;
    private Long importacaoPdfId;
    private Integer linhaTabela;
    private String codigoFornecedor;
    private String descricao;
    private BigDecimal precoUnitario;
    private Integer quantidadeEstoque;
    private String statusItem; // VINCULADO, PENDENTE_VINCULO, IGNORADO
    private String motivoPendencia;
    private Double confiancaMatching;

    // Dados do produto vinculado (se houver)
    private Long produtoVinculadoId;
    private String produtoVinculadoCodigo;
    private String produtoVinculadoNome;
    private BigDecimal precoCompraCadastrado;

    public ItemImportadoDto() {
    }

    public static ItemImportadoDto fromEntity(ItemImportadoPdf entity) {
        ItemImportadoDto dto = new ItemImportadoDto();
        dto.setId(entity.getId());
        dto.setImportacaoPdfId(entity.getImportacaoPdf() != null ? entity.getImportacaoPdf().getId() : null);
        dto.setLinhaTabela(entity.getLinhaTabela());
        dto.setCodigoFornecedor(entity.getCodigoFornecedor());
        dto.setDescricao(entity.getDescricao());
        dto.setPrecoUnitario(entity.getPrecoUnitario());
        dto.setQuantidadeEstoque(entity.getQuantidadeEstoque());
        dto.setStatusItem(entity.getStatusItem());
        dto.setMotivoPendencia(entity.getMotivoPendencia());
        dto.setConfiancaMatching(entity.getConfiancaMatching());

        if (entity.getProduto() != null) {
            dto.setProdutoVinculadoId(entity.getProduto().getId());
            dto.setProdutoVinculadoCodigo(entity.getProduto().getCodigo());
            dto.setProdutoVinculadoNome(entity.getProduto().getNome());
            dto.setPrecoCompraCadastrado(entity.getProduto().getPrecoCompraAtual());
        }

        return dto;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getImportacaoPdfId() {
        return importacaoPdfId;
    }

    public void setImportacaoPdfId(Long importacaoPdfId) {
        this.importacaoPdfId = importacaoPdfId;
    }

    public Integer getLinhaTabela() {
        return linhaTabela;
    }

    public void setLinhaTabela(Integer linhaTabela) {
        this.linhaTabela = linhaTabela;
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

    public Long getProdutoVinculadoId() {
        return produtoVinculadoId;
    }

    public void setProdutoVinculadoId(Long produtoVinculadoId) {
        this.produtoVinculadoId = produtoVinculadoId;
    }

    public String getProdutoVinculadoCodigo() {
        return produtoVinculadoCodigo;
    }

    public void setProdutoVinculadoCodigo(String produtoVinculadoCodigo) {
        this.produtoVinculadoCodigo = produtoVinculadoCodigo;
    }

    public String getProdutoVinculadoNome() {
        return produtoVinculadoNome;
    }

    public void setProdutoVinculadoNome(String produtoVinculadoNome) {
        this.produtoVinculadoNome = produtoVinculadoNome;
    }

    public BigDecimal getPrecoCompraCadastrado() {
        return precoCompraCadastrado;
    }

    public void setPrecoCompraCadastrado(BigDecimal precoCompraCadastrado) {
        this.precoCompraCadastrado = precoCompraCadastrado;
    }
}
