package com.uniesp.priceops.service;

import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.model.ImportacaoPdf;
import com.uniesp.priceops.model.ItemImportadoPdf;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfExtractorServiceTest {

    private PdfExtractorService extractorService;
    private ImportacaoPdf importacaoFake;

    @BeforeEach
    void setUp() {
        extractorService = new PdfExtractorService();

        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setId(1L);
        fornecedor.setNomeFantasia("Distribuidora Tech");

        importacaoFake = new ImportacaoPdf();
        importacaoFake.setId(10L);
        importacaoFake.setNomeOriginal("tabela_precos_novembro.pdf");
        importacaoFake.setFornecedor(fornecedor);
    }

    private byte[] gerarPdfSimulado(List<String> linhas) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.setLeading(14.5f);
                contentStream.newLineAtOffset(50, 700);

                for (String linha : linhas) {
                    contentStream.showText(linha);
                    contentStream.newLine();
                }

                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("Deve extrair itens estruturados de uma tabela simples em PDF")
    void deveExtrairItensEstruturadosDePdf() throws IOException {
        List<String> linhasTabela = List.of(
                "TABELA DE PRECOS - DISTRIBUIDORA TECH",
                "CODIGO | DESCRICAO | VALOR (R$) | QTD",
                "INF-01 | SSD Kingston 480GB A400 SATA | R$ 189,90 | 50",
                "INF-02 | Memoria RAM Kingston Fury 16GB DDR4 3200MHz | 245,50 | 25",
                "INF-03 | Processador Intel Core i5-12400F 4.4GHz | 799,00 | 12"
        );

        byte[] pdfBytes = gerarPdfSimulado(linhasTabela);
        List<ItemImportadoPdf> itens = extractorService.extrairItens(importacaoFake, pdfBytes);

        assertThat(itens).isNotNull().hasSize(3);

        ItemImportadoPdf item1 = itens.get(0);
        assertThat(item1.getCodigoFornecedor()).isEqualTo("INF-01");
        assertThat(item1.getDescricao()).contains("SSD Kingston 480GB A400");
        assertThat(item1.getPrecoUnitario()).isEqualByComparingTo(new BigDecimal("189.90"));
        assertThat(item1.getQuantidadeEstoque()).isEqualTo(50);
        assertThat(item1.getStatusItem()).isEqualTo("PENDENTE_VINCULO");

        ItemImportadoPdf item2 = itens.get(1);
        assertThat(item2.getCodigoFornecedor()).isEqualTo("INF-02");
        assertThat(item2.getDescricao()).contains("Memoria RAM Kingston Fury 16GB");
        assertThat(item2.getPrecoUnitario()).isEqualByComparingTo(new BigDecimal("245.50"));
        assertThat(item2.getQuantidadeEstoque()).isEqualTo(25);

        ItemImportadoPdf item3 = itens.get(2);
        assertThat(item3.getCodigoFornecedor()).isEqualTo("INF-03");
        assertThat(item3.getPrecoUnitario()).isEqualByComparingTo(new BigDecimal("799.00"));
        assertThat(item3.getQuantidadeEstoque()).isEqualTo(12);
    }

    @Test
    @DisplayName("Deve ignorar cabeçalhos, rodapés e linhas vazias sem valores monetários")
    void deveIgnorarCabecalhosELinhasSemPreco() throws IOException {
        List<String> linhasTabela = List.of(
                "RELATORIO DE PRODUTOS DISPONIVEIS",
                "Emitido em: 01/11/2026",
                "Condicoes de pagamento: 30 dias",
                "-----------------------------------------",
                "INF-04 | Placa Mae Asus Prime B550M-A | 620,00 | 8",
                "Aviso: Precos sujeitos a alteracao sem aviso previo."
        );

        byte[] pdfBytes = gerarPdfSimulado(linhasTabela);
        List<ItemImportadoPdf> itens = extractorService.extrairItens(importacaoFake, pdfBytes);

        assertThat(itens).hasSize(1);
        assertThat(itens.get(0).getCodigoFornecedor()).isEqualTo("INF-04");
        assertThat(itens.get(0).getPrecoUnitario()).isEqualByComparingTo(new BigDecimal("620.00"));
    }

    @Test
    @DisplayName("Deve rejeitar PDF nulo ou vazio lançando IllegalArgumentException")
    void deveRejeitarPdfVazioOuNulo() {
        assertThatThrownBy(() -> extractorService.extrairItens(importacaoFake, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("O arquivo binário do PDF não pode ser nulo ou vazio");

        assertThatThrownBy(() -> extractorService.extrairItens(importacaoFake, new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("O arquivo binário do PDF não pode ser nulo ou vazio");
    }
}
