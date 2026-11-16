package com.uniesp.priceops.controller;

import com.uniesp.priceops.dto.AvaliacaoPrecoRequest;
import com.uniesp.priceops.dto.AvaliacaoPrecoResponse;
import com.uniesp.priceops.model.AlertaPreco;
import com.uniesp.priceops.service.AlertaPrecoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/alertas")
@CrossOrigin(origins = "*") // Permite integração futura com o frontend React
public class AlertaPrecoController {

    @Autowired
    private AlertaPrecoService alertaPrecoService;

    @PostMapping("/avaliar-preco")
    public ResponseEntity<?> avaliarPreco(@RequestBody AvaliacaoPrecoRequest request) {
        try {
            AvaliacaoPrecoResponse resultado = alertaPrecoService.avaliarPreco(request);
            HttpStatus status = resultado.isAnomaliaDetectada() ? HttpStatus.CREATED : HttpStatus.OK;
            return ResponseEntity.status(status).body(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/avaliar-lote")
    public ResponseEntity<?> avaliarLote(@RequestBody List<AvaliacaoPrecoRequest> itens) {
        try {
            List<AvaliacaoPrecoResponse> resultados = alertaPrecoService.avaliarLote(itens);
            return ResponseEntity.ok(resultados);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<AlertaPreco>> listar(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severidade,
            @RequestParam(required = false) String tipoAlerta,
            @RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) Long fornecedorId) {
        return ResponseEntity.ok(
                alertaPrecoService.listarAlertas(status, severidade, tipoAlerta, produtoId, fornecedorId)
        );
    }

    @GetMapping("/notificacoes/pendentes")
    public ResponseEntity<Map<String, Object>> obterPainelNotificacoesPendentes() {
        return ResponseEntity.ok(alertaPrecoService.obterPainelNotificacoesPendentes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(alertaPrecoService.buscarPorId(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/visualizar")
    public ResponseEntity<?> marcarComoVisualizado(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(alertaPrecoService.marcarComoVisualizado(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String justificativa) {
        try {
            return ResponseEntity.ok(alertaPrecoService.atualizarStatus(id, status, justificativa));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id) {
        try {
            alertaPrecoService.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
