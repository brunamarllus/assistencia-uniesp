CREATE TABLE ordens_servico
(
    id                 BIGSERIAL   NOT NULL,
    numero_protocolo   VARCHAR(30) NOT NULL,
    cliente_id         BIGINT      NOT NULL,
    equipamento_id     BIGINT      NOT NULL,
    tecnico_id         BIGINT,
    status             VARCHAR(30) NOT NULL,
    descricao_problema TEXT        NOT NULL,
    data_abertura      TIMESTAMP   NOT NULL,
    data_finalizacao   TIMESTAMP,
    data_entrega       TIMESTAMP,

    CONSTRAINT pk_ordens_servico PRIMARY KEY (id),
    CONSTRAINT uk_ordens_servico_protocolo UNIQUE (numero_protocolo),
    CONSTRAINT fk_ordens_servico_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_ordens_servico_equipamento FOREIGN KEY (equipamento_id) REFERENCES equipamentos (id),
    CONSTRAINT fk_ordens_servico_tecnico FOREIGN KEY (tecnico_id) REFERENCES tecnicos (id)
);

CREATE TABLE historico_status
(
    id               BIGSERIAL    NOT NULL,
    ordem_servico_id BIGINT       NOT NULL,
    status_anterior  VARCHAR(30),
    novo_status      VARCHAR(30)  NOT NULL,
    data_alteracao   TIMESTAMP    NOT NULL,
    responsavel      VARCHAR(150) NOT NULL,
    observacao       VARCHAR(500),

    CONSTRAINT pk_historico_status PRIMARY KEY (id),
    CONSTRAINT fk_historico_status_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico (id)
);

CREATE TABLE diagnosticos
(
    id               BIGSERIAL NOT NULL,
    ordem_servico_id BIGINT    NOT NULL,
    tecnico_id       BIGINT    NOT NULL,
    descricao_tecnica TEXT     NOT NULL,
    solucao_proposta TEXT      NOT NULL,
    data_registro    TIMESTAMP NOT NULL,

    CONSTRAINT pk_diagnosticos PRIMARY KEY (id),
    CONSTRAINT uk_diagnosticos_os UNIQUE (ordem_servico_id),
    CONSTRAINT fk_diagnosticos_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico (id),
    CONSTRAINT fk_diagnosticos_tecnico FOREIGN KEY (tecnico_id) REFERENCES tecnicos (id)
);

CREATE TABLE orcamentos
(
    id                    BIGSERIAL      NOT NULL,
    ordem_servico_id      BIGINT         NOT NULL,
    valor_mao_de_obra     NUMERIC(10, 2) NOT NULL,
    valor_pecas           NUMERIC(10, 2) NOT NULL,
    valor_total           NUMERIC(10, 2) NOT NULL,
    data_criacao          TIMESTAMP      NOT NULL,
    data_expiracao        TIMESTAMP      NOT NULL,
    status                VARCHAR(20)    NOT NULL,
    data_resposta         TIMESTAMP,
    justificativa_cliente TEXT,

    CONSTRAINT pk_orcamentos PRIMARY KEY (id),
    CONSTRAINT uk_orcamentos_os UNIQUE (ordem_servico_id),
    CONSTRAINT fk_orcamentos_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico (id)
);

CREATE TABLE itens_orcamento
(
    id               BIGSERIAL      NOT NULL,
    orcamento_id     BIGINT         NOT NULL,
    peca_id          BIGINT         NOT NULL,
    quantidade       INTEGER        NOT NULL,
    preco_unitario   NUMERIC(10, 2) NOT NULL,
    valor_total_item NUMERIC(10, 2) NOT NULL,

    CONSTRAINT pk_itens_orcamento PRIMARY KEY (id),
    CONSTRAINT fk_itens_orcamento_orcamento FOREIGN KEY (orcamento_id) REFERENCES orcamentos (id),
    CONSTRAINT fk_itens_orcamento_peca FOREIGN KEY (peca_id) REFERENCES pecas (id)
);

CREATE TABLE os_pecas
(
    id                        BIGSERIAL      NOT NULL,
    ordem_servico_id          BIGINT         NOT NULL,
    peca_id                   BIGINT         NOT NULL,
    quantidade                INTEGER        NOT NULL,
    preco_unitario_historico  NUMERIC(10, 2) NOT NULL,
    data_registro             TIMESTAMP      NOT NULL,

    CONSTRAINT pk_os_pecas PRIMARY KEY (id),
    CONSTRAINT fk_os_pecas_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico (id),
    CONSTRAINT fk_os_pecas_peca FOREIGN KEY (peca_id) REFERENCES pecas (id)
);

CREATE TABLE anexos
(
    id                   BIGSERIAL    NOT NULL,
    ordem_servico_id     BIGINT       NOT NULL,
    nome_original        VARCHAR(255) NOT NULL,
    nome_arquivo_storage VARCHAR(255) NOT NULL,
    tipo_conteudo        VARCHAR(100) NOT NULL,
    tamanho_bytes        BIGINT       NOT NULL,
    caminho_storage      VARCHAR(500) NOT NULL,
    data_upload          TIMESTAMP    NOT NULL,

    CONSTRAINT pk_anexos PRIMARY KEY (id),
    CONSTRAINT uk_anexos_nome_storage UNIQUE (nome_arquivo_storage),
    CONSTRAINT fk_anexos_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico (id)
);
