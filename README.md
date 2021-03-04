# peer-udp


Esta aplicação utiliza um protocolo que implementa alguns mecanismos para
garantir que haja alguma transparência na comunicação entre um peer e outro 
já que se utiliza o protocolo de transporte UDP para a comunicação.
O protocolo da aplicação tenta tornar a comunicação um pouco mais stateful
com a troca de status entre os peers.

A aplicação se utiliza da arquitetura peer-to-peer onde cada peer pode
ser considerado um cliente e servidor ao mesmo tempo, isto é, cada
peer pode compartilhar serviços sem a necessidade de um servidor central.
No caso dessa aplicação apenas a troca de mensagens simples (string) é possível.

> É importante que você utilize uma versão do JDK igual ou maior que a 11 para o funcionamento. Para o desenvolvimento e execução foi utilizado o OpenJDK.

Para que você possa utilizar a aplicação, faça o download do JAR ou baixe
este repositório, e utilizando o Maven gere o executável. Depois utilize o 
seguinte comando para executar um peer em seu host:

```bash java -jar peer-udp.jar 8080 192.168.0.11 8081```

O primeiro parâmetro, a porta `8080` é a porta onde o seu peer vai ficar
escutando e pronto para a recepção de qualquer mensagem. O segundo parâmetro 
`192.168.0.10` é o endereço IP do outro host onde um outro peer vai estar 
escutando e o terceiro parâmetro `8081` é a porta em que o outro peer vai estar
escutando. A ideia básica é que quando o peer for executado ele vai poder receber
mensagem de qualquer outro peer, mas poderá apenas enviar mensagens para o peer
configurado na hora de iniciar a sua execução.

Para que você tenha a execução entre dois peers, execute esse outro comando em um
outro host:

```bash java -jar peer-udp.jar 8081 192.168.0.10 8080```

O endereço `192.168.0.10` é do peer do primeiro comando acima. Então, agora você
tem um peer que está esperando por qualquer mensagem na porta `8081` e vai enviar 
mensagens para o outro peer na porta `8080`.

Pronto, agora você pode verificar o funcionamento. Primeiro é preciso saber
que ao enviar uma mensagem o peer vai ter um timeout (intervalo) de 15000mls
para receber uma mensagem de status indicando que o outro peer recebeu a mensagem - 
o cliente do peer ficará suspenso.
O código de retorno é o `PeerStatus.OK`. Recebendo este código a prompt do peer
é modificada para dar o feedback de sucesso do envio. A prompt que apresentava
apenas um `>` agora está `>>`. E é assim que a troca de mensagem vai se dando entre
os peers.

Para finalizar a execução/comunicação do peer você pode enviar a qualquer tempo
o comando `/exit`, assim teremos também um timeout (intervalo) de 15000mls para que
esse comando chegue ao outro peer - nesse mesmo tempo a finalização do cliente já foi executada -, motivando a impressão de uma mensagem no segundo peer indicando
que o primeiro está saindo e finalizando a sua execução. Se o segundo peer receber
o `/exit`, então ele vai retornar o status `PeerStatus.CLOSE_PEER` para indicar
para o peer que enviou: *tudo bem, recebi sua mensagem de que está finalizando, pode 
parar a sua execução...*. O primeiro peer então termina finalizando o seu servidor.
Caso, acorra qualquer erro, dentro dos 15000mls e o peer não receba o `PeerStatus.CLOSE_PEER` - pode ser que o outro peer nem esteja executando em `192.168.0.11:8081` - o próprio peer envia esse status para ele mesmo para forçar o seu encerramento e garantir a finalização.

Ao receber uma mensagem o seu modo cliente é suspenso e o seu modo
servidor ganha prioridade e fica na escuta para qualquer outra mensagem que chega 
até que você pressione `ENTER`.

Se você quiser executar todos os peers em localhost apenas utilize `localhost` como
segundo parâmetro na hora de executar a aplicação e vá correlacionando cada uma das
portas.

É importante saber que durante a comunicação, dentro de cada mensagem (datagrama) também 
colocamos automaticamente o nome de usuário que está enviando a mensagem. Então, no
pacote temos o nome de usuário e a mensagem - internamente o peer tem um mecanismo para 
extrair e separar o nome de usuário da mensagem. Quando os peers estão trocando status, apenas os status são trocados.