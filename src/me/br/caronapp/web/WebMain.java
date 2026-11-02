package me.br.caronapp.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import me.br.caronapp.Auxiliar;
import me.br.caronapp.carona.*;
import me.br.caronapp.carona.Carona.Estado;
import me.br.caronapp.carona.excecoes.*;
import me.br.caronapp.usuario.Usuario;

/** Adaptador HTTP local: formulários HTML chamam o mesmo domínio usado pelo console. */
public class WebMain implements AutoCloseable {
    private final HttpServer server;
    private final Path arquivo;
    private final Map<String, Usuario> sessoes = new HashMap<>();
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public WebMain(int porta, Path arquivo) throws IOException, ClassNotFoundException {
        this.arquivo = arquivo.toAbsolutePath();
        carregar();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        server.createContext("/", this::atender);
    }

    public void iniciar() { server.start(); }
    public int getPorta() { return server.getAddress().getPort(); }
    @Override public void close() { server.stop(0); }

    public static void main(String[] args) throws Exception {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path arquivo = Path.of(args.length > 1 ? args[1] : "web-data.bin");
        WebMain app = new WebMain(porta, arquivo);
        Runtime.getRuntime().addShutdownHook(new Thread(app::close));
        app.iniciar();
        System.out.println("CaronAPP web: http://localhost:" + app.getPorta());
        System.out.println("Dados: " + arquivo.toAbsolutePath());
    }

    private synchronized void atender(HttpExchange req) throws IOException {
        try {
            if (!req.getRequestMethod().equals("GET") && !req.getRequestMethod().equals("POST")) {
                req.getResponseHeaders().set("Allow", "GET, POST");
                responder(req, 405, pagina(null, "Método não permitido."));
                return;
            }
            String caminho = req.getRequestURI().getPath();
            Usuario usuario = sessoes.get(token(req));
            if (req.getRequestMethod().equals("GET")) {
                if (!caminho.equals("/")) {
                    responder(req, 404, pagina(usuario, "Página não encontrada."));
                    return;
                }
                atualizar();
                responder(req, 200, pagina(usuario, parametros(req.getRequestURI().getRawQuery()).get("mensagem")));
                return;
            }
            // O formulário e o cookie devem vir da mesma origem local.
            String origem = req.getRequestHeaders().getFirst("Origin");
            String host = req.getRequestHeaders().getFirst("Host");
            exigir(origem == null || origem.equals("http://" + host), "Origem inválida.");
            byte[] corpo = req.getRequestBody().readNBytes(16385);
            exigir(corpo.length <= 16384, "Formulário muito grande.");
            Map<String, String> campos = parametros(new String(corpo, StandardCharsets.UTF_8));
            String mensagem;
            switch (caminho) {
                case "/cadastro":
                    exigir(usuario == null, "Saia da conta antes de cadastrar outro usuário.");
                    String username = obrigatorio(campos, "username");
                    String nome = obrigatorio(campos, "nome");
                    String cpf = obrigatorio(campos, "cpf");
                    String senha = obrigatorio(campos, "senha");
                    usuario = Auxiliar.register(username, nome, cpf, senha);
                    exigir(usuario != null, "Usuário já cadastrado.");
                    salvar();
                    autenticar(req, usuario);
                    mensagem = "Cadastro realizado com sucesso.";
                    break;
                case "/login":
                    usuario = Auxiliar.logar(obrigatorio(campos, "username"), obrigatorio(campos, "senha"));
                    exigir(usuario != null, "Usuário ou senha inválidos.");
                    autenticar(req, usuario);
                    mensagem = "Login realizado com sucesso.";
                    break;
                case "/logout":
                    sessoes.remove(token(req));
                    req.getResponseHeaders().add("Set-Cookie", "sessao=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");
                    mensagem = "Você saiu da conta.";
                    break;
                case "/caronas/criar":
                    exigir(usuario != null, "Faça login para continuar.");
                    criar(usuario, campos);
                    salvar();
                    mensagem = "Carona criada com sucesso.";
                    break;
                case "/caronas/entrar":
                case "/caronas/sair":
                case "/caronas/partir":
                case "/caronas/finalizar":
                    exigir(usuario != null, "Faça login para continuar.");
                    atualizar();
                    Carona carona = buscar(Integer.parseInt(obrigatorio(campos, "id")));
                    exigir(carona != null, "Carona não encontrada.");
                    boolean dono = carona.getHost().equals(usuario);
                    switch (caminho) {
                        case "/caronas/entrar":
                            exigir(carona.adicionaPassageiro(usuario), "Não foi possível entrar: você já participa ou a carona não está aberta.");
                            mensagem = "Participação confirmada.";
                            break;
                        case "/caronas/sair":
                            exigir(!dono && carona.getEstado() == Estado.ABERTO, "Não é possível sair dessa carona.");
                            carona.removePassageiro(usuario);
                            mensagem = "Você saiu da carona.";
                            break;
                        case "/caronas/partir":
                            exigir(dono && carona.getEstado() == Estado.AGUARDANDO, "Somente o motorista pode partir de uma carona aguardando.");
                            carona.setEstado(Estado.FECHADO);
                            mensagem = "Partida confirmada.";
                            break;
                        default:
                            exigir(dono && carona.getEstado() == Estado.FECHADO, "Somente o motorista pode finalizar uma carona em andamento.");
                            carona.setEstado(Estado.FINALIZADO);
                            mensagem = "Carona finalizada.";
                    }
                    salvar();
                    break;
                default:
                    responder(req, 404, pagina(usuario, "Página não encontrada."));
                    return;
            }
            redirecionar(req, mensagem);
        } catch (VagasExcedidasException e) {
            redirecionar(req, "Não há vagas disponíveis nessa carona.");
        } catch (UsuarioNaoEncontradoException e) {
            redirecionar(req, "Você não participa dessa carona.");
        } catch (IllegalArgumentException | DateTimeException e) {
            redirecionar(req, e instanceof NumberFormatException || e instanceof DateTimeException
                    ? "Informe números e data válidos." : e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            responder(req, 500, pagina(null, "Não foi possível concluir a operação. Confira o console do servidor."));
        } finally {
            req.close();
        }
    }

    private static void criar(Usuario usuario, Map<String, String> c) {
        int vagas = Integer.parseInt(obrigatorio(c, "vagas"));
        exigir(vagas >= 1 && vagas <= 4, "A quantidade de passageiros deve ser de 1 a 4.");
        LocalDateTime data = LocalDateTime.parse(obrigatorio(c, "data"));
        String sentido = obrigatorio(c, "sentido");
        exigir(sentido.equals("IDA") || sentido.equals("VOLTA"), "Sentido inválido.");
        Campus campus = new Campus(PontosDeEncontro.valueOf(obrigatorio(c, "campus")));
        double latitude = Double.parseDouble(obrigatorio(c, "latitude"));
        double longitude = Double.parseDouble(obrigatorio(c, "longitude"));
        exigir(Double.isFinite(latitude) && latitude >= -90 && latitude <= 90, "Latitude inválida.");
        exigir(Double.isFinite(longitude) && longitude >= -180 && longitude <= 180, "Longitude inválida.");
        String rua = obrigatorio(c, "rua");
        int numero = Integer.parseInt(obrigatorio(c, "numero"));
        exigir(numero >= 0, "Número do endereço inválido.");
        Posicao endereco = new Posicao(latitude, longitude, rua, numero, obrigatorio(c, "cep"));
        Calendar horario = GregorianCalendar.from(data.atZone(ZoneId.systemDefault()));
        Rota rota = sentido.equals("IDA") ? new Rota(endereco, campus) : new Rota(campus, endereco);
        Auxiliar.criarCarona(new Carona(usuario, vagas, horario, rota));
    }

    private static Carona buscar(int id) {
        for (Carona c : Auxiliar.caronasAtivas) if (c.getID() == id) return c;
        return null;
    }

    private void atualizar() throws IOException {
        boolean mudou = false;
        for (Carona c : Auxiliar.caronasAtivas) {
            // O console atualiza também FECHADO, reabrindo uma corrida em andamento.
            // Aqui só consultamos o relógio enquanto a carona ainda está ABERTA.
            if (c.getEstado() == Estado.ABERTO) {
                c.atualiza();
                mudou |= c.getEstado() != Estado.ABERTO;
            }
        }
        if (mudou) salvar();
    }

    private void autenticar(HttpExchange req, Usuario usuario) {
        sessoes.remove(token(req));
        String token = UUID.randomUUID().toString();
        sessoes.put(token, usuario);
        req.getResponseHeaders().add("Set-Cookie", "sessao=" + token + "; Path=/; HttpOnly; SameSite=Strict");
    }

    private static String token(HttpExchange req) {
        String cookies = req.getRequestHeaders().getFirst("Cookie");
        if (cookies != null) for (String cookie : cookies.split(";")) {
            if (cookie.trim().startsWith("sessao=")) return cookie.trim().substring(7);
        }
        return "";
    }

    private static Map<String, String> parametros(String texto) {
        Map<String, String> mapa = new HashMap<>();
        if (texto != null && !texto.isEmpty()) for (String par : texto.split("&")) {
            String[] partes = par.split("=", 2);
            mapa.put(URLDecoder.decode(partes[0], StandardCharsets.UTF_8),
                    partes.length == 2 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "");
        }
        return mapa;
    }

    private static String obrigatorio(Map<String, String> campos, String nome) {
        String valor = campos.get(nome);
        exigir(valor != null && !valor.trim().isEmpty(), "Preencha o campo " + nome + ".");
        return nome.equals("senha") ? valor : valor.trim();
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) throw new IllegalArgumentException(mensagem);
    }

    private static String html(Object valor) {
        return String.valueOf(valor).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String campo(String id, String nome, String rotulo, String tipo, String atributos) {
        return "<label for='" + id + "'>" + rotulo + "</label><input id='" + id + "' name='" + nome
                + "' type='" + tipo + "' required " + atributos + ">";
    }

    private static String acao(Carona c, String acao, String texto) {
        return "<form method='post' action='/caronas/" + acao + "'><input type='hidden' name='id' value='"
                + c.getID() + "'><button id='" + acao + "-" + c.getID() + "'>" + texto + "</button></form>";
    }

    private static String pagina(Usuario u, String mensagem) {
        StringBuilder h = new StringBuilder("<!doctype html><html lang='pt-BR'><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'><title>CaronAPP</title><style>"
                + "*{box-sizing:border-box}body{font:16px system-ui,sans-serif;background:#f3f5f7;color:#182b3a;margin:0}"
                + "header,main{max-width:1100px;margin:auto;padding:24px}header{display:flex;gap:20px;align-items:center;justify-content:space-between}"
                + "h1{margin:0;color:#175d70}h2{font-size:1.3rem}section,article{background:white;padding:24px;border:1px solid #d6dfe5;border-radius:8px;margin-bottom:20px}"
                + ".grade{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:24px}label{display:block;margin-top:12px}"
                + "input,select,button{font:inherit;padding:10px;border:1px solid #a4b4bf;border-radius:4px}input,select{width:100%;margin:5px 0}"
                + "button{background:#175d70;color:white;cursor:pointer;margin-top:14px}button:hover{background:#104454}"
                + "nav{display:flex;gap:18px;flex-wrap:wrap;margin-bottom:24px}a{color:#175d70}"
                + "#mensagem{padding:16px;background:#fff2ce;border:1px solid #b88615;border-radius:4px;margin-bottom:24px}"
                + ".acoes{display:flex;gap:12px;flex-wrap:wrap}.vazio{color:#526775}small{display:block;color:#526775}"
                + "</style></head><body><header><div><h1>CaronAPP</h1><small>Caronas entre estudantes da UFF</small></div>");
        if (u != null) h.append("<div>Olá, <strong id='usuario-logado'>").append(html(u.getName()))
                .append("</strong><form method='post' action='/logout'><button id='logout-submit'>Sair da conta</button></form></div>");
        h.append("</header><main>");
        if (mensagem != null) h.append("<div id='mensagem' role='status'>").append(html(mensagem)).append("</div>");
        if (u == null) {
            h.append("<div class='grade'><section><h2>Entrar</h2><form id='login-form' method='post' action='/login'>")
                    .append(campo("login-username", "username", "Usuário", "text", "autocomplete='username'"))
                    .append(campo("login-senha", "senha", "Senha", "password", "autocomplete='current-password'"))
                    .append("<button id='login-submit'>Entrar</button></form></section><section><h2>Criar conta</h2>"
                            + "<form id='cadastro-form' method='post' action='/cadastro'>")
                    .append(campo("cadastro-username", "username", "Usuário", "text", "autocomplete='username'"))
                    .append(campo("cadastro-nome", "nome", "Nome completo", "text", "autocomplete='name'"))
                    .append(campo("cadastro-cpf", "cpf", "CPF", "text", ""))
                    .append(campo("cadastro-senha", "senha", "Senha", "password", "autocomplete='new-password'"))
                    .append("<button id='cadastro-submit'>Cadastrar</button></form></section></div>");
        } else {
            h.append("<nav><a href='#disponiveis'>Caronas disponíveis</a><a href='#criar'>Oferecer carona</a>"
                    + "<a href='#minhas'>Minhas corridas</a><a href='#historico'>Histórico</a></nav>");
            h.append("<section id='disponiveis'><h2>Caronas disponíveis</h2><div id='lista-caronas'>");
            listar(h, u, Auxiliar.caronasAtivas, true);
            h.append("</div></section><section id='criar'><h2>Oferecer carona</h2><form id='carona-form' method='post' action='/caronas/criar'>")
                    .append(campo("carona-vagas", "vagas", "Quantidade de passageiros (1 a 4)", "number", "min='1' max='4'"))
                    .append(campo("carona-data", "data", "Data e hora da carona", "datetime-local", ""))
                    .append("<label for='carona-sentido'>Sentido</label><select id='carona-sentido' name='sentido'><option value='IDA'>Indo para a UFF</option>"
                            + "<option value='VOLTA'>Saindo da UFF</option></select><label for='carona-campus'>Campus</label><select id='carona-campus' name='campus'>");
            for (PontosDeEncontro p : PontosDeEncontro.values()) h.append("<option value='").append(p.name()).append("'>").append(html(p.getNome())).append("</option>");
            h.append("</select><p>Endereço fora da UFF (origem na ida, destino na volta)</p>")
                    .append(campo("carona-rua", "rua", "Rua", "text", ""))
                    .append(campo("carona-numero", "numero", "Número", "number", "min='0'"))
                    .append(campo("carona-cep", "cep", "CEP", "text", ""))
                    .append(campo("carona-latitude", "latitude", "Latitude", "number", "step='any' min='-90' max='90'"))
                    .append(campo("carona-longitude", "longitude", "Longitude", "number", "step='any' min='-180' max='180'"))
                    .append("<button id='carona-submit'>Criar carona</button></form></section><section id='minhas'><h2>Minhas corridas</h2>");
            listar(h, u, u.getCaronas(), false);
            h.append("</section><section id='historico'><h2>Histórico</h2>");
            listar(h, u, u.getHistorico(), false);
            h.append("</section>");
        }
        return h.append("</main></body></html>").toString();
    }

    private static void listar(StringBuilder h, Usuario u, List<Carona> caronas, boolean disponiveis) {
        int total = 0;
        for (Carona c : caronas) {
            if (disponiveis && c.getEstado() != Estado.ABERTO) continue;
            total++;
            boolean dono = c.getHost().equals(u);
            h.append("<article data-carona-id='").append(c.getID()).append("'><h3>Carona #").append(c.getID()).append(" — ")
                    .append(html(c.toString())).append("</h3><p>Motorista: ").append(html(c.getHost().getName()))
                    .append(" · ").append(c.getRota().getSentido() == Rota.Sentido.IDA ? "Indo para a UFF" : "Saindo da UFF")
                    .append("</p><p>Horário: ").append(DATA.format(c.getDataHora().toInstant().atZone(ZoneId.systemDefault())))
                    .append(" · Estado: <span class='estado'>").append(c.getEstado()).append("</span>")
                    .append(" · Vagas: <span class='vagas'>").append(c.getMaximoGuests() - c.getVagasPreenchidas()).append("</span> de ")
                    .append(c.getMaximoGuests()).append("</p>");
            Posicao endereco = c.getRota().getSentido() == Rota.Sentido.IDA ? c.getRota().getOrigem() : c.getRota().getDestino();
            h.append("<p>Endereço: ").append(html(endereco.getNomeRua())).append(", ").append(endereco.getNumero())
                    .append(" · CEP: ").append(html(endereco.getCep())).append("</p><div class='acoes'>");
            if (disponiveis && !c.estaNaCorrida(u) && c.getVagasPreenchidas() < c.getMaximoGuests())
                h.append(acao(c, "entrar", "Entrar na carona"));
            if (!disponiveis && !dono && c.getEstado() == Estado.ABERTO) h.append(acao(c, "sair", "Sair da carona"));
            if (!disponiveis && dono && c.getEstado() == Estado.AGUARDANDO) h.append(acao(c, "partir", "Confirmar partida"));
            if (!disponiveis && dono && c.getEstado() == Estado.FECHADO) h.append(acao(c, "finalizar", "Finalizar carona"));
            h.append("</div></article>");
        }
        if (total == 0) h.append("<p class='vazio'>Nenhuma carona encontrada.</p>");
    }

    private static void redirecionar(HttpExchange req, String mensagem) throws IOException {
        req.getResponseHeaders().set("Location", "/?mensagem=" + URLEncoder.encode(mensagem, StandardCharsets.UTF_8));
        req.sendResponseHeaders(303, -1);
    }

    private static void responder(HttpExchange req, int status, String conteudo) throws IOException {
        byte[] bytes = conteudo.getBytes(StandardCharsets.UTF_8);
        req.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        req.getResponseHeaders().set("Cache-Control", "no-store");
        req.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        req.getResponseHeaders().set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
        req.sendResponseHeaders(status, bytes.length);
        req.getResponseBody().write(bytes);
    }

    private void salvar() throws IOException {
        Files.createDirectories(arquivo.getParent());
        Path temporario = Files.createTempFile(arquivo.getParent(), "caronapp-", ".tmp");
        try {
            // Um único stream preserva a identidade dos usuários nas duas listas.
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temporario))) {
                out.writeInt(Carona.getSerial());
                out.writeObject(Auxiliar.usuarios);
                out.writeObject(Auxiliar.caronasAtivas);
            }
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    @SuppressWarnings("unchecked")
    private void carregar() throws IOException, ClassNotFoundException {
        if (Files.exists(arquivo)) try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(arquivo))) {
            int serial = in.readInt();
            ArrayList<Usuario> usuarios = (ArrayList<Usuario>) in.readObject();
            ArrayList<Carona> caronas = (ArrayList<Carona>) in.readObject();
            Auxiliar.usuarios = usuarios;
            Auxiliar.caronasAtivas = caronas;
            Carona.setSerial(serial);
        }
    }
}
