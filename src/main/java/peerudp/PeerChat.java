package peerudp;

import java.io.IOException;
import java.net.InetAddress;

import peerudp.peer.Peer;

public class PeerChat {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("parâmetros do comando: <port_listen> <ip_target> <port_target>");
        } else {
            var peer = new Peer(Integer.parseInt(args[0]), InetAddress.getByName(args[1]), Integer.parseInt(args[2]));
            peer.execute();
        }
    }
}
