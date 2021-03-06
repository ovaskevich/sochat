package com.sochat.shared;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Arrays;

import com.sochat.shared.Constants.MessageType;
import com.sochat.shared.io.UserIO;

public class Utils {

    /**
     * Returns whether the two sub-arrays are equal.
     * 
     * @param a1
     *            The first array
     * @param o1
     *            The offset for the first array
     * @param a2
     *            The second array
     * @param o2
     *            The offset for the second array
     * @param len
     *            How many elements to compare
     * @return
     */
    public static final boolean arrayEquals(byte[] a1, int o1, byte[] a2, int o2, int len) {
        for (int i = 0; i < len; ++i) {
            if (a1[o1 + i] != a2[o2 + i])
                return false;
        }
        return true;
    }

    /**
     * Does some preliminary detection on a packet to see if it's meant for this version of SOChat.
     * 
     * @param packet
     * @param mLogger
     * @return
     */
    public static boolean verifyPacketValid(DatagramPacket packet, UserIO mLogger) {
        int len = packet.getLength();
        byte[] buffer = packet.getData();

        // check that the length seems valid (header + message type byte)
        if (len < Constants.MESSAGE_HEADER.length + 1) {
            mLogger.logMessage("Invalid message received.");
            return false;
        }

        // check that version matches
        if (!Utils.arrayEquals(Constants.MESSAGE_HEADER, 0, buffer, 0, Constants.MESSAGE_HEADER.length)) {
            mLogger.logMessage("Packet received is not a Chat packet or is from an old version.");
            return false;
        }

        // the next byte contains the message type
        byte messageType = buffer[Constants.MESSAGE_HEADER.length];
        if (messageType < 0 || MessageType.fromId(messageType) == MessageType.UNKNOWN) {
            mLogger.logMessage("Invalid message type " + messageType);
            return false;
        }

        return true;
    }

    /**
     * Returns the header for a particular message type.
     * 
     * @param type
     * @return
     */
    public static byte[] getHeaderForMessageType(MessageType type) {
        byte[] messageHeader = new byte[Constants.MESSAGE_HEADER.length + 1];
        System.arraycopy(Constants.MESSAGE_HEADER, 0, messageHeader, 0, Constants.MESSAGE_HEADER.length);
        messageHeader[messageHeader.length - 1] = type.getId();

        return messageHeader;
    }

    private static String beforefirstspace(String str) {
        int count = 0;
        String result = "";
        for (int x = 0; x < str.length() - 1; x++) {
            if (str.charAt(x) == ' ')
                break;
            else {
                count = count + 1;
                result = result.concat(str.substring(x, x + 1));
            }
        }
        return result;
    }

    private static String afterspace(String str) {
        return str.substring(beforefirstspace(str).length() + 1, str.length());
    }

    public static String[] getMessageSplit(String message) {
        try {
            String[] result = new String[3];
            result[0] = beforefirstspace(message);
            result[1] = beforefirstspace(afterspace(message));
            result[2] = afterspace(afterspace(message));
            return result;
        } catch (Exception e) {
            // not the best code pattern, but will suffice
            return null;
        }
    }

    /**
     * Sends a UDP message.
     * 
     * @param socket
     * @param destAddr
     *            The destination address
     * @param buffer
     *            The buffer to use for sending
     * @param type
     *            The type of message
     * @param payload
     *            The byte array to send
     * @throws IOException
     * @throws SoChatException
     */
    public static void sendUdpMessage(DatagramSocket socket, SocketAddress destAddr, byte[] buffer, MessageType type,
            byte[] payload) throws IOException, SoChatException {
        // clear out buffer
        Arrays.fill(buffer, (byte) 0);

        // create header
        byte[] messageHeader = Utils.getHeaderForMessageType(type);

        if (messageHeader.length + payload.length > buffer.length)
            throw new SoChatException("Message is too long");

        // copy header and encrypted message into our buffer
        System.arraycopy(messageHeader, 0, buffer, 0, messageHeader.length);

        // copy the encrypted message
        System.arraycopy(payload, 0, buffer, messageHeader.length, payload.length);

        // compute length
        int len = messageHeader.length + payload.length;

        // send!
        socket.send(new DatagramPacket(buffer, len, destAddr));
    }

}
