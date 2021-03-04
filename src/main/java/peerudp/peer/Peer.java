package peerudp.peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Scanner;

public class Peer {
    private DatagramSocket socket;
    // É no pacote de request que obtemos os dados de quem
    // se conectou a este par.
    private DatagramPacket request;
    // Porta que estaremos a capturar os pacotes UDP pelo servidor
    private int portListen;
    // Host que a instância deste peer vai utilizar para enviar os pacotes UDP
    private InetAddress hostTarget;
    // Porta do peer de destino
    private int portTarget;
    // É através de um scanner que vamos pegar o input para a troca de mensagens
    private Scanner scanner;
    // Quando o socket estiver fechado essa flag pode ser
    // utilizada para evitar que por engano usemos um socket fechado
    private boolean isAlive;
    // Para dar feedback ao peer que a menagem enviada chegou ao outro peer usamos
    // esta flag
    private boolean promtpCheckedMessage;
    private String username;

    public Peer(int portListen, InetAddress hostTarget, int portTarget) throws SocketException {
        socket = new DatagramSocket(portListen);
        this.portListen = socket.getLocalPort();
        this.hostTarget = hostTarget;
        this.portTarget = portTarget;
        this.username = System.getProperty("user.name");
        scanner = new Scanner(System.in);
        newRequestPacket();
    }

    /**
     * Executa um peer, que executa ao mesmo tempo duas threads, uma para o cliente
     * e outra para o servidor.
     */
    public void execute() throws IOException {
        this.runClient();
        this.runServer();
    }

    /**
     * A lógica de execução da thread para o servidor...
     */
    public void runServer() {
        Runnable server = () -> {
            // A thread fica em execução até o valor da variável mudar
            var running = true;
            while (running) {
                try {
                    // Para evitar erros com buffer "sujo", carregamos sempre um
                    // novo buffer
                    newRequestPacket();
                    // A thread sempre fica na escuta de qualquer pacote UDP que chegar
                    this.socket.receive(request);

                    // Um pacote chegando no socket pelo DatagramSocket#receive,
                    // podemos agora obter esses dados e armazenar num objeto a fim de
                    // organização, através do this#getDataRequest
                    var req = getDataRequest();
                    var data = req.getData();

                    // O servidor entende isso, imprime a mensagem e então envia um código de status
                    // para informar que recebeu a mensagem do outro peer e pode fechar a conexão.
                    if (data.contains(PeerCommand.EXIT.getValue())) {
                        var usernameSource = extractUsername(data);
                        System.out.println("\n|__ (" + req.getHostAddress() + "@" + usernameSource
                                + ") Estou fechando o peer, tchau...");
                        // O servidor faz uma tentativa de resposta para o servidor do outro peer
                        send(PeerStatus.CLOSE_PEER);

                    } else if (data.equals(PeerStatus.CLOSE_PEER.getValue())) {
                        // Se o servidor receber uma mensagem com código de status para fechar
                        // (PeerStatus.CLOSE_PEER),
                        // é isso que ele irá fazer. Imprime mensagem informando o fechamento do
                        // servidor,
                        // depois faz a configuração para o laço da thread parar e dizer que o peer pode
                        // se considerar livre, desconectando o socket e fechando o mesmo.
                        System.out.println("(peer.server, fechando)");
                        running = false;
                        this.isAlive = true;
                        socket.disconnect();
                        socket.close();

                    } else if (data.equals(PeerStatus.OK.getValue())) {
                        // O servidor recebeu um código de status (PeerStatus.OK), então apenas
                        // seta o valor do prompt para true (checked) de modo a dar o feedback para o
                        // cliente na próxima interação do prompt na thread do cliente.
                        // De fato o status OK significa neste protocolo que uma mensagem chegou
                        // ao peer de destino.
                        this.promtpCheckedMessage = true;

                    } else {
                        // O servidor recebeu uma mensagem e então imprime e faz a tentativa de enviar
                        // um código de status (PeerStatus.OK) para informar ao outro peer o
                        // recebimento
                        var usernameSource = extractUsername(data);
                        var message = data.substring(data.lastIndexOf("%") + 1);

                        System.out.println("\n|__ Mensagem recebida de " + req.getHostAddress() + "@" + usernameSource
                                + ": " + message);

                        // Tentativa de informar ao peer que enviou
                        // a mensagem que ela chegou ao destino com successo
                        send(PeerStatus.OK);
                    }
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }

            }

        };

        // Criamos uma thread e armazenamos numa variável
        var serverThread = new Thread(server);
        // Iniciamos a thread e como ela possui um laço, ela vai ficar
        // de pé até que sua atividade seja finalizada. (dahhh, é o que uma thread faz
        // ;))
        serverThread.start();
    }

    /**
     * A lógica de execução da thread para o cliente..
     * 
     */
    public void runClient() {
        Runnable client = () -> {
            // Thread em loop infinito até segunda ordem
            var running = true;
            while (running) {
                // Armazena os dados que o cliente vai enviar para o servidor
                var data = "";
                // Em cada interação da prompt, verificamos se recebemos um PeerStatus.OK
                var promptSymbol = (this.promtpCheckedMessage) ? ">> " : "> ";

                try {
                    // Enquanto for uma mensagem vazia (um enter por exemplo) faça a prompt aparecer
                    // e esperar um input
                    System.out.print(InetAddress.getLocalHost().getHostAddress() + ":" + portListen + "@"
                            + this.username + promptSymbol);
                    data = this.scanner.nextLine();
                    // Depois que enviamos a mensagem, consideramos que não recebemos um
                    // PeerStatus.OK
                    this.promtpCheckedMessage = false;
                    // Se o imput for igual ao comando /exit, então comece o processo para
                    // encerrar o peer. Comandos neste protocolo sempre são iniciados pelo
                    // /<command>
                    if (data.equals(PeerCommand.EXIT.getValue())) {

                        // Enviando uma mensagem para o outro peer que pode estar conectado.
                        // Não é garantido que está mensagem chegue já
                        send(PeerCommand.EXIT);
                        System.out.println("(x) Peer fechando...");
                        // Vamos parar o loop dentro deste runnable
                        running = false;
                        System.out.println("(peer.client, fechando)");
                        scanner.close(); // Boa prática fechar

                        // Suspendo a thread e dou um tempo de 15000mls para a mensagem chegar do outro
                        // lado e deixar o servidor isAlive = true, caso contrário vamos fechar o
                        // servidor a
                        // partir desse cliente mesmo com o this#aliveTask
                        Thread.sleep(15000);
                        autoKillThread();
                    } else if (!data.isBlank() && !containsPeerStatusCode(data, PeerStatus.values())) {
                        send(data);
                        Thread.sleep(15000);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

            }
        };

        // A thread para o cliente é instanciada
        var clientThread = new Thread(client);
        // Iniciamos a thread que sempre fica escutando pelo scanner um input do teclado
        clientThread.start();
    }

    /**
     * Obtém um objeto que representa os dados de requisição recebidos pelo socket
     * 
     * @return Os dados de requisição
     */
    private DataRequest getDataRequest() {
        return new DataRequest(this.request);
    }

    /**
     * Sempre utilizado para carregarmos um novo buffer para as requisições.
     */
    private void newRequestPacket() {
        this.request = new DatagramPacket(new byte[8192], 8192);
    }

    /**
     * Este método foi construído de modo que no futuro possamos considerar um envio
     * de diferentes tipos de objetos.
     */
    public void send(Object data) throws IOException {
        // Só inicializando o buffer...
        byte[] dataSerialized = new byte[1];
        var usernameData = "%" + this.username + "%";

        if (data instanceof String) {
            // Colocando o usuário que enviou a mensagem no corpo da própria mensagem
            var dataMessage = usernameData + data;
            dataSerialized = dataMessage.getBytes();
        } else if (data instanceof PeerStatus) {
            var dataStatus = (PeerStatus) data;
            dataSerialized = dataStatus.getValue().getBytes();
        } else if (data instanceof PeerCommand) {
            // Colocando o usuário que enviou a mensagem no corpo da própria mensagem
            var dataCommand = usernameData + data;
            dataSerialized = dataCommand.getBytes();
        }

        var dataPacket = new DatagramPacket(dataSerialized, dataSerialized.length, this.hostTarget, this.portTarget);
        this.socket.send(dataPacket);
    }

    /**
     * Verifica se dentro de determinada string temos um código de status. Este
     * método visa garantir que nenhum código de status possa seja fornecido pelo
     * cliente forçando o servidor do outro peer a executar determinadas ações não
     * desejadas.
     */
    private boolean containsPeerStatusCode(String data, PeerStatus[] peerStatus) {
        var contains = false;
        for (PeerStatus status : peerStatus) {
            if (data.equals(status.getValue())) {
                contains = true;
            }
        }

        return contains;
    }

    /**
     * Finaliza um peer caso não obtenha um código de status do outro peer
     * confirmando o fechamento. Este método tenta forçar que o peer seja fechado de
     * qualquer forma, mesmo não obtendo resposta do outro peer.
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    private void autoKillThread() throws IOException, InterruptedException {
        // Se não estiver livre, envia o código.
        // Isso só acontece caso a mensagem de fechamento não chegue ao outro peer
        if (!isAlive) {
            // Enviando um código de finalização para a thread de servidor
            // deste peer, para isso reconfiguramos o novo host e port para
            // os valores do mesmo.
            // Esta parte apenas garante que o servidor seja finalizado caso não
            // receba um código de finalização do outro peer.
            this.hostTarget = InetAddress.getLocalHost();
            this.portTarget = portListen;

            send(PeerStatus.CLOSE_PEER);
            // Vamos aguardar pelo fechamento da thread do servidor
            Thread.sleep(15000);

        }
        // peer.client fechado, peer.server fechado, done!
        System.out.println("Pronto!");
        System.exit(0);
    }

    /**
     * Obtém o nome de usuário que está junto com a mensagem.
     */
    private String extractUsername(String data) {
        return data.substring(data.indexOf("%") + 1, data.lastIndexOf("%"));

    }

}
