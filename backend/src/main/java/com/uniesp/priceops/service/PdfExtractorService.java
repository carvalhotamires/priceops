package com.uniesp.priceops.service;

import com.uniesp.priceops.model.ImportacaoPdf;
import com.uniesp.priceops.model.ItemImportadoPdf;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfExtractorService {

    // Padrão para capturar valores monetários (ex: R$ 1.250,50 ou 245,50 ou 799.00)
    private static final Pattern PADRAO_PRECO = Pattern.compile("(?:R\\$\\s*)?(\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+\\.\\d{2})");
    private static final Pattern PADRAO_INTEIRO = Pattern.compile("^\\d+$");

    public List<ItemImportadoPdf> extrairItens(ImportacaoPdf importacaoPdf, byte[] pdfBytes) {
        if (importacaoPdf == null) {
            throw new IllegalArgumentException("A importação de referência não pode ser nula.");
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("O arquivo binário do PDF não pode ser nulo ou vazio.");
        }

        List<ItemImportadoPdf> itens = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String textoExtraido = stripper.getText(document);

            if (textoExtraido == null || textoExtraido.isBlank()) {
                return itens;
            }

            String[] linhas = textoExtraido.split("\\r?\\n");
            int numeroLinhaTabela = 1;

            for (String linha : linhas) {
                String linhaLimpa = linha.trim();
                if (linhaLimpa.isBlank()) {
                    continue;
                }

                ItemImportadoPdf item = parsearLinha(linhaLimpa, importacaoPdf, numeroLinhaTabela);
                if (item != null) {
                    itens.add(item);
                    numeroLinhaTabela++;
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Falha ao processar o conteúdo binário do PDF: " + e.getMessage(), e);
        }

        return itens;
    }

    private ItemImportadoPdf parsearLinha(String linha, ImportacaoPdf importacaoPdf, int numeroLinha) {
        // Formato com separador pipe (|) ou ponto e vírgula (;)
        if (linha.contains("|") || linha.contains(";")) {
            return parsearLinhaComDelimitador(linha, importacaoPdf, numeroLinha);
        }

        return parsearLinhaTextoLivre(linha, importacaoPdf, numeroLinha);
    }

    private ItemImportadoPdf parsearLinhaComDelimitador(String linha, ImportacaoPdf importacaoPdf, int numeroLinha) {
        String delimitador = linha.contains("|") ? "\\|" : ";";
        String[] partes = linha.split(delimitador);

        if (partes.length < 2) {
            return null;
        }

        String codigo = null;
        String descricao = null;
        BigDecimal preco = null;
        Integer quantidade = null;

        for (int i = 0; i < partes.length; i++) {
            String parte = partes[i].trim();
            if (parte.isBlank()) continue;

            Matcher matcherPreco = PADRAO_PRECO.matcher(parte);
            if (matcherPreco.find() && preco == null && !ehCabecalho(parte)) {
                preco = converterParaBigDecimal(matcherPreco.group(1));
                continue;
            }

            if (PADRAO_INTEIRO.matcher(parte).matches() && quantidade == null && preco != null) {
                quantidade = Integer.parseInt(parte);
                continue;
            }

            if (codigo == null && i == 0 && !ehCabecalho(parte)) {
                codigo = parte;
                continue;
            }

            if (descricao == null && !ehCabecalho(parte)) {
                descricao = parte;
            } else if (descricao != null && !ehCabecalho(parte) && preco == null) {
                descricao += " " + parte;
            }
        }

        if (preco == null || descricao == null || descricao.isBlank()) {
            return null;
        }

        ItemImportadoPdf item = new ItemImportadoPdf(importacaoPdf, codigo, descricao, preco, quantidade, numeroLinha);
        item.setMotivoPendencia("Pendente de conferência e vinculação ao catálogo interno");
        return item;
    }

    private ItemImportadoPdf parsearLinhaTextoLivre(String linha, ImportacaoPdf importacaoPdf, int numeroLinha) {
        Matcher matcherPreco = PADRAO_PRECO.matcher(linha);
        if (!matcherPreco.find()) {
            return null; // Linha informativa, cabeçalho ou rodapé sem preço
        }

        if (ehCabecalho(linha)) {
            return null;
        }

        String valorStr = matcherPreco.group(1);
        BigDecimal preco = converterParaBigDecimal(valorStr);

        int posInicioPreco = matcherPreco.start();
        int posFimPreco = matcherPreco.end();

        String antesDoPreco = linha.substring(0, posInicioPreco).trim();
        String depoisDoPreco = linha.substring(posFimPreco).trim();

        if (antesDoPreco.isBlank()) {
            return null;
        }

        // Tenta extrair código da primeira palavra de 'antesDoPreco'
        String[] tokens = antesDoPreco.split("\\s+");
        String codigo = null;
        String descricao = antesDoPreco;

        if (tokens.length > 1 && tokens[0].matches("^[A-Za-z0-9_\\-\\.]+$") && tokens[0].length() <= 30) {
            codigo = tokens[0];
            descricao = antesDoPreco.substring(codigo.length()).trim();
        }

        Integer quantidade = null;
        if (!depoisDoPreco.isBlank()) {
            String[] tokensDepois = depoisDoPreco.split("\\s+");
            if (PADRAO_INTEIRO.matcher(tokensDepois[0]).matches()) {
                quantidade = Integer.parseInt(tokensDepois[0]);
            }
        }

        ItemImportadoPdf item = new ItemImportadoPdf(importacaoPdf, codigo, descricao, preco, quantidade, numeroLinha);
        item.setMotivoPendencia("Pendente de conferência e vinculação ao catálogo interno");
        return item;
    }

    private boolean ehCabecalho(String texto) {
        String normalizado = texto.toUpperCase();
        return normalizado.contains("TABELA DE PREÇOS")
                || normalizado.contains("TABELA DE PRECOS")
                || normalizado.contains("RELATÓRIO")
                || normalizado.contains("RELATORIO")
                || normalizado.contains("VALOR (R$)")
                || normalizado.contains("CÓDIGO")
                || normalizado.contains("CODIGO")
                || normalizado.contains("PREÇO")
                || normalizado.contains("DESCRIÇÃO")
                || normalizado.contains("DESCRICAO");
    }

    private BigDecimal converterParaBigDecimal(String valorStr) {
        String limpo = valorStr.trim();
        if (limpo.contains(",")) {
            // Formato brasileiro: 1.250,50 ou 245,50
            limpo = limpo.replace(".", "").replace(",", ".");
        }
        return new BigDecimal(limpo);
    }
}
