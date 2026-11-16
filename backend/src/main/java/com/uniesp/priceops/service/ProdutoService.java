package com.uniesp.priceops.service;

import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    public List<Produto> listarTodos() {
        return produtoRepository.findAll();
    }

    public List<Produto> buscarPorNome(String nome) {
        return produtoRepository.findByNomeContainingIgnoreCase(nome);
    }

    public Produto buscarPorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com id: " + id));
    }

    public Produto salvar(Produto produto) {
        validarProduto(produto);

        String codigoNormalizado = produto.getCodigo().trim().toUpperCase();
        produto.setCodigo(codigoNormalizado);
        produto.setUnidadeMedida(produto.getUnidadeMedida().trim().toUpperCase());

        if (produto.getId() == null && produtoRepository.findByCodigo(codigoNormalizado).isPresent()) {
            throw new IllegalArgumentException("Já existe um produto cadastrado com o código: " + codigoNormalizado);
        }

        if (produto.getEstoqueAtual() == null) {
            produto.setEstoqueAtual(0);
        }
        if (produto.getEstoqueMinimo() == null) {
            produto.setEstoqueMinimo(0);
        }
        if (produto.getStatus() == null || produto.getStatus().trim().isEmpty()) {
            produto.setStatus("ATIVO");
        } else {
            produto.setStatus(produto.getStatus().trim().toUpperCase());
        }

        return produtoRepository.save(produto);
    }

    public Produto atualizar(Long id, Produto produtoAtualizado) {
        Produto produtoExistente = buscarPorId(id);

        if (produtoAtualizado.getCodigo() == null || produtoAtualizado.getCodigo().trim().isEmpty()) {
            produtoAtualizado.setCodigo(produtoExistente.getCodigo());
        }

        validarProduto(produtoAtualizado);

        produtoExistente.setNome(produtoAtualizado.getNome().trim());
        produtoExistente.setCategoria(produtoAtualizado.getCategoria().trim());
        produtoExistente.setUnidadeMedida(produtoAtualizado.getUnidadeMedida().trim().toUpperCase());
        produtoExistente.setPrecoCompraAtual(produtoAtualizado.getPrecoCompraAtual());
        produtoExistente.setPrecoVendaAtual(produtoAtualizado.getPrecoVendaAtual());
        if (produtoAtualizado.getEstoqueAtual() != null) {
            produtoExistente.setEstoqueAtual(produtoAtualizado.getEstoqueAtual());
        }
        if (produtoAtualizado.getEstoqueMinimo() != null) {
            produtoExistente.setEstoqueMinimo(produtoAtualizado.getEstoqueMinimo());
        }
        if (produtoAtualizado.getStatus() != null && !produtoAtualizado.getStatus().trim().isEmpty()) {
            produtoExistente.setStatus(produtoAtualizado.getStatus().trim().toUpperCase());
        }

        return produtoRepository.save(produtoExistente);
    }

    public void deletar(Long id) {
        Produto produto = buscarPorId(id);
        produtoRepository.delete(produto);
    }

    private void validarProduto(Produto produto) {
        if (produto == null) {
            throw new IllegalArgumentException("Os dados do produto são obrigatórios.");
        }
        if (produto.getCodigo() == null || produto.getCodigo().trim().isEmpty()) {
            throw new IllegalArgumentException("O código de referência do produto é obrigatório.");
        }
        if (produto.getNome() == null || produto.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do produto é obrigatório.");
        }
        if (produto.getCategoria() == null || produto.getCategoria().trim().isEmpty()) {
            throw new IllegalArgumentException("A categoria do produto é obrigatória.");
        }
        if (produto.getUnidadeMedida() == null || produto.getUnidadeMedida().trim().isEmpty()) {
            throw new IllegalArgumentException("A unidade de medida (ex: UN, CX) é obrigatória.");
        }
        if (produto.getPrecoVendaAtual() == null) {
            throw new IllegalArgumentException("O preço de venda é obrigatório.");
        }
        if (produto.getPrecoVendaAtual().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço de venda não pode ser negativo.");
        }
        if (produto.getPrecoCompraAtual() != null && produto.getPrecoCompraAtual().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço de compra não pode ser negativo.");
        }
        if (produto.getEstoqueAtual() != null && produto.getEstoqueAtual() < 0) {
            throw new IllegalArgumentException("O estoque atual não pode ser negativo.");
        }
        if (produto.getEstoqueMinimo() != null && produto.getEstoqueMinimo() < 0) {
            throw new IllegalArgumentException("O estoque mínimo não pode ser negativo.");
        }
    }
}
