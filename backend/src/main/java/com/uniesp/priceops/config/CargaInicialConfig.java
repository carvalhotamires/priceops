package com.uniesp.priceops.config;

import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.repository.FornecedorRepository;
import com.uniesp.priceops.repository.ProdutoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class CargaInicialConfig {

    @Bean
    CommandLineRunner carregarCatalogoInicialInformatica(
            ProdutoRepository produtoRepository,
            FornecedorRepository fornecedorRepository) {
        return args -> {
            if (produtoRepository.count() == 0) {
                Produto inf01 = criarProduto("INF-01", "Mouse Óptico USB", "Periféricos", "UN",
                        "18.50", "34.90", 45, 10);
                Produto inf02 = criarProduto("INF-02", "Cabo HDMI 2.0", "Cabos e Adaptadores", "UN",
                        "14.00", "29.90", 60, 15);
                Produto inf03 = criarProduto("INF-03", "Teclado ABNT2", "Periféricos", "UN",
                        "32.00", "59.90", 30, 8);
                Produto inf04 = criarProduto("INF-04", "Pendrive 64GB", "Armazenamento", "CX",
                        "210.00", "349.00", 12, 4);
                Produto inf05 = criarProduto("INF-05", "Headset Corporativo P2", "Áudio e Periféricos", "UN",
                        "48.00", "89.90", 25, 6);

                produtoRepository.saveAll(List.of(inf01, inf02, inf03, inf04, inf05));
            }

            if (fornecedorRepository.count() == 0) {
                Fornecedor dist1 = new Fornecedor();
                dist1.setRazaoSocial("All Nations Comércio Exterior S.A.");
                dist1.setNomeFantasia("All Nations Informática");
                dist1.setCnpj("68552215000180");
                dist1.setEmail("comercial@allnations.com.br");
                dist1.setTelefone("(21) 3523-9000");
                dist1.setContatoNome("Ricardo Mendes");
                dist1.setCidade("Rio de Janeiro");
                dist1.setUf("RJ");
                dist1.setPrazoEntregaDias(5);
                dist1.setStatus("ATIVO");

                Fornecedor dist2 = new Fornecedor();
                dist2.setRazaoSocial("Pauta Distribuição e Logística S.A.");
                dist2.setNomeFantasia("Pauta Distribuidora");
                dist2.setCnpj("83878959000105");
                dist2.setEmail("vendas@pauta.com.br");
                dist2.setTelefone("(48) 3281-7000");
                dist2.setContatoNome("Fernanda Costa");
                dist2.setCidade("São José");
                dist2.setUf("SC");
                dist2.setPrazoEntregaDias(4);
                dist2.setStatus("ATIVO");

                fornecedorRepository.saveAll(List.of(dist1, dist2));
            }
        };
    }

    private Produto criarProduto(
            String codigo,
            String nome,
            String categoria,
            String unidadeMedida,
            String precoCompra,
            String precoVenda,
            int estoqueAtual,
            int estoqueMinimo) {
        Produto p = new Produto();
        p.setCodigo(codigo);
        p.setNome(nome);
        p.setCategoria(categoria);
        p.setUnidadeMedida(unidadeMedida);
        p.setPrecoCompraAtual(new BigDecimal(precoCompra));
        p.setPrecoVendaAtual(new BigDecimal(precoVenda));
        p.setEstoqueAtual(estoqueAtual);
        p.setEstoqueMinimo(estoqueMinimo);
        p.setStatus("ATIVO");
        return p;
    }
}
