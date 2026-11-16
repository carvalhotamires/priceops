package com.uniesp.priceops.controller;

import com.uniesp.priceops.dto.ComparativoPrecoProdutoResponseDto;
import com.uniesp.priceops.dto.VariacaoHistoricaProdutoDto;
import com.uniesp.priceops.service.AnalisePrecoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/analise-precos")
@CrossOrigin(origins = "*") // Permite integração direta com o frontend React
public class AnalisePrecoController {

    @Autowired
    private AnalisePrecoService analisePrecoService;

    @GetMapping("/produtos/{produtoId}/variacao")
    public ResponseEntity<?> obterVariacaoHistorica(@PathVariable Long produtoId) {
        try {
            VariacaoHistoricaProdutoDto dto = analisePrecoService.calcularVariacaoHistorica(produtoId);
            return ResponseEntity.ok(dto);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/produtos/{produtoId}/comparativo")
    public ResponseEntity<?> obterComparativoFornecedores(@PathVariable Long produtoId) {
        try {
            ComparativoPrecoProdutoResponseDto dto = analisePrecoService.compararPrecosFornecedores(produtoId);
            return ResponseEntity.ok(dto);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/comparativo-geral")
    public ResponseEntity<List<ComparativoPrecoProdutoResponseDto>> listarComparativoGeral() {
        return ResponseEntity.ok(analisePrecoService.listarComparativoGeral());
    }
}
