package com.rogermiranda1000.watchwolf.server;

import com.rogermiranda1000.watchwolf.entities.*;
import com.rogermiranda1000.watchwolf.entities.blocks.Block;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerConnector implements Runnable, ServerStartNotifier {
    public interface ArrayAdder { public void addToArray(ArrayList<Byte> out, Object []file); }

    /**
     * The only IP allowed to talk to the socket
     */
    private final String allowedIp;

    /**
     * Socket to send server information to the ServersManager (server started/closed)
     */
    private final Socket replySocket;

    /**
     * Socket to receive <allowedIp>'s requests
     */
    private final ServerSocket serverSocket;

    /**
     * Client connected to <serverSocket>
     */
    private Socket clientSocket;

    /**
     * Key used to identify this server, while sending server status replies to the ServersManager
     */
    private final String replyKey;

    /**
     * Needed to run sync operations
     */
    private final SequentialExecutor executor;

    /**
     * Implementations of the server petitions
     */
    private final ServerPetition serverPetition;

    public ServerConnector(String allowedIp, int port, Socket reply, String key, SequentialExecutor executor, ServerPetition serverPetition) throws IOException {
        this.allowedIp = allowedIp;
        this.serverSocket = new ServerSocket(port);
        this.executor = executor;
        this.serverPetition = serverPetition;

        this.replySocket = reply;
        this.replyKey = key;

        SocketData.loadStaticBlock(BlockReader.class);
    }

    public void close() {
        try {
            this.serverSocket.close();
            if (this.clientSocket != null) this.clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* server socket */

    @Override
    public void run() {
        while (!this.serverSocket.isClosed()) {
            try {
                this.clientSocket = this.serverSocket.accept();
                System.out.println(this.clientSocket.getInetAddress().getHostAddress() + " - " + this.allowedIp); // TODO deny connection
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (this.clientSocket != null && !this.clientSocket.isClosed()) {
                try {
                    DataInputStream dis = new DataInputStream(this.clientSocket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(this.clientSocket.getOutputStream());

                    int first = dis.readUnsignedByte();
                    if ((first & 0b1111) != 0b0001) throw new UnexpectedPacketException("The packet must end with '0_001', found " + Integer.toBinaryString(first & 0b1111) + " (" + Integer.toBinaryString(first) + ")");
                    int group = ((dis.readUnsignedByte() << 4) | (first >> 4));
                    this.processGroup(group, dis, dos);
                } catch (EOFException ignore) {
                    break; // socket closed
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (UnexpectedPacketException ex) {
                    ex.printStackTrace();
                }
            }

            if (this.clientSocket != null && !this.clientSocket.isClosed()) {
                try {
                    this.clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processGroup(int group, DataInputStream dis, DataOutputStream dos) throws IOException, UnexpectedPacketException {
        switch (group) {
            case 0: // NOP
                // TODO extend timeout
                break;

            case 1:
                this.processDefaultGroup(dis, dos);
                break;

            default:
                // TODO send 'unimplemented'
                throw new UnexpectedPacketException("Unimplemented group: " + group);
        }
    }

    private void processDefaultGroup(DataInputStream dis, DataOutputStream dos) throws IOException, UnexpectedPacketException {
        // TODO implement all
        String nick;
        Position position;
        Block block;
        int operation = SocketHelper.readShort(dis);
        switch (operation) {
            case 0x0001:
                this.executor.run(() -> this.serverPetition.stopServer(null));
                break;

            case 0x0003:
                nick = SocketHelper.readString(dis);
                this.executor.run(() -> this.serverPetition.whitelistPlayer(nick));
                break;

            case 0x0004:
                nick = SocketHelper.readString(dis);
                this.executor.run(() -> this.serverPetition.opPlayer(nick));
                break;

            case 0x0005:
                position = (Position) SocketData.readSocketData(dis, Position.class);
                block = (Block) SocketData.readSocketData(dis, Block.class);
                this.executor.run(() -> this.serverPetition.setBlock(position, block));
                break;

            case 0x0006:
                position = (Position) SocketData.readSocketData(dis, Position.class);
                this.executor.run(() -> {
                    Block b = this.serverPetition.getBlock(position);
                    Message msg = new Message(dos);

                    // get block response header
                    msg.add((byte) 0b0001_1_001);
                    msg.add((byte) 0b00000000);
                    msg.add((short) 0x0006);

                    msg.add(b);

                    msg.send();
                });
                break;

            default:
                throw new UnexpectedPacketException("Operation " + (int)operation + " from group 1"); // unimplemented by this version, or error
        }
    }

    /* reply interfaces */

    @Override
    public void onServerStart() throws IOException {
        Message message = new Message(this.replySocket);

        // op player header
        message.add((byte) 0b0001_1_001);
        message.add((byte) 0b00000000);
        message.add((short) 0x0002);

        message.add(this.replyKey);

        message.send();
    }
}
