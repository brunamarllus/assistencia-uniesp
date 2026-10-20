package br.edu.uniesp.assistencia.internal.ordemservico;

import br.edu.uniesp.assistencia.internal.cliente.entity.ClienteEntity;
import br.edu.uniesp.assistencia.internal.cliente.repository.ClienteRepository;
import br.edu.uniesp.assistencia.internal.equipamento.entity.EquipamentoEntity;
import br.edu.uniesp.assistencia.internal.equipamento.repository.EquipamentoRepository;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.HistoricoStatusEntity;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.OrdemServicoEntity;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.StatusOrdemServico;
import br.edu.uniesp.assistencia.internal.ordemservico.repository.OrdemServicoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AbrirOrdemServicoIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired MockMvc mockMvc;
    @Autowired ClienteRepository clienteRepository;
    @Autowired EquipamentoRepository equipamentoRepository;
    @Autowired OrdemServicoRepository ordemServicoRepository;
    @Autowired TransactionTemplate transactionTemplate;

    @Test
    void deveAbrirOrdemComSucesso() throws Exception {
        ClienteEntity cliente = criarCliente(true);
        EquipamentoEntity equipamento = criarEquipamento(cliente);

        mockMvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(cliente.getId(), equipamento.getId(), "Notebook não liga.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.numeroProtocolo").value(matchesPattern("OS-\\d{8}-[0-9A-F]{4}")))
                .andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andExpect(jsonPath("$.dataAbertura").isNotEmpty());

        transactionTemplate.executeWithoutResult(tx -> {
            OrdemServicoEntity os = ordemServicoRepository.findAll().stream()
                    .filter(o -> o.getCliente().getId().equals(cliente.getId()))
                    .findFirst().orElseThrow();
            assertThat(os.getHistorico()).hasSize(1);
            HistoricoStatusEntity h = os.getHistorico().get(0);
            assertThat(h.getNovoStatus()).isEqualTo(StatusOrdemServico.RECEBIDA);
            assertThat(h.getStatusAnterior()).isNull();
            assertThat(h.getResponsavel()).isEqualTo("SISTEMA");
            assertThat(h.getObservacao()).isNotBlank();
        });
    }

    @Test
    void deveRetornar400QuandoCamposInvalidos() throws Exception {
        mockMvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":null,\"equipamentoId\":null,\"descricaoProblema\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.clienteId").exists())
                .andExpect(jsonPath("$.erros.equipamentoId").exists())
                .andExpect(jsonPath("$.erros.descricaoProblema").exists());
    }

    @Test
    void deveRetornar404QuandoClienteInexistente() throws Exception {
        mockMvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(999_999L, 1L, "Defeito")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.recurso").value("Cliente"));
    }

    @Test
    void deveRetornar404QuandoEquipamentoInexistente() throws Exception {
        ClienteEntity cliente = criarCliente(true);

        mockMvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(cliente.getId(), 999_999L, "Defeito")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.recurso").value("Equipamento"));
    }

    @Test
    void deveRetornar422QuandoClienteInativo() throws Exception {
        ClienteEntity cliente = criarCliente(false);
        EquipamentoEntity equipamento = criarEquipamento(cliente);

        mockMvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(cliente.getId(), equipamento.getId(), "Defeito")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail")
                        .value("Não é permitido abrir Ordem de Serviço para um cliente inativo."));
    }

    @Test
    void deveRetornar422QuandoEquipamentoDeOutroCliente() throws Exception {
        ClienteEntity dono = criarCliente(true);
        ClienteEntity outro = criarCliente(true);
        EquipamentoEntity equipamento = criarEquipamento(dono);

        mockMvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(outro.getId(), equipamento.getId(), "Defeito")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail")
                        .value("O equipamento informado não pertence ao cliente indicado."));
    }

    private ClienteEntity criarCliente(boolean ativo) {
        String sufixo = UUID.randomUUID().toString().replace("-", "");
        ClienteEntity cliente = new ClienteEntity("Cliente " + sufixo.substring(0, 6),
                sufixo.substring(0, 11), sufixo + "@teste.com");
        if (!ativo) {
            cliente.inativar();
        }
        return clienteRepository.save(cliente);
    }

    private EquipamentoEntity criarEquipamento(ClienteEntity cliente) {
        EquipamentoEntity equipamento = new EquipamentoEntity("Notebook", "Dell", "Inspiron", "SN-" + UUID.randomUUID());
        equipamento.associarCliente(cliente);
        return equipamentoRepository.save(equipamento);
    }

    private String corpo(Long clienteId, Long equipamentoId, String descricao) {
        return """
                {"clienteId": %d, "equipamentoId": %d, "descricaoProblema": "%s"}
                """.formatted(clienteId, equipamentoId, descricao);
    }
}
