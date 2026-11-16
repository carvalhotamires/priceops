package com.uniesp.priceops.service;

import com.uniesp.priceops.dto.AvaliacaoPrecoRequest;
import com.uniesp.priceops.dto.AvaliacaoPrecoResponse;
import com.uniesp.priceops.model.AlertaPreco;
import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.repository.AlertaPrecoRepository;
import com.uniesp.priceops.repository.FornecedorRepository;
import com.uniesp.priceops.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class AlertaPrecoService {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal DOIS = new BigDecimal("2");
    private static final Set<String> STATUS_VALIDOS = Set.of(
            "PENDENTE", "VISUALIZADO", "RESOLVIDO", "IGNORADO"
    );

    @Autowired
    private AlertaPrecoRepository alertaPrecoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Value("${priceops.alerta.percentual-alta-abusiva:15.00}")
    private BigDecimal limiteAltaAbusivaPadrao;

    @Value("${priceops.alerta.percentual-queda-suspeita:40.00}")
    private BigDecimal limiteQuedaSuspeitaPadrao;

    public AvaliacaoPrecoResponse avaliarPreco(AvaliacaoPrecoRequest request) {
        validarRequest(request);

        Produto produto = produtoRepository.findById(request.getProdutoId())
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado com id: " + request.getProdutoId()));

        Fornecedor fornecedor = null;
        if (request.getFornecedorId() != null) {
            fornecedor = fornecedorRepository.findById(request.getFornecedorId())
                    .orElseThrow(() -> new NoSuchElementException("Fornecedor não encontrado com id: " + request.getFornecedorId()));
        }

        BigDecimal precoOfertado = request.getPrecoOfertado().setScale(2, RoundingMode.HALF_UP);
        BigDecimal limiteAlta = request.getLimitePercentualCustomizado() != null
                ? request.getLimitePercentualCustomizado().setScale(2, RoundingMode.HALF_UP)
                : limiteAltaAbusivaPadrao.setScale(2, RoundingMode.HALF_UP);

        List<AlertaPreco> alertasGerados = new ArrayList<>();
        BigDecimal percentualVariacaoCompra = null;
        String origemFornecedor = fornecedor != null ? " pelo fornecedor '" + fornecedor.getNomeFantasia() + "'" : "";

        // 1. Análise em relação ao Preço de Compra Atual (alta abusiva e queda discrepante)
        BigDecimal precoCompraAtual = produto.getPrecoCompraAtual();
        if (precoCompraAtual != null && precoCompraAtual.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diferencaCompra = precoOfertado.subtract(precoCompraAtual).setScale(2, RoundingMode.HALF_UP);
            percentualVariacaoCompra = diferencaCompra.multiply(CEM)
                    .divide(precoCompraAtual, 2, RoundingMode.HALF_UP);

            if (percentualVariacaoCompra.compareTo(limiteAlta) >= 0) {
                String severidade = percentualVariacaoCompra.compareTo(limiteAlta.multiply(DOIS)) >= 0
                        ? "CRITICA"
                        : "ALTA";

                String mensagem = String.format(Locale.US,
                        "Variação abusiva detectada no produto '%s'%s: preço subiu %.2f%% (de R$ %.2f para R$ %.2f), ultrapassando o limite tolerado de %.2f%%.",
                        produto.getNome(), origemFornecedor, percentualVariacaoCompra,
                        precoCompraAtual, precoOfertado, limiteAlta);

                AlertaPreco alertaAlta = criarESalvarAlerta(
                        produto, fornecedor, "VARIACAO_ABUSIVA_ALTA", severidade,
                        precoCompraAtual, precoOfertado, diferencaCompra,
                        percentualVariacaoCompra, limiteAlta, mensagem
                );
                alertasGerados.add(alertaAlta);
            } else if (percentualVariacaoCompra.compareTo(limiteQuedaSuspeitaPadrao.negate()) <= 0) {
                String severidade = percentualVariacaoCompra.compareTo(new BigDecimal("-70.00")) <= 0
                        ? "ALTA"
                        : "MEDIA";

                String mensagem = String.format(Locale.US,
                        "Discrepância atípica detectada no produto '%s'%s: queda brusca de %.2f%% (de R$ %.2f para R$ %.2f). Verifique possível erro de cotação ou divergência de SKU.",
                        produto.getNome(), origemFornecedor, percentualVariacaoCompra.abs(),
                        precoCompraAtual, precoOfertado);

                AlertaPreco alertaQueda = criarESalvarAlerta(
                        produto, fornecedor, "DISCREPANCIA_QUEDA_ATIPICA", severidade,
                        precoCompraAtual, precoOfertado, diferencaCompra,
                        percentualVariacaoCompra, limiteQuedaSuspeitaPadrao, mensagem
                );
                alertasGerados.add(alertaQueda);
            }
        }

        // 2. Análise de Discrepância contra o Preço de Venda Atual da Loja (Margem Comprometida / Prejuízo)
        BigDecimal precoVendaAtual = produto.getPrecoVendaAtual();
        if (precoVendaAtual != null
                && precoVendaAtual.compareTo(BigDecimal.ZERO) > 0
                && precoOfertado.compareTo(precoVendaAtual) >= 0) {

            BigDecimal diferencaVenda = precoOfertado.subtract(precoVendaAtual).setScale(2, RoundingMode.HALF_UP);
            BigDecimal percentualSobreVenda = diferencaVenda.multiply(CEM)
                    .divide(precoVendaAtual, 2, RoundingMode.HALF_UP);

            String mensagemMargem = String.format(Locale.US,
                    "Margem comprometida no produto '%s'%s: o custo ofertado (R$ %.2f) é igual ou superior ao preço de venda atual da loja (R$ %.2f).",
                    produto.getNome(), origemFornecedor, precoOfertado, precoVendaAtual);

            AlertaPreco alertaMargem = criarESalvarAlerta(
                    produto, fornecedor, "MARGEM_COMPROMETIDA", "CRITICA",
                    precoVendaAtual, precoOfertado, diferencaVenda,
                    percentualSobreVenda, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), mensagemMargem
            );
            alertasGerados.add(alertaMargem);
        }

        AvaliacaoPrecoResponse response = new AvaliacaoPrecoResponse();
        response.setProdutoId(produto.getId());
        response.setProdutoCodigo(produto.getCodigo());
        response.setProdutoNome(produto.getNome());
        response.setPrecoCompraAtual(precoCompraAtual);
        response.setPrecoVendaAtual(precoVendaAtual);
        response.setPrecoOfertado(precoOfertado);
        response.setPercentualVariacaoCompra(percentualVariacaoCompra);
        response.setAnomaliaDetectada(!alertasGerados.isEmpty());
        response.setAlertasGerados(alertasGerados);
        response.setResumo(alertasGerados.isEmpty()
                ? "Preço dentro da faixa normal de variação. Nenhum alerta gerado."
                : "Atenção: " + alertasGerados.size() + " alerta(s) de discrepância/variação gerado(s).");

        return response;
    }

    public List<AvaliacaoPrecoResponse> avaliarLote(List<AvaliacaoPrecoRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("A lista de itens para avaliação em lote não pode estar vazia.");
        }
        List<AvaliacaoPrecoResponse> resultados = new ArrayList<>();
        for (AvaliacaoPrecoRequest item : itens) {
            resultados.add(avaliarPreco(item));
        }
        return resultados;
    }

    public List<AlertaPreco> listarAlertas(
            String status,
            String severidade,
            String tipoAlerta,
            Long produtoId,
            Long fornecedorId) {

        if (status != null && !status.trim().isEmpty()) {
            return alertaPrecoRepository.findByStatusNotificacaoIgnoreCaseOrderByCriadoEmDesc(status.trim());
        }
        if (severidade != null && !severidade.trim().isEmpty()) {
            return alertaPrecoRepository.findBySeveridadeIgnoreCaseOrderByCriadoEmDesc(severidade.trim());
        }
        if (tipoAlerta != null && !tipoAlerta.trim().isEmpty()) {
            return alertaPrecoRepository.findByTipoAlertaIgnoreCaseOrderByCriadoEmDesc(tipoAlerta.trim());
        }
        if (produtoId != null) {
            return alertaPrecoRepository.findByProdutoIdOrderByCriadoEmDesc(produtoId);
        }
        if (fornecedorId != null) {
            return alertaPrecoRepository.findByFornecedorIdOrderByCriadoEmDesc(fornecedorId);
        }
        return alertaPrecoRepository.findAllByOrderByCriadoEmDesc();
    }

    public Map<String, Object> obterPainelNotificacoesPendentes() {
        List<AlertaPreco> pendentes = alertaPrecoRepository.findByStatusNotificacaoIgnoreCaseOrderByCriadoEmDesc("PENDENTE");

        Map<String, Object> painel = new LinkedHashMap<>();
        painel.put("totalPendentes", pendentes.size());
        painel.put("criticos", alertaPrecoRepository.countByStatusNotificacaoIgnoreCaseAndSeveridadeIgnoreCase("PENDENTE", "CRITICA"));
        painel.put("altos", alertaPrecoRepository.countByStatusNotificacaoIgnoreCaseAndSeveridadeIgnoreCase("PENDENTE", "ALTA"));
        painel.put("medios", alertaPrecoRepository.countByStatusNotificacaoIgnoreCaseAndSeveridadeIgnoreCase("PENDENTE", "MEDIA"));
        painel.put("baixos", alertaPrecoRepository.countByStatusNotificacaoIgnoreCaseAndSeveridadeIgnoreCase("PENDENTE", "BAIXA"));
        painel.put("notificacoes", pendentes);
        return painel;
    }

    public AlertaPreco buscarPorId(Long id) {
        return alertaPrecoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Alerta não encontrado com id: " + id));
    }

    public AlertaPreco marcarComoVisualizado(Long id) {
        return atualizarStatus(id, "VISUALIZADO", null);
    }

    public AlertaPreco atualizarStatus(Long id, String novoStatus, String justificativa) {
        AlertaPreco alerta = buscarPorId(id);

        if (novoStatus == null || novoStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("O status da notificação é obrigatório.");
        }

        String statusNormalizado = novoStatus.trim().toUpperCase();
        if (!STATUS_VALIDOS.contains(statusNormalizado)) {
            throw new IllegalArgumentException("Status inválido. Valores permitidos: " + STATUS_VALIDOS);
        }

        alerta.setStatusNotificacao(statusNormalizado);
        if (justificativa != null && !justificativa.trim().isEmpty()) {
            alerta.setJustificativaResolucao(justificativa.trim());
        }
        alerta.setAtualizadoEm(LocalDateTime.now());

        return alertaPrecoRepository.save(alerta);
    }

    public void deletar(Long id) {
        AlertaPreco alerta = buscarPorId(id);
        alertaPrecoRepository.delete(alerta);
    }

    private void validarRequest(AvaliacaoPrecoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Os dados para avaliação de preço são obrigatórios.");
        }
        if (request.getProdutoId() == null) {
            throw new IllegalArgumentException("O ID do produto é obrigatório.");
        }
        if (request.getPrecoOfertado() == null || request.getPrecoOfertado().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O preço ofertado deve ser maior que zero.");
        }
        if (request.getLimitePercentualCustomizado() != null
                && request.getLimitePercentualCustomizado().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O limite percentual customizado deve ser maior que zero.");
        }
    }

    private AlertaPreco criarESalvarAlerta(
            Produto produto,
            Fornecedor fornecedor,
            String tipoAlerta,
            String severidade,
            BigDecimal precoReferencia,
            BigDecimal precoInformado,
            BigDecimal diferencaValor,
            BigDecimal percentualVariacao,
            BigDecimal limiteToleradoPercentual,
            String mensagem) {

        AlertaPreco alerta = new AlertaPreco();
        alerta.setProduto(produto);
        alerta.setFornecedor(fornecedor);
        alerta.setTipoAlerta(tipoAlerta);
        alerta.setSeveridade(severidade);
        alerta.setPrecoReferencia(precoReferencia);
        alerta.setPrecoInformado(precoInformado);
        alerta.setDiferencaValor(diferencaValor);
        alerta.setPercentualVariacao(percentualVariacao);
        alerta.setLimiteToleradoPercentual(limiteToleradoPercentual);
        alerta.setMensagem(mensagem);
        alerta.setStatusNotificacao("PENDENTE");

        return alertaPrecoRepository.save(alerta);
    }
}
