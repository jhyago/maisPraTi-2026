package com.clarim.api.service;

import com.clarim.api.dto.NoticiaRequest;
import com.clarim.api.dto.NoticiaResponse;
import com.clarim.api.dto.NoticiaResposta;
import com.clarim.api.dto.NoticiaResumo;
import com.clarim.api.exception.RecursoNaoEncontradoException;
import com.clarim.api.model.*;
import com.clarim.api.repository.AssinaturaRepository;
import com.clarim.api.repository.CategoriaRepository;
import com.clarim.api.repository.NoticiaRepository;
import com.clarim.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NoticiaService {

    private final NoticiaRepository noticiaRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AssinaturaRepository assinaturaRepository;

    public NoticiaService(NoticiaRepository noticiaRepository, CategoriaRepository categoriaRepository, UsuarioRepository usuarioRepository, AssinaturaRepository assinaturaRepository) {
        this.noticiaRepository = noticiaRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.assinaturaRepository = assinaturaRepository;
    }

    public List<NoticiaResumo> listarTodas() {
        return noticiaRepository.findAll()
                .stream()
                .map(this::paraDto)
                .toList();
    }

    private NoticiaResumo paraDto(Noticia noticia) {
        return new NoticiaResumo(noticia.getId(),
                noticia.getTitulo(),
                noticia.getSlug(),
                noticia.getResumo(),
                noticia.getCategoria().getNome(),
                noticia.getTexto(),
                noticia.getPremium(),
                noticia.getPublicadaEm()
        );
    }

    // PASSO 5, o "payoff" de todo o fluxo de pagamento: é aqui que a
    // assinatura paga no Stripe (passos 1 a 4, no CheckoutService e no
    // StripeSincronizacaoService) finalmente se traduz em "pode ler ou não".
    // GET /api/noticias/{id} é público (ver ConfiguracaoSeguranca) — por
    // isso `leitor` pode chegar null (visitante sem token nenhum).
    @Transactional(readOnly = true)
    public NoticiaResposta buscarPorId(Long id, Usuario leitor) {
        Noticia n = noticiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notícia não encontrada: " + id));

        // A regra inteira em uma linha: notícia aberta ao público, OU
        // (se for premium) leitor com acesso. "||" é short-circuit: se a
        // notícia não é premium, nem chamamos temAcessoPremium (não
        // precisamos consultar assinatura de ninguém para ler algo grátis).
        boolean podeLer = !n.getPremium() || temAcessoPremium(leitor);

        return new NoticiaResposta(
                n.getId(), n.getTitulo(), n.getSlug(), n.getResumo(),
                // O TEXTO SÓ SAI se puder ler. Isso é a trava de verdade:
                // o front NUNCA recebe o conteúdo pago se não tiver direito —
                // não é uma questão de "esconder na tela", o dado nem chega
                // no JSON. Repare no último parâmetro (`!podeLer` → `bloqueada`):
                // é o aviso para o React mostrar o convite para assinar
                // (ver Materia.jsx) quando o texto vier null por este motivo.
                podeLer ? n.getTexto() : null,
                n.getCategoria().getNome(), n.getUsuario().getNome(),
                n.getPremium(), n.getPublicadaEm(),
                !podeLer);
    }

    // Decide, para UM leitor, se ele pode ler conteúdo premium.
    private boolean temAcessoPremium(Usuario leitor) {
        if (leitor == null) {
            return false;                        // visitante anônimo: sem token, sem acesso
        }
        if (leitor.getPapel() != Papel.LEITOR) {
            return true;                         // editores e admin leem tudo, não precisam pagar
        }
        // A mesma regra do Assinatura.estaVigente() (status ACTIVE/TRIALING
        // e dentro do período pago), mas executada DENTRO do banco com um
        // EXISTS, em vez de carregar o histórico inteiro de assinaturas do
        // leitor para a memória e testar uma por uma em Java. Esta é a
        // consulta que só passa a dar `true` DEPOIS que o webhook do Stripe
        // gravou status/periodoFim em StripeSincronizacaoService.sincronizar().
        return assinaturaRepository.existsByUsuarioIdAndStatusInAndPeriodoFimAfter(
                leitor.getId(),
                List.of(StatusAssinatura.ACTIVE, StatusAssinatura.TRIALING),
                OffsetDateTime.now());
    }

    public NoticiaResponse criar(NoticiaRequest req) {
        Categoria categoria = categoriaRepository.findById(req.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));

        Usuario autor = usuarioRepository.findById(req.autorId()).orElseThrow();

        if(autor.getPapel() == Papel.LEITOR) {
            throw new IllegalArgumentException("Usuário não tem permissão para criar notícias");
        }

        Noticia noticia = new Noticia();
        noticia.setTitulo(req.titulo());
        noticia.setSlug(req.slug());
        noticia.setResumo(req.resumo());
        noticia.setTexto(req.texto());
        noticia.setPremium(req.premium());
        noticia.setCategoria(categoria);
        noticia.setUsuario(autor);

        Noticia salva = noticiaRepository.save(noticia);
        return new NoticiaResponse(salva.getId(), noticia.getTitulo(), noticia.getSlug(), noticia.getResumo(), noticia.getTexto(), noticia.getCategoria().getNome(), noticia.getUsuario().getNome(), noticia.getPremium(), noticia.getPublicadaEm());
    }
}
