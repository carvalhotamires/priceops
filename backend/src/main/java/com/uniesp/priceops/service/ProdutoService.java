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
        // Critério de aceitação: Preços não podem conter valores negativos
        if (produto.getPrecoVendaAtual() != null && produto.getPrecoVendaAtual().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço de venda não pode ser negativo.");
        }
        if (produto.getPrecoCompraAtual() != null && produto.getPrecoCompraAtual().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço de compra não pode ser negativo.");
        }

        // Validação de código duplicado
        if (produto.getId() == null && produtoRepository.findByCodigo(produto.getCodigo()).isPresent()) {
            throw new IllegalArgumentException("Já existe um produto cadastrado com o código: " + produto.getCodigo());
        }

        return produtoRepository.save(produto);
    }

    public Produto atualizar(Long id, Produto produtoAtualizado) {
        Produto produtoExistente = buscarPorId(id);

        if (produtoAtualizado.getPrecoVendaAtual() != null && produtoAtualizado.getPrecoVendaAtual().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço de venda não pode ser negativo.");
        }

        produtoExistente.setNome(produtoAtualizado.getNome());
        produtoExistente.setCategoria(produtoAtualizado.getCategoria());
        produtoExistente.setUnidadeMedida(produtoAtualizado.getUnidadeMedida());
        produtoExistente.setPrecoVendaAtual(produtoAtualizado.getPrecoVendaAtual());
        produtoExistente.setEstoqueAtual(produtoAtualizado.getEstoqueAtual());
        produtoExistente.setEstoqueMinimo(produtoAtualizado.getEstoqueMinimo());
        produtoExistente.setStatus(produtoAtualizado.getStatus());

        return produtoRepository.save(produtoExistente);
    }

    public void deletar(Long id) {
        Produto produto = buscarPorId(id);
        produtoRepository.delete(produto);
    }
}